package com.clearkeep.features.calls.presentation

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.NotificationTarget
import com.clearkeep.common.utilities.*
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.VideoCallRepository
import com.clearkeep.features.calls.R
import com.clearkeep.navigation.NavigationUtils
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_in_coming_call.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InComingCallActivity : AppCompatActivity(), View.OnClickListener {
    private val viewModel: InComingCallViewModel by viewModels()

    private var mUserNameInConversation: String? = null
    private var mAvatarInConversation: String? = null
    private lateinit var mOwnerId: String
    private lateinit var mDomain: String
    private lateinit var mGroupId: String
    private lateinit var mGroupType: String
    private var mIsGroupCall: Boolean = false
    private lateinit var mGroupName: String
    private lateinit var mToken: String
    private var mIsAudioMode: Boolean = false
    private lateinit var imgAnswer: ImageView
    private lateinit var imgEnd: ImageView
    private lateinit var imgThumb: CircleImageView
    private lateinit var tvUserName: TextView
    private var broadcastReceiver: BroadcastReceiver? = null
    private var acceptCallReceiver: BroadcastReceiver? = null
    private var ringtone: Ringtone? = null

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    @Inject
    lateinit var serverRepository: ServerRepository

    private var isInComingCall = true

    private val endCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)
            printlnCK("endCallReceiver mGroupId: $mGroupId  groupId: $groupId")
            if (mGroupId == groupId) {
                finishAndRemoveFromTask()
            }
        }
    }

    private fun registerAcceptCallReceiver() {
        acceptCallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.e("hungnv", "onCreate: cancel_call")
                finish()
            }
        }
        registerReceiver(
            acceptCallReceiver,
            IntentFilter("CANCEL_CALL")
        )
    }

    private fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    when (state) {
                        0 -> {
                            setSpeakerphoneOn(true)
                            Log.d("---", "Headset is unplugged, In Comming Call")
                        }
                        1 -> {
                            setSpeakerphoneOn(false)
                            Log.d("---", "Headset is plugged, In Comming Call")
                        }
                    }
                }
            }
        }
        registerReceiver(
            broadcastReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG)
        )
    }

        private fun setSpeakerphoneOn(isOn: Boolean) {
            printlnCK("setSpeakerphoneOn, isOn = $isOn")
            try {
                val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_IN_CALL
                audioManager.isSpeakerphoneOn = isOn
            } catch (e: Exception) {
                printlnCK("setSpeakerphoneOn, exception!! $e")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_coming_call)

        NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)

        allowOnLockScreen()
        playRingTone()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        printlnCK("mAvatarInConversation: $mAvatarInConversation")
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID).orEmpty()
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()
        mGroupType = intent.getStringExtra(EXTRA_GROUP_TYPE).orEmpty()
        mIsGroupCall = isGroup(mGroupType)
        mOwnerId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        mDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN) ?: ""
        mToken = intent.getStringExtra(EXTRA_GROUP_TOKEN).orEmpty()
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        initViews()

        registerEndCallReceiver()
        registerBroadcastReceiver()
        registerAcceptCallReceiver()
        GlobalScope.launch {
            delay(CALL_WAIT_TIME_OUT)
            if (isInComingCall) {
                if (!mIsGroupCall) {
                    cancelCallAPI(mGroupId.toInt())
                }
                finishAndRemoveFromTask()
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configLandscapeLayout()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configLandscapeLayout()
        } else {
            configPortraitLayout()
        }
    }

    private fun registerEndCallReceiver() {
        registerReceiver(
            endCallReceiver,
            IntentFilter(ACTION_CALL_CANCEL)
        )
    }

    private fun unRegisterEndCallReceiver() {
        try {
            unregisterReceiver(endCallReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unRegisterBroadcastReceiver() {
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playRingTone() {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, notification)
        ringtone?.play()
    }

    private fun allowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            this.window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun initViews() {
        imgAnswer = findViewById(R.id.imgAnswer)
        imgEnd = findViewById(R.id.imgEnd)
        imgThumb = findViewById(R.id.imgThumb)
        tvUserName = findViewById(R.id.tvUserName)
        imgAnswer.setOnClickListener(this)
        imgEnd.setOnClickListener(this)
        updateConversationInformation()
    }

    @SuppressLint("SetTextI18n")
    private fun updateConversationInformation() {
        if (!TextUtils.isEmpty(mGroupName)) {
            tvUserName.text = mGroupName
        }
        if (!TextUtils.isEmpty(mAvatarInConversation)) {
            Glide.with(this)
                .load(mAvatarInConversation)
                .placeholder(R.drawable.ic_bg_gradient)
                .error(R.drawable.ic_bg_gradient)
                .into(imgThumb)
        } else {
            tvNickName.visibility = View.VISIBLE
            val displayName =
                if (mGroupName.isNotBlank() && mGroupName.length >= 2) mGroupName.substring(
                    0,
                    1
                ) else mGroupName
            tvNickName.text = displayName
        }

        if (!mIsGroupCall) {
            Glide.with(this)
                .load(mAvatarInConversation)
                .placeholder(R.drawable.ic_bg_gradient)
                .error(R.drawable.ic_bg_gradient)
                .apply(bitmapTransform(BlurTransformation(25, 10)))
                .into(imageBackground)
        } else {
            imgThumb.visibility = View.GONE
            tvNickName.visibility = View.GONE
        }

        if (mIsAudioMode) {
            imgAnswer.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_button_answer))
        } else {
            imgAnswer.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_button_answer_video
                )
            )
        }

        if (mIsGroupCall) {
            if (mIsAudioMode) {
                txtAudioMode.text = getString(R.string.incoming_voice_group)
            } else {
                txtAudioMode.text = getString(R.string.incoming_video_group)
            }
        } else {
            if (mIsAudioMode) {
                txtAudioMode.text = getString(R.string.incoming_voice_single)

            } else {
                txtAudioMode.text = getString(R.string.incoming_video_single)
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgEnd -> {
                isInComingCall = false
                if (!mIsGroupCall) {
                    cancelCallAPI(mGroupId.toInt())
                }
                finishAndRemoveFromTask()
            }
            R.id.imgAnswer -> {
                isInComingCall = false
                acceptCallAPI(mGroupId.toInt())
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                val webRtcGroupId = intent.getStringExtra(EXTRA_WEB_RTC_GROUP_ID) ?: ""
                val webRtcUrl = intent.getStringExtra(EXTRA_WEB_RTC_URL) ?: ""
                val currentUserName = intent.getStringExtra(EXTRA_CURRENT_USERNAME) ?: ""
                val currentUserAvatar = intent.getStringExtra(EXTRA_CURRENT_USER_AVATAR) ?: ""
                finishAndRemoveFromTask()
                NavigationUtils.navigateToCall(
                    this,
                    mIsAudioMode,
                    mToken,
                    mGroupId,
                    mGroupType,
                    mGroupName,
                    mDomain,
                    mOwnerId,
                    mUserNameInConversation,
                    mAvatarInConversation,
                    true,
                    turnUrl = turnUrl,
                    turnUser = turnUserName,
                    turnPass = turnPassword,
                    webRtcGroupId = webRtcGroupId,
                    webRtcUrl = webRtcUrl,
                    stunUrl = stunUrl,
                    currentUserName = currentUserName,
                    currentUserAvatar = currentUserAvatar
                )
            }
        }
    }

    override fun onBackPressed() {
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: ""
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: ""
        val groupType = intent.getStringExtra(EXTRA_GROUP_TYPE) ?: ""
        val token = intent.getStringExtra(EXTRA_GROUP_TOKEN) ?: ""
        val isAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        val domain = intent.getStringExtra(EXTRA_OWNER_DOMAIN) ?: ""
        val ownerClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        val userName = intent.getStringExtra(EXTRA_USER_NAME)
        val avatar = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
        val turnUser = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
        val turnPass = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
        val webRtcGroupId = intent.getStringExtra(EXTRA_WEB_RTC_GROUP_ID) ?: ""
        val webRtcUrl = intent.getStringExtra(EXTRA_WEB_RTC_URL) ?: ""
        val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
        ringtone?.stop()
        inComingCall(
            this,
            isAudioMode,
            token,
            groupId,
            groupType,
            groupName,
            domain,
            ownerClientId,
            userName,
            avatar,
            turnUrl,
            turnUser,
            turnPass,
            webRtcGroupId,
            webRtcUrl,
            stunUrl
        )
        super.onBackPressed()
    }

    private fun inComingCall(
        context: Context,
        isAudioMode: Boolean,
        token: String,
        groupId: String, groupType: String, groupName: String,
        ownerDomain: String, ownerClientId: String,
        userName: String?, avatar: String?,
        turnUrl: String, turnUser: String, turnPass: String,
        webRtcGroupId: String, webRtcUrl: String,
        stunUrl: String,
        currentUserName: String = "",
        currentUserAvatar: String = ""
    ) {
        printlnCK("token = $token, groupID = $groupId, turnURL= $turnUrl, turnUser=$turnUser, turnPass= $turnPass, stunUrl = $stunUrl")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = INCOMING_CHANNEL_ID
        val channelName = INCOMING_CHANNEL_NAME
        val notificationId = INCOMING_NOTIFICATION_ID

        val dismissIntent = Intent(context, com.clearkeep.features.shared.DismissNotificationReceiver::class.java)
        dismissIntent.action = ACTION_CALL_CANCEL
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_TYPE, groupType)
        dismissIntent.putExtra(EXTRA_OWNER_DOMAIN, ownerDomain)
        dismissIntent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        val dismissPendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE)
            else
                PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_ONE_SHOT)
        val waitIntent = createIncomingCallIntent(
            context,
            isAudioMode,
            token,
            groupId,
            groupType,
            groupName,
            ownerDomain,
            ownerClientId,
            userName,
            avatar,
            turnUrl,
            turnUser,
            turnPass,
            webRtcGroupId,
            webRtcUrl,
            stunUrl,
            true,
            currentUserName,
            currentUserAvatar
        )
        val pendingWaitIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.getActivity(
                    context, groupId.toIntOrNull() ?: 0, waitIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            else
                PendingIntent.getActivity(
                    context, groupId.toIntOrNull() ?: 0, waitIntent,
                    PendingIntent.FLAG_ONE_SHOT
                )
        val inCallIntent = createIncomingCallIntent(
            context,
            isAudioMode,
            token,
            groupId,
            groupType,
            groupName,
            ownerDomain,
            ownerClientId,
            userName,
            avatar,
            turnUrl,
            turnUser,
            turnPass,
            webRtcGroupId,
            webRtcUrl,
            stunUrl,
            false,
            currentUserName,
            currentUserAvatar
        )
        val pendingInCallIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.getActivity(
                    context, groupId.toIntOrNull() ?: 0, inCallIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            else
                PendingIntent.getActivity(
                    context, groupId.toIntOrNull() ?: 0, inCallIntent,
                    PendingIntent.FLAG_ONE_SHOT
                )
        val titleCallResource = if (isGroup(groupType)) {
            if (isAudioMode) R.string.notification_incoming_group else R.string.notification_incoming_video_group
        } else {
            if (isAudioMode) R.string.notification_incoming_peer else R.string.notification_incoming_video_peer
        }
        val titleCall = context.resources.getString(titleCallResource)

        val headsUpLayout = RemoteViews(context.packageName, R.layout.notification_call)
        headsUpLayout.apply {
            setViewVisibility(R.id.imageButton, if (isGroup(groupType)) View.GONE else View.VISIBLE)
            setTextViewText(R.id.tvCallFrom, titleCall)
            setTextViewText(R.id.tvCallGroupName, groupName)
            setOnClickPendingIntent(R.id.tvDecline, dismissPendingIntent)
            setOnClickPendingIntent(R.id.tvAnswer, pendingInCallIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                channel =
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.setSound(notification, attributes)
                notificationManager.createNotificationChannel(channel)
            }
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(R.drawable.ic_logo)
            .setCustomHeadsUpContentView(headsUpLayout)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("$userName calling ${if (isAudioMode) "audio" else "video"}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingWaitIntent, true)
            .setAutoCancel(true)
            .setTimeoutAfter(60 * 1000)
            .setOngoing(true)
        val notification: Notification = builder.build()

        if (!isGroup(groupType)) {
            val target = NotificationTarget(
                context,
                R.id.imageButton,
                headsUpLayout,
                notification,
                notificationId
            )
            Glide.with(context.applicationContext)
                .asBitmap()
                .circleCrop()
                .load(avatar)
                .into(target)
        }

        notificationManager.notify(notificationId, notification)
    }

    private fun createIncomingCallIntent(
        context: Context,
        isAudioMode: Boolean,
        token: String,
        groupId: String?,
        groupType: String,
        groupName: String,
        domain: String,
        ownerClientId: String,
        userName: String?,
        avatar: String?,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        webRtcGroupId: String,
        webRtcUrl: String,
        stunUrl: String,
        isWaitingScreen: Boolean,
        currentUserName: String,
        currentUserAvatar: String
    ): Intent {
        val intent = if (isWaitingScreen) {
            printlnCK("createIncomingCallIntent")
            Intent(context, InComingCallActivity::class.java)
        } else {
            if (isGroup(groupType)) {
                Intent(context, InCallActivity::class.java)
            } else {
                Intent(context, com.clearkeep.features.calls.presentation.peertopeer.InCallPeerToPeerActivity::class.java)
            }
        }
        printlnCK("createIncomingCallIntent isAudioMode $isAudioMode")
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_NAME, groupName)
        intent.putExtra(EXTRA_GROUP_TYPE, groupType)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_IS_AUDIO_MODE, isAudioMode)
        intent.putExtra(EXTRA_OWNER_DOMAIN, domain)
        intent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, true)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_URL, turnUrl)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.putExtra(EXTRA_WEB_RTC_GROUP_ID, webRtcGroupId)
        intent.putExtra(EXTRA_WEB_RTC_URL, webRtcUrl)
        intent.putExtra(EXTRA_STUN_URL, stunUrl)
        intent.putExtra(EXTRA_CURRENT_USERNAME, currentUserName)
        intent.putExtra(EXTRA_CURRENT_USER_AVATAR, currentUserAvatar)
        return intent
    }

    private fun finishAndRemoveFromTask() {
        unRegisterEndCallReceiver()
        unRegisterBroadcastReceiver()
        ringtone?.stop()
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun cancelCallAPI(groupId: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            viewModel.cancelCall(groupId, com.clearkeep.domain.model.Owner(mDomain, mOwnerId))
            Log.e("hungnv", "cancelCallAPI: ImcomingAct" )
        }
    }

    private fun acceptCallAPI(groupId: Int) {
        GlobalScope.launch(Dispatchers.Main) {
//            async {
                viewModel.acceptCall(groupId, com.clearkeep.domain.model.Owner(mDomain, mOwnerId))
                Log.e("hungnv", "acceptCallAPI: ImcomingAct")
//            }.await()
        }

    }

    private fun configPortraitLayout() {
        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(rootViewCallIn as ConstraintLayout)
        mConstraintSet.setMargin(
            R.id.txtAudioMode,
            ConstraintSet.TOP,
            resources.getDimension(R.dimen._100sdp).toInt()
        )
        mConstraintSet.setMargin(
            R.id.tvEndButtonDescription,
            ConstraintSet.BOTTOM,
            resources.getDimension(R.dimen._60sdp).toInt()
        )
        mConstraintSet.setMargin(
            R.id.textView,
            ConstraintSet.BOTTOM,
            resources.getDimension(R.dimen._60sdp).toInt()
        )
        mConstraintSet.applyTo(rootViewCallIn as ConstraintLayout)
    }

    private fun configLandscapeLayout() {
        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(rootViewCallIn as ConstraintLayout)
        mConstraintSet.setMargin(
            R.id.txtAudioMode,
            ConstraintSet.TOP,
            resources.getDimension(R.dimen._20sdp).toInt()
        )
        mConstraintSet.setMargin(
            R.id.tvEndButtonDescription,
            ConstraintSet.BOTTOM,
            resources.getDimension(R.dimen._30sdp).toInt()
        )
        mConstraintSet.setMargin(
            R.id.textView,
            ConstraintSet.BOTTOM,
            resources.getDimension(R.dimen._30sdp).toInt()
        )
        mConstraintSet.applyTo(rootViewCallIn as ConstraintLayout)
    }

    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
    }
}
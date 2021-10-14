package com.clearkeep.screen.videojanus

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.clearkeep.screen.chat.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.common.AvatarImageTask
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_in_coming_call.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.videojanus.peer_to__peer.InCallPeerToPeerActivity
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.delay

@AndroidEntryPoint
class InComingCallActivity : AppCompatActivity(), View.OnClickListener {
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
    private val environment: Environment? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_coming_call)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)

        allowOnLockScreen()
        playRingTone()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        //todo mAvatarInConversation hardcode test
        mAvatarInConversation="https://toquoc.mediacdn.vn/2019/8/7/photo-1-1565165824290120736900.jpg"
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID)!!
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME)!!
        mGroupType = intent.getStringExtra(EXTRA_GROUP_TYPE)!!
        mIsGroupCall = isGroup(mGroupType)
        mOwnerId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        mDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN) ?: ""
        mToken = intent.getStringExtra(EXTRA_GROUP_TOKEN)!!
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        initViews()

        registerEndCallReceiver()
        GlobalScope.launch {
            delay(CALL_WAIT_TIME_OUT)
            if (isInComingCall) {
                if (!mIsGroupCall) {
                    cancelCallAPI(mGroupId.toInt())
                }
                finishAndRemoveFromTask()
            }
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
            this.window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
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
            AvatarImageTask(imgThumb).execute(mAvatarInConversation)
        } else {
            tvNickName.visibility = View.VISIBLE
            val displayName =
                if (mGroupName.isNotBlank() && mGroupName.length >= 2) mGroupName.substring(0, 1) else mGroupName
            tvNickName.text = displayName
        }

        if (!mIsGroupCall){
            Glide.with(this)
                .load(mAvatarInConversation)
                .placeholder(R.drawable.ic_bg_gradient)
                .error(R.drawable.ic_bg_gradient)
                .apply(bitmapTransform(BlurTransformation(25,10)))
                .into(imageBackground)
        }else{
            imgThumb.visibility=View.GONE
            tvNickName.visibility=View.GONE
        }

        if (mIsAudioMode){
            imgAnswer.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_button_answer))
        }else {
            imgAnswer.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_button_answer_video))
        }

        if (mIsGroupCall){
            if (mIsAudioMode){
                txtAudioMode.text=getString(R.string.incoming_voice_group)
            }else {
                txtAudioMode.text=getString(R.string.incoming_video_group)
            }
        }else {
            if (mIsAudioMode){
                txtAudioMode.text=getString(R.string.incoming_voice_single)

            }else {
                txtAudioMode.text=getString(R.string.incoming_video_single)
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
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                val webRtcGroupId = intent.getStringExtra(EXTRA_WEB_RTC_GROUP_ID) ?: ""
                val webRtcUrl = intent.getStringExtra(EXTRA_WEB_RTC_URL) ?: ""
                val currentUserName = intent.getStringExtra(EXTRA_CURRENT_USERNAME) ?: ""
                val currentUserAvatar = intent.getStringExtra(EXTRA_CURRENT_USER_AVATAR) ?: ""
                finishAndRemoveFromTask()
                AppCall.call(this, mIsAudioMode, mToken,
                    mGroupId, mGroupType, mGroupName,
                    mDomain, mOwnerId,
                    mUserNameInConversation, mAvatarInConversation, true,
                        turnUrl = turnUrl, turnUser = turnUserName, turnPass = turnPassword,
                    webRtcGroupId = webRtcGroupId, webRtcUrl = webRtcUrl,
                        stunUrl = stunUrl, currentUserName = currentUserName, currentUserAvatar = currentUserAvatar
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
        inComingCall(this, isAudioMode, token, groupId, groupType, groupName, domain, ownerClientId, userName, avatar, turnUrl, turnUser, turnPass, webRtcGroupId, webRtcUrl, stunUrl)
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = INCOMING_CHANNEL_ID
        val channelName = INCOMING_CHANNEL_NAME
        val notificationId = INCOMING_NOTIFICATION_ID

        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
        dismissIntent.action=ACTION_CALL_CANCEL
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_TYPE, groupType)
        dismissIntent.putExtra(EXTRA_OWNER_DOMAIN, ownerDomain)
        dismissIntent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        val dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_ONE_SHOT)

        val waitIntent = createIncomingCallIntent(context, isAudioMode, token, groupId, groupType, groupName,
            ownerDomain, ownerClientId, userName, avatar, turnUrl, turnUser, turnPass, webRtcGroupId, webRtcUrl, stunUrl, true, currentUserName, currentUserAvatar)
        val pendingWaitIntent = PendingIntent.getActivity(context, groupId.toIntOrNull() ?: 0, waitIntent,
            PendingIntent.FLAG_ONE_SHOT)

        val inCallIntent = createIncomingCallIntent(context, isAudioMode, token, groupId, groupType, groupName,
            ownerDomain, ownerClientId,
            userName, avatar, turnUrl, turnUser, turnPass, webRtcGroupId, webRtcUrl, stunUrl, false, currentUserName, currentUserAvatar)
        val pendingInCallIntent = PendingIntent.getActivity(context, groupId.toIntOrNull() ?: 0, inCallIntent,
            PendingIntent.FLAG_ONE_SHOT)

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
                channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.setSound(notification, attributes)
                notificationManager.createNotificationChannel(channel)
            }
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(R.drawable.ic_logo)
            .setCustomHeadsUpContentView(headsUpLayout)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("$userName calling ${if(isAudioMode) "audio" else "video"}")
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

    private fun createIncomingCallIntent(context: Context, isAudioMode: Boolean, token: String,
                                         groupId: String?, groupType: String, groupName: String,
                                         domain: String, ownerClientId: String,
                                         userName: String?, avatar: String?,
                                         turnUrl: String, turnUser: String, turnPass: String,
                                         webRtcGroupId: String, webRtcUrl: String,
                                         stunUrl: String, isWaitingScreen: Boolean, currentUserName: String, currentUserAvatar: String): Intent {
        val intent = if (isWaitingScreen) {
            printlnCK("createIncomingCallIntent")
            Intent(context, InComingCallActivity::class.java)
        } else {
            if (isGroup(groupType)) {
                Intent(context, InCallActivity::class.java)
            } else {
                Intent(context, InCallPeerToPeerActivity::class.java)
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
        ringtone?.stop()
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun cancelCallAPI(groupId: Int) {
        GlobalScope.launch {
            videoCallRepository.cancelCall(groupId, Owner(mDomain, mOwnerId))
        }
    }
    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
    }
}
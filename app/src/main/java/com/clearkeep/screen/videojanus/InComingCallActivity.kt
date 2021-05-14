package com.clearkeep.screen.videojanus

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.clearkeep.repo.VideoCallRepository
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
import com.clearkeep.R
import jp.wasabeef.glide.transformations.BlurTransformation

@AndroidEntryPoint
class InComingCallActivity : AppCompatActivity(), View.OnClickListener {
    private var mUserNameInConversation: String? = null
    private var mAvatarInConversation: String? = null
    private var mReceiverId: String? = null
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

    private var ringtone: Ringtone? = null

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    private val endCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)
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
        mReceiverId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID)
        mToken = intent.getStringExtra(EXTRA_GROUP_TOKEN)!!
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        initViews()

        registerEndCallReceiver()
    }

    private fun registerEndCallReceiver() {
        registerReceiver(
            endCallReceiver,
            IntentFilter(ACTION_CALL_CANCEL)
        )
    }

    private fun unRegisterEndCallReceiver() {
        unregisterReceiver(endCallReceiver)
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
                if (!mIsGroupCall) {
                    cancelCallAPI(mGroupId.toInt())
                }
                finishAndRemoveFromTask()
            }
            R.id.imgAnswer -> {
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                finishAndRemoveFromTask()
                AppCall.call(this, mIsAudioMode, mToken, mGroupId, mGroupType, mGroupName, mReceiverId, mUserNameInConversation, mAvatarInConversation, true,
                        turnUrl = turnUrl, turnUser = turnUserName, turnPass = turnPassword,
                        stunUrl = stunUrl
                )
            }
        }
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
            videoCallRepository.cancelCall(groupId)
        }
    }
}
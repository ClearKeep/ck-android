package com.clearkeep.screen.videojanus.peer_to__peer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.clearkeep.R
import com.clearkeep.januswrapper.JanusConnection
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.BaseActivity
import com.clearkeep.screen.videojanus.CallViewModel
import com.clearkeep.screen.videojanus.CallingStateData
import com.clearkeep.screen.videojanus.common.CallState
import com.clearkeep.screen.videojanus.common.CallStateView
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.*
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imageBackground
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imgEndWaiting
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imgThumb2
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvNickName
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvUserName2
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.waitingCallView
import kotlinx.android.synthetic.main.view_control_call_audio.view.*
import kotlinx.android.synthetic.main.view_control_call_video.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.webrtc.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class InCallPeerToPeerActivity : BaseActivity(){
    private val callScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    private val hideBottomButtonHandler: Handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    val callViewModel: CallViewModel by viewModels {
        viewModelFactory
    }

    private var mIsMuteVideo = false
    private var mIsSpeaker = false
    private var mIsAudioMode: Boolean = false
    private lateinit var mGroupId: String
    private lateinit var mGroupType: String
    private lateinit var mGroupName: String
    private var mIsGroupCall: Boolean = false

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    // surface and render
    private var endCallReceiver: BroadcastReceiver? = null

    private var switchVideoReceiver: BroadcastReceiver? = null
    // sound
    private var ringBackPlayer: MediaPlayer? = null
    private var busySignalPlayer: MediaPlayer? = null
    var isFromComingCall: Boolean = false
    var avatarInConversation = ""
    var groupName = ""


    @SuppressLint("ResourceType", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call_peer_to_peer)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID)!!
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME)!!
        mGroupType = intent.getStringExtra(EXTRA_GROUP_TYPE)!!
        mIsGroupCall = isGroup(mGroupType)
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        callViewModel.mIsAudioMode.postValue(mIsAudioMode)

        initViews()

        registerEndCallReceiver()
        if (mIsAudioMode) {
            configMedia(isSpeaker = false, isMuteVideo = true)
            registerSwitchVideoReceiver()
        } else {
            configMedia(isSpeaker = true, isMuteVideo = false)
        }

        requestCallPermissions()

        callViewModel.mCurrentCallState.observe(this, {
            updateCallStatus(it)
        })
        onClickControlCall()
    }

    private fun configMedia(isSpeaker: Boolean, isMuteVideo: Boolean) {
        mIsSpeaker = isSpeaker
        mIsMuteVideo = isMuteVideo
        controlCallAudioView.apply {
            toggleSpeaker?.isChecked=mIsSpeaker
        }
        controlCallVideoView.apply {
            bottomToggleFaceTime?.isChecked=!mIsMuteVideo
        }
        callViewModel.onFaceTimeChange(!isMuteVideo)
    }

    private fun initViews() {
        remoteRender.apply {
            init(callViewModel.rootEglBase.eglBaseContext, null)
            setEnableHardwareScaler(true)
        }
        localRender.apply {
            init(callViewModel.rootEglBase.eglBaseContext, null)
            setZOrderMediaOverlay(true)
            setEnableHardwareScaler(true)
        }

        val userNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        listenerCallingState.postValue(CallingStateData(true, userNameInConversation))
        avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION) ?: ""
        //todo avatarInConversation hardcode test
        avatarInConversation =
            "https://toquoc.mediacdn.vn/2019/8/7/photo-1-1565165824290120736900.jpg"

        callViewModel.listenerOnRemoteRenderAdd = listenerOnRemoteRenderAdd
        initWaitingCallView()
    }

    override fun onPermissionsAvailable() {
        isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)!!.toInt()
        callScope.launch {
            if (isFromComingCall) {
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                val token = intent.getStringExtra(EXTRA_GROUP_TOKEN) ?: ""
                startVideo(groupId, stunUrl, turnUrl, turnUserName, turnPassword, token)
            } else {
                val result = videoCallRepository.requestVideoCall(groupId, mIsAudioMode)
                if (result != null) {
                    val turnConfig = result.turnServer
                    val stunConfig = result.stunServer
                    val turnUrl = turnConfig.server
                    val stunUrl = stunConfig.server
                    val token = result.groupRtcToken
                    startVideo(groupId, stunUrl, turnUrl, turnConfig.user, turnConfig.pwd, token)
                } else {
                    runOnUiThread {

                        (CallState.CALL_NOT_READY)
                    }
                    return@launch
                }
            }
            if (!mIsGroupCall) {
                delay(CALL_WAIT_TIME_OUT)
                if (callViewModel.mCurrentCallState.value != CallState.ANSWERED) {
                    runOnUiThread {
                        updateCallStatus(CallState.CALL_NOT_READY)
                    }
                }
            }

        }

    }

    override fun onPermissionsDenied() {
        finishAndReleaseResource()
    }

    override fun onPermissionsForeverDenied() {
        showAskPermissionDialog()
    }

    override fun onDestroy() {
        listenerCallingState.postValue(CallingStateData(false))
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (isInPictureInPictureMode) {
            controlCallVideoView.visibility=View.GONE
            localRender.visibility=View.GONE
        } else {
            controlCallVideoView.visibility=View.VISIBLE
            localRender.visibility=View.VISIBLE

        }
    }

     private fun onClickControlCall() {
         controlCallAudioView.apply {
             this.toggleMute.setOnClickListener {
                 callViewModel.onMuteChange(callViewModel.mIsMute.value != true)
                 callViewModel.mIsMute.postValue(callViewModel.mIsMute.value!= true)
             }
             this.toggleFaceTime.setOnClickListener {
                 callViewModel.onFaceTimeChange((it as ToggleButton).isChecked)
                 callViewModel.mIsAudioMode.postValue(it.isChecked)
                 mIsMuteVideo = !mIsMuteVideo
                 switchToVideoMode()
                 controlCallVideoView.bottomToggleFaceTime.isChecked = true
             }
             this.toggleSpeaker.setOnClickListener {
                 callViewModel.onSpeakChange((it as ToggleButton).isChecked)
             }
         }
         controlCallVideoView.apply {
             this.bottomToggleMute.setOnClickListener {
                 callViewModel.onMuteChange(callViewModel.mIsMute.value != true)
                 callViewModel.mIsMute.postValue(callViewModel.mIsMute.value!= true)
             }
             this.bottomToggleFaceTime.setOnClickListener {
                 callViewModel.onFaceTimeChange(mIsMuteVideo)
                 mIsMuteVideo = !mIsMuteVideo
                 switchToVideoMode()
             }
             this.bottomToggleSwitchCamera.setOnClickListener {
                 callViewModel.onCameraChane((it as ToggleButton).isChecked)
             }
             this.bottomImgEndCall.setOnClickListener {
                 hangup()
                 finishAndReleaseResource()
             }
         }

         imgEndWaiting.setOnClickListener {
             hangup()
             finishAndReleaseResource()
         }
         callViewModel.mIsAudioMode.observe(this,{
             if (it==false && mIsAudioMode){
                 updateUIbyStateView(CallStateView.CALLED_VIDEO)

             }
         })

         callViewModel.mIsMute.observe(this,{
             controlCallVideoView.bottomToggleMute.isChecked=it
             controlCallAudioView.toggleMute.isChecked=it
         })

         imgWaitingBack.setOnClickListener {
             if (hasSupportPIP()) {
                 enterPIPMode()
             } else {
                 hangup()
                 finishAndReleaseResource()
             }
         }
         imgVideoCallBack.setOnClickListener {
             if (hasSupportPIP()) {
                 enterPIPMode()
             } else {
                 hangup()
                 finishAndReleaseResource()
             }
         }

     }

    private fun initWaitingCallView() {
        tvUserName2.text = mGroupName
        tvUserName.text=mGroupName
        tvNickName.visibility = View.VISIBLE
        val displayName =
            if (mGroupName.isNotBlank() && mGroupName.length >= 2) mGroupName.substring(0, 1) else mGroupName
        tvNickName.text = displayName

        if (!mIsGroupCall) {
            Glide.with(this)
                .load(avatarInConversation)
                .placeholder(R.drawable.ic_bg_gradient)
                .error(R.drawable.ic_bg_gradient)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
                .into(imageBackground)

            Glide.with(this)
                .load(avatarInConversation)
                .placeholder(R.drawable.ic_bg_gradient)
                .error(R.drawable.ic_bg_gradient)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvNickName.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvNickName.visibility = View.GONE
                        return false
                    }

                })
                .into(imgThumb2)
        } else {
            tvStateCall.text = "Calling Group"
            imgThumb2.visibility = View.GONE
            tvNickName.visibility = View.GONE
        }
    }

    private fun startVideo(
        groupId: Int,
        stunUrl: String,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        token: String
    ) {
        val ourClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID) ?: ""
        callViewModel.startVideo(context = this,localRender,ourClientId, groupId, stunUrl, turnUrl, turnUser, turnPass, token)
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        when ( callViewModel.mCurrentCallState.value) {
            CallState.CALLING -> {
                playRingBackTone()
                if (isFromComingCall) {
                    if (mIsAudioMode)
                        updateUIbyStateView(CallStateView.CALLED_AUDIO)
                    else updateUIbyStateView(CallStateView.CALLED_VIDEO)
                } else {
                    if (mIsAudioMode)
                        updateUIbyStateView(CallStateView.CALL_AUDIO_WAITING)
                    else updateUIbyStateView(CallStateView.CALL_VIDEO_WAITING)
                }
            }

            CallState.RINGING -> {
                //tvCallState.text = getString(R.string.text_ringing)
            }
            CallState.BUSY, CallState.CALL_NOT_READY -> {
                //tvCallState.text = getString(R.string.text_busy)
                stopRingBackTone()
                playBusySignalSound()
                GlobalScope.launch {
                    delay(3 * 1000)
                    hangup()
                    finishAndReleaseResource()
                }
            }
            CallState.ENDED -> {
                hangup()
                finishAndReleaseResource()
            }
            CallState.ANSWERED -> {
                stopRingBackTone()
                if (callViewModel.mIsAudioMode.value==true) {
                    updateUIbyStateView(CallStateView.CALLED_AUDIO)
                } else {
                    updateUIbyStateView(CallStateView.CALLED_VIDEO)
                }
            }
        }
    }

    private fun updateUIbyStateView(callStateView: CallStateView){
        when(callStateView){
            CallStateView.CALL_AUDIO_WAITING ->{
                waitingCallView.visibility=View.VISIBLE
                viewAudioCalled.visibility=View.GONE
                viewVideoCalled.visibility=View.GONE
            }

            CallStateView.CALL_VIDEO_WAITING->{
                waitingCallView.visibility = View.GONE
                setLocalViewFullScreen()
                tvStateVideoCall.visibility=View.VISIBLE
                tvUserNameVideoCall.visibility=View.VISIBLE
                tvUserNameVideoCall.text=mGroupName
                viewVideoCalled.visibility=View.GONE


            }
            CallStateView.CALLED_AUDIO ->{
                waitingCallView.visibility = View.VISIBLE
                tvStateVideoCall.visibility=View.GONE
                tvStateCall.visibility=View.GONE
                viewAudioCalled.visibility=View.VISIBLE

            }
            CallStateView.CALLED_VIDEO->{
                setLocalFixScreen()
                waitingCallView.visibility=View.GONE
                tvStateVideoCall.visibility=View.GONE
                tvUserNameVideoCall.visibility=View.GONE
                viewVideoCalled.visibility=View.VISIBLE
            }

        }
    }

    private fun hangup() {
        callScope.cancel()
    }

    private fun switchToVideoMode() {
        callViewModel.mIsAudioMode.postValue(false)
        callScope.launch {
            videoCallRepository.switchAudioToVideoCall(mGroupId.toInt())
        }
    }

    private fun finishAndReleaseResource() {
        hideBottomButtonHandler.removeCallbacksAndMessages(null)
        unRegisterEndCallReceiver()
        unRegisterSwitchVideoReceiver()
        stopRingBackTone()
        stopBusySignalSound()
        if (!mIsGroupCall) {
            cancelCallAPI()
        }
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun cancelCallAPI() {
        GlobalScope.launch {
            videoCallRepository.cancelCall(mGroupId.toInt())
        }
    }

    private var listenerOnRemoteRenderAdd = fun(connection: JanusConnection) {
        connection.videoTrack.addRenderer(VideoRenderer(remoteRender))
        startTimeInterval()
    }

    private fun showAskPermissionDialog() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Permissions Required")
            .setMessage(
                "You have forcefully denied some of the required permissions " +
                        "for this action. Please open settings, go to permissions and allow them."
            )
            .setPositiveButton("Settings") { _, _ ->
                openSettingScreen()
            }
            .setNegativeButton("Cancel") { _, _ ->
                finishAndReleaseResource()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun registerEndCallReceiver() {
        endCallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)
                printlnCK("receive a end call event with group id = $groupId")
                if (mGroupId == groupId) {
                    hangup()
                    finishAndReleaseResource()
                }
            }
        }
        registerReceiver(
            endCallReceiver,
            IntentFilter(ACTION_CALL_CANCEL)
        )
    }

    private fun unRegisterEndCallReceiver() {
        if (endCallReceiver != null) {
            unregisterReceiver(endCallReceiver)
            endCallReceiver = null
        }
    }

    private fun registerSwitchVideoReceiver() {
        printlnCK("registerSwitchVideoReceiver")
        switchVideoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getLongExtra(EXTRA_CALL_SWITCH_VIDEO, 0).toString()
                if (mGroupId == groupId && mIsAudioMode) {
                    printlnCK("switch group $groupId to video mode")
                    callViewModel.mIsAudioMode.postValue(false)
                    configMedia(isSpeaker = true, isMuteVideo = false)
                }
            }
        }
        registerReceiver(
            switchVideoReceiver,
            IntentFilter(ACTION_CALL_SWITCH_VIDEO)
        )
    }

    private fun unRegisterSwitchVideoReceiver() {
        if (switchVideoReceiver != null) {
            unregisterReceiver(switchVideoReceiver)
            switchVideoReceiver = null
        }
    }

    private fun playRingBackTone() {
        ringBackPlayer = MediaPlayer.create(this, R.raw.sound_ringback_tone)
        ringBackPlayer?.isLooping = true
        ringBackPlayer?.start()
    }

    private fun stopRingBackTone() {
        ringBackPlayer?.stop()
        ringBackPlayer?.release()
        ringBackPlayer = null
    }

    private fun playBusySignalSound() {
        busySignalPlayer = MediaPlayer.create(this, R.raw.sound_busy_signal)
        busySignalPlayer?.start()
    }

    private fun stopBusySignalSound() {
        busySignalPlayer?.stop()
        busySignalPlayer?.release()
        busySignalPlayer = null
    }

    private fun startTimeInterval() {
        callViewModel.totalTimeRunJob = startCoroutineTimer(delayMillis = 0, repeatMillis = 1000) {
            callViewModel.totalTimeRun+=1
            runOnUiThread {
                tvTimeCall.text= convertSecondsToHMmSs(callViewModel.totalTimeRun)
                tvVideoTimeCall.text=convertSecondsToHMmSs(callViewModel.totalTimeRun)
            }
        }
    }

    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
        var listenerCallingState = MutableLiveData<CallingStateData>()
    }


    private fun setLocalViewFullScreen(){
        localRender.layoutParams = fullView(localRender.layoutParams as ConstraintLayout.LayoutParams)
    }
    private fun setLocalFixScreen(){
        localRender.layoutParams = fixViewLocalView(localRender.layoutParams as ConstraintLayout.LayoutParams)
    }

    private fun fullView(localView: ConstraintLayout.LayoutParams): ConstraintLayout.LayoutParams {
        localView.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = 0
            leftToLeft = containerCall.id
            rightToRight = containerCall.id
            marginEnd = 0
            marginStart = 0
            topMargin = 0
            bottomMargin=0
        }
        return localView
    }

    private fun fixViewLocalView(paramsRemote: ConstraintLayout.LayoutParams): ConstraintLayout.LayoutParams {
        paramsRemote.apply {
            width = dp2px(120f)
            height = dp2px(120 * 4 / 3.toFloat())
            topToTop = ConstraintLayout.LayoutParams.UNSET
            startToStart = ConstraintLayout.LayoutParams.UNSET
            endToEnd = containerCall.id
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            marginEnd = dp2px(16f)
            bottomMargin = dp2px(48f)
        }
        return paramsRemote
    }

    private fun startCoroutineTimer(delayMillis: Long = 0, repeatMillis: Long = 0, action: () -> Unit) = GlobalScope.launch {
        delay(delayMillis)
        if (repeatMillis > 0) {
            while (true) {
                action()
                delay(repeatMillis)
            }
        } else {
            action()
        }
    }
}

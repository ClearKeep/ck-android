package com.clearkeep.screen.videojanus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.TypedArray
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import com.clearkeep.R
import com.clearkeep.databinding.ActivityInCallBinding
import com.clearkeep.januswrapper.JanusConnection
import com.clearkeep.januswrapper.JanusRTCInterface
import com.clearkeep.januswrapper.PeerConnectionClient
import com.clearkeep.januswrapper.WebSocketChannel
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.common.CallState
import com.clearkeep.screen.videojanus.common.createVideoCapture
import com.clearkeep.screen.videojanus.surface_generator.SurfacePositionFactory
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_in_call.*
import kotlinx.android.synthetic.main.activity_in_call.controlCallAudioView
import kotlinx.android.synthetic.main.activity_in_call.controlCallVideoView
import kotlinx.android.synthetic.main.activity_in_call.imgEndWaiting
import kotlinx.android.synthetic.main.activity_in_call.tvUserName2
import kotlinx.android.synthetic.main.activity_in_call.waitingCallView
import kotlinx.android.synthetic.main.toolbar_call_default.*
import kotlinx.android.synthetic.main.toolbar_call_default.view.*
import kotlinx.android.synthetic.main.view_control_call_audio.view.*
import kotlinx.android.synthetic.main.view_control_call_video.view.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import kotlin.math.log2

@AndroidEntryPoint
class InCallActivity : BaseActivity(), JanusRTCInterface,
    PeerConnectionClient.PeerConnectionEvents {
    private val callScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    private val hideBottomButtonHandler: Handler = Handler(Looper.getMainLooper())

    private var mIsMute = false
    private var mIsMuteVideo = false
    private var mIsSpeaker = false

    private var mCurrentCallState: CallState = CallState.CALLING

    private var mIsAudioMode: Boolean = false
    private lateinit var mGroupId: String
    private lateinit var mGroupType: String
    private lateinit var mGroupName: String
    private var mIsGroupCall: Boolean = false
    private lateinit var binding: ActivityInCallBinding

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    // surface and render
    private lateinit var rootEglBase: EglBase
    private var peerConnectionClient: PeerConnectionClient? = null
    private var mWebSocketChannel: WebSocketChannel? = null
    private lateinit var mLocalSurfaceRenderer: SurfaceViewRenderer

    private val remoteRenders: MutableMap<BigInteger, SurfaceViewRenderer> = HashMap()

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
        binding = ActivityInCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID)!!
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME)!!
        mGroupType = intent.getStringExtra(EXTRA_GROUP_TYPE)!!
        mIsGroupCall = isGroup(mGroupType)
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        rootEglBase = EglBase.create()

        mLocalSurfaceRenderer = SurfaceViewRenderer(this)
        mLocalSurfaceRenderer.apply {
            init(rootEglBase.eglBaseContext, null)
            setZOrderMediaOverlay(true)
            setEnableHardwareScaler(true)
        }

        initViews()

        registerEndCallReceiver()
        if (mIsAudioMode) {
            configMedia(isSpeaker = false, isMuteVideo = true)
            registerSwitchVideoReceiver()
        } else {
            configMedia(isSpeaker = true, isMuteVideo = false)
        }

        requestCallPermissions()
        updateCallStatus(mCurrentCallState)
    }

    private fun configMedia(isSpeaker: Boolean, isMuteVideo: Boolean) {
        mIsSpeaker = isSpeaker
        enableSpeaker(mIsSpeaker)

        mIsMuteVideo = isMuteVideo
        enableMuteVideo(mIsMuteVideo)

    }

    private fun initViews() {
        onAction()
        updateUIByStateAndMode()
        val userNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        listenerCallingState.postValue(CallingStateData(true, userNameInConversation))
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME)
        avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION) ?: ""
        //todo avatarInConversation hardcode test
        avatarInConversation =
            "https://toquoc.mediacdn.vn/2019/8/7/photo-1-1565165824290120736900.jpg"
        tvCallStateVideo.text = "Calling"
        tvUserName.text = groupName
        tvCallState.text = "Calling"

        if (!TextUtils.isEmpty(groupName)) {
            includeToolbar.title.text = groupName
        }

        runDelayToHideBottomButton()
        initWaitingCallView()
        updateRenders()
    }

    private fun runDelayToHideBottomButton() {
        hideBottomButtonHandler.removeCallbacksAndMessages(null)
        hideBottomButtonHandler.postDelayed(Runnable {
            if (!mIsAudioMode && mCurrentCallState == CallState.ANSWERED) {
                includeToolbar.gone()
                controlCallVideoView.gone()
            }
        }, 5 * 1000)
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
                        updateCallStatus(CallState.CALL_NOT_READY)
                    }
                    return@launch
                }
            }
            if (!mIsGroupCall) {
                delay(CALL_WAIT_TIME_OUT)
                if (remoteRenders.isEmpty()) {
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
        if (chronometerTimeCall.visibility == View.VISIBLE) {
            chronometerTimeCall.stop()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            includeToolbar.gone()
            controlCallVideoView.gone()
        } else {
            // Restore the full-screen UI.
            includeToolbar.visible()
            if (!mIsAudioMode)
                controlCallVideoView.visible()
        }
    }

    fun onAction() {
        imgEndWaiting.setOnClickListener {
            hangup()
            finishAndReleaseResource()
        }
        includeToolbar.imgBack.setOnClickListener {
            if (hasSupportPIP()) {
                enterPIPMode()
            } else {
                hangup()
                finishAndReleaseResource()
            }
        }

        controlCallAudioView.toggleSpeaker.setOnClickListener {
            mIsSpeaker = !mIsSpeaker
            enableSpeaker(mIsSpeaker)
        }

        controlCallAudioView.toggleMute.setOnClickListener {
            mIsMute = !mIsMute
            enableMute(mIsMute)
            controlCallVideoView.bottomToggleMute.isChecked = mIsMute
            runDelayToHideBottomButton()
        }

        controlCallAudioView.toggleFaceTime.setOnClickListener {
            switchToVideoMode()
        }

        controlCallVideoView.bottomToggleMute.setOnClickListener {
            mIsMute = !mIsMute
            enableMute(mIsMute)
            controlCallAudioView.toggleMute.isChecked = mIsMute
            runDelayToHideBottomButton()
        }

        controlCallVideoView.bottomToggleFaceTime.setOnClickListener {
            mIsMuteVideo = !mIsMuteVideo
            enableMuteVideo(mIsMuteVideo)
            runDelayToHideBottomButton()

        }

        controlCallVideoView.bottomToggleSwitchCamera.setOnClickListener {
            peerConnectionClient?.switchCamera()
        }

        controlCallVideoView.bottomImgEndCall.setOnClickListener {
            hangup()
            finishAndReleaseResource()

        }
        surfaceRootContainer.setOnClickListener {
            if (!mIsAudioMode && mCurrentCallState == CallState.ANSWERED)
                if (includeToolbar.visibility == View.VISIBLE) {
                    includeToolbar.gone()
                    controlCallVideoView.gone()
                } else {
                    includeToolbar.visible()
                    controlCallVideoView.visible()
                    runDelayToHideBottomButton()
                }
        }

        imgEndWaiting.setOnClickListener {
            hangup()
            finishAndReleaseResource()
        }
    }

    private fun initWaitingCallView() {
        tvUserName2.text = mGroupName
        if (mIsAudioMode)
            controlCallVideoView.gone()
        else
            controlCallVideoView.visible()
        includeToolbar.gone()
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
        printlnCK("Janus URL: $JANUS_URI")
        printlnCK(
            "startVideo: stun = $stunUrl, turn = $turnUrl, username = $turnUser, pwd = $turnPass" +
                    ", group = $groupId, token = $token"
        )
        mWebSocketChannel = WebSocketChannel(groupId, ourClientId, token, JANUS_URI)
        mWebSocketChannel!!.initConnection()
        mWebSocketChannel!!.setDelegate(this)
        val peerConnectionParameters = PeerConnectionClient.PeerConnectionParameters(
            false, 360, 480, 20, "VP8",
            true, 0, "opus", false,
            false, false, false, false,
            turnUrl, turnUser, turnPass, stunUrl
        )
        peerConnectionClient = PeerConnectionClient()
        peerConnectionClient!!.createPeerConnectionFactory(this, peerConnectionParameters, this)
        peerConnectionClient!!.startVideoSource()
        enableMuteVideo(mIsMuteVideo)
    }

    private fun displayCountUpClockOfConversation() {
        includeToolbar.chronometerTimeCall.apply {
            base = SystemClock.elapsedRealtime()
            start()
        }
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        mCurrentCallState = newState

        when (mCurrentCallState) {
            CallState.CALLING -> {
                playRingBackTone()
            }
            CallState.RINGING -> {
            }
            CallState.BUSY, CallState.CALL_NOT_READY -> {
                tvCallState.text = getString(R.string.text_busy)
                stopRingBackTone()
                playBusySignalSound()
                GlobalScope.launch {
                    delay(3 * 1000)
                    hangup()
                    finishAndReleaseResource()
                }
            }

            CallState.ENDED -> {
                tvCallState.text = getString(R.string.text_end)
                hangup()
                finishAndReleaseResource()
            }
            CallState.ANSWERED -> {
                tvCallState.text = getString(R.string.text_started)
                stopRingBackTone()
                displayCountUpClockOfConversation()
                updateUIByStateAndMode()
            }
        }
    }

    private fun switchToVideoMode() {
        mIsAudioMode = false
        configMedia(isSpeaker = true, isMuteVideo = false)
        updateUIByStateAndMode()
        callScope.launch {
            videoCallRepository.switchAudioToVideoCall(mGroupId.toInt())
        }
    }

    private fun updateUIByStateAndMode() {
        if (mCurrentCallState == CallState.ANSWERED) {
            groupAudioWaiting.gone()
            waitingCallVideoView.gone()

            if (mIsAudioMode) {
                includeToolbar.visible()
                controlCallVideoView.gone()
                tvEndButtonDescription.text="End Call"
            } else {
                waitingCallView.gone()
                includeToolbar.visible()
                controlCallVideoView.visible()
            }
        } else {
            if (!mIsAudioMode) {
                waitingCallView.gone()
                includeToolbar.gone()
                controlCallVideoView.visible()
                waitingCallVideoView.visible()
            } else {
                waitingCallView.visible()
                groupAudioWaiting.visible()
                waitingCallVideoView.gone()
            }

        }
    }

    private fun enableSpeaker(isEnable: Boolean) {
        controlCallAudioView.toggleSpeaker.isChecked = isEnable
        setSpeakerphoneOn(isEnable)
    }

    private fun enableMute(isMuting: Boolean) {
        Log.e("antx","enableMute isMuting $isMuting")
        controlCallAudioView.toggleMute.isChecked = isMuting
        controlCallVideoView.bottomToggleMute.isChecked = isMuting
        peerConnectionClient?.setAudioEnabled(!isMuting)
    }

    private fun enableMuteVideo(isMuteVideo: Boolean) {
        Log.e("antx","enableMute enableMuteVideo $isMuteVideo")

        controlCallAudioView.toggleFaceTime.isChecked = isMuteVideo
        controlCallVideoView.bottomToggleFaceTime.isChecked = isMuteVideo
        peerConnectionClient?.setLocalVideoEnable(!isMuteVideo)
    }

    private fun setSpeakerphoneOn(isOn: Boolean) {
        printlnCK("setSpeakerphoneOn, isOn = $isOn")
        try {
            val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = isOn
        } catch (e: Exception) {
            printlnCK("setSpeakerphoneOn, $e")
        }
    }

    private fun hangup() {
        callScope.cancel()
        mWebSocketChannel?.close()
        peerConnectionClient?.close()
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

    private fun offerPeerConnection(handleId: BigInteger) {
        peerConnectionClient?.createPeerConnection(
            rootEglBase.eglBaseContext,
            mLocalSurfaceRenderer,
            createVideoCapture(this),
            handleId
        )

        peerConnectionClient?.createOffer(handleId)
    }

    // interface JanusRTCInterface
    override fun onPublisherJoined(handleId: BigInteger) {
        offerPeerConnection(handleId)
    }

    override fun onPublisherRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.setRemoteDescription(handleId, sessionDescription)
    }

    override fun subscriberHandleRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.subscriberHandleRemoteJsep(handleId, sessionDescription)
    }

    override fun onLeaving(handleId: BigInteger) {
        printlnCK("onLeaving: $handleId")
        removeRemoteRender(handleId)
    }

    override fun onLocalDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.publisherCreateOffer(handleId, sdp)
    }

    override fun onRemoteDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.subscriberCreateAnswer(handleId, sdp)
    }

    override fun onIceCandidate(candidate: IceCandidate?, handleId: BigInteger) {
        if (candidate != null) {
            mWebSocketChannel?.trickleCandidate(handleId, candidate)
        } else {
            mWebSocketChannel?.trickleCandidateComplete(handleId)
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}

    override fun onIceConnected() {
    }

    override fun onIceDisconnected() {
    }

    override fun onPeerConnectionClosed() {
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {}

    override fun onPeerConnectionError(description: String) {
        printlnCK("onPeerConnectionError: $description")
    }

    override fun onRemoteRenderAdded(connection: JanusConnection) {
        printlnCK("onRemoteRenderAdded: ${connection.handleId}")
        runOnUiThread {
            if (mCurrentCallState != CallState.ANSWERED) {
                updateCallStatus(CallState.ANSWERED)
            }

            val remoteRender = SurfaceViewRenderer(this)
            remoteRender.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender))
            remoteRenders[connection.handleId] = remoteRender

            updateRenders()
        }
    }

    override fun onRemoteRenderRemoved(connection: JanusConnection) {
        printlnCK("onRemoteRenderRemoved")
        removeRemoteRender(connection.handleId)
    }

    private fun removeRemoteRender(handleId: BigInteger) {
        runOnUiThread {
            val render = remoteRenders.remove(handleId)
            render?.release()

            if (remoteRenders.isEmpty() && !isGroup(mGroupType)) {
                updateCallStatus(CallState.ENDED)
                return@runOnUiThread
            } else {
                updateRenders()
            }
        }
    }

    private fun updateRenders() {
        binding.surfaceRootContainer.removeAllViews()

        val renders = remoteRenders.values

        val surfaceGenerator =
            SurfacePositionFactory().createSurfaceGenerator(this, renders.size + 1)
        // add local stream
        val localSurfacePosition = surfaceGenerator.getLocalSurface()
        val localParams =
            LinearLayout.LayoutParams(localSurfacePosition.width, localSurfacePosition.height)
                .apply {
                    leftMargin = localSurfacePosition.marginStart
                    topMargin = localSurfacePosition.marginTop
                }
        binding.surfaceRootContainer.addView(mLocalSurfaceRenderer, localParams)

        // add remote streams
        val remoteSurfacePositions = surfaceGenerator.getRemoteSurfaces()
        remoteSurfacePositions.forEachIndexed { index, remoteSurfacePosition ->
            val params =
                LinearLayout.LayoutParams(remoteSurfacePosition.width, remoteSurfacePosition.height)
                    .apply {
                        leftMargin = remoteSurfacePosition.marginStart
                        topMargin = remoteSurfacePosition.marginTop
                    }
            binding.surfaceRootContainer.addView(renders.elementAt(index), params)
        }
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
                    mIsAudioMode = false
                    configMedia(isSpeaker = true, isMuteVideo = false)
                    updateUIByStateAndMode()
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

    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
        var listenerCallingState = MutableLiveData<CallingStateData>()
    }
}
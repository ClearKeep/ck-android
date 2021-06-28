package com.clearkeep.screen.videojanus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.iterator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.clearkeep.R
import com.clearkeep.databinding.ActivityInCallBinding
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.januswrapper.*
import com.clearkeep.repo.MultiServerRepository
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.common.CallState
import com.clearkeep.screen.videojanus.common.createVideoCapture
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition
import com.clearkeep.screen.videojanus.surface_generator.SurfacePositionFactory
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_in_call.*
import kotlinx.android.synthetic.main.activity_in_call.controlCallAudioView
import kotlinx.android.synthetic.main.activity_in_call.controlCallVideoView
import kotlinx.android.synthetic.main.activity_in_call.imageConnecting
import kotlinx.android.synthetic.main.activity_in_call.imgEndWaiting
import kotlinx.android.synthetic.main.activity_in_call.tvConnecting
import kotlinx.android.synthetic.main.activity_in_call.tvEndButtonDescription
import kotlinx.android.synthetic.main.activity_in_call.tvUserName
import kotlinx.android.synthetic.main.activity_in_call.tvUserName2
import kotlinx.android.synthetic.main.activity_in_call.viewConnecting
import kotlinx.android.synthetic.main.activity_in_call.waitingCallView
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.*
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

@AndroidEntryPoint
class InCallActivity : BaseActivity(), JanusRTCInterface,
    PeerConnectionClient.PeerConnectionEvents {
    private val callScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    private val hideBottomButtonHandler: Handler = Handler(Looper.getMainLooper())

    private var mIsMute = false
    private var mIsMuteVideo = false
    private var mIsSpeaker = false

    private var mCurrentCallState: CallState = CallState.CALLING
    private var mTimeStarted: Long = 0

    private var mIsAudioMode: Boolean = false
    private lateinit var mGroupId: String
    private lateinit var mGroupType: String
    private lateinit var mGroupName: String
    private lateinit var mUserNameInConversation: String
    private var mIsGroupCall: Boolean = false
    private lateinit var binding: ActivityInCallBinding

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    @Inject
    lateinit var groupRepository: MultiServerRepository

    @Inject
    lateinit var environment: Environment

    // surface and render
    private lateinit var rootEglBase: EglBase
    private var peerConnectionClient: PeerConnectionClient? = null
    private var mWebSocketChannel: WebSocketChannel? = null
    private lateinit var mLocalSurfaceRenderer: SurfaceViewRenderer

    private val remoteRenders: MutableMap<BigInteger, RemoteInfo> = HashMap()

    private var endCallReceiver: BroadcastReceiver? = null

    private var switchVideoReceiver: BroadcastReceiver? = null

    private var group: ChatGroup? = null

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
        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
        isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)

        rootEglBase = EglBase.create()

        mLocalSurfaceRenderer = SurfaceViewRenderer(this)
        mLocalSurfaceRenderer.apply {
            init(rootEglBase.eglBaseContext, null)
            setZOrderMediaOverlay(true)
            setEnableHardwareScaler(true)
            setMirror(true)
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
        dispatchCallStatus(true)
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
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME)
        avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION) ?: ""
        //todo avatarInConversation hardcode test
        avatarInConversation =
            "https://toquoc.mediacdn.vn/2019/8/7/photo-1-1565165824290120736900.jpg"
        tvUserName.text = groupName

        if (!TextUtils.isEmpty(groupName)) {
            includeToolbar.title.text = groupName
        }

        runDelayToHideBottomButton()
        initWaitingCallView()
        initViewConnecting()
        updateRenders()
    }

    private fun initViewConnecting() {
        if (mIsAudioMode) viewConnecting.gone()
        if (isFromComingCall) {
            tvCallStateVideo.text = "Connecting"
            tvCallState.text = "Connecting"
            tvConnecting.visible()

        }else{
            tvCallStateVideo.text = "Calling Group"
            tvCallState.text = "Calling Group"
            tvConnecting.gone()

        }
        Glide.with(this)
            .load(avatarInConversation)
            .placeholder(R.drawable.ic_bg_gradient)
            .error(R.drawable.ic_bg_gradient)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
            .into(imageConnecting)
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
        callScope.launch {
            // TODO
            group = groupRepository.getGroupByID(intent.getStringExtra(EXTRA_GROUP_ID)!!.toLong(), "", "")
            if (isFromComingCall) {
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                val token = intent.getStringExtra(EXTRA_GROUP_TOKEN) ?: ""
                val webRtcGroupId = intent.getStringExtra(EXTRA_WEB_RTC_GROUP_ID)!!.toInt()
                val webRtcUrl = intent.getStringExtra(EXTRA_WEB_RTC_URL) ?: ""

                startVideo(webRtcGroupId, webRtcUrl, stunUrl, turnUrl, turnUserName, turnPassword, token)
            } else {
                val groupId = intent.getStringExtra(EXTRA_GROUP_ID)!!.toInt()
                val result = videoCallRepository.requestVideoCall(groupId, mIsAudioMode)

                if (result != null) {
                    val turnConfig = result.turnServer
                    val stunConfig = result.stunServer
                    val turnUrl = turnConfig.server
                    val stunUrl = stunConfig.server
                    val token = result.groupRtcToken

                    val webRtcGroupId = result.groupRtcId
                    val webRtcUrl = result.groupRtcUrl
                    startVideo(webRtcGroupId.toInt(), webRtcUrl, stunUrl, turnUrl, turnConfig.user, turnConfig.pwd, token)
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
        super.onDestroy()
        dispatchCallStatus(false)
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
        if (isFromComingCall) waitingCallVideoView.gone()
    }

    private fun startVideo(
        janusGroupId: Int, janusUrl: String,
        stunUrl: String,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        token: String
    ) {
        val ourClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID) ?: ""
        printlnCK("Janus URL: $janusUrl")
        printlnCK(
            "startVideo: janusUrl = $janusUrl, janusGroupId = $janusGroupId , stun = $stunUrl, turn = $turnUrl, username = $turnUser, pwd = $turnPass" +
                    ", token = $token"
        )
        mWebSocketChannel = WebSocketChannel(janusGroupId, ourClientId, token, janusUrl)
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
        mTimeStarted = SystemClock.elapsedRealtime()
        includeToolbar.chronometerTimeCall.apply {
            base = mTimeStarted
            start()
        }
        if (isFromComingCall) {
            Handler(mainLooper).postDelayed({
                viewConnecting.gone()
            }, 2000)
        }
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        mCurrentCallState = newState

        when (mCurrentCallState) {
            CallState.CALLING -> {
                if (!isFromComingCall)
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
                dispatchCallStatus(true)
            }
        }
    }

    private fun dispatchCallStatus(isStarted: Boolean) {
        AppCall.listenerCallingState.postValue(CallingStateData(isStarted, mUserNameInConversation, false, mTimeStarted))
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
                if (isFromComingCall){
                    waitingCallVideoView.gone()
                }else {
                    waitingCallVideoView.visible()
                }
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
        Log.e("antx","finishAndReleaseResource ${!isFromComingCall} ,mCurrentCallState : $mCurrentCallState   ${mCurrentCallState==CallState.CALLING}")
        if (!isFromComingCall&& mCurrentCallState==CallState.CALLING) {
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
        runOnUiThread {
            if (!isFromComingCall)
                Handler(mainLooper).postDelayed({
                    viewConnecting.gone()
                }, 1000)
        }
    }

    override fun onPublisherRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.setRemoteDescription(handleId, sessionDescription)
    }

    override fun subscriberHandleRemoteJsep(janusHandle: JanusHandle, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.subscriberHandleRemoteJsep(janusHandle.handleId, janusHandle.display, sessionDescription)
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
            remoteRender.setMirror(true)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender))
            remoteRenders[connection.handleId] = RemoteInfo(
                connection.display,
                remoteRender
            )

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
            render?.surfaceViewRenderer?.release()

            if (remoteRenders.isEmpty() && !isGroup(mGroupType)) {
                updateCallStatus(CallState.ENDED)
                return@runOnUiThread
            } else {
                updateRenders()
            }
        }
    }

    private fun updateRenders() {
        for (child in binding.surfaceRootContainer.iterator()) {
            (child as RelativeLayout).removeAllViews()
        }
        binding.surfaceRootContainer.removeAllViews()

        val renders = remoteRenders.values

        val me = group?.clientList?.find { it.id == environment.getServer().profile.id }
        val surfaceGenerator =
            SurfacePositionFactory().createSurfaceGenerator(this, renders.size + 1)

        val localSurfacePosition = surfaceGenerator.getLocalSurface()
        val view = createRemoteView(mLocalSurfaceRenderer, me?.userName ?: ""
            , localSurfacePosition)
        binding.surfaceRootContainer.addView(view)

        // add remote streams
        val remoteSurfacePositions = surfaceGenerator.getRemoteSurfaces()
        remoteSurfacePositions.forEachIndexed { index, remoteSurfacePosition ->
            val remoteInfo = renders.elementAt(index)
            val user = group?.clientList?.find { it.id == remoteInfo.clientId }
            val view = createRemoteView(remoteInfo.surfaceViewRenderer, user?.userName ?: "unknown"
                , remoteSurfacePosition)
            binding.surfaceRootContainer.addView(view)
        }
    }

    private fun createRemoteView(
        renderer: SurfaceViewRenderer, remoteName: String,
        remoteSurfacePosition: SurfacePosition
    ): RelativeLayout {
        val params = RelativeLayout.LayoutParams(
            remoteSurfacePosition.width, remoteSurfacePosition.height
        ).apply {
            leftMargin = remoteSurfacePosition.marginStart
            topMargin = remoteSurfacePosition.marginTop
        }
        val relativeLayout = RelativeLayout(this)
        relativeLayout.layoutParams = params

        val tv = TextView(this)
        tv.text = remoteName
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setPadding(0, 0, 0, 24)
        tv.setTextColor(resources.getColor(R.color.grayscaleOffWhite))
        val nameLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        nameLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        /*val muteImage = ImageView(this)
        muteImage.setImageResource(R.drawable.ic_status_muted)
        val muteLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        muteLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)*/

        val rendererParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        relativeLayout.addView(renderer, rendererParams)
        relativeLayout.addView(tv, nameLayoutParams)
        /*relativeLayout.addView(muteImage, muteLayoutParams)*/
        return relativeLayout
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
                    configMedia(isSpeaker = true, isMuteVideo = mIsMuteVideo)
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

    data class RemoteInfo(
        val clientId: String,
        val surfaceViewRenderer: SurfaceViewRenderer
    )

    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
    }
}
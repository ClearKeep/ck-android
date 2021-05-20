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
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.clearkeep.R
import com.clearkeep.databinding.ActivityInCallBinding
import com.clearkeep.januswrapper.JanusConnection
import com.clearkeep.januswrapper.JanusRTCInterface
import com.clearkeep.januswrapper.PeerConnectionClient
import com.clearkeep.januswrapper.WebSocketChannel
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.common.AvatarImageTask
import com.clearkeep.screen.videojanus.common.createVideoCapture
import com.clearkeep.screen.videojanus.surface_generator.SurfacePositionFactory
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_in_call.*
import kotlinx.android.synthetic.main.activity_in_call.imageBackground
import kotlinx.android.synthetic.main.activity_in_call.tvNickName
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import android.widget.TextView
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition


@AndroidEntryPoint
class InCallActivity : BaseActivity(), View.OnClickListener, JanusRTCInterface, PeerConnectionClient.PeerConnectionEvents {
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

    // resource id
    private var mResIdSpeakerOn = 0
    private var mResIdSpeakerOff = 0
    private var mResIdMuteOn = 0
    private var mResIdMuteOff = 0
    private var mResIdMuteVideoOn = 0
    private var mResIdMuteVideoOff = 0

    private var endCallReceiver: BroadcastReceiver? = null

    private var switchVideoReceiver: BroadcastReceiver? = null

    // sound
    private var ringBackPlayer: MediaPlayer? = null
    private var busySignalPlayer: MediaPlayer? = null
    var isFromComingCall: Boolean=false
    var avatarInConversation=""
    var groupName=""

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

        val a: TypedArray = theme.obtainStyledAttributes(
                intArrayOf(
                        R.attr.buttonSpeakerOn, R.attr.buttonSpeakerOff,
                        R.attr.buttonMuteOn, R.attr.buttonMuteOff,
                        R.attr.buttonMuteVideoOn, R.attr.buttonMuteVideoOff
                )
        )
        try {
            mResIdSpeakerOn = a.getResourceId(0, R.drawable.ic_string_ee_sound_on)
            mResIdSpeakerOff = a.getResourceId(1, R.drawable.ic_string_ee_sound_off)
            mResIdMuteOn = a.getResourceId(2, R.drawable.ic_string_ee_mute_on)
            mResIdMuteOff = a.getResourceId(3, R.drawable.ic_string_ee_mute_off)
            mResIdMuteVideoOn = a.getResourceId(4, R.drawable.baseline_videocam_white_18)
            mResIdMuteVideoOff = a.getResourceId(5, R.drawable.baseline_videocam_off_white_18)
        } finally {
            a.recycle()
        }

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
        binding.imgBack.setOnClickListener(this)
        binding.imgSpeaker.setOnClickListener(this)
        binding.imgMute.setOnClickListener(this)
        binding.imgEnd.setOnClickListener(this)
        binding.imgVideoMute.setOnClickListener(this)
        binding.imgSwitchCamera.setOnClickListener(this)
        binding.imgSwitchAudioToVideo.setOnClickListener(this)
        binding.surfaceRootContainer.setOnClickListener(this)

        updateUIByStateAndMode()

        val userNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        listenerCallingState.postValue(CallingStateData(true,userNameInConversation))
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME)
        avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION) ?: ""
        //todo avatarInConversation hardcode test
        avatarInConversation="https://toquoc.mediacdn.vn/2019/8/7/photo-1-1565165824290120736900.jpg"
        if (!TextUtils.isEmpty(groupName)) {
            binding.tvUserName.text = groupName
        }
        if (!TextUtils.isEmpty(avatarInConversation)) {
            AvatarImageTask(binding.imgThumb).execute(avatarInConversation)
        }

        runDelayToHideBottomButton()

        // update to add first local stream
        updateRenders()

    }

    private fun runDelayToHideBottomButton() {
        hideBottomButtonHandler.removeCallbacksAndMessages(null)
        hideBottomButtonHandler.postDelayed(Runnable {
            binding.bottomButtonContainer.visibility = View.GONE
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

        if (!isFromComingCall){
            waitingCallView.visibility=View.VISIBLE
            initWaitingCallView()
        }else {
            waitingCallView.visibility=View.GONE
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
        if (binding.chronometer.visibility == View.VISIBLE) {
            binding.chronometer.stop()
        }
    }

    override fun onPictureInPictureModeChanged(
            isInPictureInPictureMode: Boolean,
            newConfig: Configuration
    ) {
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            binding.imgBack.visibility = View.GONE
            binding.imgSpeaker.visibility = View.GONE
            binding.imgMute.visibility = View.GONE
            binding.imgEnd.visibility = View.GONE
            binding.imgSwitchCamera.visibility = View.GONE
        } else {
            // Restore the full-screen UI.
            binding.imgBack.visibility = View.VISIBLE
            binding.imgSpeaker.visibility = View.VISIBLE
            binding.imgMute.visibility = View.VISIBLE
            binding.imgEnd.visibility = View.VISIBLE
            binding.imgSwitchCamera.visibility = View.VISIBLE
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgEnd -> {
                hangup()
                finishAndReleaseResource()
            }
            R.id.imgBack -> {
                if (hasSupportPIP()) {
                    enterPIPMode()
                } else {
                    hangup()
                    finishAndReleaseResource()
                }
            }
            R.id.imgSpeaker -> {
                mIsSpeaker = !mIsSpeaker
                enableSpeaker(mIsSpeaker)
                runDelayToHideBottomButton()
            }
            R.id.imgMute -> {
                mIsMute = !mIsMute
                enableMute(mIsMute)
                runDelayToHideBottomButton()
            }
            R.id.imgSwitchCamera -> {
                peerConnectionClient?.switchCamera()
            }
            R.id.imgSwitchAudioToVideo -> {
                switchToVideoMode()
            }
            R.id.imgVideoMute -> {
                mIsMuteVideo = !mIsMuteVideo
                enableMuteVideo(mIsMuteVideo)
                runDelayToHideBottomButton()
            }
            R.id.surfaceRootContainer -> {
                if (bottomButtonContainer.visibility == View.VISIBLE) {
                    binding.bottomButtonContainer.visibility = View.GONE
                } else {
                    binding.bottomButtonContainer.visibility = View.VISIBLE
                    runDelayToHideBottomButton()
                }
            }
        }
    }

   private fun initWaitingCallView(){
        tvUserName2.text = mGroupName
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
                .listener(object : RequestListener<Drawable>{
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
            txtAudioMode.text= "Calling Group"
            imgThumb2.visibility = View.GONE
            tvNickName.visibility = View.GONE
        }

        imgEndWaiting.setOnClickListener {
            hangup()
            finishAndReleaseResource()
        }
    }

    private fun startVideo(groupId: Int, stunUrl: String, turnUrl: String, turnUser: String, turnPass: String, token: String) {
        val ourClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID) ?: ""
        printlnCK("Janus URL: $JANUS_URI")
        printlnCK("startVideo: stun = $stunUrl, turn = $turnUrl, username = $turnUser, pwd = $turnPass" +
                ", group = $groupId, token = $token")
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
        if (binding.chronometer.isVisible) {
            return
        }
        binding.chronometer.visibility = View.VISIBLE
        binding.tvCallState.visibility = View.GONE

        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        mCurrentCallState = newState
        when (mCurrentCallState) {
            CallState.CALLING -> {
                binding.tvCallState.text = getString(R.string.text_calling)
                playRingBackTone()
            }
            CallState.RINGING ->
                binding.tvCallState.text = getString(R.string.text_ringing)
            CallState.BUSY, CallState.CALL_NOT_READY -> {
                binding.tvCallState.text = getString(R.string.text_busy)
                stopRingBackTone()
                playBusySignalSound()
                GlobalScope.launch {
                    delay(3 * 1000)
                    hangup()
                    finishAndReleaseResource()
                }
            }
            CallState.ENDED -> {
                binding.tvCallState.text = getString(R.string.text_end)
                hangup()
                finishAndReleaseResource()
            }
            CallState.ANSWERED -> {
                binding.tvCallState.text = getString(R.string.text_started)
                stopRingBackTone()
                displayCountUpClockOfConversation()
                updateUIByStateAndMode()

            }
        }
    }

    private fun showOrHideAvatar(isShowAvatar: Boolean) {
        runOnUiThread {
            if (isShowAvatar) {
                binding.containerUserInfo.visibility = View.VISIBLE
            } else {
                binding.containerUserInfo.visibility = View.GONE
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
        if (!mIsAudioMode) {
            binding.imgSwitchAudioToVideo.visibility = View.GONE
            binding.imgSwitchCamera.visibility = View.VISIBLE
            binding.containerAudioCover.visibility = View.GONE

            // display bottom button in video mode
            binding.imgVideoMute.visibility = View.VISIBLE
            binding.imgSpeaker.visibility = View.VISIBLE
            binding.imgMute.visibility = View.VISIBLE

            if (mCurrentCallState == CallState.ANSWERED) {
                showOrHideAvatar(false)
                waitingCallView.visibility=View.GONE
            }
        } else {
            if (mCurrentCallState != CallState.CALLING) {
                binding.imgSwitchAudioToVideo.visibility = View.VISIBLE
            }
            binding.imgSwitchCamera.visibility = View.GONE
            binding.containerAudioCover.visibility = View.VISIBLE
        }
    }

    private fun enableSpeaker(isEnable: Boolean) {
        binding.imgSpeaker.isClickable = true
        if (isEnable) {
            binding.imgSpeaker.setImageResource(mResIdSpeakerOn)
        } else {
            binding.imgSpeaker.setImageResource(mResIdSpeakerOff)
        }
        setSpeakerphoneOn(isEnable)
    }

    private fun enableMute(isMuting: Boolean) {
        binding.imgMute.isClickable = true
        if (isMuting) {
            binding.imgMute.setImageResource(mResIdMuteOn)
        } else {
            binding.imgMute.setImageResource(mResIdMuteOff)
        }
        peerConnectionClient?.setAudioEnabled(!isMuting)
    }

    private fun enableMuteVideo(isMuteVideo: Boolean) {
        binding.imgVideoMute.isClickable = true
        if (isMuteVideo) {
            binding.imgVideoMute.setImageResource(mResIdMuteVideoOff)
        } else {
            binding.imgVideoMute.setImageResource(mResIdMuteVideoOn)
        }

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

            val remoteRender1 = SurfaceViewRenderer(this)
            remoteRender1.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender1))
            remoteRenders[100.toBigInteger()] = remoteRender1

            val remoteRender2 = SurfaceViewRenderer(this)
            remoteRender2.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender2))
            remoteRenders[101.toBigInteger()] = remoteRender2

            val remoteRender3 = SurfaceViewRenderer(this)
            remoteRender3.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender3))
            remoteRenders[102.toBigInteger()] = remoteRender3

            val remoteRender4 = SurfaceViewRenderer(this)
            remoteRender4.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender4))
            remoteRenders[103.toBigInteger()] = remoteRender4

            val remoteRender5 = SurfaceViewRenderer(this)
            remoteRender5.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender5))
            remoteRenders[104.toBigInteger()] = remoteRender5

            val remoteRender6 = SurfaceViewRenderer(this)
            remoteRender6.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender6))
            remoteRenders[105.toBigInteger()] = remoteRender6

            val remoteRender7 = SurfaceViewRenderer(this)
            remoteRender7.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender7))
            remoteRenders[106.toBigInteger()] = remoteRender7

            val remoteRender8 = SurfaceViewRenderer(this)
            remoteRender8.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender8))
            remoteRenders[107.toBigInteger()] = remoteRender8

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

        val surfaceGenerator = SurfacePositionFactory().createSurfaceGenerator(this, renders.size + 1)
        // add local stream
        val localSurfacePosition = surfaceGenerator.getLocalSurface()
        val localParams = LinearLayout.LayoutParams(localSurfacePosition.width, localSurfacePosition.height).apply {
            leftMargin = localSurfacePosition.marginStart
            topMargin = localSurfacePosition.marginTop
        }
        binding.surfaceRootContainer.addView(mLocalSurfaceRenderer, localParams)

        // add remote streams
        val remoteSurfacePositions = surfaceGenerator.getRemoteSurfaces()
        remoteSurfacePositions.forEachIndexed { index, remoteSurfacePosition ->
            val view = createRemoteView(renders.elementAt(index), "Elon musk"
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

        val muteImage = ImageView(this)
        muteImage.setImageResource(R.drawable.ic_status_muted)
        val muteLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        muteLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        val rendererParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        relativeLayout.addView(renderer, rendererParams)
        relativeLayout.addView(tv, nameLayoutParams)
        relativeLayout.addView(muteImage, muteLayoutParams)
        return relativeLayout
    }

    private fun showAskPermissionDialog() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Permissions Required")
            .setMessage("You have forcefully denied some of the required permissions " +
                    "for this action. Please open settings, go to permissions and allow them.")
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

    enum class CallState {
        CALLING,
        RINGING,
        ANSWERED,
        BUSY,
        ENDED,
        CALL_NOT_READY
    }

    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
        var listenerCallingState = MutableLiveData<CallingStateData>()
    }
}
package com.clearkeep.screen.videojanus.peer_to__peer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.januswrapper.JanusConnection
import com.clearkeep.repo.ServerRepository
import com.clearkeep.repo.PeopleRepository
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.*
import com.clearkeep.screen.videojanus.common.CallState
import com.clearkeep.screen.videojanus.common.CallStateView
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_in_call.*
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.*
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.controlCallAudioView
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.controlCallVideoView
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imageBackground
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imageConnecting
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imgEndWaiting
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.imgThumb2
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvConnecting
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvEndButtonDescription
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvNickName
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvUserName
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.tvUserName2
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.viewConnecting
import kotlinx.android.synthetic.main.activity_in_call_peer_to_peer.waitingCallView
import kotlinx.android.synthetic.main.view_control_call_audio.view.*
import kotlinx.android.synthetic.main.view_control_call_video.view.*
import kotlinx.coroutines.*
import org.webrtc.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class InCallPeerToPeerActivity : BaseActivity() {
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
    private lateinit var mOwnerClientId: String
    private lateinit var mOwnerDomain: String
    private lateinit var mUserNameInConversation: String
    private lateinit var mCurrentUsername: String
    private lateinit var mPeerUserAvatar: String
    private lateinit var mCurrentUserAvatar: String
    private var mIsGroupCall: Boolean = false

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    @Inject
    lateinit var serverRepository: ServerRepository

    @Inject
    lateinit var peopleRepository: PeopleRepository

    // surface and render
    private var endCallReceiver: BroadcastReceiver? = null
    private var busyCallReceiver: BroadcastReceiver? = null

    private var switchVideoReceiver: BroadcastReceiver? = null

    // sound
    private var ringBackPlayer: MediaPlayer? = null
    private var busySignalPlayer: MediaPlayer? = null
    var isFromComingCall: Boolean = false
    var groupName = ""

    private var mTimeStarted: Long = 0
    var isShowedDialogCamera = false


    @SuppressLint("ResourceType", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call_peer_to_peer)
        //    allowOnLockScreen()
        isInPeerCall = true
        NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID)!!
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME)!!
        mGroupType = intent.getStringExtra(EXTRA_GROUP_TYPE)!!
        mOwnerDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN)!!
        mOwnerClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT)!!
        mIsGroupCall = isGroup(mGroupType)
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        printlnCK("InCallPeerToPeer onCreate isAudioMode $mIsAudioMode")
        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
        mCurrentUsername = intent.getStringExtra(EXTRA_CURRENT_USERNAME) ?: ""
        mPeerUserAvatar = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION) ?: ""
        mCurrentUserAvatar = intent.getStringExtra(EXTRA_CURRENT_USER_AVATAR) ?: ""
        //TODO: Remove test hardcode
        callViewModel.mIsAudioMode.value = mIsAudioMode
        isShowedDialogCamera = !mIsAudioMode

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
        dispatchCallStatus(true)
        createInCallNotification(this, InCallPeerToPeerActivity::class.java)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configLandscapeLayout()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        printlnCK("InCallPeerToPeer onNewIntent()")
        super.onNewIntent(intent)
    }

    private fun configMedia(isSpeaker: Boolean, isMuteVideo: Boolean) {
        mIsSpeaker = isSpeaker
        mIsMuteVideo = isMuteVideo
        controlCallAudioView.apply {
            toggleSpeaker?.isChecked = mIsSpeaker
        }
        controlCallVideoView.apply {
            bottomToggleFaceTime?.isChecked = mIsMuteVideo
        }
        callViewModel.onSpeakChange(mIsSpeaker)
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

        pipCallNamePeer.text = mCurrentUsername
        //todo avatarInConversation hardcode test
        callViewModel.listenerOnRemoteRenderAdd = listenerOnRemoteRenderAdd
        callViewModel.listenerOnPublisherJoined = listenerOnPublisherJoined
        initWaitingCallView()
        initViewConnecting()
    }

    override fun onPermissionsAvailable() {
        isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)

        callScope.launch {
            if (isFromComingCall) {
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""

                val webRtcGroupId = intent.getStringExtra(EXTRA_WEB_RTC_GROUP_ID)!!.toInt()
                val webRtcUrl = intent.getStringExtra(EXTRA_WEB_RTC_URL) ?: ""

                val token = intent.getStringExtra(EXTRA_GROUP_TOKEN) ?: ""
                startVideo(
                    webRtcGroupId,
                    webRtcUrl,
                    stunUrl,
                    turnUrl,
                    turnUserName,
                    turnPassword,
                    token
                )
                callViewModel.onSpeakChange(mIsSpeaker)
            } else {
                val groupId = intent.getStringExtra(EXTRA_GROUP_ID)!!.toInt()
                val result =
                    videoCallRepository.requestVideoCall(groupId, mIsAudioMode, getOwnerServer())
                if (result != null) {
                    val turnConfig = result.turnServer
                    val stunConfig = result.stunServer
                    val turnUrl = turnConfig.server
                    val stunUrl = stunConfig.server
                    val token = result.groupRtcToken

                    val webRtcGroupId = result.groupRtcId
                    val webRtcUrl = result.groupRtcUrl
                    startVideo(
                        webRtcGroupId.toInt(),
                        webRtcUrl,
                        stunUrl,
                        turnUrl,
                        turnConfig.user,
                        turnConfig.pwd,
                        token
                    )
                    callViewModel.onSpeakChange(mIsSpeaker)
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
                        callViewModel.mCurrentCallState.postValue(CallState.CALL_NOT_READY)
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
        dismissInCallNotification(this)
        dispatchCallStatus(false)
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (isInPictureInPictureMode) {
            controlCallVideoView.visibility = View.GONE
            controlCallAudioView.visibility = View.GONE
            imgEndWaiting.visibility = View.GONE
            tvEndButtonDescription.visibility = View.GONE
            tvVideoTimeCall.visibility = View.GONE
            tvUserName.visibility = View.GONE
            remoteRender.visibility = View.GONE
            imgVideoCallBack.visibility = View.GONE
            imgWaitingBack.visibility = View.GONE
            if (mIsMuteVideo) {
                pipInfoPeer.visible()
                localRender.gone()
            } else {
                remoteRender.visibility = View.GONE
                setLocalViewFullScreen()
            }
        } else {
            remoteRender.visibility = View.VISIBLE
            imgEndWaiting.visibility = View.VISIBLE
            tvVideoTimeCall.visibility = View.VISIBLE
            tvEndButtonDescription.visibility = View.VISIBLE
            tvUserName.visibility = View.VISIBLE
            imgVideoCallBack.visibility = View.VISIBLE
            imgWaitingBack.visibility = View.VISIBLE
            pipInfoPeer.gone()
            if (!mIsAudioMode || callViewModel.mIsAudioMode.value == false) {
                controlCallVideoView.visibility = View.VISIBLE
                localRender.visible()
                if (mIsMuteVideo) {
                } else {
                    setLocalFixScreen()
                }
            } else {
                controlCallAudioView.visibility = View.VISIBLE
            }
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

    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun getOwnerServer(): Owner {
        return Owner(mOwnerDomain, mOwnerClientId)
    }

    private fun onClickControlCall() {
        controlCallAudioView.apply {
            this.toggleMute.setOnClickListener {
                callViewModel.onMuteChange(callViewModel.mIsMute.value != true)
                callViewModel.mIsMute.postValue(callViewModel.mIsMute.value != true)
            }
            this.toggleFaceTime.setOnClickListener {
                callViewModel.onFaceTimeChange((it as ToggleButton).isChecked)
                callViewModel.mIsAudioMode.postValue(it.isChecked)
                callViewModel.onSpeakChange(true)
                mIsMuteVideo = !mIsMuteVideo
                switchToVideoMode()
                controlCallVideoView.bottomToggleFaceTime.isChecked = false
            }
            this.toggleSpeaker.setOnClickListener {
                callViewModel.onSpeakChange((it as ToggleButton).isChecked)
            }
        }
        controlCallVideoView.apply {
            this.bottomToggleMute.setOnClickListener {
                callViewModel.onMuteChange(callViewModel.mIsMute.value != true)
                callViewModel.mIsMute.postValue(callViewModel.mIsMute.value != true)
            }
            this.bottomToggleFaceTime.setOnClickListener {
                callViewModel.onFaceTimeChange(mIsMuteVideo)
                mIsMuteVideo = !mIsMuteVideo
                switchToVideoMode()
            }
            this.bottomToggleSwitchCamera.setOnClickListener {
                callViewModel.onCameraChange((it as ToggleButton).isChecked)
            }
            this.bottomImgEndCall.setOnClickListener {
                endCall()
            }
        }

        imgEndWaiting.setOnClickListener {
            endCall()
        }
        callViewModel.mIsAudioMode.observe(this, {
            if (it == false && mIsAudioMode) {
                updateUIbyStateView(CallStateView.CALLED_VIDEO)
            }
        })

        callViewModel.mIsMute.observe(this, {
            controlCallVideoView.bottomToggleMute.isChecked = it
            controlCallAudioView.toggleMute.isChecked = it
        })

        imgWaitingBack.setOnClickListener {
            handleBackPressed()
        }
        imgVideoCallBack.setOnClickListener {
            handleBackPressed()
        }
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

    private fun handleBackPressed() {
        if (hasSupportPIP()) {
            enterPIPMode()
        } else {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.dialog_leave_call_title))
                .setPositiveButton(getString(R.string.leave)) { _, _ ->
                    endCall()
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->

                }
                .create()
                .show()
        }
    }

    private fun initWaitingCallView() {
        tvUserName2.text = mGroupName
        tvUserName.text = mGroupName
        tvNickName.visibility = View.VISIBLE
        val displayName =
            if (mGroupName.isNotBlank() && mGroupName.length >= 2) mGroupName.substring(
                0,
                1
            ) else mGroupName
        tvNickName.text = displayName

        if (!mIsGroupCall) {
            Glide.with(this)
                .load(mPeerUserAvatar)
                .placeholder(R.drawable.ic_background_gradient_call)
                .error(R.drawable.ic_background_gradient_call)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
                .into(imageBackground)

            Glide.with(this)
                .load(mPeerUserAvatar)
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

            Glide.with(this)
                .load(mCurrentUserAvatar)
                .circleCrop()
                .into(pipCallAvatar)

        } else {
            tvStateCall.text = getString(R.string.calling_group)
            imgThumb2.visibility = View.GONE
            tvNickName.visibility = View.GONE
        }
    }

    private fun initViewConnecting() {
        if (isFromComingCall)
            tvConnecting.visible()
        else tvConnecting.gone()
        Glide.with(this)
            .load(mPeerUserAvatar)
            .placeholder(R.drawable.ic_background_gradient_call)
            .error(R.drawable.ic_background_gradient_call)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
            .into(imageConnecting)
    }

    private fun startVideo(
        webRtcGroupId: Int, webRtcUrl: String,
        stunUrl: String,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        token: String
    ) {
        val ourClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        callViewModel.startVideo(
            context = this,
            localRender,
            ourClientId,
            webRtcGroupId, webRtcUrl,
            stunUrl,
            turnUrl,
            turnUser,
            turnPass,
            token
        )
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        when (callViewModel.mCurrentCallState.value) {
            CallState.CALLING -> {
                if (!isFromComingCall)
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
                    endCall()
                }
            }
            CallState.ENDED -> {
                endCall()
            }
            CallState.ANSWERED -> {
                stopRingBackTone()
                if (callViewModel.mIsAudioMode.value == true) {
                    updateUIbyStateView(CallStateView.CALLED_AUDIO)
                } else {
                    updateUIbyStateView(CallStateView.CALLED_VIDEO)
                }
            }
        }
    }

    private fun updateUIbyStateView(callStateView: CallStateView) {
        when (callStateView) {
            CallStateView.CALL_AUDIO_WAITING -> {
                waitingCallView.visibility = View.VISIBLE
                viewAudioCalled.visibility = View.GONE
                viewVideoCalled.visibility = View.GONE
            }

            CallStateView.CALL_VIDEO_WAITING -> {
                waitingCallView.visibility = View.GONE
                setLocalViewFullScreen()
                tvStateVideoCall.visibility = View.VISIBLE
                tvUserNameVideoCall.visibility = View.VISIBLE
                tvUserNameVideoCall.text = mGroupName
                viewVideoCalled.visibility = View.GONE


            }
            CallStateView.CALLED_AUDIO -> {
                waitingCallView.visibility = View.VISIBLE
                tvStateVideoCall.visibility = View.GONE
                tvStateCall.visibility = View.GONE
                viewAudioCalled.visibility = View.VISIBLE

            }
            CallStateView.CALLED_VIDEO -> {
                setLocalFixScreen()
                waitingCallView.visibility = View.GONE
                tvStateVideoCall.visibility = View.GONE
                tvUserNameVideoCall.visibility = View.GONE
                viewVideoCalled.visibility = View.VISIBLE
            }

        }
    }

    private fun hangup() {
        dismissInCallNotification(this)
        callScope.cancel()
    }

    private fun switchToVideoMode() {
        callViewModel.mIsAudioMode.postValue(false)
        callScope.launch {
            videoCallRepository.switchAudioToVideoCall(mGroupId.toInt(), getOwnerServer())
        }
    }

    private fun finishAndReleaseResource() {
        hideBottomButtonHandler.removeCallbacksAndMessages(null)
        isInPeerCall = false
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
            videoCallRepository.cancelCall(mGroupId.toInt(), getOwnerServer())
        }
    }

    private var listenerOnRemoteRenderAdd = fun(connection: JanusConnection) {
        connection.videoTrack.addRenderer(VideoRenderer(remoteRender))
        startTimeInterval()
        dispatchCallStatus(true)
    }

    private var listenerOnPublisherJoined = fun() {
        runOnUiThread {
            callViewModel.onFaceTimeChange(!mIsMuteVideo)
            if (!isFromComingCall)
                Handler(mainLooper).postDelayed({
                    viewConnecting.gone()
                }, 1000)
        }
    }

    private fun showAskPermissionDialog() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.dialog_call_permission_denied_title))
            .setMessage(
                getString(R.string.dialog_call_permission_denied_text)
            )
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                openSettingScreen()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finishAndReleaseResource()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun showOpenCameraDialog() {
        isShowedDialogCamera = true
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.call_request_video_dialog_title))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                configMedia(isSpeaker = true, isMuteVideo = false)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
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
                    endCall()
                }
            }
        }

        busyCallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)
                printlnCK("busyCallReceiver: groupId : $groupId")
                if (mGroupId == groupId) {
                    stopRingBackTone()
                    playBusySignalSound()
                    GlobalScope.launch {
                        delay(3 * 1000)
                        hangup()
                        finishAndReleaseResource()
                    }
                }
            }
        }

        registerReceiver(
            endCallReceiver,
            IntentFilter(ACTION_CALL_CANCEL)
        )
        registerReceiver(
            busyCallReceiver,
            IntentFilter(ACTION_CALL_BUSY)
        )
    }

    private fun endCall() {
        hangup()
        finishAndReleaseResource()
    }

    private fun unRegisterEndCallReceiver() {
        if (endCallReceiver != null) {
            unregisterReceiver(endCallReceiver)
            endCallReceiver = null
        }
        if (busyCallReceiver != null) {
            unregisterReceiver(busyCallReceiver)
            busyCallReceiver = null
        }
    }

    private fun registerSwitchVideoReceiver() {
        printlnCK("registerSwitchVideoReceiver")
        switchVideoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getStringExtra(EXTRA_CALL_SWITCH_VIDEO).toString()
                if (mGroupId == groupId && mIsAudioMode && callViewModel.mIsAudioMode.value == true) {
                    callViewModel.mIsAudioMode.postValue(false)
                    configMedia(isSpeaker = true, isMuteVideo = mIsMuteVideo)
                    if (!isShowedDialogCamera) {
                        showOpenCameraDialog()
                    }
                }
            }
        }
        registerReceiver(switchVideoReceiver, IntentFilter(ACTION_CALL_SWITCH_VIDEO))
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
        if (isFromComingCall) {
            Handler(mainLooper).postDelayed({
                viewConnecting.gone()
            }, 2000)
        }
        mTimeStarted = SystemClock.elapsedRealtime()
        callViewModel.totalTimeRunJob =
            startCoroutineTimer(delayMillis = 1000, repeatMillis = 1000) {
                callViewModel.totalTimeRun += 1
                runOnUiThread {
                    tvTimeCall.text = convertSecondsToHMmSs(callViewModel.totalTimeRun)
                    tvVideoTimeCall.text = convertSecondsToHMmSs(callViewModel.totalTimeRun)
                }
            }
    }

    companion object {
        private var isInPeerCall = false
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
    }


    private fun setLocalViewFullScreen() {
        localRender.layoutParams =
            fullView(localRender.layoutParams as ConstraintLayout.LayoutParams)
    }

    private fun setLocalFixScreen() {
        localRender.layoutParams =
            fixViewLocalView(localRender.layoutParams as ConstraintLayout.LayoutParams)
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
            bottomMargin = 0
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

    private fun startCoroutineTimer(
        delayMillis: Long = 0,
        repeatMillis: Long = 0,
        action: () -> Unit
    ) = GlobalScope.launch {
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

    private fun configPortraitLayout() {
        controlCallAudioView.imgEndWaiting.gone()
        controlCallAudioView.tvEndButtonDescription.gone()
        tvEndButtonDescription.visible()
        imgEndWaiting.visible()

        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(controlCallAudioView as ConstraintLayout)
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.TOP,
            R.id.parent,
            ConstraintSet.TOP
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.BOTTOM,
            R.id.parent,
            ConstraintSet.BOTTOM
        )
        mConstraintSet.applyTo(controlCallAudioView as ConstraintLayout)

        val layoutParams = controlCallAudioView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 0.7f
        controlCallAudioView.layoutParams = layoutParams

        val parentConstraintSet = ConstraintSet()
        parentConstraintSet.clone(waitingCallView as ConstraintLayout)
        parentConstraintSet.setMargin(R.id.tvStateCall, ConstraintSet.TOP, resources.getDimension(R.dimen._100sdp).toInt())
        parentConstraintSet.applyTo(waitingCallView as ConstraintLayout)

        val avatarLayoutParams = imgThumb2.layoutParams
        avatarLayoutParams.width = resources.getDimension(R.dimen._159sdp).toInt()
        avatarLayoutParams.height = resources.getDimension(R.dimen._159sdp).toInt()
        imgThumb2.layoutParams = avatarLayoutParams
    }

    private fun configLandscapeLayout() {
        controlCallAudioView.imgEndWaiting.visible()
        controlCallAudioView.tvEndButtonDescription.visible()
        tvEndButtonDescription.gone()
        imgEndWaiting.gone()

        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(controlCallAudioView as ConstraintLayout)
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.END,
            R.id.parent,
            ConstraintSet.END
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.START,
            R.id.parent,
            ConstraintSet.START
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.BOTTOM,
            R.id.parent,
            ConstraintSet.BOTTOM
        )
        mConstraintSet.applyTo(controlCallAudioView as ConstraintLayout)

        val layoutParams = controlCallAudioView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 1f
        controlCallAudioView.layoutParams = layoutParams

        val parentConstraintSet = ConstraintSet()
        parentConstraintSet.clone(waitingCallView as ConstraintLayout)
        parentConstraintSet.setMargin(R.id.tvStateCall, ConstraintSet.TOP, resources.getDimension(R.dimen._20sdp).toInt())
        parentConstraintSet.applyTo(waitingCallView as ConstraintLayout)

        val avatarLayoutParams = imgThumb2.layoutParams
        avatarLayoutParams.width = resources.getDimension(R.dimen._100sdp).toInt()
        avatarLayoutParams.height = resources.getDimension(R.dimen._100sdp).toInt()
        imgThumb2.layoutParams = avatarLayoutParams
    }

    private fun dispatchCallStatus(isStarted: Boolean) {
        AppCall.listenerCallingState.postValue(
            CallingStateData(
                isStarted,
                mUserNameInConversation,
                true,
                mTimeStarted
            )
        )
    }
}

package com.clearkeep.features.calls.presentation.peertopeer

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
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.clearkeep.common.utilities.*
import com.clearkeep.features.calls.R
import com.clearkeep.features.calls.databinding.ActivityInCallPeerToPeerBinding
import com.clearkeep.features.calls.presentation.*
import com.clearkeep.features.calls.presentation.common.CallState
import com.clearkeep.features.calls.presentation.common.CallStateView
import com.clearkeep.features.shared.createInCallNotification
import com.clearkeep.features.shared.dismissInCallNotification
import com.clearkeep.features.shared.presentation.AppCall
import com.clearkeep.features.shared.presentation.CallingStateData
import com.clearkeep.januswrapper.JanusConnection
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.*
import org.webrtc.*
import java.util.*

@AndroidEntryPoint
class InCallPeerToPeerActivity : BaseActivity() {
    private val callScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    private val hideBottomButtonHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var binding: ActivityInCallPeerToPeerBinding
    val callViewModel: CallViewModel by viewModels()

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
        binding = ActivityInCallPeerToPeerBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

        callViewModel.mCurrentCallState.observe(this) {
            updateCallStatus(it)
        }
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
        binding.controlCallAudioView.apply {
            toggleSpeaker.isChecked = mIsSpeaker
        }
        binding.controlCallVideoView.apply {
            bottomToggleFaceTime.isChecked = mIsMuteVideo
        }
        callViewModel.onSpeakChange(mIsSpeaker)
        callViewModel.onFaceTimeChange(!isMuteVideo)
    }

    private fun initViews() {
        binding.remoteRender.apply {
            init(callViewModel.rootEglBase.eglBaseContext, null)
            setEnableHardwareScaler(true)
        }
        binding.localRender.apply {
            init(callViewModel.rootEglBase.eglBaseContext, null)
            setZOrderMediaOverlay(true)
            setEnableHardwareScaler(true)
        }

        binding.pipCallNamePeer.text = mCurrentUsername
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
                val result = callViewModel.requestVideoCall(groupId, mIsAudioMode, getOwnerServer())
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
            binding.controlCallVideoView.root.gone()
            binding.controlCallAudioView.root.gone()
            binding.imgEndWaiting.gone()
            binding.tvEndButtonDescription.gone()
            binding.tvVideoTimeCall.gone()
            binding.tvUserName.gone()
            binding.remoteRender.gone()
            binding.imgVideoCallBack.gone()
            binding.imgWaitingBack.gone()
            if (mIsMuteVideo) {
                binding.pipInfoPeer.visible()
                binding.localRender.gone()
            } else {
                binding.remoteRender.gone()
                setLocalViewFullScreen()
            }
        } else {
            binding.remoteRender.visible()
            binding.imgEndWaiting.visible()
            binding.tvVideoTimeCall.visible()
            binding.tvEndButtonDescription.visible()
            binding.tvUserName.visible()
            binding.imgVideoCallBack.visible()
            binding.imgWaitingBack.visible()
            binding.pipInfoPeer.gone()
            if (!mIsAudioMode || callViewModel.mIsAudioMode.value == false) {
                binding.controlCallVideoView.root.visible()
                binding.localRender.visible()
                if (mIsMuteVideo) {
                } else {
                    setLocalFixScreen()
                }
            } else {
                binding.controlCallAudioView.root.visible()
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

    private fun getOwnerServer(): com.clearkeep.domain.model.Owner {
        return com.clearkeep.domain.model.Owner(mOwnerDomain, mOwnerClientId)
    }

    private fun onClickControlCall() {
        binding.controlCallAudioView.apply {
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
                binding.controlCallVideoView.bottomToggleFaceTime.isChecked = false
            }
            this.toggleSpeaker.setOnClickListener {
                callViewModel.onSpeakChange((it as ToggleButton).isChecked)
            }
        }
        binding.controlCallVideoView.apply {
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

        binding.imgEndWaiting.setOnClickListener {
            endCall()
        }
        callViewModel.mIsAudioMode.observe(this) {
            if (it == false && mIsAudioMode) {
                updateUIbyStateView(CallStateView.CALLED_VIDEO)
            }
        }

        callViewModel.mIsMute.observe(this) {
            binding.controlCallVideoView.bottomToggleMute.isChecked = it
            binding.controlCallAudioView.toggleMute.isChecked = it
        }

        binding.imgWaitingBack.setOnClickListener {
            handleBackPressed()
        }
        binding.imgVideoCallBack.setOnClickListener {
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
        binding.tvUserName2.text = mGroupName
        binding.tvUserName.text = mGroupName
        binding.tvNickName.visible()
        val displayName =
            if (mGroupName.isNotBlank() && mGroupName.length >= 2) mGroupName.substring(
                0,
                1
            ) else mGroupName
        binding.tvNickName.text = displayName

        if (!mIsGroupCall) {
            Glide.with(this)
                .load(mPeerUserAvatar)
                .placeholder(R.drawable.ic_background_gradient_call)
                .error(R.drawable.ic_background_gradient_call)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
                .into(binding.imageBackground)

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
                        binding.tvNickName.visible()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.tvNickName.gone()
                        return false
                    }

                })
                .into(binding.imgThumb2)

            Glide.with(this)
                .load(mCurrentUserAvatar)
                .circleCrop()
                .into(binding.pipCallAvatar)

        } else {
            binding.tvStateCall.text = getString(R.string.calling_group)
            binding.imgThumb2.gone()
            binding.tvNickName.gone()
        }
    }

    private fun initViewConnecting() {
        if (isFromComingCall)
            binding.tvConnecting.visible()
        else binding.tvConnecting.gone()
        Glide.with(this)
            .load(mPeerUserAvatar)
            .placeholder(R.drawable.ic_background_gradient_call)
            .error(R.drawable.ic_background_gradient_call)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
            .into(binding.imageConnecting)
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
            binding.localRender,
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
                binding.waitingCallView.visible()
                binding.viewAudioCalled.gone()
                binding.viewVideoCalled.gone()
            }

            CallStateView.CALL_VIDEO_WAITING -> {
                binding.waitingCallView.gone()
                setLocalViewFullScreen()
                binding.tvStateVideoCall.visible()
                binding.tvUserNameVideoCall.visible()
                binding.tvUserNameVideoCall.text = mGroupName
                binding.viewVideoCalled.gone()


            }
            CallStateView.CALLED_AUDIO -> {
                binding.waitingCallView.visible()
                binding.tvStateVideoCall.gone()
                binding.tvStateCall.gone()
                binding.viewAudioCalled.visible()

            }
            CallStateView.CALLED_VIDEO -> {
                setLocalFixScreen()
                binding.waitingCallView.gone()
                binding.tvStateVideoCall.gone()
                binding.tvUserNameVideoCall.gone()
                binding.viewVideoCalled.visible()
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
            callViewModel.switchAudioToVideoCall(mGroupId.toInt(), getOwnerServer())
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
            callViewModel.cancelCall(mGroupId.toInt(), getOwnerServer())
        }
    }

    private var listenerOnRemoteRenderAdd = fun(connection: JanusConnection) {
        connection.videoTrack.addRenderer(VideoRenderer(binding.remoteRender))
        startTimeInterval()
        dispatchCallStatus(true)
    }

    private var listenerOnPublisherJoined = fun() {
        runOnUiThread {
            callViewModel.onFaceTimeChange(!mIsMuteVideo)
            if (!isFromComingCall)
                Handler(mainLooper).postDelayed({
                    binding.viewConnecting.gone()
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
                binding.viewConnecting.gone()
            }, 2000)
        }
        mTimeStarted = SystemClock.elapsedRealtime()
        callViewModel.totalTimeRunJob =
            startCoroutineTimer(delayMillis = 1000, repeatMillis = 1000) {
                callViewModel.totalTimeRun += 1
                runOnUiThread {
                    binding.tvTimeCall.text = convertSecondsToHMmSs(callViewModel.totalTimeRun)
                    binding.tvVideoTimeCall.text = convertSecondsToHMmSs(callViewModel.totalTimeRun)
                }
            }
    }

    companion object {
        private var isInPeerCall = false
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
    }


    private fun setLocalViewFullScreen() {
        binding.localRender.layoutParams =
            fullView(binding.localRender.layoutParams as ConstraintLayout.LayoutParams)
    }

    private fun setLocalFixScreen() {
        binding.localRender.layoutParams =
            fixViewLocalView(binding.localRender.layoutParams as ConstraintLayout.LayoutParams)
    }

    private fun fullView(localView: ConstraintLayout.LayoutParams): ConstraintLayout.LayoutParams {
        localView.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = 0
            leftToLeft = binding.containerCall.id
            rightToRight = binding.containerCall.id
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
            endToEnd = binding.containerCall.id
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
        binding.controlCallAudioView.imgEndWaiting.gone()
        binding.controlCallAudioView.tvEndButtonDescription.gone()
        binding.tvEndButtonDescription.visible()
        binding.imgEndWaiting.visible()

        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(binding.controlCallAudioView as ConstraintLayout)
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.TOP,
            androidx.constraintlayout.widget.R.id.parent,
            ConstraintSet.TOP
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.BOTTOM,
            androidx.constraintlayout.widget.R.id.parent,
            ConstraintSet.BOTTOM
        )
        mConstraintSet.applyTo(binding.controlCallAudioView as ConstraintLayout)

        val layoutParams =
            (binding.controlCallAudioView as ConstraintLayout) as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 0.7f
        (binding.controlCallAudioView as ConstraintLayout).layoutParams= layoutParams

        val parentConstraintSet = ConstraintSet()
        parentConstraintSet.clone(binding.waitingCallView as ConstraintLayout)
        parentConstraintSet.setMargin(
            R.id.tvStateCall,
            ConstraintSet.TOP,
            resources.getDimension(R.dimen._100sdp).toInt()
        )
        parentConstraintSet.applyTo(binding.waitingCallView as ConstraintLayout)

        val avatarLayoutParams = binding.imgThumb2.layoutParams
        avatarLayoutParams.width = resources.getDimension(R.dimen._159sdp).toInt()
        avatarLayoutParams.height = resources.getDimension(R.dimen._159sdp).toInt()
        binding.imgThumb2.layoutParams = avatarLayoutParams
    }

    private fun configLandscapeLayout() {
        binding.controlCallAudioView.imgEndWaiting.visible()
        binding.controlCallAudioView.tvEndButtonDescription.visible()
        binding.tvEndButtonDescription.gone()
        binding.imgEndWaiting.gone()

        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(binding.controlCallAudioView as ConstraintLayout)
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.END,
            androidx.constraintlayout.widget.R.id.parent,
            ConstraintSet.END
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.START,
            androidx.constraintlayout.widget.R.id.parent,
            ConstraintSet.START
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.BOTTOM,
            androidx.constraintlayout.widget.R.id.parent,
            ConstraintSet.BOTTOM
        )
        mConstraintSet.applyTo(binding.controlCallAudioView as ConstraintLayout)

        val layoutParams =
            (binding.controlCallAudioView as ConstraintLayout).layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 1f
        (binding.controlCallAudioView as ConstraintLayout).layoutParams = layoutParams

        val parentConstraintSet = ConstraintSet()
        parentConstraintSet.clone(binding.waitingCallView as ConstraintLayout)
        parentConstraintSet.setMargin(
            R.id.tvStateCall,
            ConstraintSet.TOP,
            resources.getDimension(R.dimen._20sdp).toInt()
        )
        parentConstraintSet.applyTo(binding.waitingCallView as ConstraintLayout)

        val avatarLayoutParams = binding.imgThumb2.layoutParams
        avatarLayoutParams.width = resources.getDimension(R.dimen._100sdp).toInt()
        avatarLayoutParams.height = resources.getDimension(R.dimen._100sdp).toInt()
        binding.imgThumb2.layoutParams = avatarLayoutParams
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
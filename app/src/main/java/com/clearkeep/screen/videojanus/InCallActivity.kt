package com.clearkeep.screen.videojanus

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clearkeep.R
import com.clearkeep.databinding.ActivityInCallBinding
import com.clearkeep.services.CallState
import com.clearkeep.services.InCallForegroundService
import com.clearkeep.screen.videojanus.common.AvatarImageTask
import com.clearkeep.utilities.*
import org.webrtc.VideoRenderer
import org.webrtc.VideoRenderer.*
import org.webrtc.VideoRendererGui
import org.webrtc.VideoTrack
import java.math.BigInteger
import java.util.*


class InCallActivity : Activity(), View.OnClickListener, InCallForegroundService.CallListener {
    private var mIsMute = false
    private var mIsMuteVideo = false
    private var mIsSpeaker = false
    private var mIsSurfaceCreated = false
    private var mBound = false
    private var mService: InCallForegroundService? = null

    private lateinit var binding: ActivityInCallBinding

    // surface and render
    private var localRender: VideoRenderer? = null
    private var localCallBackRender: Callbacks? = null
    private val remoteRenders: MutableMap<BigInteger, Callbacks> = HashMap()

    // resource id
    private var mResIdSpeakerOn = 0
    private var mResIdSpeakerOff = 0
    private var mResIdMuteOn = 0
    private var mResIdMuteOff = 0
    private var mResIdMuteVideoOn = 0
    private var mResIdMuteVideoOff = 0

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
                className: ComponentName,
                service: IBinder
        ) {
            val binder = service as InCallForegroundService.LocalBinder
            mService = binder.service
            mService!!.registerCallListener(this@InCallActivity)
            mBound = true

            updateMediaUIWithService()
            startCall()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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

        initViews()
        requestCallPermissions()
    }

    private fun initViews() {
        binding.imgBack.setOnClickListener(this)
        binding.imgSpeaker.setOnClickListener(this)
        binding.imgMute.setOnClickListener(this)
        binding.imgEnd.setOnClickListener(this)
        binding.imgSwitchCamera.setOnClickListener(this)
        binding.imgVideoMute.setOnClickListener(this)

        binding.glview.preserveEGLContextOnPause = true
        binding.glview.keepScreenOn = true
        /*binding.glview.setWillNotDraw(false)*/
        VideoRendererGui.setView(binding.glview) {
            mIsSurfaceCreated = true
            startCall()
        }

        val userNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        val avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        if (!TextUtils.isEmpty(userNameInConversation)) {
            binding.tvUserName.text = userNameInConversation
        }
        if (!TextUtils.isEmpty(avatarInConversation)) {
            AvatarImageTask(binding.imgThumb).execute(avatarInConversation)
        }
    }

    fun requestCallPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                    )
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS
                    )
                            != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(
                        this, arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                ),
                        REQUEST_PERMISSIONS
                )
                return
            }
        }
        onCallPermissionsAvailable()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS &&
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCallPermissionsAvailable()
        } else {
            finishAndRemoveFromTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (binding.chronometer.visibility == View.VISIBLE) {
            binding.chronometer.stop()
        }
        unBindCallService()
    }

    private fun onCallPermissionsAvailable() {
        if (!isServiceRunning(this, InCallForegroundService::class.java.name)) {
            startServiceAsForeground()
        }
        bindCallService()
    }

    private fun startCall() {
        if (mIsSurfaceCreated && mBound) {
            /*val remoteStreams = mService!!.getRemoteStreams()
            remoteStreams.forEach { (id, remoteTrack) ->
                val remoteRender: Callbacks = createVideoRender()
                remoteRenders[id] = remoteRender
                remoteTrack.addRenderer(VideoRenderer(remoteRender))
            }
            updateRenderPosition(mService!!.getLocalTrack())*/

            val currentState = mService!!.getCurrentState()
            if (CallState.ANSWERED != currentState) {
                val con = VideoRendererGui.getEGLContext()
                mService?.startCall(con)
            }
        }
    }

    private fun updateMediaUIWithService() {
        if (mService != null) {
            mIsMute = mService!!.isMicMuting()
            mIsMuteVideo = mService!!.isVideoMuting()
            mIsSpeaker = mService!!.isSpeakerOn()
            enableMute(mIsMute)
            enableMuteVideo(mIsMuteVideo)
            enableSpeaker(mIsSpeaker)
        }
    }

    private fun displayCountUpClockOfConversation() {
        binding.chronometer.visibility = View.VISIBLE
        binding.tvCallState.visibility = View.GONE
    }

    override fun onCallStateChanged(status: String, state: CallState) {
        if (CallState.CALLING == state || CallState.RINGING == state) {
            runOnUiThread { binding.tvCallState.text = status }
        } else if (CallState.BUSY == state || CallState.ENDED == state || CallState.CALL_NOT_READY == state) {
            runOnUiThread { binding.tvCallState.text = status }
            finishAndRemoveFromTask()
        } else if (CallState.ANSWERED == state) {
            if (mService != null) {
                runOnUiThread {
                    displayCountUpClockOfConversation()
                    val lastTimeConnectedCall = mService!!.getLastTimeConnectedCall()
                    if (lastTimeConnectedCall > 0) {
                        binding.chronometer.base = lastTimeConnectedCall
                        binding.chronometer.start()
                    }
                }
            }
        }
    }

    override fun onLocalStream() {
        updateRenderPosition()
    }

    override fun onRemoteStreamAdd(
            remoteTrack: VideoTrack,
            remoteClientId: BigInteger
    ) {
        Log.i("Test", "onRemoteStreamAdd $remoteClientId")
        val oldRemoteRender = remoteRenders[remoteClientId]
        if (oldRemoteRender != null) {
            VideoRendererGui.remove(oldRemoteRender)
        }

        @Suppress("INACCESSIBLE_TYPE")
        val remoteRender: Callbacks = createVideoRender()
        remoteRenders[remoteClientId] = remoteRender
        remoteTrack.addRenderer(VideoRenderer(remoteRender))

        updateRenderPosition()
    }

    private fun createVideoRender(): Callbacks {
        return VideoRendererGui.create(
                25,
                25,
                25,
                25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                false
        )
    }

    override fun onStreamRemoved(remoteClientId: BigInteger) {
        Log.i("Test", "onStreamRemoved $remoteClientId")
        val render = remoteRenders.remove(remoteClientId)
        if (render != null) {
            VideoRendererGui.remove(render)
        }

        if (remoteRenders.isNotEmpty()) {
            updateRenderPosition()
        }
    }

    private fun updateRenderPosition() {
        val list: List<Callbacks> = ArrayList(remoteRenders.values)
        val isShowAvatar = list.isEmpty()
        showOrHideAvatar(isShowAvatar)
        Log.i("Test", "updateRenderPosition, list = ${list.size}")

        when (list.size) {
            1 -> {
                VideoRendererGui.update(list[0], 0, 0, 100, 100,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
            }
            2 -> {
                VideoRendererGui.update(list[0], 0, 0, 100, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
                VideoRendererGui.update(list[1], 0, 50, 100, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
            }
            3 -> {
                VideoRendererGui.update(list[0], 0, 0, 50, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
                VideoRendererGui.update(list[1], 0, 50, 50, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
                VideoRendererGui.update(list[2], 50, 50, 50, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
            }
        }

        updateLocalRender(mIsMuteVideo)
    }

    private fun updateLocalRender(isMuteLocal: Boolean) {
        val localTrack: VideoTrack = mService?.getLocalTrack() ?: return

        if (localCallBackRender != null) {
            VideoRendererGui.remove(localCallBackRender)
        }

        if (localRender != null) {
            localTrack.removeRenderer(localRender)
        }

        if (isMuteLocal) {
            Log.i("Test", "updateLocalRender, removed local render")
            return
        }

        if (remoteRenders.isEmpty()) {
            Log.i("Test", "updateLocalRender, create local as full")
            @Suppress("INACCESSIBLE_TYPE")
            localCallBackRender = VideoRendererGui.create(
                0,
                0,
                100,
                100,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                false
            )
        } else {
            Log.i("Test", "updateLocalRender, create local as small")
            @Suppress("INACCESSIBLE_TYPE")
            localCallBackRender = VideoRendererGui.create(
                0,
                0,
                25,
                25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                false
            )
        }
        localRender = VideoRenderer(localCallBackRender)
        localTrack.addRenderer(localRender)
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

    override fun onUserLeaveHint() {
        if (hasSupportPIP()) {
            enterPictureInPictureMode()
        }
    }

    private fun hasSupportPIP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,
                                               newConfig: Configuration) {
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
                mService?.hangup()
                finishAndRemoveFromTask()
            }
            R.id.imgBack -> {
                if (hasSupportPIP()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val aspectRatio = Rational(3, 4)
                        val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                        enterPictureInPictureMode(params)
                    } else {
                        enterPictureInPictureMode()
                    }
                } else {
                    mService?.hangup()
                    finishAndRemoveFromTask()
                }
            }
            R.id.imgSpeaker -> {
                mIsSpeaker = !mIsSpeaker
                enableSpeaker(mIsSpeaker)
                mService?.setSpeakerphoneOn(mIsSpeaker)
            }
            R.id.imgMute -> {
                mIsMute = !mIsMute
                enableMute(mIsMute)
                mService?.muteMic(mIsMute)
            }
            R.id.imgSwitchCamera -> {
                mService?.switchCamera()
            }
            R.id.imgVideoMute -> {
                mIsMuteVideo = !mIsMuteVideo
                enableMuteVideo(mIsMuteVideo)
                /*updateLocalRender(mIsMuteVideo)
                binding.glview.postInvalidate()*/
                mService?.muteVideo(mIsMuteVideo)
            }
        }
    }

    private fun enableSpeaker(isEnable: Boolean) {
        binding.imgSpeaker.isClickable = true
        if (isEnable) {
            binding.imgSpeaker.setImageResource(mResIdSpeakerOn)
        } else {
            binding.imgSpeaker.setImageResource(mResIdSpeakerOff)
        }
    }

    private fun enableMute(isMuting: Boolean) {
        binding.imgMute.isClickable = true
        if (isMuting) {
            binding.imgMute.setImageResource(mResIdMuteOn)
        } else {
            binding.imgMute.setImageResource(mResIdMuteOff)
        }
    }

    private fun enableMuteVideo(isMuteVideo: Boolean) {
        binding.imgVideoMute.isClickable = true
        if (isMuteVideo) {
            binding.imgVideoMute.setImageResource(mResIdMuteVideoOff)
        } else {
            binding.imgVideoMute.setImageResource(mResIdMuteVideoOn)
        }
    }

    private fun finishAndRemoveFromTask() {
        unBindCallService()
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun bindCallService() {
        val intent = Intent(this, InCallForegroundService::class.java)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    private fun unBindCallService() {
        if (mBound) {
            mService!!.unregisterCallListener(this@InCallActivity)
            unbindService(connection)
            mBound = false
        }
    }

    private fun startServiceAsForeground() {
        val isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)
        val userNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        val avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        val callId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        val ourClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID)
        val token = intent.getStringExtra(EXTRA_GROUP_TOKEN)

        val serviceIntent = Intent(applicationContext, InCallForegroundService::class.java)
        serviceIntent.putExtra(EXTRA_FROM_IN_COMING_CALL, isFromComingCall)
        serviceIntent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatarInConversation)
        serviceIntent.putExtra(EXTRA_USER_NAME, userNameInConversation)
        serviceIntent.putExtra(EXTRA_GROUP_ID, callId)
        serviceIntent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        serviceIntent.putExtra(EXTRA_GROUP_TOKEN, token)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
    }
}
package com.clearkeep.januswrapper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.util.Rational
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clearkeep.januswrapper.common.AvatarImageTask
import com.clearkeep.januswrapper.databinding.ActivityInCallBinding
import com.clearkeep.januswrapper.services.CallState
import com.clearkeep.januswrapper.services.InCallForegroundService
import com.clearkeep.januswrapper.services.InCallForegroundService.*
import com.clearkeep.januswrapper.utils.Constants
import com.clearkeep.januswrapper.utils.Utils
import org.webrtc.VideoRenderer
import org.webrtc.VideoRenderer.*
import org.webrtc.VideoRendererGui
import org.webrtc.VideoTrack
import java.math.BigInteger
import java.util.*


class InCallActivity : Activity(), View.OnClickListener, CallListener {
    private var mIsMute = false
    private var mIsSpeaker = false
    private var mIsSurfaceCreated = false
    private var mBound = false
    private var mService: InCallForegroundService? = null

    private lateinit var binding: ActivityInCallBinding

    // surface and render
    private var localRender: Callbacks? = null
    private val remoteRenders: MutableMap<BigInteger, Callbacks> = HashMap()

    // resource id
    private var mResIdSpeakerOn = 0
    private var mResIdSpeakerOff = 0
    private var mResIdMuteOn = 0
    private var mResIdMuteOff = 0

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
                className: ComponentName,
                service: IBinder
        ) {
            Log.i("Test", "onServiceConnected")
            val binder = service as LocalBinder
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

        val a: TypedArray = theme.obtainStyledAttributes(
                intArrayOf(
                        R.attr.buttonSpeakerOn, R.attr.buttonSpeakerOff,
                        R.attr.buttonMuteOn, R.attr.buttonMuteOff
                )
        )
        try {
            mResIdSpeakerOn = a.getResourceId(0, R.drawable.ic_string_ee_sound_on)
            mResIdSpeakerOff = a.getResourceId(1, R.drawable.ic_string_ee_sound_off)
            mResIdMuteOn = a.getResourceId(2, R.drawable.ic_string_ee_mute_on)
            mResIdMuteOff = a.getResourceId(3, R.drawable.ic_string_ee_mute_off)
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

        binding.glview.preserveEGLContextOnPause = true
        binding.glview.keepScreenOn = true
        VideoRendererGui.setView(binding.glview) {
            mIsSurfaceCreated = true
            startCall()
        }

        val userNameInConversation = intent.getStringExtra(Constants.EXTRA_USER_NAME)
        val avatarInConversation = intent.getStringExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION)
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
        if (!Utils.isServiceRunning(this, InCallForegroundService::class.java.name)) {
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
            mIsMute = mService!!.isMuting()
            mIsSpeaker = mService!!.isSpeakerOn()
            enableMute(mIsMute)
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
                    Log.i("Test", "lastTimeConnectedCall=$lastTimeConnectedCall")
                    if (lastTimeConnectedCall > 0) {
                        binding.chronometer.base = lastTimeConnectedCall
                        binding.chronometer.start()
                    }
                }
            }
        }
    }

    override fun onLocalStream(localTrack: VideoTrack?, remoteTracks: List<VideoTrack>?) {
        updateRenderPosition(localTrack)
    }

    override fun onRemoteStreamAdd(
            localTrack: VideoTrack?,
            remoteTrack: VideoTrack,
            remoteClientId: BigInteger
    ) {
        Log.i("Test", "onRemoteStreamAdd $remoteClientId")
        val oldRemoteRender = remoteRenders[remoteClientId]
        if (oldRemoteRender == null) {
            @Suppress("INACCESSIBLE_TYPE")
            val remoteRender: Callbacks = createVideoRender()
            remoteRenders[remoteClientId] = remoteRender
            remoteTrack.addRenderer(VideoRenderer(remoteRender))
        }
        updateRenderPosition(localTrack)
    }

    private fun createVideoRender(): Callbacks {
        return VideoRendererGui.create(
                25,
                25,
                25,
                25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                true
        )
    }

    override fun onStreamRemoved(localTrack: VideoTrack?, remoteClientId: BigInteger) {
        Log.i("Test", "onStreamRemoved $remoteClientId")
        val render = remoteRenders.remove(remoteClientId)
        if (render != null) {
            VideoRendererGui.remove(render)
        }

        if (remoteRenders.isNotEmpty()) {
            updateRenderPosition(localTrack)
        }
    }

    private fun updateRenderPosition(localTrack: VideoTrack?) {
        if (localRender != null) {
            VideoRendererGui.remove(localRender)
        }

        val list: List<Callbacks> = ArrayList(remoteRenders.values)
        val isShowAvatar = list.isEmpty()
        showOrHideAvatar(isShowAvatar)
        Log.i("Test", "updateRenderPosition, list = ${list.size}")

        when (list.size) {
            1 -> {
                VideoRendererGui.update(list[0], 0, 0, 100, 100,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            }
            2 -> {
                VideoRendererGui.update(list[0], 0, 0, 100, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
                VideoRendererGui.update(list[1], 0, 50, 100, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            }
            3 -> {
                VideoRendererGui.update(list[0], 0, 0, 50, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
                VideoRendererGui.update(list[1], 0, 50, 50, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
                VideoRendererGui.update(list[2], 50, 50, 50, 50,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            }
        }

        if (list.isEmpty()) {
            @Suppress("INACCESSIBLE_TYPE")
            localRender = VideoRendererGui.create(
                    0,
                    0,
                    100,
                    100,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                    false
            )
        } else {
            @Suppress("INACCESSIBLE_TYPE")
            localRender = VideoRendererGui.create(
                    0,
                    0,
                    25,
                    25,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,
                    false
            )
        }
        localTrack?.addRenderer(VideoRenderer(localRender))
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enterPictureInPictureMode()
        }
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
                /*finishAndRemoveFromTask()*/
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        /*val metrics = windowManager.currentWindowMetrics
                        val size = metrics.bounds
                        val width: Int = size.width()
                        val height: Int = size.height()*/
                        val aspectRatio = Rational(3, 4)
                        val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                        enterPictureInPictureMode(params)
                    } else {
                        enterPictureInPictureMode()
                    }
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
                mService?.mute(mIsMute)
            }
            R.id.imgSwitchCamera -> {
                mService?.switchCamera()
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

    private fun enableMute(isEnable: Boolean) {
        binding.imgMute.isClickable = true
        if (isEnable) {
            binding.imgMute.setImageResource(mResIdMuteOn)
        } else {
            binding.imgMute.setImageResource(mResIdMuteOff)
        }
    }

    private fun finishAndRemoveFromTask() {
        if (localRender != null) {
            VideoRendererGui.remove(localRender)
        }
        val list: List<Callbacks> = ArrayList(remoteRenders.values)
        for (render in list) {
            VideoRendererGui.remove(render)
        }
        remoteRenders.clear()
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
        val isFromComingCall = intent.getBooleanExtra(Constants.EXTRA_FROM_IN_COMING_CALL, false)
        val userNameInConversation = intent.getStringExtra(Constants.EXTRA_USER_NAME)
        val avatarInConversation = intent.getStringExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION)
        val callId = intent.getLongExtra(Constants.EXTRA_GROUP_ID, 0)
        val ourClientId = intent.getStringExtra(Constants.EXTRA_OUR_CLIENT_ID)
        val token = intent.getStringExtra(Constants.EXTRA_GROUP_TOKEN)

        val serviceIntent = Intent(applicationContext, InCallForegroundService::class.java)
        serviceIntent.putExtra(Constants.EXTRA_FROM_IN_COMING_CALL, isFromComingCall)
        serviceIntent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, avatarInConversation)
        serviceIntent.putExtra(Constants.EXTRA_USER_NAME, userNameInConversation)
        serviceIntent.putExtra(Constants.EXTRA_GROUP_ID, callId)
        serviceIntent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, ourClientId)
        serviceIntent.putExtra(Constants.EXTRA_GROUP_TOKEN, token)
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
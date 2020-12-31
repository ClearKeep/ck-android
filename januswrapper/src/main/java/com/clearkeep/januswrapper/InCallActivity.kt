package com.clearkeep.januswrapper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.View
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
    // input
    private var mFromComingCall = false
    private var mUserNameInConversation: String? = null
    private var mAvatarInConversation: String? = null

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
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mFromComingCall = intent.getBooleanExtra(Constants.EXTRA_FROM_IN_COMING_CALL, false)
        mUserNameInConversation = intent.getStringExtra(Constants.EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION)

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

        if (!TextUtils.isEmpty(mUserNameInConversation)) {
            binding.tvUserName.text = mUserNameInConversation
        }
        if (!TextUtils.isEmpty(mAvatarInConversation)) {
            AvatarImageTask(binding.imgThumb).execute(mAvatarInConversation)
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
            val currentState = mService!!.getCurrentState()
            if (CallState.ANSWERED != currentState) {
                val con = VideoRendererGui.getEGLContext()
                if (mFromComingCall) {
                    mService?.answer(con)
                } else {
                    mService?.makeCall(con)
                }
            }
        }
    }

    private fun updateMediaUIWithService() {
        if (mService != null) {
            mIsMute = mService!!.isMuting()
            mIsSpeaker = mService!!.isSpeakerOn()
            enableMute(mIsMute)
            enableSpeaker(mIsSpeaker)

            val currentState = mService!!.getCurrentState()
            if (CallState.ANSWERED == currentState) {
                val remoteStreams = mService!!.getRemoteStreams()
                remoteStreams.forEach {(id, remoteTrack) ->
                    val remoteRender: Callbacks = createVideoRender()
                    remoteRenders[id] = remoteRender
                    remoteTrack.addRenderer(VideoRenderer(remoteRender))
                }
                updateRenderPosition(mService!!.getLocalTrack())
            }
        }
    }

    private fun displayCountUpClockOfConversation() {
        binding.chronometer.visibility = View.VISIBLE
        binding.tvCallState.visibility = View.GONE
    }

    override fun onCallStateChanged(status: String, callState: CallState) {
        if (CallState.CALLING == callState || CallState.RINGING == callState) {
            runOnUiThread { binding.tvCallState.text = status }
        } else if (CallState.BUSY == callState || CallState.ENDED == callState || CallState.CALL_NOT_READY == callState) {
            runOnUiThread { binding.tvCallState.text = status }
            finishAndRemoveFromTask()
        } else if (CallState.ANSWERED == callState) {
            if (mService != null) {
                runOnUiThread {
                    displayCountUpClockOfConversation()
                    val lastTimeConnectedCall = mService!!.getLastTimeConnectedCall()
                    Log.i("Test", "lastTimeConnectedCall=$lastTimeConnectedCall");
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

        if (list.size == 1) {
            VideoRendererGui.update(list[0], 0, 25, 25, 25,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
        } else if (list.size == 2) {
            VideoRendererGui.update(list[0], 0, 25, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            VideoRendererGui.update(list[1], 25, 25, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
        } else if (list.size == 3) {
            VideoRendererGui.update(list[0], 0, 25, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            VideoRendererGui.update(list[1], 25, 25, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            VideoRendererGui.update(list[2], 50, 25, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
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

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgEnd -> {
                mService?.hangup()
                finishAndRemoveFromTask()
            }
            R.id.imgBack -> {
                finishAndRemoveFromTask()
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
        val intent = Intent(applicationContext, InCallForegroundService::class.java)
        intent.putExtra(Constants.EXTRA_FROM_IN_COMING_CALL, mFromComingCall)
        intent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, mAvatarInConversation)
        intent.putExtra(Constants.EXTRA_USER_NAME, mUserNameInConversation)
        val callId = getIntent().getLongExtra(Constants.EXTRA_GROUP_ID, 0)
        intent.putExtra(Constants.EXTRA_GROUP_ID, callId)
        val ourClientId = getIntent().getStringExtra(Constants.EXTRA_OUR_CLIENT_ID)
        intent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, ourClientId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
    }
}
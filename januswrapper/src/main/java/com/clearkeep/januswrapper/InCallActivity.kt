package com.clearkeep.januswrapper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.View
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clearkeep.januswrapper.common.AvatarImageTask
import com.clearkeep.januswrapper.services.InCallForegroundService
import com.clearkeep.januswrapper.services.InCallForegroundService.*
import com.clearkeep.januswrapper.utils.Constants
import com.clearkeep.januswrapper.utils.Utils
import de.hdodenhof.circleimageview.CircleImageView
import org.webrtc.VideoRenderer
import org.webrtc.VideoRendererGui
import org.webrtc.VideoTrack
import java.math.BigInteger
import java.util.*

class InCallActivity : Activity(), View.OnClickListener, CallListener {
    private var mIsMute = false
    private var mIsSpeaker = false
    private var mBound = false
    private var mFromComingCall = false
    private var mService: InCallForegroundService? = null
    private var mUserNameInConversation: String? = null
    private var mAvatarInConversation: String? = null

    private lateinit var imgSpeaker: ImageView
    private lateinit var imgMute: ImageView
    private lateinit var imgEnd: ImageView
    private lateinit var imgSwitchCamera: ImageView

    private lateinit var imgThumb: CircleImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvCallState: TextView
    private lateinit var containerUserInfo: View
    private lateinit var mChronometer: Chronometer
    private lateinit var surfaceView: GLSurfaceView
    private var localRender: VideoRenderer.Callbacks? = null
    private val remoteRenders: MutableMap<BigInteger, VideoRenderer.Callbacks> = HashMap()
    private var mIsSurfaceCreated = false

    private var mResIdSpeakerOn = 0
    private var mResIdSpeakerOff = 0
    private var mResIdMuteOn = 0
    private var mResIdMuteOff = 0

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            val binder = service as LocalBinder
            mService = binder.service
            mService!!.registerCallListener(this@InCallActivity)
            mBound = true
            updateMediaUI()
            executeCall()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call)

        val a: TypedArray = theme.obtainStyledAttributes(intArrayOf(R.attr.buttonSpeakerOn, R.attr.buttonSpeakerOff,
                R.attr.buttonMuteOn, R.attr.buttonMuteOff))
        try {
            mResIdSpeakerOn = a.getResourceId(0, R.drawable.ic_string_ee_sound_on)
            mResIdSpeakerOff = a.getResourceId(1, R.drawable.ic_string_ee_sound_off)
            mResIdMuteOn = a.getResourceId(2, R.drawable.ic_string_ee_mute_on)
            mResIdMuteOff = a.getResourceId(3, R.drawable.ic_string_ee_mute_off)
        } finally {
            a.recycle()
        }

        mFromComingCall = intent.getBooleanExtra(Constants.EXTRA_FROM_IN_COMING_CALL, false)
        mUserNameInConversation = intent.getStringExtra(Constants.EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION)
        initViews()
        requestCallPermissions()
    }

    fun requestCallPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS &&
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCallPermissionsAvailable()
        } else {
            finishAndRemoveFromTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mChronometer.visibility == View.VISIBLE) {
            mChronometer.stop()
        }
        unBindCallService()
    }

    private fun onCallPermissionsAvailable() {
        if (!Utils.isServiceRunning(this, InCallForegroundService::class.java.name)) {
            startServiceAsForeground()
        }
        bindCallService()
    }

    private fun initViews() {
        val imgBack = findViewById<ImageView>(R.id.imgBack)
        imgSpeaker = findViewById(R.id.imgSpeaker)
        imgMute = findViewById(R.id.imgMute)
        imgEnd = findViewById(R.id.imgEnd)
        imgSwitchCamera = findViewById(R.id.imgSwitchCamera)

        imgThumb = findViewById(R.id.imgThumb)
        tvCallState = findViewById(R.id.tvCallState)
        containerUserInfo = findViewById(R.id.containerUserInfo)
        tvUserName = findViewById(R.id.tvUserName)
        mChronometer = findViewById(R.id.chronometer)
        imgBack.setOnClickListener(this)

        imgSpeaker.setOnClickListener(this)
        imgMute.setOnClickListener(this)
        imgEnd.setOnClickListener(this)
        imgSwitchCamera.setOnClickListener(this)

        updateConversationInformation()
        surfaceView = findViewById(R.id.glview)
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.keepScreenOn = true
        VideoRendererGui.setView(surfaceView) {
            mIsSurfaceCreated = true
            executeCall()
        }
    }

    private fun executeCall() {
        if (mIsSurfaceCreated && mBound) {
            val con = VideoRendererGui.getEGLContext()
            mService!!.executeMakeCall(con)
        }
    }

    private fun updateConversationInformation() {
        if (!TextUtils.isEmpty(mUserNameInConversation)) {
            tvUserName.text = mUserNameInConversation
        }
        if (!TextUtils.isEmpty(mAvatarInConversation)) {
            AvatarImageTask(imgThumb).execute(mAvatarInConversation)
        }
    }

    private fun updateMediaUI() {
        /*if (mService != null) {
            val currentState = mService.getCurrentState()
            if (CallState.ANSWERED == currentState) {
                displayUiAsInConversation()
            }
        }*/
    }

    private fun displayCountUpClockOfConversation() {
        mChronometer.visibility = View.VISIBLE
        tvCallState.visibility = View.GONE
    }

    override fun onCallStateChanged(status: String?, callState: CallState) {
        if (CallState.CALLING == callState || CallState.RINGING == callState) {
            runOnUiThread { tvCallState.text = status }
        } else if (CallState.BUSY == callState || CallState.ENDED == callState || CallState.CALL_NOT_READY == callState) {
            runOnUiThread { tvCallState.text = status }
            finishAndRemoveFromTask()
        } else if (CallState.ANSWERED == callState) {
            if (mService != null) {
                runOnUiThread {
                    /*if (mFromComingCall) {
                        displayUiAsInConversation();
                    }*/displayCountUpClockOfConversation()
                }
            }
        }
    }

    override fun onLocalStream(localTrack: VideoTrack?, remoteTracks: List<VideoTrack>?) {
        updateRenderPosition(localTrack)
    }

    override fun onRemoteStreamAdd(localTrack: VideoTrack?, remoteTrack: VideoTrack, remoteClientId: BigInteger) {
        val oldRemoteRender = remoteRenders[remoteClientId]
        if (oldRemoteRender == null) {
            val remoteRender: VideoRenderer.Callbacks = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            remoteRenders[remoteClientId] = remoteRender
            remoteTrack.addRenderer(VideoRenderer(remoteRender))
        }
        updateRenderPosition(localTrack)
    }

    override fun onStreamRemoved(localTrack: VideoTrack?, remoteClientId: BigInteger) {
        val render = remoteRenders.remove(remoteClientId)
        if (render != null) {
            VideoRendererGui.remove(render)
        }
        if (remoteRenders.isEmpty()) {
            mService?.hangup()
            finishAndRemoveFromTask()
            return
        }
        updateRenderPosition(localTrack)
    }

    private fun updateRenderPosition(localTrack: VideoTrack?) {
        val list: List<VideoRenderer.Callbacks> = ArrayList(remoteRenders.values)
        val isShowAvatar = list.isEmpty()
        showOrHideAvatar(isShowAvatar)
        if (list.size == 1) {
            VideoRendererGui.update(list[0], 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
        } else if (list.size == 2) {
            VideoRendererGui.update(list[0], 0, 0, 100, 50, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
            VideoRendererGui.update(list[1], 0, 50, 100, 50, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)
        }
        if (localRender != null) {
            VideoRendererGui.remove(localRender)
        }
        localRender = VideoRendererGui.create(5, 5, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
        localTrack!!.addRenderer(VideoRenderer(localRender))
    }

    private fun showOrHideAvatar(isShowAvatar: Boolean) {
        runOnUiThread {
            if (isShowAvatar) {
                containerUserInfo.visibility = View.VISIBLE
            } else {
                containerUserInfo.visibility = View.GONE
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgEnd -> {
                if (mService != null) {
                    mService!!.hangup()
                }
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
        if (isEnable) {
            imgSpeaker.setImageResource(mResIdSpeakerOn)
        } else {
            imgSpeaker.setImageResource(mResIdSpeakerOff)
        }
    }

    private fun enableMute(isEnable: Boolean) {
        if (isEnable) {
            imgMute.setImageResource(mResIdMuteOn)
        } else {
            imgMute.setImageResource(mResIdMuteOff)
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
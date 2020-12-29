package com.clearkeep.januswrapper.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.opengl.EGLContext
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.clearkeep.januswrapper.InCallActivity
import com.clearkeep.januswrapper.R
import com.clearkeep.januswrapper.utils.Constants
import computician.janusclientapi.*
import org.json.JSONObject
import org.webrtc.MediaStream
import org.webrtc.PeerConnection.IceServer
import org.webrtc.VideoTrack
import java.math.BigInteger
import java.util.*

class InCallForegroundService : Service() {
    enum class CallState {
        CALLING, RINGING, ANSWERED, BUSY, ENDED, CALL_NOT_READY
    }

    interface CallListener {
        fun onCallStateChanged(status: String?, state: CallState)
        fun onLocalStream(localTrack: VideoTrack?, remoteTracks: List<VideoTrack>?)
        fun onRemoteStreamAdd(localTrack: VideoTrack?, remoteTrack: VideoTrack, remoteClientId: BigInteger)
        fun onStreamRemoved(localTrack: VideoTrack?, remoteClientId: BigInteger)
    }

    private val mBinder: IBinder = LocalBinder()
    private var mIsInComingCall = false
    private var mAvatarInConversation: String? = null
    private var mUserNameInConversation: String? = null
    private var mIsSpeakerOn = false
    private var mIsMuting = false

    var mGroupId: Long? = null
    var mOurClientId: String? = null
    private var mCurrentCallStatus: String? = null
    var currentState: CallState? = null
        private set

    // janus
    private var janusServer: JanusServer? = null
    private var mListener: CallListener? = null
    private var localStream: MediaStream? = null
    private var localTrack: VideoTrack? = null
    private val remoteStreams: MutableMap<BigInteger, VideoTrack> = HashMap()

    inner class LocalBinder : Binder() {
        val service: InCallForegroundService
            get() = this@InCallForegroundService
    }

    private val mEndCallReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            hangup()
        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        mCurrentCallStatus = getString(R.string.text_calling)
        currentState = CallState.CALLING
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_END_CALL)
        registerReceiver(mEndCallReceiver, intentFilter)

        setSpeakerphoneOn(false)
        mute(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unregisterReceiver(mEndCallReceiver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        mIsInComingCall = intent.getBooleanExtra(Constants.EXTRA_FROM_IN_COMING_CALL, false)
        mUserNameInConversation = intent.getStringExtra(Constants.EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION)
        /*mGroupId = intent.getStringExtra(EXTRA_GROUP_ID);*/
        mGroupId = java.lang.Long.valueOf(1223)
        mOurClientId = intent.getStringExtra(Constants.EXTRA_OUR_CLIENT_ID)
        notifyStartForeground(getString(R.string.text_notification_calling), mCurrentCallStatus)
        return START_NOT_STICKY
    }

    fun executeMakeCall(con: EGLContext?) {
        Log.i(TAG, "executeMakeCall, groupID = $mGroupId, our client id = $mOurClientId")
        if (janusServer != null) {
            return
        }
        janusServer = JanusServer(JanusGatewayCallbacksImpl())
        janusServer!!.initializeMediaContext(this, true, true, true, con)
        janusServer!!.Connect()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun setSpeakerphoneOn(isOn: Boolean) {
        mIsSpeakerOn = isOn
        val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = isOn
    }

    fun mute(isMute: Boolean) {
        mIsMuting = isMute
        localStream?.audioTracks?.get(0)?.setEnabled(!isMute)
    }

    fun hangup() {
        if (janusServer != null) {
            janusServer!!.Destroy()
        }
        stopSelf()
    }

    fun registerCallListener(listener: CallListener) {
        mListener = listener
    }

    fun unregisterCallListener(listener: CallListener) {
        mListener = null
    }

    private fun notifyStartForeground(title: String?, content: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID,
                    "channel_voice", NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent()
        notificationIntent.putExtra("is_from_notification", true)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationIntent.putExtra(Constants.EXTRA_FROM_IN_COMING_CALL, mIsInComingCall)
        notificationIntent.putExtra(Constants.EXTRA_USER_NAME, mUserNameInConversation)
        notificationIntent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, mAvatarInConversation)
        if (mIsInComingCall) {
            notificationIntent.putExtra(Constants.EXTRA_GROUP_ID, mGroupId)
            notificationIntent.setClassName(packageName, InCallActivity::class.java.name)
        } else {
            notificationIntent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, mOurClientId)
            notificationIntent.setClassName(packageName, InCallActivity::class.java.name)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val pi = PendingIntent.getBroadcast(this, 0,
                Intent(ACTION_END_CALL), 0)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notify_small)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_close_call, "End Call", pi)
                .setShowWhen(false)
                .setAutoCancel(false)
        startForeground(NOTIFICATION_ID, builder.build())
    }

    internal inner class ListenerAttachCallbacks(private val groupId: Long?, private val mRemoteClientId: BigInteger) : IJanusPluginCallbacks {
        private var listenerHandle
        : JanusPluginHandle? = null
        override fun success(handle: JanusPluginHandle) {
            listenerHandle = handle
            try {
                val body = JSONObject()
                val msg = JSONObject()
                body.put(Constants.REQUEST, "join")
                body.put("room", groupId)
                body.put("ptype", "listener")
                body.put("feed", mRemoteClientId)
                msg.put(Constants.MESSAGE, body)
                handle.sendMessage(PluginHandleSendMessageCallbacks(msg))
            } catch (ex: Exception) {
                Log.e("Test", ex.toString())
            }
        }

        override fun onMessage(msg: JSONObject, jsep: JSONObject?) {
            try {
                val event = msg.getString("videoroom")
                if (event == "attached" && jsep != null) {
                    listenerHandle!!.createAnswer(object : IPluginHandleWebRTCCallbacks {
                        override fun onSuccess(obj: JSONObject) {
                            try {
                                val mymsg = JSONObject()
                                val body = JSONObject()
                                body.put(Constants.REQUEST, "start")
                                body.put("room", groupId)
                                mymsg.put(Constants.MESSAGE, body)
                                mymsg.put("jsep", obj)
                                mymsg.put("token", "a1b2c3d4")
                                listenerHandle!!.sendMessage(PluginHandleSendMessageCallbacks(mymsg))
                            } catch (ex: Exception) {
                            }
                        }

                        override fun getJsep(): JSONObject {
                            return jsep
                        }

                        override fun getMedia(): JanusMediaConstraints {
                            val cons = JanusMediaConstraints()
                            cons.video = null
                            cons.recvAudio = true
                            cons.recvVideo = true
                            cons.sendAudio = false
                            return cons
                        }

                        override fun getTrickle(): Boolean {
                            return true
                        }

                        override fun onCallbackError(error: String) {}
                    })
                }
            } catch (ex: Exception) {
                Log.e(TAG, ex.toString())
            }
        }

        override fun onLocalStream(stream: MediaStream) {}
        override fun onRemoteStream(stream: MediaStream) {
            remoteStreams[mRemoteClientId] = stream.videoTracks[0]
            mListener!!.onRemoteStreamAdd(localTrack, stream.videoTracks[0], mRemoteClientId)
        }

        override fun onDataOpen(data: Any) {}
        override fun onData(data: Any) {}
        override fun onCleanup() {}
        override fun onDetached() {}
        override fun getPlugin(): JanusSupportedPluginPackages {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM
        }

        override fun onCallbackError(error: String) {}
    }

    inner class JanusPublisherPluginCallbacks : IJanusPluginCallbacks {
        private var handle: JanusPluginHandle? = null

        override fun success(janusPluginHandle: JanusPluginHandle) {
            Log.e(TAG, "JanusPublisherPluginCallbacks, success")
            handle = janusPluginHandle
            joinRoomAsPublisher()
        }

        override fun onMessage(msg: JSONObject, jsep: JSONObject?) {
            Log.e(TAG, "JanusPublisherPluginCallbacks, onMessage: $msg")
            try {
                val event = msg.getString("videoroom")
                if (event == "joined") {
                    createOffer()
                    if (msg.has(Constants.PUBLISHERS)) {
                        val pubs = msg.getJSONArray(Constants.PUBLISHERS)
                        for (i in 0 until pubs.length()) {
                            val pub = pubs.getJSONObject(i)
                            val remoteId = BigInteger(pub.getString("id"))
                            attachRemoteClient(remoteId)
                        }
                    }
                } else if (event == "hangup") {
                    //hangup();
                } else if (event == "event") {
                    when {
                        msg.has(Constants.PUBLISHERS) -> {
                            val pubs = msg.getJSONArray(Constants.PUBLISHERS)
                            for (i in 0 until pubs.length()) {
                                val pub = pubs.getJSONObject(i)
                                attachRemoteClient(BigInteger(pub.getString("id")))
                            }
                        }
                        msg.has("leaving") -> {
                            val id = BigInteger(msg.getString("leaving"))
                            mListener!!.onStreamRemoved(localTrack, id)
                        }
                        msg.has("unpublished") -> {
                            val id = BigInteger(msg.getString("unpublished"))
                            mListener!!.onStreamRemoved(localTrack, id)
                        }
                        else -> {
                            //todo error
                        }
                    }
                }
                if (jsep != null) {
                    handle!!.handleRemoteJsep(PluginHandleWebRTCCallbacks(null, jsep, false))
                }
            } catch (ex: Exception) {
                Log.e(TAG, "JanusPublisherPluginCallbacks, onMessage: error =$ex")
            }
        }

        override fun onLocalStream(stream: MediaStream) {
            Log.e(TAG, "JanusPublisherPluginCallbacks, onLocalStream")
            localStream = stream
            localTrack = stream.videoTracks[0]
            val list: List<VideoTrack> = ArrayList(remoteStreams.values)
            mListener!!.onLocalStream(localTrack, list)
        }

        override fun onRemoteStream(stream: MediaStream) {
            Log.e(TAG, "JanusPublisherPluginCallbacks, onRemoteStream")
        }

        override fun onDataOpen(data: Any) {}
        override fun onData(data: Any) {}
        override fun onCleanup() {
            Log.e(TAG, "JanusPublisherPluginCallbacks, onCleanup")
        }

        override fun onDetached() {
            Log.e(TAG, "JanusPublisherPluginCallbacks, onDetached")
        }

        override fun getPlugin(): JanusSupportedPluginPackages {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM
        }

        override fun onCallbackError(error: String) {
            Log.e(TAG, "JanusPublisherPluginCallbacks, onCallbackError: $error")
        }

        private fun createOffer() {
            if (handle != null) {
                handle!!.createOffer(object : IPluginHandleWebRTCCallbacks {
                    override fun onSuccess(obj: JSONObject) {
                        try {
                            val msg = JSONObject()
                            val body = JSONObject()
                            body.put(Constants.REQUEST, "configure")
                            body.put("audio", true)
                            body.put("video", true)
                            msg.put(Constants.MESSAGE, body)
                            msg.put("jsep", obj)
                            handle!!.sendMessage(PluginHandleSendMessageCallbacks(msg))
                        } catch (ex: Exception) {
                            Log.e(TAG, "createOffer: $ex")
                        }
                    }

                    override fun getJsep(): JSONObject? {
                        return null
                    }

                    override fun getMedia(): JanusMediaConstraints {
                        val cons = JanusMediaConstraints()
                        cons.recvAudio = false
                        cons.recvVideo = false
                        cons.sendAudio = true
                        return cons
                    }

                    override fun getTrickle(): Boolean {
                        return true
                    }

                    override fun onCallbackError(error: String) {}
                })
            }
        }

        private fun joinRoomAsPublisher() {
            if (handle != null) {
                val obj = JSONObject()
                val msg = JSONObject()
                try {
                    obj.put(Constants.REQUEST, "join")
                    obj.put("room", mGroupId)
                    obj.put("ptype", "publisher")
                    obj.put("display", mOurClientId)
                    msg.put(Constants.MESSAGE, obj)
                } catch (ex: Exception) {
                    Log.e(TAG, "joinRoomAsPublisher: $ex")
                }
                handle!!.sendMessage(PluginHandleSendMessageCallbacks(msg))
            }
        }

        private fun attachRemoteClient(remoteId: BigInteger) {
            janusServer!!.Attach(ListenerAttachCallbacks(mGroupId, remoteId))
        }
    }

    inner class JanusGatewayCallbacksImpl : IJanusGatewayCallbacks {
        override fun onSuccess() {
            janusServer!!.Attach(JanusPublisherPluginCallbacks())
        }

        override fun onDestroy() {}
        override fun getServerUri(): String {
            return Constants.JANUS_URI
        }

        override fun getIceServers(): List<IceServer> {
            return ArrayList()
        }

        override fun getIpv6Support(): Boolean {
            return java.lang.Boolean.FALSE
        }

        override fun getMaxPollEvents(): Int {
            return 0
        }

        override fun onCallbackError(error: String) {}
    }

    companion object {
        private const val TAG = "InCallForegroundService"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "ck_call_01"
        private const val ACTION_END_CALL = "ck.action.end.call"
    }
}
package com.clearkeep.januswrapper.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.EGLContext;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.clearkeep.januswrapper.InCallActivity;
import com.clearkeep.januswrapper.R;
import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;
import computician.janusclientapi.PluginHandleWebRTCCallbacks;
import static com.clearkeep.januswrapper.utils.Constants.*;

public class InCallForegroundService extends Service {

    public enum CallState {
        CALLING,
        RINGING,
        ANSWERED,
        BUSY,
        ENDED,
        CALL_NOT_READY
    }

    public interface CallListener {
        void onCallStateChanged(String status, CallState state);
        void onLocalStream(VideoTrack videoTrack);
        void onRemoteStream(BigInteger remoteClientId, VideoTrack videoTrack);
    }

    private static final String TAG = "InCallForegroundService";

    private static final int NOTIFICATION_ID = 101;

    private static final String CHANNEL_ID = "fast_go_call_01";

    private static final String ACTION_END_CALL = "fg.action.end.call";

    private final IBinder mBinder = new LocalBinder();

    boolean mIsInComingCall;

    String mAvatarInConversation;

    String mUserNameInConversation;

    String mGroupId;

    String mOurClientId;

    private String mCurrentCallStatus;

    private CallState mCurrentCallState;

    // janus
    private JanusServer janusServer;
    private CallListener mListener;

    public class LocalBinder extends Binder {
        public InCallForegroundService getService() {
            return InCallForegroundService.this;
        }
    }

    private BroadcastReceiver mEndCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hangup();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mCurrentCallStatus = getString(R.string.text_calling);
        mCurrentCallState = CallState.CALLING;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_END_CALL);
        registerReceiver(mEndCallReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        mIsInComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false);
        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME);
        mAvatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION);
        /*mGroupId = intent.getStringExtra(EXTRA_GROUP_ID);*/
        mGroupId = "1234";
        mOurClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID);
        notifyStartForeground(getString(R.string.text_notification_calling), mCurrentCallStatus);

        return START_NOT_STICKY;
    }

    public void answer() {
    }

    public void executeMakeCall(EGLContext con) {
        Log.i(TAG, "executeMakeCall, groupID = " + mGroupId + ", our client id = " + mOurClientId);
        if (janusServer != null) {
            return;
        }
        janusServer = new JanusServer(new JanusGatewayCallbacksImpl());
        janusServer.initializeMediaContext(this, true, true, true, con);
        janusServer.Connect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // TODO
        unregisterReceiver(mEndCallReceiver);
        janusServer.Destroy();
        janusServer = null;
    }

    public void hangup() {
        /*if (mCurrentCall != null) {
            mCurrentCall.hangup();
            stopSelf();
        }*/
        stopSelf();
    }

    public CallState getCurrentState() {
        return mCurrentCallState;
    }

    public void registerCallListener(CallListener listener) {
        mListener = listener;
    }

    public void unregisterCallListener(CallListener listener) {
        mListener = null;
    }

    void notifyStartForeground(String title, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "channel_voice", NotificationManager.IMPORTANCE_HIGH);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        Intent notificationIntent = new Intent();
        notificationIntent.putExtra("is_from_notification", true);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(EXTRA_FROM_IN_COMING_CALL, mIsInComingCall);
        notificationIntent.putExtra(EXTRA_USER_NAME, mUserNameInConversation);
        notificationIntent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, mAvatarInConversation);
        if (mIsInComingCall) {
            notificationIntent.putExtra(EXTRA_GROUP_ID, mGroupId);
            notificationIntent.setClassName(getPackageName(), InCallActivity.class.getName());
        } else {
            notificationIntent.putExtra(EXTRA_OUR_CLIENT_ID, mOurClientId);
            notificationIntent.setClassName(getPackageName(), InCallActivity.class.getName());
        }
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_END_CALL), 0);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(R.drawable.ic_notify_small)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .addAction(R.drawable.ic_close_call, "End Call", pi)
                        .setShowWhen(false)
                        .setAutoCancel(false);
        startForeground(NOTIFICATION_ID, builder.build());
    }

    class ListenerAttachCallbacks implements IJanusPluginCallbacks{
        final private BigInteger mRemoteClientId;
        final private Integer groupId;
        private JanusPluginHandle listener_handle = null;

        public ListenerAttachCallbacks(String groupId, BigInteger remoteClientId){
            this.mRemoteClientId = remoteClientId;
            this.groupId = Integer.parseInt(groupId);
        }

        @Override
        public void success(JanusPluginHandle handle) {
            listener_handle = handle;
            try
            {
                JSONObject body = new JSONObject();
                JSONObject msg = new JSONObject();
                body.put(REQUEST, "join");
                body.put("room", groupId);
                body.put("ptype", "listener");
                body.put("feed", mRemoteClientId);
                msg.put(MESSAGE, body);
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
            catch(Exception ex) {
                Log.e("Test", ex.toString());
            }
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsep) {
            try {
                String event = msg.getString("videoroom");
                if (event.equals("attached") && jsep != null) {
                    final JSONObject remoteJsep = jsep;
                    listener_handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                        @Override
                        public void onSuccess(JSONObject obj) {
                            try {
                                JSONObject mymsg = new JSONObject();
                                JSONObject body = new JSONObject();
                                body.put(REQUEST, "start");
                                body.put("room", groupId);
                                mymsg.put(MESSAGE, body);
                                mymsg.put("jsep", obj);
                                mymsg.put("token", "a1b2c3d4");
                                listener_handle.sendMessage(new PluginHandleSendMessageCallbacks(mymsg));
                            } catch (Exception ex) {

                            }
                        }

                        @Override
                        public JSONObject getJsep() {
                            return remoteJsep;
                        }

                        @Override
                        public JanusMediaConstraints getMedia() {
                            JanusMediaConstraints cons = new JanusMediaConstraints();
                            cons.setVideo(null);
                            cons.setRecvAudio(true);
                            cons.setRecvVideo(true);
                            cons.setSendAudio(false);
                            return cons;
                        }

                        @Override
                        public Boolean getTrickle() {
                            return true;
                        }

                        @Override
                        public void onCallbackError(String error) {

                        }
                    });
                }
            }
            catch(Exception ex) {
                Log.e("Test", ex.toString());
            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {

        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            mListener.onRemoteStream(mRemoteClientId, stream.videoTracks.get(0));
        }

        @Override
        public void onDataOpen(Object data) {

        }

        @Override
        public void onData(Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public void onDetached() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {
        private JanusPluginHandle handle = null;

        @Override
        public void success(JanusPluginHandle janusPluginHandle) {
            Log.e("Test", "JanusPublisherPluginCallbacks, success");
            handle = janusPluginHandle;
            joinRoomAsPublisher();
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsep) {
            try
            {
                String event = msg.getString("videoroom");
                if(event.equals("joined")) {
                    createOffer();
                    if(msg.has(PUBLISHERS)){
                        JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
                            BigInteger remoteId = new BigInteger(pub.getString("id"));
                            attachRemoteClient(remoteId);
                        }
                    }
                } else if(event.equals("destroyed")) {

                } else if(event.equals("event")) {
                    if(msg.has(PUBLISHERS)){
                        JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
                            attachRemoteClient(new BigInteger(pub.getString("id")));
                        }
                    } else if(msg.has("leaving")) {

                    } else if(msg.has("unpublished")) {

                    } else {
                        //todo error
                    }
                }
                if(jsep != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsep, false));
                }
            }
            catch (Exception ex) {
                Log.e("Test", "onMessage: error =" + ex.toString());
            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {
            mListener.onLocalStream(stream.videoTracks.get(0));
        }

        @Override
        public void onRemoteStream(MediaStream stream) {
        }

        @Override
        public void onDataOpen(Object data) {
        }

        @Override
        public void onData(Object data) {
        }

        @Override
        public void onCleanup() {
        }

        @Override
        public void onDetached() {
        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
        }

        @Override
        public void onCallbackError(String error) {
        }

        private void createOffer() {
            if(handle != null) {
                handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                    @Override
                    public void onSuccess(JSONObject obj) {
                        try
                        {
                            JSONObject msg = new JSONObject();
                            JSONObject body = new JSONObject();
                            body.put(REQUEST, "configure");
                            body.put("audio", true);
                            body.put("video", true);
                            msg.put(MESSAGE, body);
                            msg.put("jsep", obj);
                            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                        }catch (Exception ex) {
                            Log.e("Test", ex.toString());
                        }
                    }

                    @Override
                    public JSONObject getJsep() {
                        return null;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        JanusMediaConstraints cons = new JanusMediaConstraints();
                        cons.setRecvAudio(false);
                        cons.setRecvVideo(false);
                        cons.setSendAudio(true);
                        return cons;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return true;
                    }

                    @Override
                    public void onCallbackError(String error) {

                    }
                });
            }
        }

        private void joinRoomAsPublisher() {
            if(handle != null) {
                JSONObject obj = new JSONObject();
                JSONObject msg = new JSONObject();
                try
                {
                    obj.put(REQUEST, "join");
                    obj.put("room", Integer.parseInt(mGroupId));
                    obj.put("ptype", "publisher");
                    obj.put("display", mOurClientId);
                    msg.put(MESSAGE, obj);
                }
                catch(Exception ex)
                {
                    Log.e("Test", "joinRoomAsPublisher: error =" + ex.toString());
                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }

        private void attachRemoteClient(BigInteger remoteId) {
            janusServer.Attach(new ListenerAttachCallbacks(mGroupId, remoteId));
        }
    }

    public class JanusGatewayCallbacksImpl implements IJanusGatewayCallbacks {
        public void onSuccess() {
            janusServer.Attach(new JanusPublisherPluginCallbacks());
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public String getServerUri() {
            return JANUS_URI;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            return new ArrayList<PeerConnection.IceServer>();
        }

        @Override
        public Boolean getIpv6Support() {
            return Boolean.FALSE;
        }

        @Override
        public Integer getMaxPollEvents() {
            return 0;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }
}

package com.clearkeep.januswrapper;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;


public class WebSocketChannel {
    private static final String TAG = "WebSocketChannel";

    private static final int PREFERRED_BIT_RATE = 256 * 1000;

    private final Handler mHandler;
    private final HandlerThread mHandlerThread;

    private final int roomId;
    private final String displayName;
    private final String token;
    private final String uri;

    private final ConcurrentHashMap<String, JanusTransaction> transactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BigInteger, JanusHandle> handles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BigInteger, JanusHandle> feeds = new ConcurrentHashMap<>();

    private WebSocket mWebSocket;
    private BigInteger mSessionId;
    private JanusRTCInterface delegate;

    public WebSocketChannel(int roomId, String displayName, String token, String uri) {
        this.roomId = roomId;
        this.displayName = displayName;
        this.token = token;
        this.uri = uri;
        mHandlerThread = new HandlerThread("keep_alive");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void initConnection() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .addInterceptor(chain -> {
                    Request.Builder builder = chain.request().newBuilder();
                    builder.addHeader("Sec-WebSocket-Protocol", "janus-protocol");
                    return chain.proceed(builder.build());
                })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        Request request = new Request.Builder().url(uri).build();
        mWebSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.e(TAG, "onOpen");
                createSession();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.e(TAG, "onMessage");
                WebSocketChannel.this.onMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.e(TAG, "onClosing, " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "onFailure" + t.toString());
                // TODO: crash if server reset janus during calling
            }
        });
    }

    private void onMessage(String message) {
        Log.e(TAG, "onMessage" + message);
        try {
            JSONObject jo = new JSONObject(message);
            String janus = jo.optString("janus");
            switch (janus) {
                case "success": {
                    String transaction = jo.optString("transaction");
                    JanusTransaction jt = transactions.get(transaction);
                    if (jt != null && jt.success != null) {
                        jt.success.success(jo);
                    }
                    transactions.remove(transaction);
                    break;
                }
                case "error": {
                    String transaction = jo.optString("transaction");
                    JanusTransaction jt = transactions.get(transaction);
                    if (jt != null && jt.error != null) {
                        jt.error.error(jo);
                    }
                    transactions.remove(transaction);
                    break;
                }
                case "ack":
                    Log.e(TAG, "Just an ack");
                    break;
                default:
                    JanusHandle handle = handles.get(new BigInteger(jo.optString("sender")));
                    if (handle == null) {
                        Log.e(TAG, "missing handle");
                    } else if (janus.equals("event")) {
                        JSONObject plugin = jo.optJSONObject("plugindata").optJSONObject("data");
                        if (plugin.optString("videoroom").equals("joined")) {
                            handle.onJoined.onJoined(handle);
                        }

                        JSONArray publishers = plugin.optJSONArray("publishers");
                        if (publishers != null && publishers.length() > 0) {
                            for (int i = 0, size = publishers.length(); i <= size - 1; i++) {
                                JSONObject publisher = publishers.optJSONObject(i);
                                BigInteger feed = new BigInteger(publisher.optString("id"));
                                String display = publisher.optString("display");
                                subscriberCreateHandle(feed, display);
                            }
                        }

                        String leaving = plugin.optString("leaving");
                        if (!TextUtils.isEmpty(leaving)) {
                            JanusHandle leavingHandle = feeds.get(new BigInteger(leaving));
                            if (leavingHandle != null) {
                                leavingHandle.onLeaving.onJoined(leavingHandle);
                            }
                        }

                        JSONObject jsep = jo.optJSONObject("jsep");
                        if (jsep != null) {
                            handle.onRemoteJsep.onRemoteJsep(handle, jsep);
                        }

                    } else if (janus.equals("detached")) {
                        handle.onLeaving.onJoined(handle);
                    }
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (mWebSocket != null) {
            mWebSocket.cancel();
        }
        mHandler.removeCallbacks(null);
        mHandlerThread.quitSafely();
        transactions.clear();
        handles.clear();
        feeds.clear();
    }

    private void createSession() {
        try {
            String transaction = randomString(12);
            JanusTransaction jt = new JanusTransaction();
            jt.tid =  transaction;
            jt.success = jo -> {
                mSessionId = new BigInteger(jo.optJSONObject("data").optString("id"));
                mHandler.postDelayed(fireKeepAlive, 30000);
                publisherCreateHandle();
            };
            jt.error = jo -> {
            };
            transactions.put(transaction, jt);
            JSONObject msg = new JSONObject();
            try {
                msg.putOpt("janus", "create");
                msg.putOpt("transaction", transaction);
                msg.putOpt("token", token);
                Log.e(TAG, "-------------"  + msg.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mWebSocket.send(msg.toString());
        } catch (Exception e) {
            Log.e(TAG, "create session error: " + e.toString());
        }
    }

    private void publisherCreateHandle() {
        String transaction = randomString(12);
        JanusTransaction jt = new JanusTransaction();
        jt.tid = transaction;
        jt.success = jo -> {
            Log.e(TAG, "publisherCreateHandle_success");
            JanusHandle janusHandle = new JanusHandle();
            janusHandle.handleId = new BigInteger(jo.optJSONObject("data").optString("id"));
            janusHandle.onJoined = jh -> delegate.onPublisherJoined(jh.handleId);
            janusHandle.onRemoteJsep = (jh, jsep) -> delegate.onPublisherRemoteJsep(jh.handleId, jsep);
            handles.put(janusHandle.handleId, janusHandle);
            publisherJoinRoom(janusHandle);
        };
        jt.error = jo -> {
        };
        transactions.put(transaction, jt);
        JSONObject msg = new JSONObject();
        try {
            msg.putOpt("janus", "attach");
            msg.putOpt("plugin", "janus.plugin.videoroom");
            msg.putOpt("transaction", transaction);
            msg.putOpt("session_id", mSessionId);
            msg.putOpt("token", token);
            Log.e(TAG, "-------------"  + msg.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(msg.toString());
    }

    private void publisherJoinRoom(JanusHandle handle) {
        JSONObject msg = new JSONObject();
        JSONObject body = new JSONObject();
        try {
            body.putOpt("request", "join");
            body.putOpt("room", roomId);
            body.putOpt("ptype", "publisher");
            body.putOpt("display", displayName);

            msg.putOpt("janus", "message");
            msg.putOpt("body", body);
            msg.putOpt("transaction", randomString(12));
            msg.putOpt("session_id", mSessionId);
            msg.putOpt("handle_id", handle.handleId);
            msg.putOpt("token", token);
            Log.e(TAG, "-------------"  + msg.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(msg.toString());
    }

    public void publisherCreateOffer(final BigInteger handleId, final SessionDescription sdp) {
        JSONObject publish = new JSONObject();
        JSONObject jsep = new JSONObject();
        JSONObject message = new JSONObject();
        try {
            publish.putOpt("request", "configure");
            publish.putOpt("audio", true);
            publish.putOpt("video", true);
            publish.putOpt("bitrate", PREFERRED_BIT_RATE);

            jsep.putOpt("type", sdp.type);
            jsep.putOpt("sdp", sdp.description);

            message.putOpt("janus", "message");
            message.putOpt("body", publish);
            message.putOpt("jsep", jsep);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", mSessionId);
            message.putOpt("handle_id", handleId);
            message.putOpt("token", token);
            Log.e(TAG, "-------------"  + message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(message.toString());
    }

    public void subscriberCreateAnswer(final BigInteger handleId, final SessionDescription sdp) {
        JSONObject body = new JSONObject();
        JSONObject jsep = new JSONObject();
        JSONObject message = new JSONObject();

        try {
            body.putOpt("request", "start");
            body.putOpt("room", roomId);

            jsep.putOpt("type", sdp.type);
            jsep.putOpt("sdp", sdp.description);
            message.putOpt("janus", "message");
            message.putOpt("body", body);
            message.putOpt("jsep", jsep);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", mSessionId);
            message.putOpt("handle_id", handleId);
            message.putOpt("token", token);
            Log.e(TAG, "-------------"  + message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mWebSocket.send(message.toString());
    }

    public void trickleCandidate(final BigInteger handleId, final IceCandidate iceCandidate) {
        JSONObject candidate = new JSONObject();
        JSONObject message = new JSONObject();
        try {
            candidate.putOpt("candidate", iceCandidate.sdp);
            candidate.putOpt("sdpMid", iceCandidate.sdpMid);
            candidate.putOpt("sdpMLineIndex", iceCandidate.sdpMLineIndex);

            message.putOpt("janus", "trickle");
            message.putOpt("candidate", candidate);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", mSessionId);
            message.putOpt("handle_id", handleId);
            message.putOpt("token", token);
            Log.e(TAG, "-------------"  + message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(message.toString());
    }

    public void trickleCandidateComplete(final BigInteger handleId) {
        JSONObject candidate = new JSONObject();
        JSONObject message = new JSONObject();
        try {
            candidate.putOpt("completed", true);

            message.putOpt("janus", "trickle");
            message.putOpt("candidate", candidate);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", mSessionId);
            message.putOpt("handle_id", handleId);
            message.putOpt("token", token);
            Log.e(TAG, "-------------"  + message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void subscriberCreateHandle(final BigInteger feed, final String display) {
        String transaction = randomString(12);
        JanusTransaction jt = new JanusTransaction();
        jt.tid = transaction;
        jt.success = jo -> {
            JanusHandle janusHandle = new JanusHandle();
            janusHandle.handleId = new BigInteger(jo.optJSONObject("data").optString("id"));
            janusHandle.feedId = feed;
            janusHandle.display = display;
            janusHandle.onRemoteJsep = (jh, jsep) -> delegate.subscriberHandleRemoteJsep(jh.handleId, jsep);
            janusHandle.onLeaving = jh -> subscriberOnLeaving(jh);
            handles.put(janusHandle.handleId, janusHandle);
            feeds.put(janusHandle.feedId, janusHandle);
            subscriberJoinRoom(janusHandle);
        };
        jt.error = jo -> Log.e(TAG, "-------------"  + jo.toString());

        transactions.put(transaction, jt);
        JSONObject msg = new JSONObject();
        try {
            msg.putOpt("janus", "attach");
            msg.putOpt("plugin", "janus.plugin.videoroom");
            msg.putOpt("transaction", transaction);
            msg.putOpt("session_id", mSessionId);
            msg.putOpt("token", token);
            Log.e(TAG, "-------------"  + msg.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mWebSocket.send(msg.toString());
    }

    private void subscriberJoinRoom(JanusHandle handle) {

        JSONObject msg = new JSONObject();
        JSONObject body = new JSONObject();
        try {
            body.putOpt("request", "join");
            body.putOpt("room", roomId);
            body.putOpt("ptype", "listener");
            body.putOpt("feed", handle.feedId);

            msg.putOpt("janus", "message");
            msg.putOpt("body", body);
            msg.putOpt("transaction", randomString(12));
            msg.putOpt("session_id", mSessionId);
            msg.putOpt("handle_id", handle.handleId);
            msg.putOpt("token", token);
            Log.e(TAG, "-------------"  + msg.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(msg.toString());
    }

    private void subscriberOnLeaving(final JanusHandle handle) {
        String transaction = randomString(12);
        JanusTransaction jt = new JanusTransaction();
        jt.tid = transaction;
        jt.success = jo -> {
            delegate.onLeaving(handle.handleId);
            handles.remove(handle.handleId);
            feeds.remove(handle.feedId);
        };
        jt.error = jo -> {
        };

        transactions.put(transaction, jt);

        JSONObject jo = new JSONObject();
        try {
            jo.putOpt("janus", "detach");
            jo.putOpt("transaction", transaction);
            jo.putOpt("session_id", mSessionId);
            jo.putOpt("handle_id", handle.handleId);
            jo.putOpt("token", token);

            Log.e(TAG, "-------------"  + jo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(jo.toString());
    }

    private void keepAlive() {
        String transaction = randomString(12);
        JSONObject msg = new JSONObject();
        try {
            msg.putOpt("janus", "keepalive");
            msg.putOpt("session_id", mSessionId);
            msg.putOpt("transaction", transaction);
            msg.putOpt("token", token);

            Log.e(TAG, "-------------"  + msg.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mWebSocket.send(msg.toString());
    }

    private final Runnable fireKeepAlive = new Runnable() {
        @Override
        public void run() {
            keepAlive();
            mHandler.postDelayed(fireKeepAlive, 30000);
        }
    };

    public void setDelegate(JanusRTCInterface delegate) {
        this.delegate = delegate;
    }

    private String randomString(Integer length) {
        final String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(str.charAt(rnd.nextInt(str.length())));
        }
        return sb.toString();
    }
}

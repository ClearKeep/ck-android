package computician.janusclientapi;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.math.BigInteger;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONObject;


/**
 * Created by ben.trent on 5/7/2015.
 */
public class JanusWebsocketMessenger implements IJanusMessenger {

    private final String uri;
    private final IJanusMessageObserver handler;
    private final JanusMessengerType type = JanusMessengerType.websocket;
    private WebSocket client = null;
    private Handler mMessageHandler;
    private Handler mSendMessageHandler;

    public JanusWebsocketMessenger(String uri, IJanusMessageObserver handler) {
        this.uri = uri;
        this.handler = handler;
        HandlerThread handlerThread = new HandlerThread("message_handler");
        handlerThread.start();
        mMessageHandler = new MessageHandler(handlerThread.getLooper(), handler);
    }

    @Override
    public JanusMessengerType getMessengerType() {
        return type;
    }

    public void connect() {
        AsyncHttpClient.getDefaultInstance().websocket(uri, "janus-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                Log.i("JANUSCLIENT", "JanusWebsocketMessenger#onCompleted " + uri);
                if (ex != null) {
                    Log.i("JANUSCLIENT", "JanusWebsocketMessenger,ex= " + ex.toString());
                    handler.onError(ex);
                }
                client = webSocket;
                if (client == null) {
                    Log.i("JANUSCLIENT", "JanusWebsocketMessenger, websocket null");
                    return;
                }
                client.setWriteableCallback(() -> Log.d("JANUSCLIENT", "On writable"));
                client.setPongCallback(s -> Log.d("JANUSCLIENT", "Pong callback"));
                client.setDataCallback((emitter, bb) -> Log.d("JANUSCLIENT", "New Data"));
                client.setEndCallback(ex12 -> Log.d("JANUSCLIENT", "Client End"));
                client.setStringCallback(s -> onMessage(s));
                client.setClosedCallback(ex1 -> {
                    Log.d("JANUSCLIENT", "Socket closed for some reason");
                    if (ex1 != null) {
                        Log.d("JANUSCLIENT", "SOCKET EX " + ex1.getMessage());
                        StringWriter writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        ex1.printStackTrace(printWriter);
                        printWriter.flush();
                        Log.d("JANUSCLIENT", "StackTrace \n\t" + writer.toString());
                    }
                    if (ex1 != null) {
                        onError(ex1);
                    } else {
                        onClose(-1, "unknown", true);
                    }
                });

                handler.onOpen();
            }
        });
    }

    private void onMessage(String message) {
        Log.d("JANUSCLIENT", "Recv: \n\t" + message);
        receivedMessage(message);
    }

    private void onClose(int code, String reason, boolean remote) {
        Log.e("JANUSCLIENT", "closed websocket");
        handler.onClose();
        mSendMessageHandler.removeCallbacks(null);
        mMessageHandler.removeCallbacks(null);
    }

    private void onError(Exception ex) {
        handler.onError(ex);
    }

    @Override
    public void disconnect() {
        client.close();
    }

    @Override
    public void sendMessage(String message) {
        Log.d("JANUSCLIENT", "Sent: \n\t" + message);
        if (client == null) {
            Log.d("JANUSCLIENT", "sent error: client is null");
            return;
        }

        if (mSendMessageHandler == null) {
            HandlerThread sendHandlerThread = new HandlerThread("send_message_handler");
            sendHandlerThread.start();
            mSendMessageHandler = new SendMessageHandler(sendHandlerThread.getLooper(), client);
        }

        Message msgHandler = mSendMessageHandler.obtainMessage(1, message);
        mSendMessageHandler.sendMessage(msgHandler);
        /*client.send(message);*/
    }

    @Override
    public void sendMessage(String message, BigInteger session_id) {
        sendMessage(message);
    }

    @Override
    public void sendMessage(String message, BigInteger session_id, BigInteger handle_id) {
        sendMessage(message);
    }

    @Override
    public void receivedMessage(String msg) {
        /*try {
            JSONObject obj = new JSONObject(msg);
            handler.receivedNewMessage(obj);
        } catch (Exception ex) {
            handler.onError(ex);
        }*/
        Message message = mMessageHandler.obtainMessage(1, msg);
        mMessageHandler.sendMessage(message);
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<IJanusMessageObserver> handlerWeaf;

        private Handler mainHandler = new Handler(Looper.getMainLooper());

        private MessageHandler(Looper looper, IJanusMessageObserver handler) {
            super(looper);
            this.handlerWeaf = new WeakReference<>(handler);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = (String) msg.obj;
            IJanusMessageObserver handler = handlerWeaf.get();
            if (handler != null) {
                try {
                    JSONObject obj = new JSONObject(message);
                    handler.receivedNewMessage(obj);
                    /*mainHandler.post(() -> handler.receivedNewMessage(obj));*/
                } catch (Exception ex) {
                    /*mainHandler.post(() -> handler.onError(ex));*/
                    handler.onError(ex);
                }
            }
        }
    }

    private static class SendMessageHandler extends Handler {
        private final WeakReference<WebSocket> clientWeaf;

        private SendMessageHandler(Looper looper, WebSocket client) {
            super(looper);
            this.clientWeaf = new WeakReference<>(client);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = (String) msg.obj;
            WebSocket client = clientWeaf.get();
            if (client != null) {
                try {
                    client.send(message);
                } catch (Exception ex) {
                    Log.e("Test", "send message: " + ex.toString());
                }
            }
        }
    }
}

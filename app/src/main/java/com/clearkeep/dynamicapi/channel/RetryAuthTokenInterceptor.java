
package com.clearkeep.dynamicapi.channel;

import android.util.Log;

import com.clearkeep.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import auth.AuthOuterClass;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

class RetryAuthTokenInterceptor implements ClientInterceptor {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> method,
            final CallOptions callOptions,
            final Channel next) {

        class RetryingUnaryRequestClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
            Listener listener;
            Metadata metadata;
            ReqT msg;
            int req;
            ClientCall call;

            @Override
            public void start(Listener listener, Metadata metadata) {
                Log.d("antx: ", "  line = 30: start");
                this.listener = listener;
                this.metadata = metadata;
            }

            @Override
            public void sendMessage(ReqT msg) {
                Log.d("antx: ", "  line = 37: " + msg);
                assert this.msg == null;
                this.msg = msg;
            }

            @Override
            public void request(int num) {
                Log.d("antx: ", "  line = 43: request : " + num);
                req += num;
                assert this.msg == null;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void halfClose() {
                startCall(new CheckingListener());
            }

            private void startCall(Listener listener) {
                Log.d("antx: ", "  line = 57: startCall " + method.getFullMethodName());
                call = next.newCall(method, callOptions);
                Metadata headers = new Metadata();
                headers.merge(metadata);
                call.start(listener, headers);
                assert this.msg != null;
                call.request(req);
                call.sendMessage(msg);
                call.halfClose();
            }

            @Override
            public void cancel(String s, Throwable t) {
                if (call != null) { // need synchronization
                    call.cancel(s, t);
                }
                listener.onClose(Status.CANCELLED.withDescription(s).withCause(t), new Metadata());
            }

            class CheckingListener extends ForwardingClientCallListener {

                Listener<RespT> delegate;

                @Override
                protected Listener delegate() {
                    if (delegate == null) {
                        throw new IllegalStateException();
                    }
                    return delegate;
                }

                @Override
                public void onHeaders(Metadata headers) {
                    delegate = listener;
                    super.onHeaders(headers);
                }

                @Override
                public void onClose(Status status, Metadata trailers) {
                    if (delegate != null) {
                        Log.d("antx: ", "  line = 101: +" + status);
                        String code = "";
                        if (status.getCode() == Status.INTERNAL.getCode()) {
                            String getDescription = status.getDescription();
                            JSONArray jsonarray = null;
                            try {
                                jsonarray = new JSONArray(getDescription);
                                for (int i = 0; i < jsonarray.length(); i++) {
                                    JSONObject jsonobject = jsonarray.getJSONObject(i);
                                    code = jsonobject.getString("code");
                                    if (code.equals("1077")) {
                                        break;
                                    }
                                }
                            } catch (JSONException e) {
                                super.onClose(status, trailers);
                                return;
                            }
                            if ((code.equals("1077"))) {
                                String metadataTest= metadata.toString();
                                Log.d("antx: ", "  line = 128: metadataTest: "+trailers );
                              ManagedChannel mChannel =
                                      ManagedChannelBuilder.forAddress(BuildConfig.BASE_URL,
                                              BuildConfig.PORT)
                                              .usePlaintext()
                                              .build();


                                //startCall(listener);
                                Log.d("antx: ", "  line = 124: need refresh token" );
                                //reques refesh token
                            }
                            super.onClose(status, trailers);
                            return;
                        } else {
                            super.onClose(status, trailers);
                            return;
                        }
                    }
                    Log.d("antx: ", "  line = 99: ======== $ : " + status.getDescription());

/*                    if (!needToRetry(status, trailers)) { // YOUR CODE HERE
                        delegate = listener;
                        super.onClose(status, trailers);
                        return;
                    }*/

                    //startCall(listener); // Only retry once
                    // startCall(new CheckingListener()); // to allow multiple retries
                }
            }

        }
        return new RetryingUnaryRequestClientCall<>();
    }

    private void authenticaion(){}


}

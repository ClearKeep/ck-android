/*
import android.util.Log
import io.grpc.*
import org.json.JSONArray
import org.json.JSONException

class RetryAuthTokenInterceptor : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        class RetryingUnaryRequestClientCall<ReqT, RespT> : ClientCall<ReqT, RespT>() {
            var listener: Listener<RespT>? = null
            var metadata: Metadata? = null
            var msg: ReqT? = null
            var req = 0
            var call: ClientCall<*, *>? = null
            override fun start(listener: Listener<RespT>?, metadata: Metadata?) {
                Log.d("antx: ", "  line = 30: start")
                this.listener = listener
                this.metadata = metadata
            }

            override fun sendMessage(msg: ReqT) {
                Log.d("antx: ", "  line = 37: $msg")
                this.msg = msg
            }

            override fun request(num: Int) {
                Log.d("antx: ", "  line = 43: request : $num")
                req += num
            }

            override fun isReady(): Boolean {
                return false
            }

            override fun halfClose() {
               // startCall(CheckingListener())
            }

            private fun startCall(listener: Listener<RespT>) {
                Log.d("antx: ", "  line = 57: startCall " + method.fullMethodName)
                call = next.newCall(method, callOptions)
                val headers = Metadata()
                headers.merge(metadata)
                (call as ClientCall<ReqT, RespT>?)?.start(listener, headers)
                call?.let {
                    request(req)
                    sendMessage(msg!!)
                    halfClose()
                }
            }

            override fun cancel(s: String?, t: Throwable?) {
                if (call != null) { // need synchronization
                    call!!.cancel(s, t)
                }
                listener!!.onClose(Status.CANCELLED.withDescription(s).withCause(t), Metadata())
            }

            inner class CheckingListener : ForwardingClientCallListener<RespT>() {
                var delegate: Listener<RespT>? = null
                override fun delegate(): Listener<RespT>? {
                    checkNotNull(delegate)
                    return delegate
                }

                override fun onHeaders(headers: Metadata) {
                    delegate = listener
                    super.onHeaders(headers)
                }

                override fun onClose(status: Status, trailers: Metadata) {
                    if (delegate != null) {
                        Log.d("antx: ", "  line = 101: +$status")
                        var code = ""
                        if (status.code == Status.INTERNAL.code) {
                            val getDescription = status.description
                            var jsonarray: JSONArray? = null
                            try {
                                jsonarray = JSONArray(getDescription)
                                for (i in 0 until jsonarray.length()) {
                                    val jsonobject = jsonarray.getJSONObject(i)
                                    code = jsonobject.getString("code")
                                    if (code == "1077") {
                                        break
                                    }
                                }
                            } catch (e: JSONException) {
                                super.onClose(status, trailers)
                                return
                            }
                            if (code == "1077") {
                                //startCall(listener);
                                Log.d("antx: ", "  line = 124: need refresh token")
                                //reques refesh token
                            }
                            super.onClose(status, trailers)
                            return
                        } else {
                            super.onClose(status, trailers)
                            return
                        }
                    }
                    Log.d("antx: ", "  line = 99: ======== $ : " + status.description)
                    */
/*                    if (!needToRetry(status, trailers)) { // YOUR CODE HERE
                        delegate = listener;
                        super.onClose(status, trailers);
                        return;
                    }*//*

                    //startCall(listener); // Only retry once
                    // startCall(new CheckingListener()); // to allow multiple retries
                }
            }


        }
        return RetryingUnaryRequestClientCall()
    }
}
*/

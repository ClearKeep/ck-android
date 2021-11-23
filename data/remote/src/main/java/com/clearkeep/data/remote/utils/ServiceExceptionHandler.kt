package com.clearkeep.data.remote.utils

import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import io.grpc.*

class ServiceExceptionHandler : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel?
    ): ClientCall<ReqT, RespT> {
        printlnCK("")
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next?.newCall(method, callOptions)) {
            override fun cancel(message: String?, cause: Throwable?) {
                if (cause is StatusRuntimeException) {
                    printlnCK("ServiceExceptionHandler cancel caught exception method $method message $message cause ${parseError(cause)}")
                }
                super.cancel(message, cause)
            }
        }
    }
}
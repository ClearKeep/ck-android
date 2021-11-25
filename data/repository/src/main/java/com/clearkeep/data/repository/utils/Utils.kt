package com.clearkeep.data.repository.utils

import com.clearkeep.common.utilities.ERROR_CODE_TIMEOUT
import com.clearkeep.common.utilities.network.ProtoResponse
import com.clearkeep.common.utilities.network.TokenExpiredException
import com.clearkeep.common.utilities.printlnCK
import com.google.gson.Gson
import io.grpc.StatusRuntimeException

val errorRegex = "\\{.+\\}".toRegex()

fun parseError(e: StatusRuntimeException): ProtoResponse {
    if (e.status.code == io.grpc.Status.Code.DEADLINE_EXCEEDED || e.status.code == io.grpc.Status.Code.UNAVAILABLE) {
        return ProtoResponse(
            ERROR_CODE_TIMEOUT,
            "We are unable to detect an internet connection. Please try again when you have a stronger connection."
        )
    }

    val rawError = errorRegex.find(e.message ?: "")?.value ?: ""
    return try {
        val response = Gson().fromJson(rawError, ProtoResponse::class.java)
        if (e.status.code == io.grpc.Status.Code.DEADLINE_EXCEEDED && response.code == 1000 || response.code == 1077) {
            response.copy(cause = TokenExpiredException())
        } else {
            response
        }
    } catch (e: Exception) {
        printlnCK("parseError exception rawError $rawError, exception ${e.message}")
        ProtoResponse(0, rawError)
    }
}
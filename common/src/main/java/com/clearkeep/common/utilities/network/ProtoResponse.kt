package com.clearkeep.common.utilities.network

data class ProtoResponse(val code: Int, val message: String, val cause: Exception? = null)
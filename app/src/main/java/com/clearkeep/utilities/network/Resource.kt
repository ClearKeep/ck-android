package com.clearkeep.utilities.network

import com.clearkeep.utilities.network.Status.ERROR
import com.clearkeep.utilities.network.Status.LOADING
import com.clearkeep.utilities.network.Status.SUCCESS

data class Resource<out T>(val status: Status, val data: T?, val message: String?, val errorCode: Int = 0) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(SUCCESS, data, null)
        }

        fun <T> error(msg: String, data: T?, errorCode: Int = 0): Resource<T> {
            return Resource(ERROR, data, msg, errorCode)
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(LOADING, data, null)
        }
    }
}

package com.clearkeep.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.clearkeep.utilities.storage.Storage
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


private const val USER_NAME = "user_name"
private const val ACCESS_TOKEN = "access_token"
private const val REFRESH_TOKEN = "refresh_token"
private const val HASH_KEY = "hash_key"
private const val CLIENT_ID = "client_id"
private const val DEVICE_ID = "device_id"

@Singleton
class UserManager @Inject constructor(
        private val storage: Storage,
) {
    fun isUserRegistered() = !storage.getString(USER_NAME).isNullOrEmpty()

    fun saveAccessKey(accessKey: String) {
        storage.setString(ACCESS_TOKEN, accessKey)
    }

    fun getAccessKey() : String {
        return storage.getString(ACCESS_TOKEN)
    }

    fun saveHashKey(hashKey: String) {
        storage.setString(HASH_KEY, hashKey)
    }

    fun getHashKey() : String {
        return storage.getString(HASH_KEY)
    }

    fun saveUserName(userName: String) {
        storage.setString(USER_NAME, userName)
    }

    fun getUserName() : String {
        return storage.getString(USER_NAME)
    }

    fun saveClientId(clientId: String) {
        storage.setString(CLIENT_ID, clientId)
    }

    fun getClientId() : String {
        return storage.getString(CLIENT_ID)
    }

    fun saveRefreshToken(refreshToken: String) {
        storage.setString(REFRESH_TOKEN, refreshToken)
    }

    fun getRefreshToken() : String {
        return storage.getString(REFRESH_TOKEN)
    }

    @SuppressLint("HardwareIds")
    fun getUniqueDeviceID(): String {
        var deviceId = storage.getString(DEVICE_ID)
        if (deviceId.isNullOrEmpty()) {
            /*deviceId = UUID.randomUUID().toString()*/
            /*deviceId = Settings.Secure.getString(context.contentResolver,
                    Settings.Secure.ANDROID_ID)*/
            deviceId = "12345"
            storage.setString(DEVICE_ID, deviceId)
        }
        return deviceId
    }
}
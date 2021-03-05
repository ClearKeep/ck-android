package com.clearkeep.utilities

import com.clearkeep.utilities.storage.PersistPreferencesStorage
import com.clearkeep.utilities.storage.UserPreferencesStorage
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


private const val USER_NAME = "user_name"
private const val ACCESS_TOKEN = "access_token"
private const val REFRESH_TOKEN = "refresh_token"
private const val HASH_KEY = "hash_key"
private const val CLIENT_ID = "client_id"
private const val DEVICE_ID = "device_id"
private const val TIME_USER_LOGIN = "time_user_login"

@Singleton
class UserManager @Inject constructor(
        private val userStorage: UserPreferencesStorage,
        private val persistStorage: PersistPreferencesStorage,
) {
    fun isUserRegistered() = !userStorage.getString(USER_NAME).isNullOrEmpty()

    fun saveAccessKey(accessKey: String) {
        userStorage.setString(ACCESS_TOKEN, accessKey)
    }

    fun getAccessKey() : String {
        return userStorage.getString(ACCESS_TOKEN)
    }

    fun saveHashKey(hashKey: String) {
        userStorage.setString(HASH_KEY, hashKey)
    }

    fun getHashKey() : String {
        return userStorage.getString(HASH_KEY)
    }

    fun saveUserName(userName: String) {
        userStorage.setString(USER_NAME, userName)
    }

    fun getUserName() : String {
        return userStorage.getString(USER_NAME)
    }

    fun saveClientId(clientId: String) {
        userStorage.setString(CLIENT_ID, clientId)
    }

    fun getClientId() : String {
        return userStorage.getString(CLIENT_ID)
    }

    fun saveRefreshToken(refreshToken: String) {
        userStorage.setString(REFRESH_TOKEN, refreshToken)
    }

    fun getRefreshToken() : String {
        return userStorage.getString(REFRESH_TOKEN)
    }

    fun saveLoginTime(time: Long) {
        userStorage.setLong(TIME_USER_LOGIN, time)
    }

    fun getLoginTime() : Long {
        return userStorage.getLong(TIME_USER_LOGIN)
    }

    fun getUniqueDeviceID(): String {
        var deviceId = persistStorage.getString(DEVICE_ID)
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            printlnCK("generate new device id: $deviceId")
            persistStorage.setString(DEVICE_ID, deviceId)
        }
        return deviceId
    }
}
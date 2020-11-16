package com.clearkeep.repository

import com.clearkeep.utilities.storage.Storage
import javax.inject.Inject
import javax.inject.Singleton

private const val CLIENT_ID = "client_id"
private const val ACCESS_TOKEN = "access_token"
private const val HASH_KEY = "hash_key"

@Singleton
class UserRepository @Inject constructor(
        private val storage: Storage,
) {
    fun isUserRegistered() = storage.getString(CLIENT_ID).isNotEmpty()

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
        storage.setString(CLIENT_ID, userName)
    }

    fun getUserName() : String {
        return storage.getString(CLIENT_ID)
    }
}
package com.clearkeep.domain.repository

interface UserRepository {
    fun getUniqueDeviceID(): String
    fun getFirebaseToken(): String
    fun setFirebaseToken(token: String)
}
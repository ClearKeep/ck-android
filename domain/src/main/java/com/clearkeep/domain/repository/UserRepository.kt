package com.clearkeep.domain.repository

interface UserRepository {
    fun getUniqueDeviceID(): String
}
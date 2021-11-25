package com.clearkeep.data.repository

import com.clearkeep.data.local.preference.AppStorage
import com.clearkeep.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(private val appStorage: AppStorage) : UserRepository {
    override fun getUniqueDeviceID(): String {
        return appStorage.getUniqueDeviceID()
    }
}
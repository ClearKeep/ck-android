package com.clearkeep.data.repository

import com.clearkeep.common.utilities.FIREBASE_TOKEN
import com.clearkeep.data.local.preference.AppStorage
import com.clearkeep.data.local.preference.UserPreferencesStorage
import com.clearkeep.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val appStorage: AppStorage,
    private val storage: UserPreferencesStorage,
) : UserRepository {
    override fun getUniqueDeviceID(): String {
        return appStorage.getUniqueDeviceID()
    }

    override fun getFirebaseToken(): String {
        return storage.getString(FIREBASE_TOKEN)
    }

    override fun setFirebaseToken(token: String) {
        return storage.setString(FIREBASE_TOKEN, token)
    }
}
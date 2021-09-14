package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.dao.UserKeyDao
import com.clearkeep.db.clear_keep.model.UserKey
import javax.inject.Inject

class UserKeyRepository @Inject constructor(
    private val userKeyDao: UserKeyDao
) {
    suspend fun insert(userKey: UserKey) {
        userKeyDao.insert(userKey)
    }

    suspend fun updateKey(serverDomain: String, userId: String, key: String) {
        userKeyDao.updateKey(serverDomain, userId, key)
    }

    suspend fun updateSalt(serverDomain: String, userId: String, salt: String) {
        userKeyDao.updateSalt(serverDomain, userId, salt)
    }

    suspend fun getKey(serverDomain: String, userId: String) : UserKey? {
        return userKeyDao.getUserKey(serverDomain, userId)
    }
}
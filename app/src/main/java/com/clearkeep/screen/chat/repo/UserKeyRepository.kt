package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.dao.UserKeyDAO
import com.clearkeep.db.clear_keep.dao.UserPreferenceDAO
import com.clearkeep.db.clear_keep.model.UserKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserKeyRepository @Inject constructor(
    private val userKeyDAO: UserKeyDAO
) {
    suspend fun insert(userKey: UserKey) {
        userKeyDAO.insert(userKey)
    }

    suspend fun get(domain: String, userId: String): UserKey {
        return userKeyDAO.getKey(domain, userId) ?: UserKey(domain, userId, "", "")
    }
}
package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.UserKeyDAO
import com.clearkeep.domain.model.UserKey
import com.clearkeep.domain.repository.UserKeyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserKeyRepositoryImpl @Inject constructor(
    private val userKeyDAO: UserKeyDAO
): UserKeyRepository {
    override suspend fun insert(userKey: UserKey) {
        userKeyDAO.insert(userKey)
    }

    override suspend fun get(domain: String, userId: String): UserKey {
        return userKeyDAO.getKey(domain, userId) ?: UserKey(domain, userId, "", "")
    }
}
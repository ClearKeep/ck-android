package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.UserKeyDAO
import com.clearkeep.data.local.model.toLocal
import com.clearkeep.domain.model.UserKey
import com.clearkeep.domain.repository.UserKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class UserKeyRepositoryImpl @Inject constructor(
    private val userKeyDAO: UserKeyDAO
): UserKeyRepository {
    override suspend fun insert(userKey: UserKey) = withContext(Dispatchers.IO) {
        userKeyDAO.insert(userKey.toLocal())
    }

    override suspend fun get(domain: String, userId: String): UserKey = withContext(Dispatchers.IO) {
        return@withContext userKeyDAO.getKey(domain, userId)?.toEntity() ?: UserKey(
            domain,
            userId,
            "",
            ""
        )
    }
}
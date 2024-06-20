package com.clearkeep.data.repository.userkey

import com.clearkeep.data.local.clearkeep.userkey.UserKeyDAO
import com.clearkeep.domain.model.UserKey
import com.clearkeep.domain.repository.UserKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserKeyRepositoryImpl @Inject constructor(
    private val userKeyDAO: UserKeyDAO
): UserKeyRepository {
    override suspend fun insert(userKey: UserKey) = withContext(Dispatchers.IO) {
        userKeyDAO.insert(userKey.toEntity())
    }

    override suspend fun get(domain: String, userId: String): UserKey = withContext(Dispatchers.IO) {
        return@withContext userKeyDAO.getKey(domain, userId)?.toModel() ?: UserKey(
            domain,
            userId,
            "",
            ""
        )
    }
}
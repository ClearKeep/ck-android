package com.clearkeep.domain.repository

import com.clearkeep.db.clearkeep.model.UserKey

interface UserKeyRepository {
    suspend fun insert(userKey: UserKey)
    suspend fun get(domain: String, userId: String): UserKey
}
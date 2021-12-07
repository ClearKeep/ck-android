package com.clearkeep.data.repository.userkey

import com.clearkeep.data.local.clearkeep.userkey.UserKeyEntity
import com.clearkeep.domain.model.UserKey

fun UserKeyEntity.toModel() = UserKey(serverDomain, userId, salt, iv)

fun UserKey.toEntity() = UserKeyEntity(serverDomain, userId, salt, iv)
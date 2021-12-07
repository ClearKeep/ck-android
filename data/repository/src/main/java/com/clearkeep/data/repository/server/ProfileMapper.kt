package com.clearkeep.data.repository.server

import com.clearkeep.data.local.clearkeep.server.ProfileEntity
import com.clearkeep.domain.model.Profile

fun Profile.toEntity() =
    ProfileEntity(generateId, userId, userName, email, phoneNumber, updatedAt, avatar)

fun ProfileEntity.toModel() = Profile(
    generateId,
    userId,
    userName,
    email,
    phoneNumber,
    updatedAt,
    avatar
)
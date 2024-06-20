package com.clearkeep.data.repository.people

import com.clearkeep.domain.model.UserEntity

fun UserEntity.toEntity() = com.clearkeep.data.local.clearkeep.user.UserEntity(
    generateId,
    userId,
    userName,
    domain,
    ownerClientId,
    ownerDomain,
    userStatus,
    phoneNumber,
    avatar,
    email
)

fun com.clearkeep.data.local.clearkeep.user.UserEntity.toModel() = UserEntity(
    generateId,
    userId,
    userName,
    domain,
    ownerClientId,
    ownerDomain,
    userStatus,
    phoneNumber,
    avatar,
    email
)
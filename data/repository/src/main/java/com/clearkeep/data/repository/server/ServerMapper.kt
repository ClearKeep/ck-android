package com.clearkeep.data.repository.server

import com.clearkeep.data.local.clearkeep.server.ServerEntity
import com.clearkeep.domain.model.Server

fun Server.toEntity() = ServerEntity(
    id,
    serverName,
    serverDomain,
    ownerClientId,
    serverAvatar,
    loginTime,
    accessKey,
    hashKey,
    refreshToken,
    isActive,
    profile.toEntity()
)

fun ServerEntity.toModel() = Server(
    id,
    serverName,
    serverDomain,
    ownerClientId,
    serverAvatar,
    loginTime,
    accessKey,
    hashKey,
    refreshToken,
    isActive,
    profile.toModel()
)
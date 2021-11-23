package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Server

interface NotificationRepository {
    suspend fun registerPushNotificationTokenByOwner(
        token: String,
        deviceId: String,
        server: com.clearkeep.domain.model.Server
    ): Boolean
}
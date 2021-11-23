package com.clearkeep.data.repository

import com.clearkeep.data.remote.service.PushNotificationService
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.NotificationRepository
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(private val pushNotificationService: PushNotificationService): NotificationRepository {
    override suspend fun registerPushNotificationTokenByOwner(
        token: String,
        deviceId: String,
        server: com.clearkeep.domain.model.Server
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    pushNotificationService.registerPushNotificationToken(deviceId, token, server)
                return@withContext response.error.isNullOrEmpty()
            } catch (e: StatusRuntimeException) {
                return@withContext false
            } catch (e: Exception) {
                return@withContext false
            }
        }
}
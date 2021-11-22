package com.clearkeep.domain.usecase.notification

import com.clearkeep.domain.repository.NotificationRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.AppStorage
import javax.inject.Inject

class RegisterTokenUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val serverRepository: ServerRepository,
    private val userManager: AppStorage
) {
    suspend operator fun invoke(token: String) {
        val servers = serverRepository.getServers()

        servers.forEach { server ->
            val deviceId = userManager.getUniqueDeviceID()
            notificationRepository.registerPushNotificationTokenByOwner(token, deviceId, server)
        }
    }
}
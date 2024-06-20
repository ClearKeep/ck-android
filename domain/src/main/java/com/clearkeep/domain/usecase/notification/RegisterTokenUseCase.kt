package com.clearkeep.domain.usecase.notification

import com.clearkeep.domain.repository.NotificationRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.UserRepository
import javax.inject.Inject

class RegisterTokenUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val serverRepository: ServerRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke() {
        val token = userRepository.getFirebaseToken()
        if (!token.isNullOrEmpty()) {
            val servers = serverRepository.getServers()

            servers.forEach { server ->
                val deviceId = userRepository.getUniqueDeviceID()
                notificationRepository.registerPushNotificationTokenByOwner(token, deviceId, server)
            }
        }
    }
}
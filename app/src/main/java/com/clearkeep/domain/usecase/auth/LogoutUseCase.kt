package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.*
import com.clearkeep.utilities.printlnCK
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(): Boolean {
        val server = serverRepository.getDefaultServer()
        val response = authRepository.logoutFromAPI(server)

        if (response.data?.error.isNullOrBlank()) {
            server.id?.let {
                val removeResult = serverRepository.deleteServer(it)
                groupRepository.deleteGroup(
                    server.serverDomain,
                    server.ownerClientId
                )
                messageRepository.deleteMessageByDomain(
                    server.serverDomain,
                    server.ownerClientId
                )
                if (removeResult > 0) {
                    return if (serverRepository.getServers().isNotEmpty()) {
                        val firstServer = serverRepository.getServers()[0]
                        serverRepository.setActiveServer(firstServer)
                        false
                    } else {
                        true
                    }
                }
            }
        } else {
            printlnCK("signOut error")
            return false
        }
        return false
    }
}
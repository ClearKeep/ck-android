package com.clearkeep.domain.usecase.auth

import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.UserStateTypeInGroup
import com.clearkeep.domain.repository.*
import com.clearkeep.utilities.RECEIVER_DEVICE_ID
import com.clearkeep.utilities.SENDER_DEVICE_ID
import com.clearkeep.utilities.printlnCK
import org.whispersystems.libsignal.groups.SenderKeyName
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val environment: Environment,
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val signalKeyRepository: SignalKeyRepository
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
            val server = environment.getServer()
            val owner = Owner(server.serverDomain, server.ownerClientId)
            val groups = groupRepository.getAllRooms()
            val profile = serverRepository.getDefaultServer().profile
            val groupsInServer = groups.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
                        && it.clientList.firstOrNull { it.userId == profile.userId }?.userState == UserStateTypeInGroup.ACTIVE.value
            }

            deleteKey(owner, server, groupsInServer)
        } else {
            printlnCK("signOut error")
            return false
        }
        return false
    }

    private suspend fun deleteKey(owner: Owner, server: Server, chatGroups: List<ChatGroup>?) {
        val (domain, clientId) = owner

        signalKeyRepository.deleteIdentityKeyByOwnerDomain(domain, clientId)
        val senderAddress = CKSignalProtocolAddress(
            Owner(server.serverDomain, server.ownerClientId),
            RECEIVER_DEVICE_ID
        )

        signalKeyRepository.deleteSenderPreKey(server.serverDomain, server.ownerClientId)

        chatGroups?.forEach { group ->
            val groupSender2 = SenderKeyName(group.groupId.toString(), senderAddress)
            signalKeyRepository.deleteGroupSenderKey(groupSender2)
            group.clientList.forEach {
                val senderAddress = CKSignalProtocolAddress(
                    Owner(
                        it.domain,
                        it.userId
                    ), SENDER_DEVICE_ID
                )
                val groupSender = SenderKeyName(group.groupId.toString(), senderAddress)
                signalKeyRepository.deleteGroupSenderKey(groupSender.groupId, groupSender.sender.name)
            }
        }
    }
}
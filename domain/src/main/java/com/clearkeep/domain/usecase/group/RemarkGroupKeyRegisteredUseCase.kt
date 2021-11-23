package com.clearkeep.domain.usecase.group

import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.printlnCK
import javax.inject.Inject

class RemarkGroupKeyRegisteredUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val environment: Environment
) {
    suspend operator fun invoke(groupId: Long): com.clearkeep.domain.model.ChatGroup {
        val server = serverRepository.getServer(getDomain(), getClientId())
        val group = groupRepository.getGroupByID(groupId, getDomain(), getClientId(), server, false)
        group.data?.let {
            val updateGroup = com.clearkeep.domain.model.ChatGroup(
                generateId = it.generateId,
                groupId = it.groupId,
                groupName = it.groupName,
                groupAvatar = it.groupAvatar,
                groupType = it.groupType,
                createBy = it.createBy,
                createdAt = it.createdAt,
                updateBy = it.updateBy,
                updateAt = it.updateAt,
                rtcToken = it.rtcToken,
                clientList = it.clientList,

                // update
                isJoined = true,
                ownerDomain = it.ownerDomain,
                ownerClientId = it.ownerClientId,

                lastMessage = it.lastMessage,
                lastMessageAt = it.lastMessageAt,
                lastMessageSyncTimestamp = it.lastMessageSyncTimestamp,
                isDeletedUserPeer = it.isDeletedUserPeer
            )
            groupRepository.updateGroup(updateGroup)
            return updateGroup
        }
        return com.clearkeep.domain.model.ChatGroup(
            null,
            0,
            "",
            "",
            "",
            "",
            0L,
            "",
            0,
            "",
            emptyList(),
            false,
            "",
            "",
            null,
            0L,
            0L,
            false
        )
    }

    private fun getClientId(): String = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain
}
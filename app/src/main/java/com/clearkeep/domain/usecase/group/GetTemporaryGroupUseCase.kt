package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.GROUP_ID_TEMPO
import com.clearkeep.domain.model.User
import javax.inject.Inject

class GetTemporaryGroupUseCase @Inject constructor() {
    operator fun invoke(createPeople: User, receiverPeople: User): ChatGroup {
        return ChatGroup(
            groupId = GROUP_ID_TEMPO,
            groupName = receiverPeople.userName,
            groupAvatar = "",
            groupType = "peer",
            createBy = createPeople.userId,
            createdAt = 0,
            updateBy = createPeople.userId,
            updateAt = 0,
            rtcToken = "",
            clientList = listOf(createPeople, receiverPeople),

            isJoined = false,
            ownerDomain = createPeople.domain,
            ownerClientId = createPeople.userId,

            lastMessage = null,
            lastMessageAt = 0,
            lastMessageSyncTimestamp = 0,
            isDeletedUserPeer = false
        )
    }
}
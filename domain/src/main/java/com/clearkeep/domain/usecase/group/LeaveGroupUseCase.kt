package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.common.utilities.RECEIVER_DEVICE_ID
import com.clearkeep.common.utilities.SENDER_DEVICE_ID
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LeaveGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val signalKeyRepository: SignalKeyRepository,
) {
    suspend operator fun invoke(groupId: Long, owner: Owner): Boolean = withContext(Dispatchers.IO) {
        val response = groupRepository.leaveGroup(groupId, owner) ?: return@withContext false

        if (response.isEmpty()) {
            groupRepository.getListClientInGroup(groupId, owner.domain)?.forEach {
                val addressReceiver = CKSignalProtocolAddress(
                    Owner(owner.domain, it),
                    groupId,
                    RECEIVER_DEVICE_ID
                )
                val addressSender = CKSignalProtocolAddress(
                    Owner(owner.domain, it),
                    groupId,
                    SENDER_DEVICE_ID
                )
                signalKeyRepository.deleteGroupSenderKey(addressReceiver)
                signalKeyRepository.deleteGroupSenderKey(addressSender)
            }
            groupRepository.deleteGroup(groupId, owner.domain, owner.clientId)
            return@withContext true
        }
        return@withContext false
    }
}
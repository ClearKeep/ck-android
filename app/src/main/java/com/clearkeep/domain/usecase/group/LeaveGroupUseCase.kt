package com.clearkeep.domain.usecase.group

import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.utilities.RECEIVER_DEVICE_ID
import com.clearkeep.utilities.SENDER_DEVICE_ID
import com.clearkeep.utilities.printlnCK
import org.whispersystems.libsignal.groups.SenderKeyName
import javax.inject.Inject

class LeaveGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val signalKeyRepository: SignalKeyRepository,
) {
    suspend operator fun invoke(groupId: Long, owner: Owner): Boolean {
        val response = groupRepository.leaveGroup(groupId, owner) ?: return false

        if (response.error.isNullOrEmpty()) {
            groupRepository.getListClientInGroup(groupId, owner.domain)?.forEach {
                val senderAddress2 = CKSignalProtocolAddress(
                    Owner(
                        owner.domain,
                        it
                    ), RECEIVER_DEVICE_ID
                )
                val senderAddress1 = CKSignalProtocolAddress(
                    Owner(
                        owner.domain,
                        it
                    ), SENDER_DEVICE_ID
                )
                val groupSender2 = SenderKeyName(groupId.toString(), senderAddress2)
                val groupSender = SenderKeyName(groupId.toString(), senderAddress1)
                signalKeyRepository.deleteGroupSenderKey(groupSender2)
                signalKeyRepository.deleteGroupSenderKey(groupSender)
            }
            groupRepository.deleteGroup(groupId, owner.domain, owner.clientId)
            return true
        }
        return false
    }
}
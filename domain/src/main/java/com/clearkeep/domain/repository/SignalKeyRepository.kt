package com.clearkeep.domain.repository

import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.GroupGetClientKeyResponse
import com.clearkeep.domain.model.response.PeerGetClientKeyResponse
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage


interface SignalKeyRepository {
    suspend fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey?
    suspend fun saveIdentityKey(signalIdentityKey: SignalIdentityKey)
    suspend fun deleteIdentityKeyByOwnerDomain(domain: String, clientId: String)
    suspend fun deleteSenderPreKey(domain: String, clientId: String)
    suspend fun deleteGroupSenderKey(senderKeyName: CKSignalProtocolAddress)
    suspend fun deleteGroupSenderKey(groupId: String, groupSender: String)

    suspend fun registerSenderKeyToGroup(
        server: Server,
        deviceId: Int,
        groupId: Long,
        clientKeyDistribution: SenderKeyDistributionMessage,
        encryptedGroupPrivateKey: ByteArray?,
        key: ECKeyPair?,
        senderKeyId: Int?,
        senderKey: ByteArray?
    ): Resource<Any>

    suspend fun hasSenderKey(senderKeyName: CKSignalProtocolAddress): Boolean
    suspend fun storeSenderKey(senderKeyName: CKSignalProtocolAddress, record: SenderKeyRecord)
    suspend fun loadSenderKey(senderKeyName: CKSignalProtocolAddress): SenderKeyRecord?
    suspend fun getUserKey(serverDomain: String, userId: String): UserKey
    suspend fun getGroupClientKey(server: Server, groupId: Long, fromClientId: String): GroupGetClientKeyResponse?
    suspend fun getPeerClientKey(server: Server, clientId: String, domain: String): PeerGetClientKeyResponse?
}
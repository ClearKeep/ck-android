package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.UserKeyDAO
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.data.local.signal.dao.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.dao.SignalKeyDAO
import com.clearkeep.data.local.signal.dao.SignalPreKeyDAO
import com.clearkeep.data.local.signal.model.SignalIdentityKey
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.utilities.*
import com.clearkeep.common.utilities.network.Resource
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.ecc.ECKeyPair
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import signal.Signal
import javax.inject.Inject

class SignalKeyRepositoryImpl @Inject constructor(
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val signalPreKeyDAO: SignalPreKeyDAO,
    private val userKeyDAO: UserKeyDAO,
    private val signalKeyDAO: SignalKeyDAO,
    private val signalKeyDistributionService: SignalKeyDistributionService
) : SignalKeyRepository {
    override suspend fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey? = withContext(Dispatchers.IO) {
        return@withContext signalIdentityKeyDAO.getIdentityKey(clientId, domain)
    }

    override suspend fun saveIdentityKey(signalIdentityKey: SignalIdentityKey) =
        withContext(Dispatchers.IO) {
            signalIdentityKeyDAO.insert(signalIdentityKey)
        }

    override suspend fun deleteIdentityKeyByOwnerDomain(domain: String, clientId: String): Unit = withContext(Dispatchers.IO) {
        signalIdentityKeyDAO
            .deleteSignalKeyByOwnerDomain(domain, clientId)
    }

    override suspend fun deleteSenderPreKey(domain: String, clientId: String): Unit = withContext(Dispatchers.IO) {
        signalPreKeyDAO.deleteSignalSenderKey(domain, clientId)
    }

    override suspend fun deleteGroupSenderKey(senderKeyName: SenderKeyName) = withContext(Dispatchers.IO) {
        senderKeyStore.deleteSenderKey(senderKeyName)
    }

    override suspend fun deleteGroupSenderKey(groupId: String, groupSender: String): Unit = withContext(Dispatchers.IO) {
        signalKeyDAO.deleteSignalSenderKey(groupId, groupSender)
    }

    override suspend fun hasSenderKey(senderKeyName: SenderKeyName): Boolean = withContext(Dispatchers.IO) {
        printlnCK("SignalKeyRepositoryImpl hasSenderKey ${senderKeyName.serialize()}")
        return@withContext senderKeyStore.hasSenderKey(senderKeyName)
    }

    override suspend fun storeSenderKey(senderKeyName: SenderKeyName, record: SenderKeyRecord) = withContext(Dispatchers.IO) {
        printlnCK("SignalKeyRepositoryImpl storeSenderKey ${senderKeyName.serialize()}")
        senderKeyStore.storeSenderKey(senderKeyName, record)
    }

    override suspend fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord = withContext(Dispatchers.IO) {
        return@withContext senderKeyStore.loadSenderKey(senderKeyName)
    }

    override suspend fun getUserKey(serverDomain: String, userId: String): com.clearkeep.domain.model.UserKey = withContext(Dispatchers.IO) {
        return@withContext userKeyDAO.getKey(serverDomain, serverDomain) ?: com.clearkeep.domain.model.UserKey(
            serverDomain,
            serverDomain,
            "",
            ""
        )
    }

    override suspend fun registerSenderKeyToGroup(
        server: com.clearkeep.domain.model.Server,
        deviceId: Int,
        groupId: Long,
        clientKeyDistribution: SenderKeyDistributionMessage,
        encryptedGroupPrivateKey: ByteArray?,
        key: ECKeyPair,
        senderKeyId: Int,
        senderKey: ByteArray
    ): com.clearkeep.common.utilities.network.Resource<Any> =
        withContext(Dispatchers.IO) {
            try {
                val response = signalKeyDistributionService.registerClientKey(
                    server,
                    deviceId,
                    groupId,
                    clientKeyDistribution,
                    encryptedGroupPrivateKey,
                    key,
                    senderKeyId,
                    senderKey
                )
                if (response?.error.isNullOrEmpty()) {
                    return@withContext com.clearkeep.common.utilities.network.Resource.success(null)
                }
                return@withContext com.clearkeep.common.utilities.network.Resource.error("", null)
            } catch (e: StatusRuntimeException) {
                printlnCK("registerSenderKeyToGroup: $e")

                val parsedError = parseError(e)
                return@withContext com.clearkeep.common.utilities.network.Resource.error(
                    parsedError.message,
                    null,
                    parsedError.code,
                    parsedError.cause
                )
            } catch (e: Exception) {
                printlnCK("registerSenderKeyToGroup: $e")
                return@withContext com.clearkeep.common.utilities.network.Resource.error("", null, error = e)
            }
        }

    override suspend fun getGroupClientKey(server: com.clearkeep.domain.model.Server, groupId: Long, fromClientId: String): Signal.GroupGetClientKeyResponse? = withContext(Dispatchers.IO) {
        return@withContext try {
            signalKeyDistributionService.getGroupClientKey(server, groupId, fromClientId)
        } catch (e: StatusRuntimeException) {
            null
        } catch (e: java.lang.Exception) {
            printlnCK("initSessionUserInGroup:${fromClientId} ${e.message}")
            null
        }
    }
}
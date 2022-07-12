package com.clearkeep.data.repository.signal

import android.util.Log
import com.clearkeep.data.local.clearkeep.userkey.UserKeyDAO
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.data.local.signal.identitykey.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.senderkey.SignalKeyDAO
import com.clearkeep.data.local.signal.prekey.SignalPreKeyDAO
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.signal.model.toLocal
import com.clearkeep.data.repository.userkey.toModel
import com.clearkeep.data.repository.utils.parseError
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.GroupClientKeyObject
import com.clearkeep.domain.model.response.GroupGetClientKeyResponse
import com.clearkeep.domain.model.response.PeerGetClientKeyResponse
import com.clearkeep.domain.repository.SenderKeyStore
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.ecc.ECKeyPair
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import javax.inject.Inject

class SignalKeyRepositoryImpl @Inject constructor(
    private val senderKeyStore: SenderKeyStore,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val signalPreKeyDAO: SignalPreKeyDAO,
    private val userKeyDAO: UserKeyDAO,
    private val signalKeyDAO: SignalKeyDAO,
    private val signalKeyDistributionService: SignalKeyDistributionService
) : SignalKeyRepository {
    override suspend fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey? =
        withContext(Dispatchers.IO) {
            return@withContext signalIdentityKeyDAO.getIdentityKey(clientId, domain)?.toEntity()
        }

    override suspend fun saveIdentityKey(signalIdentityKey: SignalIdentityKey) =
        withContext(Dispatchers.IO) {
            signalIdentityKeyDAO.insert(signalIdentityKey.toLocal())
        }

    override suspend fun deleteIdentityKeyByOwnerDomain(domain: String, clientId: String): Unit =
        withContext(Dispatchers.IO) {
            signalIdentityKeyDAO
                .deleteSignalKeyByOwnerDomain(domain, clientId)
        }

    override suspend fun deleteSenderPreKey(domain: String, clientId: String): Unit =
        withContext(Dispatchers.IO) {
            signalPreKeyDAO.deleteSignalSenderKey(domain, clientId)
        }

    override suspend fun deleteGroupSenderKey(senderKeyName: SenderKeyName) =
        withContext(Dispatchers.IO) {
            senderKeyStore.deleteSenderKey(senderKeyName)
        }

    override suspend fun deleteGroupSenderKey(groupId: String, groupSender: String): Unit =
        withContext(Dispatchers.IO) {
            signalKeyDAO.deleteSignalSenderKey(groupId, groupSender)
        }

    override suspend fun hasSenderKey(senderKeyName: SenderKeyName): Boolean =
        withContext(Dispatchers.IO) {
            printlnCK("SignalKeyRepositoryImpl hasSenderKey ${senderKeyName.serialize()}")
            return@withContext senderKeyStore.hasSenderKey(senderKeyName)
        }

    override suspend fun storeSenderKey(senderKeyName: SenderKeyName, record: SenderKeyRecord) =
        withContext(Dispatchers.IO) {
            printlnCK("SignalKeyRepositoryImpl storeSenderKey ${senderKeyName.serialize()}")
            senderKeyStore.storeSenderKey(senderKeyName, record)
        }

    override suspend fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord =
        withContext(Dispatchers.IO) {
            val senderKey=senderKeyStore.loadSenderKey(senderKeyName)
            Log.d("antx: ", "SignalKeyRepositoryImpl loadSenderKey line = 81:${senderKeyName} ${senderKey.isEmpty} " );
            return@withContext senderKey
        }

    override suspend fun getUserKey(serverDomain: String, userId: String): UserKey =
        withContext(Dispatchers.IO) {
            return@withContext userKeyDAO.getKey(serverDomain, userId)?.toModel() ?: UserKey(
                serverDomain,
                serverDomain,
                "",
                ""
            )
        }

    override suspend fun registerSenderKeyToGroup(
        server: Server,
        deviceId: Int,
        groupId: Long,
        clientKeyDistribution: SenderKeyDistributionMessage,
        encryptedGroupPrivateKey: ByteArray?,
        key: ECKeyPair,
        senderKeyId: Int,
        senderKey: ByteArray
    ): Resource<Any> =
        withContext(Dispatchers.IO) {
            try {
                val response = signalKeyDistributionService.groupRegisterClientKey(
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
                    return@withContext Resource.success(null)
                }
                return@withContext Resource.error("", null)
            } catch (e: StatusRuntimeException) {
                printlnCK("registerSenderKeyToGroup: $e")

                val parsedError = parseError(e)
                return@withContext Resource.error(
                    parsedError.message,
                    null,
                    parsedError.code,
                    parsedError.cause
                )
            } catch (e: Exception) {
                printlnCK("registerSenderKeyToGroup: $e")
                return@withContext Resource.error("", null, error = e)
            }
        }

    override suspend fun getGroupClientKey(
        server: Server,
        groupId: Long,
        fromClientId: String
    ): GroupGetClientKeyResponse? = withContext(Dispatchers.IO) {
        return@withContext try {
            val rawResponse =
                signalKeyDistributionService.getGroupClientKey(server, groupId, fromClientId)
            rawResponse.run {
                val clientKey = rawResponse.clientKey.run {
                    GroupClientKeyObject(
                        workspaceDomain,
                        clientId,
                        deviceId,
                        clientKeyDistribution.toByteArray()
                    )
                }
                GroupGetClientKeyResponse(groupId, clientKey)
            }
        } catch (e: StatusRuntimeException) {
            null
        } catch (e: java.lang.Exception) {
            printlnCK("initSessionUserInGroup:${fromClientId} ${e.message}")
            null
        }
    }

    override suspend fun getPeerClientKey(
        server: Server,
        clientId: String,
        domain: String
    ): PeerGetClientKeyResponse? = withContext(Dispatchers.IO) {
        return@withContext try {
            val rawResponse =
                signalKeyDistributionService.getPeerClientKey(server, clientId, domain)
            rawResponse.run {
                PeerGetClientKeyResponse(
                    clientId,
                    workspaceDomain,
                    registrationId,
                    deviceId,
                    identityKeyPublic.toByteArray(),
                    preKeyId,
                    preKey.toByteArray(),
                    signedPreKeyId,
                    signedPreKey.toByteArray(),
                    signedPreKeySignature.toByteArray(),
                    identityKeyEncrypted
                )
            }
        } catch (e: StatusRuntimeException) {
            null
        } catch (e: java.lang.Exception) {
            null
        }
    }
}
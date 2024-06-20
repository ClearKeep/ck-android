package com.clearkeep.domain.usecase.chat

import android.text.TextUtils
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.SENDER_DEVICE_ID
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.domain.repository.SignalProtocolStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.lang.Exception
import javax.inject.Inject

class ProcessPeerKeyUseCase @Inject constructor(
    private val serverRepository: ServerRepository,
    private val signalProtocolStore: SignalProtocolStore,
    private val signalKeyRepository: SignalKeyRepository,
) {
    suspend operator fun invoke(
        receiverId: String,
        receiverWorkspaceDomain: String,
        senderId: String,
        ownerWorkSpace: String
    ): Boolean = withContext(Dispatchers.IO) {
        val signalProtocolAddress =
            CKSignalProtocolAddress(
                Owner(
                    receiverWorkspaceDomain,
                    receiverId
                ), null,SENDER_DEVICE_ID
            )
        val signalProtocolAddress2 =
            CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId),null, SENDER_DEVICE_ID)
        initSessionUserPeer(
            signalProtocolAddress2,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
        return@withContext initSessionUserPeer(
            signalProtocolAddress,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
    }

    private suspend fun initSessionUserPeer(
        signalProtocolAddress: CKSignalProtocolAddress,
        signalProtocolStore: SignalProtocolStore,
        owner: Owner
    ): Boolean = withContext(Dispatchers.IO) {
        val remoteClientId = signalProtocolAddress.owner.clientId
        printlnCK("initSessionUserPeer with $remoteClientId, domain = ${signalProtocolAddress.owner.domain}")
        if (TextUtils.isEmpty(remoteClientId)) {
            return@withContext false
        }
        try {
            val server = serverRepository.getServerByOwner(owner)
            if (server == null) {
                printlnCK("initSessionUserPeer: server must be not null")
                return@withContext false
            }

            val remoteKeyBundle = signalKeyRepository.getPeerClientKey(
                server,
                remoteClientId,
                signalProtocolAddress.owner.domain
            ) ?: return@withContext false

            val preKey = PreKeyRecord(remoteKeyBundle.preKey)
            val signedPreKey = SignedPreKeyRecord(remoteKeyBundle.signedPreKey)
            val identityKeyPublic = IdentityKey(remoteKeyBundle.identityKeyPublic, 0)

            val retrievedPreKey = PreKeyBundle(
                remoteKeyBundle.registrationId,
                signalProtocolAddress.deviceId,
                preKey.id,
                preKey.keyPair.publicKey,
                remoteKeyBundle.signedPreKeyId,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPublic
            )

            val sessionBuilder = SessionBuilder(signalProtocolStore, signalProtocolAddress)

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey)
            printlnCK("initSessionUserPeer: success")
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("initSessionUserPeer: $e")
        }

        return@withContext false
    }
}
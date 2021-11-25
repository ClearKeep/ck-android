/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.data.local.signal.store

import androidx.annotation.WorkerThread
import com.clearkeep.data.local.signal.dao.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.model.SignalIdentityKeyLocal
import com.clearkeep.domain.repository.Environment
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.util.KeyHelper
import java.util.HashMap

class InMemoryIdentityKeyStore(
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val environment: Environment
) : IdentityKeyStore, Closeable {
    private val trustedKeys: MutableMap<SignalProtocolAddress, IdentityKey> = HashMap()

    private var identityKey: SignalIdentityKeyLocal? = null

    @WorkerThread
    override fun getIdentityKeyPair(): IdentityKeyPair {
        val clientId = environment.getTempServer().profile.userId
        val domain = environment.getTempServer().serverDomain

        if (identityKey == null || identityKey!!.domain != domain || identityKey!!.userId != clientId) {
            identityKey = getOrGenerateIdentityKey(clientId, domain)
        }
        return identityKey!!.identityKeyPair
    }

    @WorkerThread
    override fun getLocalRegistrationId(): Int {
        val clientId = environment.getTempServer().profile.userId
        val domain = environment.getTempServer().serverDomain

        if (identityKey == null || identityKey!!.domain != domain || identityKey!!.userId != clientId) {
            identityKey = getOrGenerateIdentityKey(clientId, domain)
        }

        return identityKey!!.registrationId
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val existing = trustedKeys[address]
        return if (identityKey != existing) {
            trustedKeys[address] = identityKey
            true
        } else {
            false
        }
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        /*val trusted = trustedKeys[address]
        return trusted == null || trusted == identityKey*/
        return true
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return trustedKeys[address]
    }

    fun generateIdentityKeyPair(clientId: String, domain: String): SignalIdentityKeyLocal {
        val identityKeyPair = KeyHelper.generateIdentityKeyPair()
        val registrationID = KeyHelper.generateRegistrationId(false)
        return SignalIdentityKeyLocal(identityKeyPair, registrationID, domain, clientId)
    }


    @WorkerThread
    private fun getOrGenerateIdentityKey(clientId: String, domain: String): SignalIdentityKeyLocal {
        var signalIdentityKey = signalIdentityKeyDAO.getIdentityKey(clientId, domain)

        if (signalIdentityKey == null) {
            signalIdentityKey = generateIdentityKeyPair(clientId, domain)

            signalIdentityKeyDAO.insert(signalIdentityKey)
        }
        return signalIdentityKey
    }

    override fun clear() {
        trustedKeys.clear()
    }
}
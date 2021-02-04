/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store

import androidx.annotation.WorkerThread
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.model.SignalIdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.util.KeyHelper
import java.util.HashMap

class InMemoryIdentityKeyStore(
        private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
) : IdentityKeyStore, Closeable {
    private val trustedKeys: MutableMap<SignalProtocolAddress, IdentityKey> = HashMap()

    private var identityKey: SignalIdentityKey? = null

    @WorkerThread
    override fun getIdentityKeyPair(): IdentityKeyPair {
        if (identityKey == null) {
            identityKey = getOrGenerateIdentityKey()
        }
        return identityKey!!.identityKeyPair
    }

    @WorkerThread
    override fun getLocalRegistrationId(): Int {
        if (identityKey == null) {
            identityKey = getOrGenerateIdentityKey()
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

    override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: IdentityKeyStore.Direction): Boolean {
        val trusted = trustedKeys[address]
        return trusted == null || trusted == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return trustedKeys[address]
    }

    @WorkerThread
    private fun getOrGenerateIdentityKey() : SignalIdentityKey {
        var signalIdentityKey = signalIdentityKeyDAO.getIdentityKey()
        if (signalIdentityKey == null) {
            val identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val registrationID = KeyHelper.generateRegistrationId(false)
            signalIdentityKey = SignalIdentityKey(identityKeyPair, registrationID)
            signalIdentityKeyDAO.insert(signalIdentityKey)
        }
        return signalIdentityKey
    }

    override fun clear() {
        trustedKeys.clear()
    }
}
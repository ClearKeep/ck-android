/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.data.local.signal.store

import com.clearkeep.data.local.signal.prekey.SignalPreKeyDAO
import com.clearkeep.data.local.signal.prekey.SignalPreKey
import com.clearkeep.domain.repository.Environment
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore
import java.io.IOException
import java.util.*
import javax.inject.Singleton

@Singleton
class InMemorySignedPreKeyStore(
    private val preKeyDAO: SignalPreKeyDAO,
    private val environment: Environment
) : SignedPreKeyStore, Closeable {
    private var store: MutableMap<Int, ByteArray> = HashMap()

    @Throws(InvalidKeyIdException::class)
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return try {
            var record = store[signedPreKeyId]
            if (record == null) {
                record = preKeyDAO.getSignedPreKey(signedPreKeyId)?.preKeyRecord ?: null

                if (record != null) {
                    store[signedPreKeyId] = record
                }
            }

            if (record == null) {
                throw InvalidKeyIdException("CKLog_InMemorySignedPreKeyStore, No such prekeyrecord! for $signedPreKeyId")
            }
            SignedPreKeyRecord(record)
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        try {
            return preKeyDAO.getAllSignedPreKey().map { SignedPreKeyRecord(it.preKeyRecord) }
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        store[signedPreKeyId] = record.serialize()
        val server = environment.getTempServer()
        preKeyDAO.insert(
            SignalPreKey(
                signedPreKeyId,
                record.serialize(),
                true,
                server.serverDomain,
                server.profile.userId
            )
        )
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        environment.getTempServer()
        return store.containsKey(signedPreKeyId)
                || (preKeyDAO.getSignedPreKey(signedPreKeyId) != null)
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {}

    override fun clear() {
        store.clear()
    }
}
/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store

import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.db.signal_key.model.SignalPreKey
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.utilities.printlnCK
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore
import java.io.IOException
import java.util.*

class InMemorySignedPreKeyStore(
        private val preKeyDAO: SignalPreKeyDAO,
        private val environment: Environment
) : SignedPreKeyStore, Closeable {
    private var store: MutableMap<Int, ByteArray> = HashMap()

    @Throws(InvalidKeyIdException::class)
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return try {
            printlnCK("loadSignedPreKey: signedPreKeyId: $signedPreKeyId server: ${environment.getTempServer()}")
            var record = store[signedPreKeyId]
            if (record == null) {
                val allKeys = preKeyDAO.getAllSignedPreKey()
                printlnCK("loadSignedPreKey allKeys $allKeys")
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
        return try {
            return preKeyDAO.getAllSignedPreKey().map { SignedPreKeyRecord(it.preKeyRecord) }
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        store[signedPreKeyId] = record.serialize()
        val server = environment.getTempServer()
        preKeyDAO.insert(SignalPreKey(signedPreKeyId, record.serialize(), true, server.serverDomain, server.profile.userId))
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        val server = environment.getTempServer()
        return store.containsKey(signedPreKeyId)
                || (preKeyDAO.getSignedPreKey(signedPreKeyId) != null)
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        /*store.remove(signedPreKeyId);*/
    }

    override fun clear() {
        store.clear()
    }
}
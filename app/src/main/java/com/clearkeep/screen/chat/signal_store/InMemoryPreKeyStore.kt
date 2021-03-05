/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store

import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.db.signal_key.model.SignalPreKey
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import java.io.IOException
import java.util.*

class InMemoryPreKeyStore(
        private val preKeyDAO: SignalPreKeyDAO,
) : PreKeyStore, Closeable {
    private var store: MutableMap<Int, ByteArray> = HashMap()

    @Throws(InvalidKeyIdException::class)
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return try {
            var record = store[preKeyId]
            if (record == null) {
                record = preKeyDAO.getUnSignedPreKey(preKeyId)?.preKeyRecord ?: null
                if (record != null) {
                    store[preKeyId] = record
                }
            }

            if (record == null) {
                throw InvalidKeyIdException("InMemoryPreKeyStore, No such prekeyrecord!")
            }
            PreKeyRecord(record)
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        store[preKeyId] = record.serialize()
        preKeyDAO.insert(SignalPreKey(preKeyId, record.serialize(), false))
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return store.containsKey(preKeyId)
                || (preKeyDAO.getUnSignedPreKey(preKeyId) != null)
    }

    override fun removePreKey(preKeyId: Int) {
        /*store.remove(preKeyId);*/
    }

    override fun clear() {
        store.clear()
    }
}
/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.data.local.signal.store

import android.util.Log
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.signal.prekey.SignalPreKeyDAO
import com.clearkeep.data.local.signal.prekey.SignalPreKey
import com.clearkeep.domain.repository.Environment
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import java.io.IOException
import java.util.*
import javax.inject.Singleton

@Singleton
class InMemoryPreKeyStore(
    private val preKeyDAO: SignalPreKeyDAO,
    private val environment: Environment
) : PreKeyStore, Closeable {
    private var store: MutableMap<Int, ByteArray> = HashMap()

    @Throws(InvalidKeyIdException::class)
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return try {
            val server = environment.getTempServer()
            val index = getIndex(preKeyId)
            var record = store[index]
            if (record == null) {
                record = preKeyDAO.getUnSignedPreKey(
                    preKeyId,
                    server.serverDomain,
                    server.profile.userId
                )?.preKeyRecord
                if (record != null) {
                    store[index] = record
                }
            }

            if (record == null) {
                throw InvalidKeyIdException("CKLog_InMemoryPreKeyStore, No such prekeyrecord for $preKeyId")
            }
            PreKeyRecord(record)
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        val server = environment.getTempServer()
        val index = getIndex(preKeyId)
        store[index] = record.serialize()
        preKeyDAO.insert(
            SignalPreKey(
                preKeyId,
                record.serialize(),
                false,
                server.serverDomain,
                server.profile.userId
            )
        )
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        val server = environment.getTempServer()
        val index = getIndex(preKeyId)
        return store.containsKey(index)
                || (preKeyDAO.getUnSignedPreKey(
            preKeyId,
            server.serverDomain,
            server.profile.userId
        ) != null)
    }

    override fun removePreKey(preKeyId: Int) {
        store.remove(preKeyId);
    }

    override fun clear() {
        store.clear()
    }

    private fun getIndex(preKeyId: Int): Int {
        //Temp fix for hardcoded preKeyId
        val server = environment.getTempServer()
        return (preKeyId.toString() + server.serverDomain + server.profile.userId).hashCode()
    }
}
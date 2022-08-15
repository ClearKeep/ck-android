/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.data.local.signal.store

import org.signal.libsignal.protocol.InvalidMessageException
import org.signal.libsignal.protocol.NoSessionException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import java.io.IOException
import java.util.*
import javax.inject.Singleton

@Singleton
class InMemorySessionStore : SessionStore, Closeable {
    private val sessions: MutableMap<SignalProtocolAddress, ByteArray> = HashMap()

    @Synchronized
    override fun loadSession(remoteAddress: SignalProtocolAddress): SessionRecord {
        return try {
            if (containsSession(remoteAddress)) {
                SessionRecord(sessions[remoteAddress])
            } else {
                SessionRecord()
            }
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>?): MutableList<SessionRecord> {
        val resultSessions: MutableList<SessionRecord> = LinkedList()
        for (remoteAddress in addresses!!) {
            val serialized = sessions[remoteAddress] ?: throw NoSessionException("no session for $remoteAddress")
            try {
                resultSessions.add(SessionRecord(serialized))
            } catch (e: InvalidMessageException) {
                throw java.lang.AssertionError(e)
            }
        }
        return resultSessions
    }

    @Synchronized
    override fun getSubDeviceSessions(name: String): List<Int> {
        val deviceIds: MutableList<Int> = LinkedList()
        for (key in sessions.keys) {
            if (key.name == name &&
                key.deviceId != 1
            ) {
                deviceIds.add(key.deviceId)
            }
        }
        return deviceIds
    }

    @Synchronized
    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        sessions[address] = record.serialize()
    }

    @Synchronized
    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return sessions.containsKey(address)
    }

    @Synchronized
    override fun deleteSession(address: SignalProtocolAddress) {
        sessions.remove(address)
    }

    @Synchronized
    override fun deleteAllSessions(name: String) {
        for (key in sessions.keys) {
            if (key.name == name) {
                sessions.remove(key)
            }
        }
    }

    override fun clear() {
        sessions.clear()
    }
}
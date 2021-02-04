/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store

import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import java.io.IOException
import java.util.*

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

    @Synchronized
    override fun getSubDeviceSessions(name: String): List<Int> {
        val deviceIds: MutableList<Int> = LinkedList()
        for (key in sessions.keys) {
            if (key.name == name &&
                    key.deviceId != 1) {
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
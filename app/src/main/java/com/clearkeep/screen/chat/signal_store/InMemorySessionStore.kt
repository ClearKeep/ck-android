/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store

import com.clearkeep.utilities.printlnCK
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
                printlnCK("InMemorySessionStore loadSession already contain address $remoteAddress}")
                SessionRecord(sessions[remoteAddress])
            } else {
                printlnCK("InMemorySessionStore loadSession new address $remoteAddress}")
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
        printlnCK("InMemorySessionStore storeSession address $address record ${Arrays.toString(record.serialize())}")
        sessions[address] = record.serialize()
    }

    @Synchronized
    override fun containsSession(address: SignalProtocolAddress): Boolean {
        printlnCK("InMemorySessionStore containsSession address $address ${sessions.containsKey(address)}")
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
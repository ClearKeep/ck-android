package com.clearkeep.db.signalkey

import com.clearkeep.db.clearkeep.model.Owner
import org.whispersystems.libsignal.SignalProtocolAddress

class CKSignalProtocolAddress(
    val owner: Owner,
    private var deviceId: Int
) :
    SignalProtocolAddress(getName(owner), deviceId) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun getName(): String {
        return super.getName()
    }

    override fun getDeviceId(): Int {
        return super.getDeviceId()
    }
}

fun getName(owner: Owner): String {
    return "${owner.domain}_${owner.clientId}"
}

package com.clearkeep.domain.model

import org.signal.libsignal.protocol.SignalProtocolAddress

class CKSignalProtocolAddress(
    val owner: Owner,
    val groupId: Long?,
    private var deviceId: Int
) : SignalProtocolAddress(getName(owner,groupId), deviceId) {
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

    fun getGroupID(): Long? = groupId
}

fun getName(owner: Owner, groupId: Long?): String {
    return if (groupId != null)
        "${owner.domain}_${owner.clientId}_$groupId"
    else "${owner.domain}_${owner.clientId}"

}

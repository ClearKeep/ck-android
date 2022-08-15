package com.clearkeep.domain.model

import org.signal.libsignal.protocol.IdentityKeyPair


data class SignalIdentityKey(
    val identityKeyPair: IdentityKeyPair,
    val registrationId: Int,
    val domain: String,
    val userId: String,
    val iv: String? = "",
    val salt: String? = "",
    var id: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignalIdentityKey

        if (identityKeyPair == other.identityKeyPair) return false
        if (registrationId != other.registrationId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identityKeyPair.hashCode()
        result = 31 * result + registrationId
        return result
    }

    override fun toString(): String {
        val keyPair = identityKeyPair.serialize()
        return "key ${keyPair[0]} ${keyPair[1]} ${keyPair[2]} ${keyPair[keyPair.size - 1]} ${keyPair[keyPair.size - 2]} ${keyPair[keyPair.size - 3]} domain $domain userId $userId regId $registrationId"
    }
}
package com.clearkeep.data.local.signal.prekey

import androidx.room.*

@Entity(primaryKeys = ["preKeyId", "domain", "user_id"])
data class SignalPreKey(
    val preKeyId: Int,
    @ColumnInfo(name = "pre_key_record") val preKeyRecord: ByteArray,
    @ColumnInfo(name = "is_signed_key") val isSignedKey: Boolean,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "user_id") val userId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignalPreKey

        if (preKeyId != other.preKeyId) return false
        if (!preKeyRecord.contentEquals(other.preKeyRecord)) return false
        if (isSignedKey != other.isSignedKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = preKeyId
        result = 31 * result + preKeyRecord.contentHashCode()
        result = 31 * result + isSignedKey.hashCode()
        return result
    }

    override fun toString(): String {
        return "key ${preKeyRecord[0]} ${preKeyRecord[1]} ${preKeyRecord[2]} ${preKeyRecord[preKeyRecord.size - 1]} ${preKeyRecord[preKeyRecord.size - 2]} ${preKeyRecord[preKeyRecord.size - 3]} domain $domain userId $userId isSignedKey $isSignedKey"
    }
}
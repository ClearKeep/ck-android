package com.clearkeep.db.signal.model

import androidx.room.*

@Entity
data class SignalPreKey(
        @PrimaryKey val preKeyId: Int,
        @ColumnInfo(name = "pre_key_record") val preKeyRecord: ByteArray,
        @ColumnInfo(name = "is_signed_key") val isSignedKey: Boolean,
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
}
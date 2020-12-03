package com.clearkeep.db.signal.model

import android.util.Base64
import androidx.room.*
import org.whispersystems.libsignal.IdentityKeyPair

@Entity
@TypeConverters(SignalIdentityKeyPairConverter::class)
data class SignalIdentityKey(
        @ColumnInfo(name = "identity_key_pair") val identityKeyPair: IdentityKeyPair,
        @ColumnInfo(name = "registration_id") val registrationId: Int,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

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
}

class SignalIdentityKeyPairConverter {
    @TypeConverter
    fun restore(identityAsString: String): IdentityKeyPair {
        val array = Base64.decode(identityAsString, Base64.DEFAULT)
        return IdentityKeyPair(array)
    }

    @TypeConverter
    fun save(identityKeyPair: IdentityKeyPair): String? {
        return Base64.encodeToString(identityKeyPair.serialize(), Base64.DEFAULT)
    }
}
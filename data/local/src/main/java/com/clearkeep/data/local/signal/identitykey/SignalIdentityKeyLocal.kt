package com.clearkeep.data.local.signal.identitykey

import android.util.Base64
import androidx.room.*
import com.clearkeep.domain.model.SignalIdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair

@Entity(tableName = "SignalIdentityKey")
@TypeConverters(SignalIdentityKeyPairConverter::class)
data class SignalIdentityKeyLocal(
    @ColumnInfo(name = "identity_key_pair") val identityKeyPair: IdentityKeyPair,
    @ColumnInfo(name = "registration_id") val registrationId: Int,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "iv") val iv: String? = "",
    @ColumnInfo(name = "salt") val salt: String? = ""
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignalIdentityKeyLocal

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

    fun toEntity() = SignalIdentityKey(identityKeyPair, registrationId, domain, userId, iv, salt, id)
}

fun SignalIdentityKey.toLocal() = SignalIdentityKeyLocal(identityKeyPair, registrationId, domain, userId, iv, salt)

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
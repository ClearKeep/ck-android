package com.clearkeep.data.local.signal

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.data.local.signal.identitykey.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.senderkey.SignalKeyDAO
import com.clearkeep.data.local.signal.prekey.SignalPreKeyDAO
import com.clearkeep.data.local.signal.model.SignalIdentityKeyLocal
import com.clearkeep.data.local.signal.prekey.SignalPreKey
import com.clearkeep.data.local.signal.senderkey.SignalSenderKey

@Database(
    entities = [
        SignalSenderKey::class,
        SignalPreKey::class,
        SignalIdentityKeyLocal::class,
    ], version = 4, exportSchema = false
)
abstract class SignalKeyDatabase : RoomDatabase() {
    abstract fun signalKeyDao(): SignalKeyDAO
    abstract fun signalIdentityKeyDao(): SignalIdentityKeyDAO
    abstract fun signalPreKeyDao(): SignalPreKeyDAO
}
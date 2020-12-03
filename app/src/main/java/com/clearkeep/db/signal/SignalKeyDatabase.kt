package com.clearkeep.db.signal

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.signal.model.SignalIdentityKey
import com.clearkeep.db.signal.model.SignalPreKey
import com.clearkeep.db.signal.model.SignalSenderKey

@Database(entities = [
    SignalSenderKey::class,
    SignalPreKey::class,
    SignalIdentityKey::class,
], version = 2, exportSchema = false)
abstract class SignalKeyDatabase : RoomDatabase() {
    abstract fun signalKeyDao(): SignalKeyDAO
    abstract fun signalIdentityKeyDao(): SignalIdentityKeyDAO
    abstract fun signalPreKeyDao(): SignalPreKeyDAO
}
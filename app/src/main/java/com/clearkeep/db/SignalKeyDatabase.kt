package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.dao.SignalKeyDAO
import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.db.signal_key.model.SignalIdentityKey
import com.clearkeep.db.signal_key.model.SignalPreKey
import com.clearkeep.db.signal_key.model.SignalSenderKey

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
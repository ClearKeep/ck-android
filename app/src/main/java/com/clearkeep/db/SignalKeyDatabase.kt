package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.signalkey.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signalkey.dao.SignalKeyDAO
import com.clearkeep.db.signalkey.dao.SignalPreKeyDAO
import com.clearkeep.db.signalkey.model.SignalIdentityKey
import com.clearkeep.db.signalkey.model.SignalPreKey
import com.clearkeep.db.signalkey.model.SignalSenderKey

@Database(
    entities = [
        SignalSenderKey::class,
        SignalPreKey::class,
        SignalIdentityKey::class,
    ], version = 3, exportSchema = false
)
abstract class SignalKeyDatabase : RoomDatabase() {
    abstract fun signalKeyDao(): SignalKeyDAO
    abstract fun signalIdentityKeyDao(): SignalIdentityKeyDAO
    abstract fun signalPreKeyDao(): SignalPreKeyDAO
}
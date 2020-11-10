package com.clearkeep.db.signal

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.signal.model.SignalSenderKey

@Database(entities = [
    SignalSenderKey::class,
], version = 1, exportSchema = false)
abstract class SignalKeyDatabase : RoomDatabase() {
    abstract fun signalKeyDao(): SignalKeyDAO
}
package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearkeep.db.clear_keep.model.UserEntity
import com.clearkeep.db.clear_keep.model.UserPreference

@Dao
interface UserPreferenceDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userPreference: UserPreference)

    @Query("SELECT * FROM userpreference WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun getPreference(serverDomain: String, userId: String): UserPreference?

    @Query("UPDATE userpreference SET show_notification_preview = :value WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateNotificationPreview(serverDomain: String, userId: String, value: Boolean)

    @Query("UPDATE userpreference SET notification_sound_vibrate = :value WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateNotificationSoundVibrate(serverDomain: String, userId: String, value: Boolean)

    @Query("UPDATE userpreference SET do_not_disturb = :value WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateDoNotDisturb(serverDomain: String, userId: String, value: Boolean)
}
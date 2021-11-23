package com.clearkeep.data.local.clearkeep.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearkeep.domain.model.UserPreference

@Dao
interface UserPreferenceDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userPreference: UserPreference)

    @Query("SELECT * FROM userpreference WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun getPreference(serverDomain: String, userId: String): UserPreference?

    @Query("SELECT * FROM userpreference WHERE server_domain = :serverDomain AND user_id = :userId")
    fun getPreferenceLiveData(serverDomain: String, userId: String): LiveData<UserPreference>

    @Query("UPDATE userpreference SET show_notification_preview = :value WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateNotificationPreview(serverDomain: String, userId: String, value: Boolean)

    @Query("UPDATE userpreference SET do_not_disturb = :value WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateDoNotDisturb(serverDomain: String, userId: String, value: Boolean)

    @Query("UPDATE userpreference SET mfa = :value WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateMfa(serverDomain: String, userId: String, value: Boolean)
}
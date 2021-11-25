package com.clearkeep.data.local.clearkeep.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.data.local.model.ProfileLocal
import com.clearkeep.data.local.model.ServerLocal
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server

@Dao
interface ServerDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(server: ServerLocal)

    @Query("DELETE FROM server WHERE id =:serverId")
    suspend fun deleteServer(serverId: Int): Int

    @Query("SELECT * FROM server WHERE server_domain = :domain AND owner_client_id = :clientId")
    suspend fun getServer(domain: String, clientId: String): ServerLocal?

    @Query("SELECT * FROM server WHERE is_active = 1 LIMIT 1")
    suspend fun getDefaultServer(): ServerLocal?

    @Query("UPDATE server SET is_active = CASE server_domain WHEN :domain THEN 1 ELSE 0 END")
    suspend fun setDefaultServerByDomain(domain: String)

    @Query("UPDATE server SET owner = :profile WHERE server_domain = :domain")
    suspend fun updateDefaultServerProfile(domain: String, profile: ProfileLocal)

    @Query("SELECT * FROM server")
    suspend fun getServers(): List<ServerLocal>

    @Query("SELECT * FROM server WHERE server_domain = :domain")
    suspend fun getServerByDomain(domain: String): ServerLocal?

    @Query("SELECT * FROM server")
    fun getServersAsState(): LiveData<List<ServerLocal>>

    @Query("SELECT * FROM server WHERE is_active = 1 LIMIT 1")
    fun getDefaultServerAsState(): LiveData<ServerLocal?>
}
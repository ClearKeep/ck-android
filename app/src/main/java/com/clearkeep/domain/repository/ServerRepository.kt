package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.clearkeep.model.Profile
import com.clearkeep.db.clearkeep.model.Server

interface ServerRepository {
    val isLogout : MutableLiveData<Boolean>
    val activeServer : MutableLiveData<Server>
    fun getServersAsState(): LiveData<List<Server>>
    suspend fun getServers(): List<Server>
    suspend fun getServerByDomain(url: String): Server?
    suspend fun getServer(domain: String, ownerId: String): Server?
    suspend fun getServerByOwner(owner: Owner): Server?
    suspend fun insertServer(server: Server)
    suspend fun getDefaultServer(): Server
    suspend fun setActiveServer(server: Server)
    fun getDefaultServerAsState(): LiveData<Server?>
    suspend fun deleteServer(serverId: Int): Int
    fun getDefaultServerProfileAsState(): LiveData<Profile>
    suspend fun updateServerProfile(domain: String, profile: Profile)
    suspend fun getOwnerClientIds(): List<String>
}
package com.clearkeep.repo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.ServerDAO
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.dynamicapi.Environment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    // dao
    private val serverDAO: ServerDAO,
    private val environment: Environment,
) {
    var profile = MutableLiveData<Profile>()

    var activeServer = MutableLiveData<Server>()

    val isLogout = MutableLiveData<Boolean>()

    fun getServersAsState() = serverDAO.getServersAsState()

    suspend fun getServers() = serverDAO.getServers()

    suspend fun getServerByDomain(url: String) = serverDAO.getServerByDomain(url)

    suspend fun getServer(domain: String, ownerId: String) = serverDAO.getServer(domain, ownerId)

    suspend fun getServerByOwner(owner: Owner) = serverDAO.getServer(owner.domain, owner.clientId)

    suspend fun insertServer(server: Server) {
        serverDAO.insert(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    suspend fun getDefaultServer() = serverDAO.getDefaultServer()

    suspend fun setActiveServer(server: Server) {
        environment.setUpDomain(server)
        profile.postValue(server.profile)
        activeServer.postValue(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    fun getDefaultServerAsState() = serverDAO.getDefaultServerAsState()

    suspend fun deleteServer(serverId: Int) : Int{
        return serverDAO.deleteServer(serverId)
    }

    fun getDefaultServerProfileAsState() = serverDAO.getDefaultServerAsState().map {
        it?.profile ?: Profile(null, "", "", "", "", 0L, "")
    }

    suspend fun updateServerProfile(domain: String, profile: Profile) {
        serverDAO.updateDefaultServerProfile(domain, profile)
    }

    suspend fun getOwnerClientIds(): List<String> {
        return getServers().map { it.ownerClientId }
    }
}
package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.clearkeep.db.clearkeep.dao.ServerDAO
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.clearkeep.model.Profile
import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.dynamicapi.Environment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDAO: ServerDAO,
    private val environment: Environment,
): ServerRepository {
    private var profile = MutableLiveData<Profile>()
    override val activeServer = MutableLiveData<Server>()
    override val isLogout = MutableLiveData<Boolean>()

    override fun getServersAsState(): LiveData<List<Server>> = serverDAO.getServersAsState()

    override suspend fun getServers(): List<Server> = serverDAO.getServers()

    override suspend fun getServerByDomain(url: String): Server? = serverDAO.getServerByDomain(url)

    override suspend fun getServer(domain: String, ownerId: String): Server? = serverDAO.getServer(domain, ownerId)

    override suspend fun getServerByOwner(owner: Owner): Server? = serverDAO.getServer(owner.domain, owner.clientId)

    override suspend fun insertServer(server: Server) {
        serverDAO.insert(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    override suspend fun getDefaultServer(): Server = serverDAO.getDefaultServer()

    override suspend fun setActiveServer(server: Server) {
        environment.setUpDomain(server)
        profile.postValue(server.profile)
        activeServer.postValue(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    override fun getDefaultServerAsState(): LiveData<Server?> = serverDAO.getDefaultServerAsState()

    override suspend fun deleteServer(serverId: Int): Int {
        return serverDAO.deleteServer(serverId)
    }

    override fun getDefaultServerProfileAsState(): LiveData<Profile> = serverDAO.getDefaultServerAsState().map {
        it?.profile ?: Profile(null, "", "", "", "", 0L, "")
    }

    override suspend fun updateServerProfile(domain: String, profile: Profile) {
        serverDAO.updateDefaultServerProfile(domain, profile)
    }

    override suspend fun getOwnerClientIds(): List<String> {
        return getServers().map { it.ownerClientId }
    }
}
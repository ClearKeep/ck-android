package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.clearkeep.data.local.clearkeep.dao.ServerDAO
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class ServerRepositoryImpl @Inject constructor(
    private val serverDAO: ServerDAO,
    private val environment: Environment,
) : ServerRepository {
    private var profile = MutableLiveData<Profile>()
    override val activeServer = MutableLiveData<Server>()
    override val isLogout = MutableLiveData<Boolean>()

    override fun getServersAsState(): LiveData<List<Server>> = serverDAO.getServersAsState()

    override suspend fun getServers(): List<Server> =
        withContext(Dispatchers.IO) { return@withContext serverDAO.getServers() }

    override suspend fun getServerByDomain(url: String): Server? = withContext(Dispatchers.IO) {
        return@withContext serverDAO.getServerByDomain(url)
    }

    override suspend fun getServer(domain: String, ownerId: String): Server? =
        withContext(Dispatchers.IO) {
            serverDAO.getServer(domain, ownerId)
        }

    override suspend fun getServerByOwner(owner: Owner): Server? = withContext(Dispatchers.IO) {
        serverDAO.getServer(owner.domain, owner.clientId)
    }

    override suspend fun insertServer(server: Server) = withContext(Dispatchers.IO) {
        serverDAO.insert(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    override suspend fun getDefaultServer(): Server =
        withContext(Dispatchers.IO) { serverDAO.getDefaultServer() }

    override suspend fun setActiveServer(server: Server) = withContext(Dispatchers.IO) {
        environment.setUpDomain(server)
        profile.postValue(server.profile)
        activeServer.postValue(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    override fun getDefaultServerAsState(): LiveData<Server?> = serverDAO.getDefaultServerAsState()

    override suspend fun deleteServer(serverId: Int): Int = withContext(Dispatchers.IO) {
        return@withContext serverDAO.deleteServer(serverId)
    }

    override fun getDefaultServerProfileAsState(): LiveData<Profile> =
        serverDAO.getDefaultServerAsState().map {
            it?.profile ?: Profile(null, "", "", "", "", 0L, "")
        }

    override suspend fun updateServerProfile(domain: String, profile: Profile) =
        withContext(Dispatchers.IO) {
            serverDAO.updateDefaultServerProfile(domain, profile)
        }

    override suspend fun getOwnerClientIds(): List<String> = withContext(Dispatchers.IO) {
        return@withContext getServers().map { it.ownerClientId }
    }
}
package com.clearkeep.repo

import com.clearkeep.db.clear_keep.dao.ServerDAO
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Server
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    // dao
    private val serverDAO: ServerDAO,
) {
    fun getServersAsState() = serverDAO.getServersAsState()

    suspend fun getServers() = serverDAO.getServers()

    suspend fun getServer(domain: String, ownerId: String) = serverDAO.getServer(domain, ownerId)

    suspend fun getServerByOwner(owner: Owner) = serverDAO.getServer(owner.domain, owner.clientId)

    suspend fun insertServer(server: Server) {
        serverDAO.insert(server)
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    suspend fun getDefaultServer() = serverDAO.getDefaultServer()

    suspend fun setDefaultServer(server: Server) {
        serverDAO.setDefaultServerByDomain(server.serverDomain)
    }

    fun getDefaultServerAsState() = serverDAO.getDefaultServerAsState()
}
package com.clearkeep.repo

import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.screen.chat.repo.GroupRepository
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiServerRepository @Inject constructor(
    // dao
    private val groupDAO: GroupDAO,

    // network calls
    private val apiProvider: ParamAPIProvider,

    // data
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,

    private val environment: Environment
) {
    fun getAllRooms() = groupDAO.getRoomsAsState()

    fun getClientId() = environment.getServer().profile.id

    suspend fun fetchGroups() = withContext(Dispatchers.IO) {
        printlnCK("fetchGroups")
        val server = serverRepository.getServers()
        server?.forEach { server ->
            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val groupGrpc = apiProvider.provideGroupBlockingStub(paramAPI)
            val signalGrpc = apiProvider.provideSignalKeyDistributionBlockingStub(paramAPI)
            try {
                val groups = groupRepository.getRoomsFromAPI(groupGrpc, server.profile.id)
                for (group in groups) {
                    printlnCK("fetchGroups: ${group.toString()}")
                    val decryptedGroup = groupRepository.convertGroupFromResponse(group, signalGrpc, server.serverDomain, server.profile.id)
                    groupDAO.insert(decryptedGroup)
                }
            } catch(exception: Exception) {
                printlnCK("fetchGroups: $exception")
            }
        }
    }

    suspend fun getGroupByID(groupId: Long, domain: String, ownerClientId: String) : ChatGroup? {
        printlnCK("getGroupByIDzz: groupId = $groupId, $domain, owner = $ownerClientId")
        val server = serverRepository.getServer(domain, ownerClientId)
        if (server == null) {
            printlnCK("getGroupByIDzz: null server")
            return null
        }
        val signalGrpc = apiProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
        return groupRepository.getGroupByIDWithGrpc(groupId, domain, ownerClientId, signalGrpc)
    }
}
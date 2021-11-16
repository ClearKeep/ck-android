package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.data.remote.GroupService
import com.clearkeep.db.clearkeep.dao.UserDAO
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.clearkeep.model.User
import com.clearkeep.db.clearkeep.model.UserEntity
import com.clearkeep.domain.repository.PeopleRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepositoryImpl @Inject constructor(
    private val peopleDao: UserDAO,
    private val environment: Environment,
    private val serverRepository: ServerRepository,
    private val groupService: GroupService
): PeopleRepository {
    override fun getFriends(ownerDomain: String, ownerClientId: String): LiveData<List<User>> =
        peopleDao.getFriends(ownerDomain, ownerClientId).map { list ->
            if (list.isNotEmpty()) {
                val response = groupService.getUsersInServer()
                val activeUserIds = response.lstUserOrBuilderList.map { it.id }

                list.filter { it.userId in activeUserIds }
                    .map { userEntity -> convertEntityToUser(userEntity) }
                    .sortedBy { it.userName.toLowerCase() }
            } else {
                emptyList()
            }
        }

    override suspend fun getFriend(friendClientId: String, friendDomain: String, owner: Owner): User? =
        withContext(Dispatchers.IO) {
            printlnCK("getFriend: $friendClientId + $friendDomain")
            val ret =
                peopleDao.getFriend(friendClientId, friendDomain, owner.domain, owner.clientId)
            return@withContext if (ret != null) convertEntityToUser(ret) else null
        }

    override suspend fun getFriendFromID(friendClientId: String): User? = withContext(Dispatchers.IO) {
        val ret = peopleDao.getFriendFromUserId(friendClientId)
        printlnCK("getFriendFromID: $ret id: $friendClientId")
        return@withContext if (ret != null) convertEntityToUser(ret) else null
    }

    override suspend fun updatePeople(): Resource<Nothing> {
        try {
            val friends = getFriendsFromAPI()
            if (friends.status == com.clearkeep.utilities.network.Status.SUCCESS) {
                printlnCK("updatePeople: $friends")
                peopleDao.insertPeopleList(friends.data ?: emptyList())
            } else {
                return Resource.error(friends.message ?: "", null, friends.errorCode)
            }
            return Resource.success(null)
        } catch (exception: Exception) {
            printlnCK("updatePeople: $exception")
            return Resource.error(exception.toString(), null)
        }
    }

    override suspend fun updateAvatarUserEntity(user: User, owner: Owner): UserEntity? {
        val userEntity = peopleDao.getFriend(user.userId, user.domain, owner.domain, owner.clientId)
        if (userEntity != null) {
            userEntity.avatar = user.avatar
            peopleDao.insert(userEntity)
        }
        return userEntity
    }

    override suspend fun deleteFriend(clientId: String) {
        try {
            val result = peopleDao.deleteFriend(clientId)
            if (result > 0) {
                printlnCK("deleteFriend: clientId: $clientId")
            } else {
                printlnCK("deleteFriend: clientId: $clientId fail")
            }
        } catch (e: Exception) {
            printlnCK("updatePeople:Exception $e")
        }
    }

    override suspend fun insertFriend(friend: User, owner: Owner) {
        val oldUser = peopleDao.getFriend(
            friend.userId,
            friend.domain,
            ownerDomain = owner.domain,
            ownerClientId = owner.clientId
        )
        if (oldUser == null) {
            peopleDao.insert(
                UserEntity(
                    userId = friend.userId,
                    userName = friend.userName,
                    domain = friend.domain,
                    ownerDomain = owner.domain,
                    ownerClientId = owner.clientId,
                    phoneNumber = friend.phoneNumber,
                    email = friend.email,
                    avatar = friend.avatar

                )
            )
        }

    }

    private suspend fun getFriendsFromAPI(): Resource<List<UserEntity>> =
        withContext(Dispatchers.IO) {
            printlnCK("getFriendsFromAPI")
            try {
                val response = groupService.getUsersInServer()
                return@withContext Resource.success(response.lstUserOrBuilderList
                    .map { userInfoResponse ->
                        convertUserResponse(
                            userInfoResponse,
                            Owner(
                                environment.getServer().serverDomain,
                                environment.getServer().profile.userId
                            )
                        )
                    })
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("getFriendsFromAPI token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }

                return@withContext Resource.error(message, emptyList(), parsedError.code)
            } catch (e: Exception) {
                printlnCK("getFriendsFromAPI: $e")
                return@withContext Resource.error(e.toString(), emptyList())
            }
        }

    private suspend fun convertUserResponse(
        userInfoResponse: UserOuterClass.UserInfoResponseOrBuilder,
        owner: Owner
    ): UserEntity {
        val oldUser = peopleDao.getFriend(
            userInfoResponse.id, userInfoResponse.workspaceDomain,
            ownerDomain = owner.domain, ownerClientId = owner.clientId
        )
        return UserEntity(
            generateId = oldUser?.generateId ?: null,
            userId = userInfoResponse.id,
            userName = userInfoResponse.displayName,
            domain = userInfoResponse.workspaceDomain,
            ownerDomain = owner.domain,
            ownerClientId = owner.clientId,
            avatar = oldUser?.avatar
        )
    }

    private fun convertEntityToUser(userEntity: UserEntity): User {
        return User(
            userId = userEntity.userId,
            userName = userEntity.userName,
            domain = userEntity.domain,
            avatar = userEntity.avatar
        )
    }

    override suspend fun sendPing(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = groupService.sendPing()
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("sendPing token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext false
        } catch (e: Exception) {
            printlnCK("sendPing: $e")
            return@withContext false
        }
    }

    override suspend fun updateStatus(status: String?): Boolean = withContext(Dispatchers.IO) {
        printlnCK("updateStatus")
        try {
            val response = groupService.updateStatus(status)
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("updateStatus token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext false
        } catch (e: Exception) {
            printlnCK("updateStatus: $e")
            return@withContext false
        }
    }

    override suspend fun getListClientStatus(list: List<User>): List<User>? = withContext(Dispatchers.IO) {
        try {
            val response = groupService.getListClientStatus(list)
            list.map { user ->
                val newUser = response.lstClientList.find {
                    user.userId == it.clientId
                }
                user.userStatus = newUser?.status
                user.avatar = newUser?.avatar
                printlnCK("avata: ${user.avatar}")
            }
            return@withContext list
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("getListClientStatus token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            printlnCK("updateStatus: $e")
            return@withContext null
        }
    }

    override suspend fun getUserInfo(userId: String, userDomain: String): Resource<User> =
        withContext(Dispatchers.IO) {
            try {
                val response = groupService.getUserInfo(userId, userDomain)

                return@withContext Resource.success(
                    User(
                        response.id,
                        response.displayName,
                        response.workspaceDomain
                    )
                )
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)

                val errorMessage = when (parsedError.code) {
                    1008, 1005 -> "Profile link is incorrect."
                    1077 -> {
                        printlnCK("getUserInfo token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> {
                        if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
                            "Network error,We are unable to detect an internet connection. Please try again when you have a stronger connection."
                        } else {
                            parsedError.message
                        }
                    }
                }
                printlnCK("getUserInfo exception: $e")
                return@withContext Resource.error(errorMessage, null)
            } catch (e: Exception) {
                printlnCK("getUserInfo exception: $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }
}
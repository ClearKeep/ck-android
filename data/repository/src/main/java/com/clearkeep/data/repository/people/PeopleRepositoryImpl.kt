package com.clearkeep.data.repository.people

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status.SUCCESS
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.clearkeep.user.UserDAO
import com.clearkeep.data.remote.service.GroupService
import com.clearkeep.data.repository.utils.parseError
import com.clearkeep.domain.model.User
import com.clearkeep.domain.model.UserEntity
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.repository.PeopleRepository
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import javax.inject.Inject

class PeopleRepositoryImpl @Inject constructor(
    private val peopleDao: UserDAO,
    private val environment: Environment,
    private val groupService: GroupService
): PeopleRepository {
    override fun getFriendsAsState(ownerDomain: String, ownerClientId: String): LiveData<List<User>> =
        peopleDao.getFriends(ownerDomain, ownerClientId).map { list ->
            if (list.isNotEmpty()) {
                val response = groupService.getUsersInServer()
                val activeUserIds = response.lstUserOrBuilderList.map { it.id }

                list.filter { it.userId in activeUserIds }
                    .map { userEntity -> convertEntityToUser(userEntity.toModel()) }
                    .sortedBy { it.userName.toLowerCase() }
            } else {
                emptyList()
            }
        }

    override suspend fun getFriend(friendClientId: String, friendDomain: String, owner: com.clearkeep.domain.model.Owner): User? =
        withContext(Dispatchers.IO) {
            val ret =
                peopleDao.getFriend(friendClientId, friendDomain, owner.domain, owner.clientId)
            return@withContext if (ret != null) convertEntityToUser(ret.toModel()) else null
        }

    override suspend fun getFriendFromID(friendClientId: String): User? = withContext(Dispatchers.IO) {
        val ret = peopleDao.getFriendFromUserId(friendClientId)
        printlnCK("getFriendFromID: $ret id: $friendClientId")
        return@withContext if (ret != null) convertEntityToUser(ret.toModel()) else null
    }

    override suspend fun updatePeople(): Resource<Nothing> = withContext(Dispatchers.IO) {
        try {
            val friends = getFriendsFromAPI()
            if (friends.status == SUCCESS) {
                printlnCK("updatePeople: $friends")
                peopleDao.insertPeopleList(friends.data?.map { it.toEntity() } ?: emptyList())
            } else {
                return@withContext Resource.error(friends.message ?: "", null, friends.errorCode)
            }
            return@withContext Resource.success(null)
        } catch (exception: Exception) {
            printlnCK("updatePeople: $exception")
            return@withContext Resource.error(exception.toString(), null)
        }
    }

    override suspend fun updateAvatarUserEntity(user: User, owner: com.clearkeep.domain.model.Owner): UserEntity? = withContext(Dispatchers.IO) {
        val userEntity = peopleDao.getFriend(user.userId, user.domain, owner.domain, owner.clientId)
        if (userEntity != null) {
            userEntity.avatar = user.avatar
            userEntity.userName = user.userName
            peopleDao.insert(userEntity)
        }
        return@withContext userEntity?.toModel()
    }

    override suspend fun deleteFriend(clientId: String): Unit = withContext(Dispatchers.IO) {
        try {
            peopleDao.deleteFriend(clientId)
        } catch (e: Exception) {
            printlnCK("updatePeople:Exception $e")
        }
    }

    override suspend fun insertFriend(friend: User, owner: com.clearkeep.domain.model.Owner) = withContext(Dispatchers.IO) {
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
                ).toEntity()
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
                            com.clearkeep.domain.model.Owner(
                                environment.getServer().serverDomain,
                                environment.getServer().profile.userId
                            )
                        )
                    })
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                return@withContext Resource.error(parsedError.message, emptyList(), parsedError.code)
            } catch (e: Exception) {
                printlnCK("getFriendsFromAPI: $e")
                return@withContext Resource.error(e.toString(), emptyList())
            }
        }

    private suspend fun convertUserResponse(
        userInfoResponse: UserOuterClass.UserInfoResponseOrBuilder,
        owner: com.clearkeep.domain.model.Owner
    ): UserEntity = withContext(Dispatchers.IO) {
        val oldUser = peopleDao.getFriend(
            userInfoResponse.id, userInfoResponse.workspaceDomain,
            ownerDomain = owner.domain, ownerClientId = owner.clientId
        )
        return@withContext UserEntity(
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
            return@withContext false
        } catch (e: Exception) {
            printlnCK("updateStatus: $e")
            return@withContext false
        }
    }

    override suspend fun getListClientStatus(list: List<User>): List<User>? = withContext(Dispatchers.IO) {
        try {
            val response = groupService.getListClientStatus(list)
            Log.d("---", response.toString())
            list.map { user ->
                val newUser = response.lstClientList.find {
                    user.userId == it.clientId
                }
                user.userStatus = newUser?.status
                user.avatar = newUser?.avatar
                user.userName = newUser?.displayName.orEmpty()
                peopleDao.updateAvatar(newUser?.avatar.orEmpty(), user.userId)
                peopleDao.updateUserName(newUser?.displayName.orEmpty(), user.userId)
                printlnCK("avata: ${user.avatar}")
                printlnCK("username: ${user.userName}")
            }
            printlnCK("list: ${list}")
            return@withContext list
        } catch (e: StatusRuntimeException) {
            return@withContext emptyList()
        } catch (e: Exception) {
            printlnCK("updateStatus: $e")
            return@withContext null
        }
    }

    override suspend fun getUserInfo(userId: String, userDomain: String): com.clearkeep.common.utilities.network.Resource<User> =
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

    override suspend fun getFriendByEmail(emailHard: String): List<User> =
        withContext(Dispatchers.IO) {
            return@withContext groupService.findUserByEmail(emailHard)
        }

    override suspend fun getListUserEntity(): List<User> {
        return peopleDao.getListUserEntity().map {
            convertEntityToUser(it.toModel())
        }
    }
}
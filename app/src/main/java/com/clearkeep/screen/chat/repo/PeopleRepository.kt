package com.clearkeep.screen.chat.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.UserDao
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserEntity
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    // dao
    private val peopleDao: UserDao,

    private val environment: Environment,

    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val serverRepository: ServerRepository
) {
    fun getFriends(ownerDomain: String, ownerClientId: String) : LiveData<List<User>> = peopleDao.getFriends(ownerDomain, ownerClientId).map { list ->
        list.map { userEntity -> convertEntityToUser(userEntity) }.sortedBy { it.userName.toLowerCase() }
    }

    suspend fun getFriend(friendClientId: String, friendDomain: String, owner: Owner) : User? = withContext(Dispatchers.IO) {
        printlnCK("getFriend: $friendClientId + $friendDomain")
        val ret = peopleDao.getFriend(friendClientId, friendDomain, owner.domain, owner.clientId)
        return@withContext if (ret != null) convertEntityToUser(ret) else null
    }

    suspend fun getFriendFromID(friendClientId: String) : User? = withContext(Dispatchers.IO) {
        val ret = peopleDao.getFriendFromUserId(friendClientId)
        printlnCK("getFriendFromID: ${ret?.avatar}  id: $friendClientId")
        return@withContext if (ret != null) convertEntityToUser(ret) else null
    }


    suspend fun searchUser(userName: String) : List<User>  = withContext(Dispatchers.IO) {
        printlnCK("searchUser: $userName")
        try {
            val request = UserOuterClass.SearchUserRequest.newBuilder()
                    .setKeyword(userName).build()
            val response = dynamicAPIProvider.provideUserBlockingStub().searchUser(request)
            return@withContext response.lstUserOrBuilderList
                    .map { userInfoResponseOrBuilder ->
                        User(
                            userId = userInfoResponseOrBuilder.id,
                            userName = userInfoResponseOrBuilder.displayName,
                            domain = userInfoResponseOrBuilder.workspaceDomain,
                        )
                    }
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            printlnCK("searchUser: $e")
            return@withContext emptyList()
        }
    }

    suspend fun updatePeople() {
        try {
            val friends = getFriendsFromAPI()
            if (friends.isNotEmpty()) {
                printlnCK("updatePeople: $friends")
                peopleDao.insertPeopleList(friends)
            }
        } catch(exception: Exception) {
            printlnCK("updatePeople: $exception")
        }
    }

    suspend fun updateAvatarUserEntity(user: User, owner: Owner): UserEntity? {
        val userEntity = peopleDao.getFriend(user.userId, user.domain, owner.domain, owner.clientId)
        if (userEntity != null) {
            userEntity.avatar = user.avatar
            peopleDao.insert(userEntity)
        }
        return userEntity
    }

    suspend fun deleteFriend(clientId: String) {
        try {
            val result = peopleDao.deleteFriend(clientId)
            if (result > 0) {
                printlnCK("deleteFriend: clientId: ${clientId}")
            } else {
                printlnCK("deleteFriend: clientId: ${clientId} fail")
            }
        } catch (e: Exception) {
            printlnCK("updatePeople:Exception $e")
        }
    }

    suspend fun insertFriend(friend: User, owner: Owner) {
        val oldUser = peopleDao.getFriend(friend.userId, friend.domain, ownerDomain = owner.domain, ownerClientId = owner.clientId)
        if (oldUser == null) {
            peopleDao.insert(
                UserEntity(
                    userId = friend.userId,
                    userName = friend.userName,
                    domain = friend.domain,
                    ownerDomain = owner.domain,
                    ownerClientId = owner.clientId,
                    phoneNumber = friend.phoneNumber,
                    email =  friend.email,
                    avatar = friend.avatar

                )
            )
        }

    }

    private suspend fun getFriendsFromAPI() : List<UserEntity>  = withContext(Dispatchers.IO) {
        printlnCK("getFriendsFromAPI")
        try {
            val request = UserOuterClass.Empty.newBuilder()
                .build()
            val response = dynamicAPIProvider.provideUserBlockingStub().getUsers(request)
            return@withContext response.lstUserOrBuilderList
                .map { userInfoResponse ->
                    convertUserResponse(userInfoResponse, Owner(environment.getServer().serverDomain, environment.getServer().profile.userId))
                }
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }

            return@withContext emptyList()
        } catch (e: Exception) {
            printlnCK("getFriendsFromAPI: $e")
            return@withContext emptyList()
        }
    }

    private suspend fun convertUserResponse(userInfoResponse: UserOuterClass.UserInfoResponseOrBuilder, owner: Owner) : UserEntity {
        val oldUser = peopleDao.getFriend(userInfoResponse.id, userInfoResponse.workspaceDomain,
            ownerDomain = owner.domain, ownerClientId = owner.clientId)
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

    private fun convertEntityToUser(userEntity: UserEntity) : User {
        return User(
            userId = userEntity.userId,
            userName = userEntity.userName,
            domain = userEntity.domain,
            avatar = userEntity.avatar
        )
    }

    suspend fun sendPing(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass.PingRequest.newBuilder().build()
            val response = dynamicAPIProvider.provideUserBlockingStub().pingRequest(request)
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
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

     suspend fun updateStatus(status:String?) : Boolean= withContext(Dispatchers.IO){
        printlnCK("updateStatus")
        try {
            val request = UserOuterClass.SetUserStatusRequest.newBuilder().setStatus(status).build()
            val response = dynamicAPIProvider.provideUserBlockingStub().updateStatus(request)
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
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

     suspend fun getListClientStatus(list: List<User>):List<User>? = withContext(Dispatchers.IO){
        try {
            printlnCK("getListClientStatus")

            val listMemberInfoRequest= list.map {
                UserOuterClass.MemberInfoRequest.newBuilder().setClientId(it.userId).setWorkspaceDomain(it.domain).build()
            }
            val request = UserOuterClass.GetClientsStatusRequest.newBuilder().addAllLstClient(listMemberInfoRequest).setShouldGetProfile(false)
                .build()
            val response = dynamicAPIProvider.provideUserBlockingStub().getClientsStatus(request)
            list.map { user ->
                val newUser = response.lstClientList.find {
                    user.userId == it.clientId
                }
                user.userStatus = newUser?.status
                // user.avatar = newUser?.avatar
            }
            return@withContext list
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
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

    suspend fun getUserInfo(userId: String, userDomain: String): Resource<User> =
        withContext(Dispatchers.IO) {
            try {
                val request = UserOuterClass.GetUserRequest.newBuilder()
                    .setClientId(userId)
                    .setWorkspaceDomain(userDomain)
                    .build()

                val response = dynamicAPIProvider.provideUserBlockingStub().getUserInfo(request)

                printlnCK("getUserInfo response display name ${response.displayName} id ${response.id}")

                return@withContext Resource.success(User(response.id, response.displayName, response.workspaceDomain))
            } catch (e: StatusRuntimeException) {
                val rawError = parseError(e)
                val errorMessage = when (rawError.code) {
                    1008, 1005 -> "Profile link is incorrect."
                    else -> rawError.message
                }
                printlnCK("getUserInfo exception: $e")
                return@withContext Resource.error(errorMessage, null)
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
                return@withContext Resource.error("", null)
            } catch (e: Exception) {
                printlnCK("getUserInfo exception: $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }
}
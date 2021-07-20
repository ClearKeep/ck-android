package com.clearkeep.screen.chat.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.UserDao
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserEntity
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.utilities.printlnCK
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
) {
    fun getFriends(ownerDomain: String, ownerClientId: String) : LiveData<List<User>> = peopleDao.getFriends(ownerDomain, ownerClientId).map { list ->
        list.map { userEntity -> convertEntityToUser(userEntity) }.sortedBy { it.userName.toLowerCase() }
    }

    suspend fun getFriend(friendClientId: String, friendDomain: String, owner: Owner) : User? = withContext(Dispatchers.IO) {
        printlnCK("getFriend: $friendClientId + $friendDomain")
        val ret = peopleDao.getFriend(friendClientId, friendDomain, owner.domain, owner.clientId)
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

    suspend fun insertFriend(friend: User, owner: Owner) {
        val oldUser = peopleDao.getFriend(friend.userId, friend.domain, ownerDomain = owner.domain, ownerClientId = owner.clientId)
        if (oldUser == null) {
            peopleDao.insert(
                UserEntity(
                    userId = friend.userId,
                    userName = friend.userName,
                    domain = friend.domain,
                    ownerDomain = owner.domain,
                    ownerClientId = owner.clientId
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
            ownerClientId = owner.clientId
        )
    }

    private fun convertEntityToUser(userEntity: UserEntity) : User {
        return User(
            userId = userEntity.userId,
            userName = userEntity.userName,
            domain = userEntity.domain
        )
    }
}
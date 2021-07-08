package com.clearkeep.screen.chat.repo

import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.UserDao
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.dynamicapi.DynamicAPIProvider
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

    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,
) {
    fun getFriends() = peopleDao.getFriends().map { list ->
        list.sortedBy { it.userName.toLowerCase() }
    }

    suspend fun updatePeople() {
        try {
            val friends = getFriendsFromAPI()
            if (friends.isNotEmpty()) {
                printlnCK("updatePeople: $friends")
                insertFriends(friends)
            }
        } catch(exception: Exception) {
            printlnCK("updatePeople: $exception")
        }
    }

    suspend fun getFriend(friendId: String, domain: String) : User? = withContext(Dispatchers.IO) {
        printlnCK("getFriend: $friendId + $domain")
        return@withContext peopleDao.getFriend(friendId, domain)
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
                            ownerDomain = userInfoResponseOrBuilder.workspaceDomain,
                        )
                    }
        } catch (e: Exception) {
            printlnCK("searchUser: $e")
            return@withContext emptyList()
        }
    }

    suspend fun insertFriend(friend: User) {
        val oldGroup = peopleDao.getFriend(friend.userId, friend.ownerDomain)
        if (oldGroup != null) {
            peopleDao.insert(
                User(
                    generateId = oldGroup.generateId,
                    userId = friend.userId,
                    userName = friend.userName,
                    ownerDomain = friend.ownerDomain
                )
            )
        } else {
            peopleDao.insert(friend)
        }
    }

    private suspend fun insertFriends(friends: List<User>) {
        peopleDao.insertPeopleList(friends)
    }

    private suspend fun getFriendsFromAPI() : List<User>  = withContext(Dispatchers.IO) {
        printlnCK("getFriendsFromAPI")
        try {
            val request = UserOuterClass.Empty.newBuilder()
                .build()
            val response = dynamicAPIProvider.provideUserBlockingStub().getUsers(request)
            return@withContext response.lstUserOrBuilderList
                .map { userInfoResponse ->
                    convertUserResponse(userInfoResponse)
                }
        } catch (e: Exception) {
            printlnCK("getFriendsFromAPI: $e")
            return@withContext emptyList()
        }
    }

    private suspend fun convertUserResponse(userInfoResponse: UserOuterClass.UserInfoResponseOrBuilder) : User {
        val oldGroup = peopleDao.getFriend(userInfoResponse.id, userInfoResponse.workspaceDomain)
        return User(
            generateId = oldGroup?.generateId ?: null,
            userId = userInfoResponse.id,
            userName = userInfoResponse.displayName,
            ownerDomain = userInfoResponse.workspaceDomain,
        )
    }
}
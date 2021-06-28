package com.clearkeep.screen.chat.repo

import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.UserDao
import com.clearkeep.db.clear_keep.model.User
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

    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,

    private val environment: Environment
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

    suspend fun getFriend(friendId: String) : User? = withContext(Dispatchers.IO) {
        val friend = peopleDao.getFriend(friendId)
        return@withContext friend ?: getFriendFromAPI(friendId)
    }

    suspend fun getFriends(idList: List<String>): List<User> = withContext(Dispatchers.IO) {
        return@withContext idList.mapNotNull { id ->
            peopleDao.getFriend(id)
        }
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
                            userInfoResponseOrBuilder.id,
                            userInfoResponseOrBuilder.displayName,
                            userInfoResponseOrBuilder.workspaceDomain,
                        )
                    }
        } catch (e: Exception) {
            printlnCK("searchUser: $e")
            return@withContext emptyList()
        }
    }

    suspend fun insertFriend(friend: User) {
        peopleDao.insert(friend)
    }

    private suspend fun insertFriends(friends: List<User>) {
        peopleDao.insertPeopleList(friends)
    }

    private suspend fun getFriendsFromAPI() : List<User>  = withContext(Dispatchers.IO) {
        printlnCK("getFriendsFromAPI")
        try {
            val server = environment.getServer()
            val request = UserOuterClass.Empty.newBuilder()
                .build()
            val response = dynamicAPIProvider.provideUserBlockingStub().getUsers(request)
            return@withContext response.lstUserOrBuilderList
                .map { userInfoResponseOrBuilder ->
                    User(
                        userInfoResponseOrBuilder.id,
                        userInfoResponseOrBuilder.displayName,
                        userInfoResponseOrBuilder.workspaceDomain,
                    )
                }
        } catch (e: Exception) {
            printlnCK("getFriendsFromAPI: $e")
            return@withContext emptyList()
        }
    }

    private suspend fun getFriendFromAPI(friendId: String) : User?  = withContext(Dispatchers.IO) {
        try {
            val server = environment.getServer()
            val request = UserOuterClass.GetUserRequest.newBuilder().setClientId(friendId)
                .build()
            val response = dynamicAPIProvider.provideUserBlockingStub().getUserInfo(request)
            return@withContext User(
                response.id,
                response.displayName,
                response.workspaceDomain,
            )
        } catch (e: Exception) {
            printlnCK("getFriendFromAPI: $e")
            return@withContext null
        }
    }
}
package com.clearkeep.repo

import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.PeopleDao
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserGrpc
import user.UserOuterClass
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
        private val peopleDao: PeopleDao,
        private val userStub: UserGrpc.UserBlockingStub
) {
    fun getFriends(clientId: String) = liveData {
        emitSource(
                peopleDao.getFriends().map {
                    Resource.success(it)
                }
        )
        /*val disposable = emitSource(
                peopleDao.getFriends().map {
                    Resource.loading(it)
                }
        )
        try {
            val friends = getFriendsFromAPI()
            // Stop the previous emission to avoid dispatching the updated user
            // as `loading`.
            disposable.dispose()
            // Update the database.
            insertFriends(friends)
            // Re-establish the emission with success type.
            emitSource(
                    peopleDao.getFriends().map {
                        Resource.success(it)
                    }
            )
        } catch(exception: IOException) {
            // Any call to `emit` disposes the previous one automatically so we don't
            // need to dispose it here as we didn't get an updated value.
            emitSource(
                    peopleDao.getFriends().map {
                        Resource.error(exception.toString(), it)
                    }
            )
        }*/
    }

    suspend fun updatePeople() {
        try {
            val friends = getFriendsFromAPI()
            if (friends.isNotEmpty()) {
                insertFriends(friends)
            }
        } catch(exception: IOException) {
        }
    }

    suspend fun getFriend(friendId: String) : People? = withContext(Dispatchers.IO) {
        val friend = peopleDao.getFriend(friendId)
        return@withContext friend ?: getFriendFromAPI(friendId)
    }

    suspend fun getFriends(idList: List<String>): List<People> = withContext(Dispatchers.IO) {
        return@withContext idList.mapNotNull { id ->
            peopleDao.getFriend(id)
        }
    }

    suspend fun searchUser(userName: String) : List<People>  = withContext(Dispatchers.IO) {
        printlnCK("searchUser: $userName")
        try {
            val request = UserOuterClass.SearchUserRequest.newBuilder()
                    .setKeyword(userName).build()
            val response = userStub.searchUser(request)
            return@withContext response.lstUserOrBuilderList
                    .map { userInfoResponseOrBuilder ->
                        People(
                            userInfoResponseOrBuilder.id,
                            userInfoResponseOrBuilder.displayName
                        )
                    }
        } catch (e: Exception) {
            printlnCK("searchUser: $e")
            return@withContext emptyList()
        }
    }

    private suspend fun insertFriends(friends: List<People>) {
        peopleDao.insertPeopleList(friends)
    }

    private suspend fun getFriendsFromAPI() : List<People>  = withContext(Dispatchers.IO) {
        printlnCK("getFriendsFromAPI")
        try {
            val request = UserOuterClass.Empty.newBuilder()
                .build()
            val response = userStub.getUsers(request)
            printlnCK("getFriendsFromAPI: $response")
            return@withContext response.lstUserOrBuilderList
                .map { userInfoResponseOrBuilder ->
                    People(
                        userInfoResponseOrBuilder.id,
                        userInfoResponseOrBuilder.displayName
                    )
                }
        } catch (e: Exception) {
            printlnCK("getFriendsFromAPI: $e")
            return@withContext emptyList()
        }
    }

    private suspend fun getFriendFromAPI(friendId: String) : People?  = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass.GetUserRequest.newBuilder().setClientId(friendId)
                .build()
            val response = userStub.getUserInfo(request)
            return@withContext People(
                response.id,
                response.displayName
            )
        } catch (e: Exception) {
            printlnCK("getFriendFromAPI: $e")
            return@withContext null
        }
    }
}
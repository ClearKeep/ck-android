package com.clearkeep.chat.repositories

import com.clearkeep.db.PeopleDao
import com.clearkeep.db.model.People
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserGrpc
import user.UserOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val peopleDao: PeopleDao,
    private val userStub: UserGrpc.UserBlockingStub
) {
    fun getFriends() = peopleDao.getPeople()

    suspend fun addFriend(people: People) = withContext(Dispatchers.IO) {
        peopleDao.insert(people)
    }

    suspend fun searchUser(userName: String) : List<People>  = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass.SearchUserRequest.newBuilder()
                .setKeyword(userName).build()
            val response = userStub.searchUser(request)
            return@withContext response.lstUserOrBuilderList
                .map { userInfoResponseOrBuilder ->
                    People(userInfoResponseOrBuilder.username
                    )
                }
        } catch (e: Exception) {
            printlnCK("searchUser: $e")
            return@withContext emptyList()
        }
    }

    suspend fun getProfile() : List<People>  = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass.Empty.newBuilder()
                .build()
            val response = userStub.getProfile(request)
            return@withContext listOf(People(response.username))
        } catch (e: Exception) {
            printlnCK("getProfile: $e")
            return@withContext emptyList()
        }
    }
}
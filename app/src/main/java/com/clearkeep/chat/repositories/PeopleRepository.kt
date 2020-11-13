package com.clearkeep.chat.repositories

import com.clearkeep.db.PeopleDao
import com.clearkeep.db.model.People
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val peopleDao: PeopleDao,
) {
    fun getFriends() = peopleDao.getPeople()

    suspend fun addFriend(people: People) = withContext(Dispatchers.IO) {
        peopleDao.insert(people)
    }
}
package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.model.UserEntity
import com.clearkeep.common.utilities.network.Resource

interface PeopleRepository {
    fun getFriendsAsState(ownerDomain: String, ownerClientId: String): LiveData<List<User>>
    suspend fun getFriend(friendClientId: String, friendDomain: String, owner: Owner): User?
    suspend fun getFriendFromID(friendClientId: String): User?
    suspend fun updatePeople(): Resource<Nothing>
    suspend fun updateAvatarUserEntity(user: User, owner: Owner): UserEntity?
    suspend fun deleteFriend(clientId: String)
    suspend fun insertFriend(friend: User, owner: Owner)
    suspend fun sendPing(): Boolean
    suspend fun updateStatus(status: String?): Boolean
    suspend fun getListClientStatus(list: List<User>): List<User>?
    suspend fun getUserInfo(userId: String, userDomain: String): Resource<User>
    suspend fun getFriendByEmail(emailHard:String): List<User>
    suspend fun getListUserEntity(): List<User>
}
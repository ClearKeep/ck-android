package com.clearkeep.features.chat.presentation.groupinvite

import android.util.Log
import androidx.lifecycle.*
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.User
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.people.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class InviteGroupViewModel @Inject constructor(
    private val environment: Environment,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val insertFriendUseCase: InsertFriendUseCase,
    private val updatePeopleUseCase: UpdatePeopleUseCase,
    private val getFriendUseCase: GetFriendUseCase,
    getFriendsUseCase: GetFriendsUseCase
) : ViewModel() {
    fun getClientId() = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain

    private var textSearch = MutableLiveData<String>()

    val friends: LiveData<List<User>> = getFriendsUseCase(getDomain(), getClientId())

    val checkUserUrlResponse = MutableLiveData<Resource<User>>()

    private var checkUserUrlJob: Job? = null

    private val friendsByEmail = MutableLiveData<List<User>>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    val filterFriends = liveData<List<User>> {
        val result = MediatorLiveData<List<User>>()
        result.addSource(friends) { _ ->
        }
        result.addSource(textSearch) { text ->
            result.value = getFilterFriends(friends.value ?: emptyList(), text)
        }
        result.addSource(friendsByEmail) {
            result.value = friendsByEmail.value
        }
        emitSource(result)
    }

    private fun getFilterFriends(list: List<User>, search: String): List<User> {
        if (search.isEmpty()) return emptyList()
        return list.filter { search.isBlank() || it.userName.toLowerCase().contains(search) }
    }

    fun insertFriend(people: User) {
        viewModelScope.launch {
            _isLoading.value = true
            insertFriendUseCase(people, owner = getOwner())
            _isLoading.value = false
        }
    }

    private fun getOwner(): com.clearkeep.domain.model.Owner {
        return com.clearkeep.domain.model.Owner(getDomain(), getClientId())
    }

    fun search(text: String) {
        textSearch.value = text.trim().toLowerCase()
    }

    fun updateContactList() {
        printlnCK("update contact list from remote API")
        viewModelScope.launch {
            updatePeopleUseCase()
        }
    }

    fun findEmail(email: String) {
        viewModelScope.launch {
            Log.d("antx: ", "InviteGroupViewModel findEmail line = 97:$email " );
            var hashUser = sha256(email)
            val result = getFriendUseCase.getFriendByEmail(hashEmail = hashUser ?: "")
            result.forEach {
                Log.d("antx: ", "InviteGroupViewModel findEmail line = 93: $it");
            }
            friendsByEmail.postValue(result.filter { return@filter it.userId!=getClientId() })
        }
    }

    fun checkUserUrlValid(userId: String, userDomain: String) {
        checkUserUrlJob?.cancel()

        checkUserUrlJob = viewModelScope.launch {
            _isLoading.value = true
            checkUserUrlResponse.value = getUserInfoUseCase(userId, userDomain)
            _isLoading.value = false
        }
    }

    fun sha256(base: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(base.toByteArray(charset("UTF-8")))
            val hexString = StringBuilder()
            for (i in hash.indices) {
                val hex = Integer.toHexString(0xff and hash[i].toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

}
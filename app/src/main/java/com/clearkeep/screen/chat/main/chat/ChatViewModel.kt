package com.clearkeep.screen.chat.main.chat

import androidx.lifecycle.ViewModel
import com.clearkeep.db.model.People
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.screen.chat.main.people.PeopleRepository
import com.clearkeep.screen.chat.repositories.ChatRepository
import com.clearkeep.screen.chat.repositories.GroupRepository
import com.clearkeep.utilities.UserManager
import javax.inject.Inject

class ChatViewModel @Inject constructor(
        private val roomRepository: GroupRepository,
        private val profileRepository: ProfileRepository,
        private val chatRepository: ChatRepository,
        private val userManager: UserManager
): ViewModel() {
    fun getClientId() = profileRepository.getClientId()

    fun getClientName() = userManager.getUserName()

    fun getChatHistoryList() = roomRepository.getAllRooms()
}

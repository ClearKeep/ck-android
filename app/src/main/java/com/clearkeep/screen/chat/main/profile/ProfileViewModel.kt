package com.clearkeep.screen.chat.main.profile

import androidx.lifecycle.ViewModel
import com.clearkeep.screen.chat.repositories.ChatRepository
import com.clearkeep.screen.chat.repositories.RoomRepository
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: RoomRepository,
): ViewModel() {
    fun logout() {

    }
}

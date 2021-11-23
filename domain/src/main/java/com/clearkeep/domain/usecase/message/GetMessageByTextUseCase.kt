package com.clearkeep.domain.usecase.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.presentation.screen.chat.contactsearch.MessageSearchResult
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessageByTextUseCase @Inject constructor(private val messageRepository: MessageRepository, private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    operator fun invoke(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<MessageSearchResult>> {
        return messageRepository.getMessageByText(ownerDomain, ownerClientId, query).asFlow().map {
            val groupsList = it.map { it.groupId }.distinct().map {
                val server = serverRepository.getServer(ownerDomain, ownerClientId)
                groupRepository.getGroupByID(it, ownerDomain, ownerClientId, server, false).data
            }
            val clientList =
                groupsList.map { it?.clientList ?: emptyList() }.flatten().distinctBy { it.userId }
            val result = it.map { message ->
                MessageSearchResult(
                    message,
                    clientList.find { message.senderId == it.userId },
                    groupsList.find { message.groupId == it?.groupId })
            }
            result
        }.asLiveData()
    }

}
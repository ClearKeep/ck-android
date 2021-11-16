package com.clearkeep.presentation.screen.chat.contact_search

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.presentation.components.CKSimpleTheme
import com.clearkeep.domain.model.User
import com.clearkeep.presentation.screen.chat.room.RoomActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchUserActivity : AppCompatActivity() {
    private val searchViewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKSimpleTheme {
                SearchUserScreen(
                    searchViewModel,
                    navigateToPeerChat = { people ->
                        if (people != null) {
                            searchViewModel.insertFriend(people)
                            navigateToRoomScreen(people)
                        }
                    },
                    navigateToChatGroup = {
                        navigateToChatGroup(it)
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }
    }

    private fun navigateToRoomScreen(friend: User) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.DOMAIN, searchViewModel.getDomainOfActiveServer())
        intent.putExtra(RoomActivity.CLIENT_ID, searchViewModel.getClientIdOfActiveServer())
        intent.putExtra(RoomActivity.FRIEND_ID, friend.userId)
        intent.putExtra(RoomActivity.FRIEND_DOMAIN, friend.domain)
        startActivity(intent)
    }

    private fun navigateToChatGroup(chatGroup: ChatGroup) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, chatGroup.groupId)
        intent.putExtra(RoomActivity.DOMAIN, searchViewModel.getDomainOfActiveServer())
        intent.putExtra(RoomActivity.CLIENT_ID, searchViewModel.getClientIdOfActiveServer())
        startActivity(intent)
    }
}
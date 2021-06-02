package com.clearkeep.screen.chat.group_create

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.screen.chat.group_invite.InviteGroupScreen
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.compose.*
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.group_invite.InsertFriendScreen

@AndroidEntryPoint
class CreateGroupActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val createGroupViewModel: CreateGroupViewModel by viewModels {
        viewModelFactory
    }

    private val inviteGroupViewModel: InviteGroupViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDirectChat = intent.getBooleanExtra(EXTRA_IS_DIRECT_CHAT, false)

        setContent {
            val navController = rememberNavController()
            val selectedItem = remember { mutableStateListOf<People>() }
            CKSimpleTheme {
                NavHost(navController, startDestination = "invite_group") {
                    composable("invite_group") {
                        inviteGroupViewModel.updateContactList()
                        InviteGroupScreen(
                            inviteGroupViewModel,
                            selectedItem = selectedItem,
                            onFriendSelected = { friends ->
                                createGroupViewModel.setFriendsList(friends)
                                navController.navigate("enter_group_name")
                            },

                            onDirectFriendSelected = { handleDirectChat(it) },
                            onInsertFriend = {
                                navController.navigate("insert_friend")
                            },
                            onBackPressed = {
                                finish()
                            },
                            isCreatePeerGroup = isDirectChat
                        )
                    }
                    composable("enter_group_name") {
                        EnterGroupNameScreen(
                                navController,
                                createGroupViewModel,
                        )
                    }
                    composable("insert_friend") {
                        InsertFriendScreen(
                            navController,
                            onInsertFriend = { people ->
                                inviteGroupViewModel.insertFriend(people)
                                selectedItem.add(people)
                                onBackPressed()
                            },
                        )
                    }
                }
            }
        }

        subscribe()
    }

    private fun handleDirectChat(people: People) {
        val intent = Intent()
        intent.putExtra(EXTRA_PEOPLE_ID, people.id)
        intent.putExtra(EXTRA_IS_DIRECT_CHAT, true)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun subscribe() {
        createGroupViewModel.createGroupState.observe(this, {
            if (it == CreateGroupSuccess) {
                val intent = Intent()
                intent.putExtra(EXTRA_GROUP_ID, createGroupViewModel.groupId)
                intent.putExtra(EXTRA_IS_DIRECT_CHAT, false)
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_PEOPLE_ID = "extra_people_id"
        const val EXTRA_IS_DIRECT_CHAT = "extra_is_direct"
    }
}
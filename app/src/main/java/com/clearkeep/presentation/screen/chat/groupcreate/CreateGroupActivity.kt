package com.clearkeep.presentation.screen.chat.groupcreate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.MutableLiveData
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.presentation.screen.chat.groupinvite.InviteGroupScreen
import com.clearkeep.presentation.screen.chat.groupinvite.InviteGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.clearkeep.presentation.components.CKSimpleTheme
import com.clearkeep.domain.model.User
import com.clearkeep.presentation.screen.chat.groupinvite.InsertFriendScreen
import com.clearkeep.presentation.screen.chat.groupinvite.InviteMemberUIType
import com.clearkeep.utilities.printlnCK

@AndroidEntryPoint
class CreateGroupActivity : AppCompatActivity() {
    private val createGroupViewModel: CreateGroupViewModel by viewModels()
    private val inviteGroupViewModel: InviteGroupViewModel by viewModels()

    private var isDirectChat: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDirectChat = intent.getBooleanExtra(EXTRA_IS_DIRECT_CHAT, false)

        setContent {
            val navController = rememberNavController()
            val selectedItem = remember { mutableStateListOf<User>() }
            CKSimpleTheme {
                NavHost(navController, startDestination = "invite_group") {
                    composable("invite_group") {
                        inviteGroupViewModel.updateContactList()
                        InviteGroupScreen(
                            InviteMemberUIType,
                            inviteGroupViewModel,
                            selectedItem = selectedItem,
                            chatGroup = MutableLiveData(),
                            onFriendSelected = { friends ->
                                createGroupViewModel.setFriendsList(friends)
                                navController.navigate("enter_group_name")
                            },
                            onDirectFriendSelected = {
                                inviteGroupViewModel.insertFriend(it)
                                handleDirectChat(it)
                            },
                            onBackPressed = {
                                finish()
                            },
                            isCreateDirectGroup = isDirectChat
                        )
                    }
                    composable("enter_group_name") {
                        EnterGroupNameScreen(
                            navController,
                            createGroupViewModel,
                        )
                    }
                    composable("insert_friend") {
                        inviteGroupViewModel.updateContactList()
                        InsertFriendScreen(
                            inviteGroupViewModel,
                            navController,
                            onInsertFriend = { people ->
                                printlnCK("people insert = $people")
                                inviteGroupViewModel.insertFriend(people)
                                if (!isDirectChat) {
                                    selectedItem.add(people)
                                }
                                onBackPressed()
                            },
                        )
                    }
                }
            }
        }

        subscribe()
    }

    private fun handleDirectChat(people: User) {
        val intent = Intent()
        intent.putExtra(EXTRA_PEOPLE_ID, people.userId)
        intent.putExtra(EXTRA_PEOPLE_DOMAIN, people.domain)
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
        const val EXTRA_PEOPLE_DOMAIN = "extra_people_domain"
        const val EXTRA_IS_DIRECT_CHAT = "extra_is_direct"
    }
}
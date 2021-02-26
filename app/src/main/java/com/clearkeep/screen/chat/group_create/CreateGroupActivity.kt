package com.clearkeep.screen.chat.group_create

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
import com.clearkeep.components.CKTheme
import com.clearkeep.screen.chat.group_invite.InviteGroupScreen
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.compose.*
import com.clearkeep.db.clear_keep.model.People

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

        setContent {
            val navController = rememberNavController()
            val selectedItem = remember { mutableStateListOf<People>() }
            CKTheme {
                NavHost(navController, startDestination = "invite_group") {
                    composable("invite_group") {
                        inviteGroupViewModel.updateContactList()
                        InviteGroupScreen(
                                inviteGroupViewModel,
                                onFriendSelected = { friends ->
                                    createGroupViewModel.setFriendsList(friends)
                                    navController.navigate("enter_group_name")
                                },
                                onBackPressed = {
                                    finish()
                                },
                                isSelectionOnly = true,
                            selectedItem = selectedItem,
                        )
                    }
                    composable("enter_group_name") {
                        EnterGroupNameScreen(
                                navController,
                                createGroupViewModel,
                        )
                    }
                }
            }
        }

        subscribe()
    }

    private fun subscribe() {
        createGroupViewModel.createGroupState.observe(this, {
            if (it == CreateGroupSuccess) {
                finish()
            }
        })
    }
}
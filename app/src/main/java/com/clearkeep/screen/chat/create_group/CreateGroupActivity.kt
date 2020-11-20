package com.clearkeep.screen.chat.create_group

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKScaffold
import com.clearkeep.screen.chat.invite_group.InviteGroupScreen
import com.clearkeep.screen.chat.invite_group.InviteGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.compose.*

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
            CKScaffold {
                NavHost(navController, startDestination = "invite_group") {
                    composable("invite_group") {
                        InviteGroupScreen(
                                navController,
                                inviteGroupViewModel,
                                onFriendSelected = { friends ->
                                    createGroupViewModel.setFriendsList(friends)
                                    navController.navigate("enter_group_name")
                                }
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
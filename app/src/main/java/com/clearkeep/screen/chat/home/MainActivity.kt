package com.clearkeep.screen.chat.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.screen.auth.login.LoginActivity
import com.clearkeep.screen.chat.contact_search.SearchUserActivity
import com.clearkeep.screen.chat.group_create.CreateGroupActivity
import com.clearkeep.screen.chat.group_create.CreateGroupActivity.Companion.EXTRA_IS_DIRECT_CHAT
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.services.ChatService
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.chat.banned_users.BannedUserActivity
import com.clearkeep.screen.chat.change_pass_word.ChangePasswordActivity
import com.clearkeep.screen.chat.invite.InviteActivity
import com.clearkeep.screen.chat.notification_setting.NotificationSettingActivity
import com.clearkeep.screen.chat.profile.ProfileActivity
import com.clearkeep.screen.chat.settings.ServerSettingActivity
import com.clearkeep.utilities.restartToRoot

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory
    }

    private val startCreateGroupForResult =
        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    val isDirectChat = intent.getBooleanExtra(EXTRA_IS_DIRECT_CHAT, true)
                    if (isDirectChat) {
                        val friendId = intent.getStringExtra(CreateGroupActivity.EXTRA_PEOPLE_ID)
                        val friendDomain = intent.getStringExtra(CreateGroupActivity.EXTRA_PEOPLE_DOMAIN)
                        if (!friendId.isNullOrBlank() && !friendDomain.isNullOrBlank()) {
                            navigateToRoomScreenWithFriendId(friendId, friendDomain)
                        }
                    } else {
                        val groupId = intent.getLongExtra(CreateGroupActivity.EXTRA_GROUP_ID, -1)
                        if (groupId > 0) {
                            navigateToRoomScreen(groupId)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        setContent {
            CKSimpleTheme {
                homeViewModel.prepareState.observeAsState().value.let { prepareState ->
                    when (prepareState) {
                        PrepareSuccess -> {
                            HomeScreen(
                                homeViewModel,
                                gotoSearch = {
                                    navigateToSearchScreen()
                                },
                                createGroupChat = {
                                    navigateToCreateGroupScreen(isDirectGroup = it)
                                },
                                gotoRoomById = {
                                    navigateToRoomScreen(it)
                                },
                                onlogOut = {
                                    logout()
                                },
                                onJoinServer = {
                                    navigateToJoinServer(it)
                                },
                                onNavigateServerSetting = {
                                    navigateToServerSettingScreen()
                                },
                                onNavigateAccountSetting = {
                                    navigateToProfileScreen()
                                },
                                onNavigateNotificationSetting = {
                                    navigateToNotificationSettingScreen()
                                },
                                onNavigateInvite = {
                                    navigateToInviteScreen()
                                },
                                onNavigateBannedUser = {
                                    navigateToBannedUserScreen()
                                }
                            )
                        }
                        PrepareProcessing -> {
                            LoadingComposable()
                        }
                        PrepareError -> {
                            ErrorComposable()
                        }
                    }
                }
            }
        }
        subscriberLogout()
        homeViewModel.prepare()
    }

    @Composable
    private fun LoadingComposable() {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CKCircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorComposable() {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Please try again")
            Spacer(Modifier.height(20.dp))
            CKButton(
                "Try again",
                onClick = {
                },
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        printlnCK("CK app go into background")
        stopChatService()

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        printlnCK("CK app go into foregrounded")
        startChatService()
    }

    private fun startChatService() {
        Intent(this, ChatService::class.java).also { intent ->
            startService(intent)
        }
    }

    private fun stopChatService() {
        Intent(this, ChatService::class.java).also { intent ->
            stopService(intent)
        }
    }

    private fun subscriberLogout() {
        homeViewModel.isLogOutCompleted.observe(this, { completed ->
            if (completed) {
                restartToRoot(this)
            }
        })
    }

    @Composable
    private fun LogoutProgress() {
        homeViewModel.isLogOutProcessing.observeAsState().value?.let { isProcessing ->
            if (isProcessing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = Color.Blue)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Log out...",
                            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    private fun logout() {
        homeViewModel.logOut()
    }

    private fun navigateToProfileScreen() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToServerSettingScreen() {
        val intent = Intent(this, ServerSettingActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToChangePassword(){
        val intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(groupId: Long) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, groupId)
        intent.putExtra(RoomActivity.DOMAIN, homeViewModel.getDomainOfActiveServer())
        intent.putExtra(RoomActivity.CLIENT_ID, homeViewModel.getClientIdOfActiveServer())
        startActivity(intent)
    }

    private fun navigateToRoomScreenWithFriendId(friendId: String, friendDomain: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friendId)
        intent.putExtra(RoomActivity.FRIEND_DOMAIN, friendDomain)
        intent.putExtra(RoomActivity.DOMAIN, homeViewModel.getDomainOfActiveServer())
        intent.putExtra(RoomActivity.CLIENT_ID, homeViewModel.getClientIdOfActiveServer())
        startActivity(intent)
    }

    private fun navigateToCreateGroupScreen(isDirectGroup: Boolean) {
        val intent = Intent(this, CreateGroupActivity::class.java)
        intent.putExtra(EXTRA_IS_DIRECT_CHAT, isDirectGroup)
        startCreateGroupForResult.launch(intent)
    }

    private fun navigateToSearchScreen() {
        val intent = Intent(this, SearchUserActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToJoinServer(domain: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.IS_JOIN_SERVER, true)
        intent.putExtra(LoginActivity.SERVER_DOMAIN, domain)
        startActivity(intent)
    }

    private fun navigateToNotificationSettingScreen() {
        val intent = Intent(this, NotificationSettingActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToInviteScreen() {
        val intent = Intent(this, InviteActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToBannedUserScreen() {
        val intent = Intent(this, BannedUserActivity::class.java)
        startActivity(intent)
    }
}
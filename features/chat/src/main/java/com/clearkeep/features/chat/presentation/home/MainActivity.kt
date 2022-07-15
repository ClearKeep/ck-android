package com.clearkeep.features.chat.presentation.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.*
import com.clearkeep.common.presentation.components.CKSimpleTheme
import com.clearkeep.common.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
import com.clearkeep.common.presentation.components.base.CKButton
import com.clearkeep.common.presentation.components.base.CKCircularProgressIndicator
import com.clearkeep.features.chat.presentation.bannedusers.BannedUserActivity
import com.clearkeep.common.utilities.sdp
import com.clearkeep.features.chat.presentation.contactsearch.SearchUserActivity
import com.clearkeep.features.chat.presentation.groupcreate.CreateGroupActivity
import com.clearkeep.features.chat.presentation.groupcreate.CreateGroupActivity.Companion.EXTRA_IS_DIRECT_CHAT
import com.clearkeep.features.chat.presentation.invite.InviteActivity
import com.clearkeep.features.chat.presentation.notificationsetting.NotificationSettingActivity
import com.clearkeep.features.chat.presentation.profile.ProfileActivity
import com.clearkeep.features.chat.presentation.room.RoomActivity
import com.clearkeep.features.chat.presentation.settings.ServerSettingActivity
import com.clearkeep.navigation.NavigationUtils

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LifecycleObserver {
    private val homeViewModel: HomeViewModel by viewModels()

    private val startCreateGroupForResult =
        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    val isDirectChat = intent.getBooleanExtra(EXTRA_IS_DIRECT_CHAT, true)
                    if (isDirectChat) {
                        val friendId = intent.getStringExtra(CreateGroupActivity.EXTRA_PEOPLE_ID)
                        val friendDomain =
                            intent.getStringExtra(CreateGroupActivity.EXTRA_PEOPLE_DOMAIN)
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

        homeViewModel.isLogout.observe(this) {
            if (it) {
                homeViewModel.signOut()
            }
        }

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
                                onSignOut = {
                                    signOut()
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
                                }
                            ) {
                                navigateToBannedUserScreen()
                            }
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
        Column(
            modifier = Modifier
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Please try again")
            Spacer(Modifier.height(20.sdp()))
            CKButton(
                "Try again",
                onClick = {
                },
                modifier = Modifier.padding(vertical = 5.sdp())
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
        NavigationUtils.startChatService(this)
    }

    private fun stopChatService() {
        NavigationUtils.stopChatService(this)
    }

    private fun subscriberLogout() {
        homeViewModel.shouldReLogin.observe(this) { completed ->
            if (completed) {
                NavigationUtils.restartToRoot(this)
            }
        }
    }

    private fun signOut() {
        Log.d("antx: ", "MainActivity signOut line = 186: ");
        homeViewModel.signOut()
    }

    private fun navigateToProfileScreen() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToServerSettingScreen() {
        val intent = Intent(this, ServerSettingActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(groupId: Long) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_GROUP_ID, groupId)
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_DOMAIN, homeViewModel.getDomainOfActiveServer())
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_CLIENT_ID, homeViewModel.getClientIdOfActiveServer())
        startActivity(intent)
    }

    private fun navigateToRoomScreenWithFriendId(friendId: String, friendDomain: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_FRIEND_ID, friendId)
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_FRIEND_DOMAIN, friendDomain)
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_DOMAIN, homeViewModel.getDomainOfActiveServer())
        intent.putExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_CLIENT_ID, homeViewModel.getClientIdOfActiveServer())
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
        NavigationUtils.navigateToJoinServer(this, domain)
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
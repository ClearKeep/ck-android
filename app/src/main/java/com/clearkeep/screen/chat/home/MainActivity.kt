package com.clearkeep.screen.chat.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CkTopCalling
import com.clearkeep.screen.auth.login.LoginActivity
import com.clearkeep.screen.chat.contact_search.SearchUserActivity
import com.clearkeep.screen.chat.group_create.CreateGroupActivity
import com.clearkeep.screen.chat.group_create.CreateGroupActivity.Companion.EXTRA_IS_DIRECT_CHAT
import com.clearkeep.screen.chat.home.home.HomeScreen
import com.clearkeep.screen.chat.home.home.HomeViewModel
import com.clearkeep.screen.chat.home.profile.ProfileScreen
import com.clearkeep.screen.chat.home.profile.ProfileViewModel
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.videojanus.InCallActivity
import com.clearkeep.services.ChatService
import com.clearkeep.utilities.printlnCK
import com.facebook.login.LoginManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val mainViewModel: MainViewModel by viewModels {
        viewModelFactory
    }

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        viewModelFactory
    }

    private val startCreateGroupForResult = (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                val isDirectChat = intent.getBooleanExtra(EXTRA_IS_DIRECT_CHAT, true)
                if (isDirectChat) {
                    val friendId = intent.getStringExtra(CreateGroupActivity.EXTRA_PEOPLE_ID)
                    if (!friendId.isNullOrBlank()) {
                        navigateToRoomScreenWithFriendId(friendId)
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
                MainComposable()
            }
        }
        subscriberLogout()
        mainViewModel.updateFirebaseToken()
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
        mainViewModel.isLogOutCompleted.observe(this, { completed ->
            if (completed) {
                restartActivityToRoot()
            }
        })
    }

    @Composable
    private fun MainComposable() {
        val navController = rememberNavController()
        val remember = InCallActivity.listenerCallingState.observeAsState()
        Scaffold(
            topBar = {
                remember.value?.let {
                    if (it.isCalling)
                        CkTopCalling(title = {
                            Box{
                                Text(
                                    text = it.nameInComeCall ?: "",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                            navigationIcon = {
                                Icon(imageVector = Icons.Filled.Call, contentDescription = "")
                            },
                            modifier = Modifier.clickable {
                                AppCall.openCallAvailable(this)
                            })
                }
            },
        ) {
            Box {
                NavHost(navController, startDestination = "home_screen") {
                    composable("home_screen") {
                        HomeScreen(
                            homeViewModel,profileViewModel,navController, gotoSearch = {
                                navigateToSearchScreen()
                            }, createGroupChat = {
                                navigateToCreateGroupScreen(isDirectGroup = it)
                            },gotoRoomById = {
                                navigateToRoomScreen(it)
                            },logout = {
                                navigateToProfileScreen(navController)
                            }
                        )
                    }
                    composable("profile"){
                        ProfileScreen(profileViewModel = profileViewModel, homeViewModel = mainViewModel) {
                            logout()
                        }
                    }
                }
                LogoutProgress()
            }
        }
    }

    @Composable
    private fun LogoutProgress() {
        mainViewModel.isLogOutProcessing.observeAsState().value?.let { isProcessing ->
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
                        Text(text = "Log out...", style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }

    private fun logout() {
        mainViewModel.logOut()
        mainViewModel.logOutGoogle(this) {}
        mainViewModel.onLogOutMicrosoft(this)
        LoginManager.getInstance().logOut()
    }

    private fun navigateToProfileScreen(navController: NavController){
        navController.navigate("profile")
    }

    private fun navigateToRoomScreen(groupId: Long) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, groupId)
        startActivity(intent)
    }

    private fun navigateToRoomScreenWithFriendId(friendId: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friendId)
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

    private fun restartActivityToRoot() {
        printlnCK("restartActivityToRoot")
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        exitProcess(2)
    }
}
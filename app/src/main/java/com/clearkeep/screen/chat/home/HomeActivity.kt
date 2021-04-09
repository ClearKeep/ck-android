package com.clearkeep.screen.chat.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.navigation.compose.*
import com.clearkeep.R
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CkTopCalling
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.auth.login.LoginActivity
import com.clearkeep.screen.chat.contact_search.SearchUserActivity
import com.clearkeep.screen.chat.group_create.CreateGroupActivity
import com.clearkeep.screen.chat.home.chat_history.ChatHistoryScreen
import com.clearkeep.screen.chat.home.chat_history.ChatViewModel
import com.clearkeep.screen.chat.home.contact_list.PeopleScreen
import com.clearkeep.screen.chat.home.contact_list.PeopleViewModel
import com.clearkeep.screen.chat.home.profile.ProfileScreen
import com.clearkeep.screen.chat.home.profile.ProfileViewModel
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.videojanus.InCallActivity
import com.clearkeep.services.ChatService
import com.clearkeep.utilities.printlnCK
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess


val items = listOf(
        Screen.Chat,
        Screen.People,
        Screen.Profile,
)

private val BottomNavigationHeight = 56.dp

@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val peopleViewModel: PeopleViewModel by viewModels {
        viewModelFactory
    }

    private val chatViewModel: ChatViewModel by viewModels {
        viewModelFactory
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        viewModelFactory
    }

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory
    }

    private val startCreateGroupForResult = (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val groupId = result.data?.getLongExtra(CreateGroupActivity.EXTRA_GROUP_ID, -1) ?: -1
            if (groupId > 0) {
                navigateToRoomScreen(groupId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        setContent {
            CKTheme {
                MainComposable()
            }
        }
        subscriberLogout()
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

            bottomBar = {
                BottomNavigation(
                    backgroundColor = MaterialTheme.colors.primary,
                ){
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)
                    items.forEach { screen ->
                        BottomNavigationItem(
                            selectedContentColor = MaterialTheme.colors.surface,
                            unselectedContentColor = MaterialTheme.colors.onBackground,
                            icon = {
                                Icon(
                                    imageVector = screen.iconId,
                                    contentDescription = ""
                                )
                            },
                            label = {
                                Text(
                                    stringResource(screen.resourceId),
                                    style = MaterialTheme.typography.body2.copy(fontSize = 10.sp)
                                )
                            },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.popBackStack(
                                        navController.graph.startDestination,
                                        false
                                    )
                                    navController.navigate(screen.route)
                                }
                            }
                        )
                    }
                }
            }
        ) {
            Box {
                Column {
                    Row(modifier = Modifier.weight(1.0f, true)) {
                        NavHost(navController, startDestination = Screen.Chat.route) {
                            composable(Screen.Chat.route) {
                                ChatHistoryScreen(
                                        chatViewModel,
                                        onRoomSelected = { room ->
                                            navigateToRoomScreen(room.id)
                                        },
                                        onCreateGroup = {
                                            navigateToCreateGroupScreen()
                                        }
                                )
                            }
                            composable(Screen.People.route) {
                                peopleViewModel.updateContactList()
                                PeopleScreen(
                                        peopleViewModel,
                                        onFriendSelected = { friend ->
                                            navigateToRoomScreen(friend)
                                        },
                                        onNavigateToSearch = {
                                            navigateToSearchScreen()
                                        }
                                )
                            }
                            composable(Screen.Profile.route) {
                                ProfileScreen(
                                        profileViewModel,
                                        homeViewModel,
                                        onLogout = {
                                            logout()
                                        }
                                )
                            }
                        }
                    }
                    // TODO: work around issue of scaffold
                    Spacer(modifier = Modifier.height(BottomNavigationHeight))
                }
                LogoutProgress()
            }
        }
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
                        Text(text = "Log out...", style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }

    private fun logout() {
        homeViewModel.logOut()
        homeViewModel.logOutGoogle(this) {}
        homeViewModel.onLogOutMicrosoft(this)
    }

    private fun navigateToRoomScreen(groupId: Long) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, groupId)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(friend: People) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friend.id)
        startActivity(intent)
    }

    private fun navigateToCreateGroupScreen() {
        val intent = Intent(this, CreateGroupActivity::class.java)
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

sealed class Screen(val route: String, @StringRes val resourceId: Int, val iconId: ImageVector) {
    object Chat : Screen("chat_screen", R.string.bottom_nav_single, Icons.Filled.Message)
    object People : Screen("contact_screen", R.string.bottom_nav_group, Icons.Filled.Contacts)
    object Profile : Screen("profile_screen", R.string.bottom_nav_profile, Icons.Filled.Person)
}
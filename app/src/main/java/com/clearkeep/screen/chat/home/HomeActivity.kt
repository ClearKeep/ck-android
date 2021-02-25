package com.clearkeep.screen.chat.home

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.*
import com.clearkeep.R
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.db.clear_keep.model.ChatGroup
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
import com.clearkeep.utilities.printlnCK
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
class HomeActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CKTheme {
                homeViewModel.loginState.observeAsState().value.let { prepareState ->
                    when (prepareState) {
                        PrepareSuccess -> {
                            MainComposable()
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

        homeViewModel.prepareChat()
        subscriberLogout()
        registerNetworkChange()

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                startActivityForResult(intent, 0)
            }
        }*/
    }

    private fun registerNetworkChange() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    printlnCK("network available again")
                    homeViewModel.reInitAfterNetworkAvailable()
                }
                override fun onLost(network: Network) {
                    printlnCK("network lost")
                    //take action when network connection is lost
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
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
                        homeViewModel.prepareChat()
                    },
                    modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    @Composable
    private fun MainComposable() {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                BottomNavigation {
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
                                            navigateToRoomScreen(room)
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
    }

    private fun navigateToRoomScreen(group: ChatGroup) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, group.id)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(friend: People) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friend.id)
        startActivity(intent)
    }

    private fun navigateToCreateGroupScreen() {
        val intent = Intent(this, CreateGroupActivity::class.java)
        startActivity(intent)
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
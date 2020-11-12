package com.clearkeep.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.Text
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.VectorAsset
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearkeep.R
import dagger.hilt.android.AndroidEntryPoint
import com.clearkeep.ui.CKScaffold
import androidx.navigation.compose.*
import com.clearkeep.chat.group.GroupChatScreen
import com.clearkeep.chat.group.GroupChatViewModel
import com.clearkeep.chat.single.SingleChatScreen
import com.clearkeep.chat.single.PeerChatViewModel
import javax.inject.Inject

val items = listOf(
        Screen.SingleChat,
        Screen.GroupChat,
)

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val singleViewModel: PeerChatViewModel by viewModels {
        viewModelFactory
    }

    private val groupChatViewModel: GroupChatViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Scaffold (
                    bottomBar = {
                        BottomNavigation {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)
                            items.forEach { screen ->
                                BottomNavigationItem(
                                        icon = { Icon(screen.iconId) },
                                        label = { Text(stringResource(screen.resourceId)) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            // This is the equivalent to popUpTo the start destination
                                            navController.popBackStack(navController.graph.startDestination, false)

                                            // This if check gives us a "singleTop" behavior where we do not create a
                                            // second instance of the composable if we are already on that destination
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route)
                                            }
                                        }
                                )
                            }
                        }
                    }
            ) {
                NavHost(navController, startDestination = Screen.SingleChat.route) {
                    composable(Screen.SingleChat.route) { SingleChatScreen(singleViewModel) }
                    composable(Screen.GroupChat.route) { GroupChatScreen(groupChatViewModel) }
                }
            }
        }
    }
}

sealed class Screen(val route: String, @StringRes val resourceId: Int, val iconId: VectorAsset) {
    object SingleChat : Screen("single_chat", R.string.bottom_nav_single, Icons.Filled.Person)
    object GroupChat : Screen("group_chat", R.string.bottom_nav_group, Icons.Filled.Person)
}
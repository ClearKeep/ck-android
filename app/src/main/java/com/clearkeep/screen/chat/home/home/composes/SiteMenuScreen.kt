package com.clearkeep.screen.chat.home.site_menu

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.SideBarLabel
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.screen.chat.home.home.HomeViewModel
import com.clearkeep.screen.chat.home.home.composes.CircleAvatarSite
import com.clearkeep.screen.chat.home.profile.LogoutConfirmDialog
import com.clearkeep.screen.chat.home.profile.ProfileViewModel

@Composable
fun SiteMenuScreen(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    navController:NavController,
    closeSiteMenu: (() -> Unit),
    onLogout: (()->Unit)
) {
    val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .background(grayscaleOverlay)
            .focusable()
            .clickable(enabled = true, onClick = { null })
    ) {
        Row(Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isSystemInDarkTheme()) grayscale1 else backgroundGradientStart40,
                        if (isSystemInDarkTheme()) grayscale5 else backgroundGradientEnd40
                    ))
                )) {
            Box(
                Modifier
                    .width(108.dp)
            ) {

            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, bottom = 20.dp)
                    .background(
                        if (isSystemInDarkTheme()) grayscale1 else Color.White,
                        shape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp),
                    ),

                ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.size(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = { closeSiteMenu.invoke() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "",
                                tint = MaterialTheme.colors.primaryVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    HeaderSite(profileViewModel)
                    Spacer(modifier = Modifier.size(24.dp))
                    Divider(color = grayscale3)
                    SettingServer(
                        "CK Development", navController
                    )
                    Divider(color = grayscale3)
                    SettingGeneral(navController) {
                        setShowReminderDialog.invoke(true)
                    }
                    LogoutConfirmDialog(showReminder, setShowReminderDialog, onLogout)

                }
            }
        }
    }


}

@Composable
fun HeaderSite(profileViewModel: ProfileViewModel) {
    val profile = profileViewModel.profile.observeAsState()

    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarSite(url = "", name = profile.value?.userName ?: "", status = "")
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            CKHeaderText(
                text = profile.value?.userName ?: "",
                headerTextType = HeaderTextType.Normal,
                color = primaryDefault
            )
            Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Online",
                    style = TextStyle(color = colorSuccessDefault, fontSize = 14.sp)
                )
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_chev_down),
                        null,
                        alignment = Alignment.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SettingServer(
    serverName: String, navController: NavController,
) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        CKHeaderText(text = serverName, headerTextType = HeaderTextType.Normal, color = grayscale2)
        ItemSiteSetting("Server Settings", R.drawable.ic_adjustment)
        ItemSiteSetting("Invite other", R.drawable.ic_user_plus)
        ItemSiteSetting("Banned users", R.drawable.ic_user_off)
        ItemSiteSetting("Leave $serverName", R.drawable.ic_logout, textColor = errorDefault)
    }
}

@Composable
fun SettingGeneral(
    navController: NavController,
    onClickAction: () -> Unit
) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        CKHeaderText(text = "General", headerTextType = HeaderTextType.Normal, color = grayscale2)
        ItemSiteSetting("Account Settings", R.drawable.ic_user,{
            navController.navigate("profile")
        })
        ItemSiteSetting("Application Settings", R.drawable.ic_gear)
        ItemSiteSetting("Logout", R.drawable.ic_logout, textColor = errorDefault,onClickAction = onClickAction)
    }
}

@Composable
fun ItemSiteSetting(
    name: String,
    icon: Int,
    onClickAction: (() -> Unit)? = null,
    textColor: Color? = null
) {
    Row(modifier = Modifier
        .padding(top = 16.dp)
        .clickable { onClickAction?.invoke() },verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), contentDescription = null)
        SideBarLabel(
            text = name, color = textColor, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.dp)
        )
    }
}

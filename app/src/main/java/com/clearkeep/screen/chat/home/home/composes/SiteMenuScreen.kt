package com.clearkeep.screen.chat.home.site_menu

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.clearkeep.components.base.CkTextNormalHome
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.screen.chat.home.home.HomeViewModel
import com.clearkeep.screen.chat.home.home.composes.CircleAvatarSite
import com.clearkeep.screen.chat.home.profile.ProfileViewModel

@Composable
fun SiteMenuScreen(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    navController:NavController,
    closeSiteMenu: (() -> Unit)
) {
    Surface(
        color=Color.White.copy(0.6f),
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isSystemInDarkTheme()) grayscale1 else backgroundGradientStart40,
                        if (isSystemInDarkTheme()) grayscale5 else backgroundGradientEnd40
                    )
                )

            )
            .focusable()
            .clickable(enabled = true, onClick = { null })
    ) {
        Row() {
            Box(
                Modifier
                    .width(108.dp)
                    .background(color = grayscaleOverlay),
            ) {

            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, bottom = 20.dp)
                    .background(
                        Color.White,
                        shape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp)
                    ),

                ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.size(36.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cross),
                            contentDescription = null, modifier = Modifier
                                .clickable {
                                    closeSiteMenu.invoke()
                                }
                        )
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                    HeaderSite(profileViewModel)
                    Spacer(modifier = Modifier.size(24.dp))
                    Divider(color = grayscale3)
                    SettingServer(
                        "CK Development", navController
                    )
                    Divider(color = grayscale3)
                    SettingGeneral(navController)
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
                Box(modifier = Modifier.padding(8.dp)) {
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
) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        CKHeaderText(text = "General", headerTextType = HeaderTextType.Normal, color = grayscale2)
        ItemSiteSetting("Account Settings", R.drawable.ic_user,{
            navController.navigate("profile")
        })
        ItemSiteSetting("Application Settings", R.drawable.ic_gear)
        ItemSiteSetting("Logout", R.drawable.ic_logout, textColor = errorDefault)
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
        .clickable { onClickAction?.invoke() }) {
        Image(painter = painterResource(icon), contentDescription = null)
        CkTextNormalHome(
            text = name, color = textColor, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.dp)
        )
    }
}

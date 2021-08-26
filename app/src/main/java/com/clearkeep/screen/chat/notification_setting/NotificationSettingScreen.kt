package com.clearkeep.screen.chat.notification_setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.CKSetting
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.primaryDefault
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp


@Composable
fun NotificationSettingScreen(
    notificationSettingsViewModel: NotificationSettingsViewModel,
    onCloseView: () -> Unit
) {
    val userPreference = notificationSettingsViewModel.userPreference.observeAsState()

    Column(
        Modifier
            .padding(horizontal = 16.sdp())
            .fillMaxSize()
    ) {
        HeaderNotificationSetting(onCloseView)
        Spacer(modifier = Modifier.height(26.sdp()))
        CKSetting(
            modifier = Modifier,
            name = "Show previews",
            description = "Tips: Show message previews in alerts and banners when you are not using app",
            checked = userPreference.value?.showNotificationPreview ?: true
        ) {
            notificationSettingsViewModel.toggleShowPreview(it)
        }
        Spacer(modifier = Modifier.height(40.sdp()))
        CKSetting(
            modifier = Modifier,
            name = "Do not disturb",
            checked = userPreference.value?.doNotDisturb ?: false
        ) {
            notificationSettingsViewModel.toggleDoNotDisturb(it)
        }
    }
}

@Composable
fun HeaderNotificationSetting(onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(32.sdp()))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    }.size(24.sdp()),
                contentScale = ContentScale.FillBounds,
                alignment = Alignment.CenterStart
            )
        }
        Spacer(modifier = Modifier.size(16.sdp()))
        CKHeaderText("Notification", headerTextType = HeaderTextType.Medium)
    }
}
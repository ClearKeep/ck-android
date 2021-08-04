package com.clearkeep.screen.chat.main.notification_setting

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.primaryDefault
import com.clearkeep.screen.chat.notification_setting.NotificationSettingsViewModel


@Composable
fun NotificationSettingScreen(
    notificationSettingsViewModel: NotificationSettingsViewModel,
    onCloseView: () -> Unit
) {
    val userPreference = notificationSettingsViewModel.userPreference.observeAsState()

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        HeaderNotificationSetting(onCloseView)
        Spacer(modifier = Modifier.height(26.dp))
        CKSetting(
            modifier = Modifier,
            name = "Show previews",
            description = "Tips: Show message previews in alerts and banners when you are not using app",
            checked = userPreference.value?.showNotificationPreview ?: true
        ) {
            notificationSettingsViewModel.toggleShowPreview(it)
        }
        Spacer(modifier = Modifier.height(40.dp))
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
        Spacer(Modifier.size(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                alignment = Alignment.CenterStart
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        CKHeaderText("Notification", headerTextType = HeaderTextType.Medium)
    }
}

@Composable
private fun CKSetting(
    modifier: Modifier,
    name: String,
    description: String = "",
    checked: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Column(modifier) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                name,
                modifier = Modifier.weight(0.66f)
            )
            Column(
                modifier = Modifier.clickable { },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryDefault, checkedTrackColor = primaryDefault,
                        uncheckedThumbColor = grayscale3, uncheckedTrackColor = grayscale3
                    ),
                    modifier = Modifier
                        .width(64.dp)
                        .height(36.dp)
                )
            }
        }
        if (description.isNotBlank()) {
            Text(
                text = description, style = MaterialTheme.typography.body1.copy(
                    color = grayscale2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ), modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}

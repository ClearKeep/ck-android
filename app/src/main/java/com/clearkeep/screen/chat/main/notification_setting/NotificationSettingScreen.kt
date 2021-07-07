package com.clearkeep.screen.chat.main.notification_setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.primaryDefault


@Composable
fun NotificationSettingScreen() {
    Column(Modifier.padding(horizontal = 16.dp).fillMaxSize()) {
        HeaderNotificationSetting {}
        Spacer(modifier = Modifier.height(26.dp))
        CKSetting(modifier = Modifier, name = "Show previews", description = "Tips: Show message previews in alerts and banners when you are not using app", checked = mutableStateOf(false))
        Spacer(modifier = Modifier.height(40.dp))
        CKSetting(modifier = Modifier, name = "Sounds and vibrations", checked = mutableStateOf(false))
        Spacer(modifier = Modifier.height(40.dp))
        CKSetting(modifier = Modifier, name = "Do not disturb", checked = mutableStateOf(false))
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
private fun CKSetting(modifier: Modifier, name: String, description: String = "", checked: MutableState<Boolean>) {
    Column(modifier) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(name,
                modifier = Modifier.weight(0.66f)
            )
            Column(
                modifier = Modifier.clickable { },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Switch(
                    checked = checked.value,
                    onCheckedChange = { checked.value = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = primaryDefault,checkedTrackColor = primaryDefault,
                        uncheckedThumbColor = grayscale3,uncheckedTrackColor = grayscale3
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

@Preview
@Composable
fun NotificationSettingPreview() {
    NotificationSettingScreen()
}

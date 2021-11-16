package com.clearkeep.screen.chat.notification_setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.CKHeaderText
import com.clearkeep.presentation.components.base.CKSetting
import com.clearkeep.presentation.components.base.HeaderTextType
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
            .verticalScroll(rememberScrollState())
    ) {
        HeaderNotificationSetting(onCloseView)
        Spacer(modifier = Modifier.height(26.sdp()))
        CKSetting(
            modifier = Modifier,
            name = stringResource(R.string.notification_setting_preview),
            description = stringResource(R.string.notification_setting_preview_description),
            checked = userPreference.value?.showNotificationPreview ?: true
        ) {
            notificationSettingsViewModel.toggleShowPreview(it)
        }
        Spacer(modifier = Modifier.height(40.sdp()))
        CKSetting(
            modifier = Modifier,
            name = stringResource(R.string.notification_setting_do_not_disturb),
            checked = userPreference.value?.doNotDisturb ?: false
        ) {
            notificationSettingsViewModel.toggleDoNotDisturb(it)
        }
        Spacer(Modifier.size(32.sdp()))
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
                    }
                    .size(24.sdp()),
                contentScale = ContentScale.FillBounds,
                alignment = Alignment.CenterStart,
                colorFilter = LocalColorMapping.current.closeIconFilter
            )
        }
        Spacer(modifier = Modifier.size(16.sdp()))
        CKHeaderText(stringResource(R.string.notification), headerTextType = HeaderTextType.Medium, color = LocalColorMapping.current.headerText)
    }
}
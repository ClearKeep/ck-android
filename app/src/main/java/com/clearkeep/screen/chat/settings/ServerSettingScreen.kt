package com.clearkeep.screen.chat.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp

@Composable
fun ServerSettingScreen(
    server: Server,
    onCopiedServerDomain: (url: String) -> Unit,
    onBackPressed: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.sdp())
    ) {
        HeaderServerSetting(onBackPressed)
        Spacer(modifier = Modifier.height(28.sdp()))
        CKText(
            stringResource(R.string.server_setting_server_name),
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground,
                fontSize = defaultNonScalableTextSize()
            )
        )
        CKTextInputField(
            placeholder = server.serverName,
            textValue = null,
            keyboardType = KeyboardType.Text,
            singleLine = true,
            readOnly = true
        )
        Spacer(modifier = Modifier.height(16.sdp()))
        CKText(
            stringResource(R.string.server_url), style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground,
                fontSize = defaultNonScalableTextSize()
            )
        )
        Spacer(modifier = Modifier.height(4.sdp()))
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1.0f, true)) {
                CKTextInputField(
                    placeholder = server.serverDomain,
                    textValue = null,
                    keyboardType = KeyboardType.Text,
                    singleLine = true,
                    readOnly = true
                )
            }
            Spacer(modifier = Modifier.width(16.sdp()))
            Image(
                painter = painterResource(R.drawable.ic_copy_link),
                contentDescription = "",
                modifier = Modifier
                    .size(52.sdp())
                    .clickable(
                        onClick = {
                            onCopiedServerDomain(server.serverDomain)
                        }
                    )
            )
        }
        Spacer(Modifier.size(32.sdp()))
    }
}

@Composable
fun HeaderServerSetting(onCloseView: () -> Unit) {
    val focusManager = LocalFocusManager.current

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
                    },
                alignment = Alignment.CenterStart
            )
        }
        Spacer(modifier = Modifier.size(16.sdp()))
        CKHeaderText(
            stringResource(R.string.server_settings),
            headerTextType = HeaderTextType.Medium
        )
        Spacer(modifier = Modifier.size(16.sdp()))
    }
}

@Composable
fun ServerItem(
    iconPainter: Painter,
    title: String,
    subTitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = iconPainter,
            contentDescription = "",
            modifier = Modifier.clickable(
                onClick = onClick
            )
        )
        Spacer(modifier = Modifier.width(21.sdp()))
        Column {
            CKText(
                title, style = MaterialTheme.typography.h6.copy(
                    color = MaterialTheme.colors.primaryVariant,
                    fontSize = defaultNonScalableTextSize()
                )
            )
            Spacer(modifier = Modifier.height(2.sdp()))
            CKText(
                subTitle, style = MaterialTheme.typography.overline.copy(
                    color = MaterialTheme.colors.onSecondary,
                    fontSize = defaultNonScalableTextSize()
                )
            )
        }
    }
}
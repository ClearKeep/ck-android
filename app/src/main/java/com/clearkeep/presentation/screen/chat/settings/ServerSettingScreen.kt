package com.clearkeep.presentation.screen.chat.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.*
import com.clearkeep.domain.model.Server
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp

@Composable
fun ServerSettingScreen(
    server: com.clearkeep.domain.model.Server,
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
                color = LocalColorMapping.current.headerText,
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
                color = LocalColorMapping.current.inputLabel,
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
                alignment = Alignment.CenterStart,
                colorFilter = LocalColorMapping.current.closeIconFilter
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
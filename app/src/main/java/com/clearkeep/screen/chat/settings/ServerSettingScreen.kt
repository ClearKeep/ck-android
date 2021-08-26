package com.clearkeep.screen.chat.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.components.base.CKTopAppBarSample
import com.clearkeep.components.base.TopAppBarSampleType
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.screen.chat.home.composes.CircleAvatarWorkSpace
import com.clearkeep.utilities.sdp

@Composable
fun ServerSettingScreen(
    server: Server,
    onCopiedServerDomain: (url: String) -> Unit,
    onBackPressed: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        CKTopAppBarSample(
            title = server.serverName,
            onBackPressed = onBackPressed,
            type = TopAppBarSampleType.Black
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.sdp())
        ) {
            Spacer(modifier = Modifier.height(45.sdp()))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircleAvatarWorkSpace(server, false, contentSize = 85.sdp())
            }
            Spacer(modifier = Modifier.height(25.sdp()))
            Text("General", style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground
            ))
            Spacer(modifier = Modifier.height(8.sdp()))
            ServerItem(
                iconPainter = painterResource(R.drawable.ic_user),
                title = "Notification",
                subTitle = "18 Members",
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.sdp()))
            ServerItem(
                iconPainter = painterResource(R.drawable.ic_server_notification),
                title = "See Members",
                subTitle = "Current: On",
                onClick = {}
            )
            Spacer(modifier = Modifier.height(25.sdp()))
            Text("Details", style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground
            ))
            Spacer(modifier = Modifier.height(16.sdp()))
            Text("Server Name", style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground
            ))
            CKTextInputField(
                placeholder = server.serverName,
                textValue = null,
                keyboardType = KeyboardType.Text,
                singleLine = true,
                readOnly = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Server URL", style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground
            ))
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1.0f, true)
                ) {
                    CKTextInputField(
                        placeholder = server.serverDomain,
                        textValue = null,
                        keyboardType = KeyboardType.Text,
                        singleLine = true,
                        readOnly = true
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Image(
                    painter = painterResource(R.drawable.ic_copy_link),
                    contentDescription = "",
                    modifier = Modifier.size(52.dp).clickable(
                        onClick = {
                            onCopiedServerDomain(server.serverDomain)
                        }
                    )
                )
            }
        }
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
            Text(title, style = MaterialTheme.typography.h6.copy(
                color = MaterialTheme.colors.primaryVariant
            ))
            Spacer(modifier = Modifier.height(2.sdp()))
            Text(subTitle, style = MaterialTheme.typography.overline.copy(
                color = MaterialTheme.colors.onSecondary
            ))
        }
    }
}
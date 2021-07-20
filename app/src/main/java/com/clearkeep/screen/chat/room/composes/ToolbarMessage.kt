package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding


@Composable
fun ToolbarMessage(
    modifier: Modifier = Modifier,
    title: String = "",
    isGroup: Boolean = false,
    onBackClick: () -> Unit,
    onUserClick: () -> Unit,
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit

) {
    Row(modifier = Modifier.statusBarsHeight(58.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = Color.White
            )
        }

        if (!isGroup) {
            CircleAvatar(emptyList(),
                name = title,
                size = 36.dp,
                isGroup = isGroup)
        }

        Spacer(Modifier.width(10.dp))

        Row(
            modifier = Modifier
                .weight(1.0f, true)
                .clickable {
                    onUserClick()
                },
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAudioClick,
                modifier
                    .padding(8.dp)
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_icon_call_audio),
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onVideoClick,
                modifier
                    .padding(8.dp)
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_icon_call_video),
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(10.dp))
        }
    }
}
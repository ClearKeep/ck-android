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
import com.clearkeep.R
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding

@Composable
fun ToolbarMessage(
    modifier: Modifier = Modifier,
    title: String = "",
    avatars: List<String>? = arrayListOf(),
    isGroup: Boolean = false,
    isNote: Boolean = false,
    onBackClick: () -> Unit,
    onUserClick: () -> Unit,
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    Row(modifier = Modifier.statusBarsHeight(58.sdp()).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = Color.White,
            )
        }

        if (!isGroup && !isNote) {
            CircleAvatar(
                avatars ?: arrayListOf(),
                name = title,
                size = 36.sdp(),
                isGroup = isGroup
            )
        }

        Spacer(Modifier.width(10.sdp()))

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
                fontSize = 16.sdp().toNonScalableTextSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isNote) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAudioClick,
                    modifier
                        .padding(8.sdp())
                        .width(36.sdp())
                        .height(36.sdp())
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
                        .padding(8.sdp())
                        .width(36.sdp())
                        .height(36.sdp())
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_icon_call_video),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(10.sdp()))
            }
        }
    }
}
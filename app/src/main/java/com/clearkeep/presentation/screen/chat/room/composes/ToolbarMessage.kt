package com.clearkeep.presentation.screen.chat.room.composes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.grayscaleDarkModeDarkGrey2
import com.clearkeep.presentation.screen.chat.composes.CircleAvatar
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
    isDeletedUserChat: Boolean = false,
    onBackClick: () -> Unit,
    onUserClick: () -> Unit,
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    val backgroundModifier = if (LocalColorMapping.current.isDarkTheme) {
        Modifier.background(
            grayscaleDarkModeDarkGrey2
        ).height(58.sdp())
    } else {
        Modifier
            .statusBarsHeight(58.sdp())
            .statusBarsPadding()
    }

    if (LocalColorMapping.current.isDarkTheme) {
        Box(Modifier.fillMaxWidth().background(grayscaleDarkModeDarkGrey2).statusBarsHeight())
    }

    Row(
        modifier = Modifier
            .then(backgroundModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = LocalColorMapping.current.bodyText,
            )
        }

        if (!isGroup && !isNote && title.isNotBlank()) {
            CircleAvatar(
                avatars ?: arrayListOf(),
                name = if (isDeletedUserChat) "" else title,
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
                color = LocalColorMapping.current.bodyText,
                fontSize = 16.sdp().toNonScalableTextSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isNote && !isDeletedUserChat) {
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
                        tint = LocalColorMapping.current.bodyText,
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
                        tint = LocalColorMapping.current.bodyText,
                    )
                }
                Spacer(Modifier.width(10.sdp()))
            }
        }
    }
}
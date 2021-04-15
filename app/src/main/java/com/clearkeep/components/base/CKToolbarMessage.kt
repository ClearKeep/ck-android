package com.clearkeep.components.base

import android.widget.ImageButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.colorTest
import com.clearkeep.screen.chat.home.composes.CircleAvatar


@Composable
fun CKToolbarMessage(
    modifier: Modifier = Modifier,
    title: String = "",
    isGroup: Boolean = false,
    onBackClick: () -> Unit,
    onUserClick: () -> Unit,
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit

) {
    Row(modifier = Modifier.height(58.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = Color.White
            )
        }

        if (isGroup) {
            Image(
                imageVector = Icons.Filled.Groups,
                contentDescription = "",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        color = colorTest
                    )
            )
        } else {
            Image(
                imageVector = Icons.Filled.Person,
                contentDescription = "",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        color = colorTest
                    )
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(text = title, color = Color.White, fontSize = 16.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
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
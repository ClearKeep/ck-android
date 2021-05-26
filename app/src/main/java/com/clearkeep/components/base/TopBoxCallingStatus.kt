package com.clearkeep.components.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearkeep.components.colorSuccessDefault
import com.clearkeep.components.grayscale5
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.videojanus.CallingStateData

@Composable
fun TopBoxCallingStatus(
    modifier: Modifier = Modifier,
    callingStateData: CallingStateData,
    onClick: (isCallPeer: Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomEnd = 32.dp,
            bottomStart = 32.dp
        ),
        color = colorSuccessDefault,
        modifier = modifier.clickable {
            onClick(callingStateData.isCallPeer)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (callingStateData.isCallPeer) {
                Row(modifier = Modifier.padding(end = 16.dp)) {
                    CircleAvatar(emptyList(),
                        name = callingStateData.nameInComeCall ?: "Unknown",
                        size = 34.dp,
                        isGroup = false)
                }
            }
            Column(modifier = Modifier
                .weight(1.0f, true)) {
                Text(text = callingStateData.nameInComeCall ?: "", style = MaterialTheme.typography.h6.copy(
                    color = grayscale5,
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Tap here to return to call screen", style = MaterialTheme.typography.caption.copy(
                    color = grayscale5,
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (callingStateData.timeStarted > 0) {
                CKChronometer(base = callingStateData.timeStarted, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.components.colorSuccessDefault
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.components.primaryDefault
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString

@Composable
fun MessageFromOther(messageDisplayInfo: MessageDisplayInfo) {
    Column {
        Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.dp else 2.dp ))
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.width(26.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (messageDisplayInfo.showAvatarAndName) {
                    CircleAvatar(emptyList(), messageDisplayInfo.userName, size = 18.dp)
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (messageDisplayInfo.showAvatarAndName) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = messageDisplayInfo.userName,
                            style = MaterialTheme.typography.body2.copy(
                                color = colorSuccessDefault,
                                fontWeight = FontWeight.W600
                            ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                            style = MaterialTheme.typography.caption.copy(
                                fontWeight = FontWeight.Medium,
                                color = grayscale3,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.weight(1.0f, true),
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.weight(2.0f, true),
                    ) {
                        Card (
                            backgroundColor = primaryDefault,
                            shape = messageDisplayInfo.cornerShape,
                        ) {
                            Text(
                                text = messageDisplayInfo.message.message,
                                style = MaterialTheme.typography.body2.copy(
                                    color = grayscaleOffWhite
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1.0f, true),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (!messageDisplayInfo.showAvatarAndName) {
                            Text(
                                text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                                style = MaterialTheme.typography.caption.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = grayscale3,
                                    textAlign = TextAlign.End
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
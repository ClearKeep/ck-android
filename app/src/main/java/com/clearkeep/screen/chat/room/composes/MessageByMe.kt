package com.clearkeep.screen.chat.room.composes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString

@ExperimentalFoundationApi
@Composable
fun MessageByMe(messageDisplayInfo: MessageDisplayInfo) {
    val message = messageDisplayInfo.message.message
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth().wrapContentHeight(), horizontalAlignment = Alignment.End) {
        Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.dp else 2.dp))
        Text(
            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
            style = MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.Medium,
                color = grayscale3,
                textAlign = TextAlign.Start
            ),
        )
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (message.contains(tempImageRegex))
                CircularProgressIndicator(Modifier.size(20.dp), grayscale1, 2.dp)
            Spacer(Modifier.width(18.dp))
            Card(
                backgroundColor = grayscale2,
                shape = messageDisplayInfo.cornerShape,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (isImageMessage(message)) {
                        ImageMessageContent(
                            Modifier.padding(24.dp, 16.dp),
                            getImageUriStrings(message)
                        )
                    } else if (isFileMessage(message)) {
                        FileMessageContent(getFileUriStrings(message)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            ContextCompat.startActivity(context, intent, null)
                        }
                    }
                    val messageContent = getMessageContent(message)
                    if (messageContent.isNotBlank()) {
                        Row(Modifier.align(Alignment.End).wrapContentHeight()) {
                            ClickableLinkContent(messageContent)
                        }
                    }
                }
            }
        }
    }
}

private fun isImageMessage(content: String): Boolean {
    return content.contains(remoteImageRegex) || content.contains(tempImageRegex)
}

private fun isFileMessage(content: String): Boolean {
    return content.contains(remoteFileRegex)
}

private fun getImageUriStrings(content: String): List<String> {
    val temp = remoteImageRegex.findAll(content).map {
        it.value.split(" ")
    }.toMutableList()
    temp.add(tempImageRegex.findAll(content).map { it.value.split(" ") }.toList().flatten())
    return temp.flatten()
}

private fun getFileUriStrings(content: String): List<String> {
    val temp = remoteFileRegex.findAll(content).map {
        it.value.split(" ")
    }.toMutableList()
    return temp.flatten()
}

private fun getMessageContent(content: String): String {
    val temp = remoteImageRegex.replace(content, "")
    val temp2 = remoteFileRegex.replace(temp, "")
    return tempImageRegex.replace(temp2, "")
}

private val remoteImageRegex =
    "(https://s3.amazonaws.com/storage.clearkeep.io/[dev|prod].+[a-zA-Z0-9\\/\\_\\-\\.]+(\\.png|\\.jpeg|\\.jpg|\\.gif|\\.PNG|\\.JPEG|\\.JPG|\\.GIF))".toRegex()

private val remoteFileRegex =
    "(https://s3.amazonaws.com/storage.clearkeep.io/[dev|prod].+)".toRegex()

private val tempImageRegex =
    "content://media/external/images/media/\\d+".toRegex()
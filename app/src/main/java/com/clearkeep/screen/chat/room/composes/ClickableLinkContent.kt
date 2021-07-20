package com.clearkeep.screen.chat.room.composes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.clearkeep.components.grayscaleOffWhite

@Composable
fun ClickableLinkContent(message: String) {
    val context = LocalContext.current

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        URL_REGEX.findAll(message).forEach {
            val first = it.range.first
            val last = it.range.last
            if (currentIndex < first) {
                append(message.substring(currentIndex, first))
            }
            val link = message.substring(first, last + 1)
            pushStringAnnotation("URL", link)
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(link)
            }
            pop()
            currentIndex = last + 1
        }
        if (currentIndex == 0) {
            //If there is no link in message
            append(message)
        } else if (currentIndex < message.length - 1) {
            append(message.substring(currentIndex, message.length))
        }
    }
    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.body2.copy(
            color = grayscaleOffWhite
        ),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        onClick = {
                offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset,
                end = offset)
                .firstOrNull()?.let { annotation ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                    ContextCompat.startActivity(context, intent, null)
                }
        }
    )
}

private val URL_REGEX = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)".toRegex()
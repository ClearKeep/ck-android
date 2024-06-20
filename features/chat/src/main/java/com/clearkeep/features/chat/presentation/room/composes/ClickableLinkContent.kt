package com.clearkeep.features.chat.presentation.room.composes

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import com.clearkeep.common.presentation.components.grayscale2
import com.clearkeep.common.presentation.components.grayscaleOffWhite
import com.clearkeep.common.utilities.sdp

@Composable
fun ClickableLinkContent(message: String, isQuoteMessage: Boolean, longClickKey: Int, onLongClick: () -> Unit) {
    val context = LocalContext.current

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val matcher = Patterns.WEB_URL.matcher(message)
        while (matcher.find()) {
            val first = matcher.start()
            val last = matcher.end()
            if (currentIndex < first) {
                append(message.substring(currentIndex, first))
            }
            val link = message.substring(first, last)
            pushStringAnnotation("URL", link)
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(link)
            }
            pop()
            currentIndex = last
        }
        if (currentIndex == 0) {
            //If there is no link in message
            append(message)
        } else if (currentIndex < message.length - 1) {
            append(message.substring(currentIndex, message.length))
        }
    }
    BasicText(
        text = annotatedString,
        style = MaterialTheme.typography.body2.copy(
            color = if (isQuoteMessage) grayscale2 else grayscaleOffWhite
        ),
        modifier = Modifier
            .padding(horizontal = 24.sdp(), vertical = 8.sdp())
            .pointerInput(longClickKey) {
                detectTapGestures(
                    onLongPress = {
                        onLongClick()
                    },
                    onTap = {
                        layoutResult.value?.let { layoutResult ->
                            val offset = layoutResult.getOffsetForPosition(it)
                            annotatedString
                                .getStringAnnotations(
                                    tag = "URL",
                                    start = offset,
                                    end = offset
                                )
                                .firstOrNull()
                                ?.let { annotation ->
                                    val validatedUrl =
                                        if (annotation.item.contains("http(s)?://".toRegex())) annotation.item else "http://${annotation.item}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validatedUrl))
                                    ContextCompat.startActivity(context, intent, null)
                                }
                        }
                    }
                )
            },
        onTextLayout = {
            layoutResult.value = it
        }
    )
}
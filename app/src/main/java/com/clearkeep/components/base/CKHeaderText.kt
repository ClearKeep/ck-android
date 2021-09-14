package com.clearkeep.components.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.components.grayscaleBlack
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun CKHeaderText(
    text: String,
    modifier: Modifier = Modifier,
    headerTextType: HeaderTextType = HeaderTextType.Normal,
    color: Color = grayscaleBlack
) {
    //todo disable dark mode
    Text(
        text = text,
        modifier = modifier,
        style = getTypography(headerTextType).copy(
            color = if (isSystemInDarkTheme()) color else color,
        ),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}



@Composable
fun getTypography(headerTextType: HeaderTextType): TextStyle {
    return when (headerTextType) {
        HeaderTextType.Normal -> MaterialTheme.typography.h6.copy(fontSize = 16.sdp().toNonScalableTextSize())
        HeaderTextType.Medium -> MaterialTheme.typography.h5.copy(fontSize = 20.sdp().toNonScalableTextSize())
        HeaderTextType.Large -> MaterialTheme.typography.h4.copy(fontSize = 24.sdp().toNonScalableTextSize())
    }
}

enum class HeaderTextType {
    Normal,
    Medium,
    Large
}
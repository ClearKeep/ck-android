package com.clearkeep.components.base

import android.widget.ImageButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.clearkeep.R


@Composable
fun CKToolbarBack(
    modifier: Modifier = Modifier,
    title: String = "",
    onClick: () -> Unit,
    ) {
    Row(modifier=modifier,verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = Color.White
            )
        }
        Text(text = title,color = Color.White,fontSize = 16.sp)

    }
}
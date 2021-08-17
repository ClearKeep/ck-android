package com.clearkeep.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.R

@Composable
fun CKTopAppBarSample(
    modifier: Modifier = Modifier,
    title: String = "",
    onBackPressed: () -> Unit,
    type: TopAppBarSampleType = TopAppBarSampleType.White
    ) {
    Row(
        modifier = modifier.then(Modifier.padding(vertical = 8.dp, horizontal = 6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = if (type == TopAppBarSampleType.White) Color.White else Color.Black
            )
        }
        Column {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.h5.copy(
                    color = if (type == TopAppBarSampleType.White) Color.White else Color.Black
                )
            )
        }
    }
}

enum class TopAppBarSampleType {
    White,
    Black,
}
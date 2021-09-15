package com.clearkeep.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun CKTopAppBarSample(
    modifier: Modifier = Modifier,
    title: String = "",
    onBackPressed: () -> Unit,
    type: TopAppBarSampleType = TopAppBarSampleType.White
) {
    Row(
        modifier = modifier.padding(vertical = 8.sdp(), horizontal = 6.sdp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed, modifier = Modifier.size(24.sdp())) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = if (type == TopAppBarSampleType.White) Color.White else Color.Black,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column {
            Spacer(modifier = Modifier.height(2.sdp()))
            Text(
                text = title,
                style = MaterialTheme.typography.h5.copy(
                    color = if (type == TopAppBarSampleType.White) Color.White else Color.Black,
                ),
                fontSize = 16.sdp().toNonScalableTextSize()
            )
        }
    }
}

enum class TopAppBarSampleType {
    White,
    Black,
}
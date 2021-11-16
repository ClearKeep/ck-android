package com.clearkeep.presentation.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun CKTopAppBarSample(
    modifier: Modifier = Modifier,
    title: String = "",
    onBackPressed: () -> Unit
) {
    Row(
        modifier = modifier.padding(vertical = 8.sdp(), horizontal = 6.sdp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed, modifier = Modifier.size(24.sdp())) {
            Icon(
                painter = painterResource(R.drawable.ic_chev_left),
                contentDescription = null,
                tint = LocalColorMapping.current.topAppBarTitle,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column {
            Spacer(modifier = Modifier.height(2.sdp()))
            Text(
                text = title,
                style = MaterialTheme.typography.h5.copy(
                    color = LocalColorMapping.current.topAppBarContent,
                ),
                fontSize = 16.sdp().toNonScalableTextSize()
            )
        }
    }
}
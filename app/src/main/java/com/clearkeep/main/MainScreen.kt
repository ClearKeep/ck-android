package com.clearkeep.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.ui.base.CKButton

@Composable
fun MainScreen(
    onOpenSingleChat: () -> Unit,
    onOpenGroupChat: () -> Unit
) {
    Column (
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxHeight().fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CKButton(
            "SingleChat",
            onClick = {
                onOpenSingleChat()
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        CKButton(
            "GroupChat",
            onClick = {
                onOpenGroupChat()
            }
        )
    }
}
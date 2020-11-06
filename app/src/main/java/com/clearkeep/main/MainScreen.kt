package com.clearkeep.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.ui.ButtonGeneral

@Composable
fun MainScreen(
    onOpenSingleChat: () -> Unit,
    onOpenGroupChat: () -> Unit
) {
    Column (
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ButtonGeneral(
            "SingleChat",
            onClick = {
                onOpenSingleChat()
            }
        )
        ButtonGeneral(
            "GroupChat",
            onClick = {
                onOpenGroupChat()
            }
        )
    }
}
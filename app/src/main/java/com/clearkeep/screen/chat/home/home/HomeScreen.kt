package com.clearkeep.screen.chat.home.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
) {
    val rooms = homeViewModel.groups.observeAsState()
    Column {
        CKTopAppBar(
                title = {
                    Text(text = "Chat")
                },
        )
    }
}
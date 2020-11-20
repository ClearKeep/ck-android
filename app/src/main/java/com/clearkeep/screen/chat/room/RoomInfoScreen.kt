package com.clearkeep.screen.chat.room

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun RoomInfoScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {
    val group = roomViewModel.group.observeAsState()
    group?.let {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(text = group?.value?.groupName ?: "")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navHostController.popBackStack(navHostController.graph.startDestination, false)
                        }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
            )
            Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
                0.66f
            )) {
                group?.value?.isGroup()?.let {
                    IconButton(
                        onClick = {
                        }
                    ) {
                        Icon(asset = Icons.Filled.Person)
                    }
                }
            }
        }
    }
}
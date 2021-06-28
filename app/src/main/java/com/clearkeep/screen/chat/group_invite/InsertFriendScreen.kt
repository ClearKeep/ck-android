package com.clearkeep.screen.chat.group_invite

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.utils.getPeopleFromLink

@Composable
fun InsertFriendScreen(
    navController: NavHostController,
    onInsertFriend: (people: User) -> Unit,
) {
    val link = remember {mutableStateOf("")}

    Surface(
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.0f, true),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = {
                            navController.popBackStack(navController.graph.startDestination, false)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "",
                            tint = MaterialTheme.colors.primaryVariant
                        )
                    }
                }

                CKTextButton(
                    title = "Create",
                    onClick = {
                        val people = getPeopleFromLink(link.value)
                        if (people != null) {
                            onInsertFriend(people)
                        }
                    },
                    fontSize = 16.sp,
                    textButtonType = TextButtonType.Blue
                )
            }
            Spacer(modifier = Modifier.height(25.dp))
            CKHeaderText("New User", headerTextType = HeaderTextType.Medium)
            Spacer(modifier = Modifier.height(24.dp))
            CKTextInputField(
                "Profile url",
                link
            )
        }
    }
}
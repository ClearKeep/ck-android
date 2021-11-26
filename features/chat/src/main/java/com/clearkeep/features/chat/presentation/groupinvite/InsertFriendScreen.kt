package com.clearkeep.features.chat.presentation.groupinvite

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.domain.model.User
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize
import com.clearkeep.features.chat.presentation.utils.getPeopleFromLink

@Composable
fun InsertFriendScreen(
    inviteGroupViewModel: InviteGroupViewModel,
    navController: NavHostController,
    onInsertFriend: (people: User) -> Unit,
) {
    val link = rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.sdp())
        ) {
            Row(
                modifier = Modifier.padding(start = 5.sdp(), end = 8.sdp(), top = 24.sdp()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.0f, true),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = {
                            navController.popBackStack(navController.graph.findStartDestination().id, false)
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
                    title = stringResource(R.string.create),
                    onClick = {
                        val people = getPeopleFromLink(link.value)
                        if (people != null) {
                            if (people.userId != inviteGroupViewModel.getClientId()) {
                                onInsertFriend(people)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.insert_friend_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    fontSize = 16.sdp().toNonScalableTextSize(),
                    textButtonType = TextButtonType.Blue
                )
            }
            Spacer(modifier = Modifier.height(25.sdp()))
            CKHeaderText(
                stringResource(R.string.insert_friend_new_user),
                headerTextType = HeaderTextType.Medium
            )
            Spacer(modifier = Modifier.height(24.sdp()))
            CKTextInputField(
                stringResource(R.string.insert_friend_profile_url),
                link
            )
        }
    }
}
package com.clearkeep.screen.chat.group_invite

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.composes.FriendListItem
import com.clearkeep.screen.chat.composes.FriendListItemSelectable
import com.clearkeep.screen.chat.utils.getPeopleFromLink
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InviteGroupScreen(
    managerMember: MemberUIType? = InviteMemberUIType,
    inviteGroupViewModel: InviteGroupViewModel,
    selectedItem: SnapshotStateList<User>,
    listMemberInGroup: List<User>,
    onFriendSelected: (List<User>) -> Unit,
    onDirectFriendSelected: (User) -> Unit,
    onBackPressed: () -> Unit,
    onInsertFriend: () -> Unit,
    isCreateDirectGroup: Boolean = true
) {
    val friends = inviteGroupViewModel.filterFriends.observeAsState()
    val textSearch = remember { mutableStateOf("") }
    val useCustomServerChecked = remember { mutableStateOf(false) }
    val urlOtherServer = remember { mutableStateOf("") }
    val addUserFromOtherServerError = remember { mutableStateOf("") }
    val checkUserUrlResponse = inviteGroupViewModel.checkUserUrlResponse.observeAsState()
    val context = LocalContext.current

    CKSimpleTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            Column(
                modifier = Modifier
                    .weight(0.66f)
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.sdp(), top = 24.sdp()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1.0f, true),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackPressed
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chev_left),
                                contentDescription = "",
                                tint = grayscale1
                            )
                        }
                        CKHeaderText(
                            text = when (managerMember) {
                                InviteMemberUIType -> {
                                    if (isCreateDirectGroup) "Create direct message" else "Create group"
                                }
                                AddMemberUIType -> "Add member"
                                else -> "Create group"
                            }, headerTextType = HeaderTextType.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.sdp()))
                Row(modifier = Modifier.padding(horizontal = 16.sdp())) {
                    CKSearchBox(
                        textSearch,
                        placeholder = when {
                            isCreateDirectGroup -> stringResource(R.string.search)
                            managerMember == AddMemberUIType -> stringResource(R.string.search_people)
                            else -> stringResource(R.string.create_group_search)
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(all = 16.sdp())
                ) {
                    if (selectedItem.isNotEmpty()) {
                        SelectedHorizontalBox(selectedItem,
                            unSelectItem = { people ->
                                selectedItem.remove(people)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.sdp())
                        .clickable {
                            useCustomServerChecked.value = !useCustomServerChecked.value
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = if (useCustomServerChecked.value) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
                        ""
                    )
                    Spacer(modifier = Modifier.width(16.sdp()))
                    Text(
                        text = "Add user from other server",
                        modifier = Modifier.padding(vertical = 16.sdp()),
                        style = MaterialTheme.typography.body1.copy(
                            color = grayscaleBlack,
                            fontSize = defaultNonScalableTextSize(),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(Modifier.padding(horizontal = 16.sdp())) {
                    AnimatedVisibility(
                        visible = useCustomServerChecked.value,
                        enter = expandIn(
                            expandFrom = Alignment.BottomStart,
                            animationSpec = tween(300, easing = LinearOutSlowInEasing)
                        ),
                        exit = shrinkOut(
                            shrinkTowards = Alignment.CenterStart,
                            targetSize = { fullSize ->
                                IntSize(
                                    fullSize.width / 10,
                                    fullSize.height / 10
                                )
                            },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )

                    ) {
                        Column {
                            Row(
                                modifier = Modifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CKTextInputField(
                                        stringResource(R.string.invite_url_placeholder),
                                        textValue = urlOtherServer,
                                        keyboardType = KeyboardType.Text,
                                        singleLine = true,
                                    )
                                }
                            }

                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.sdp()))
                friends.value?.let { values ->
                    val listShow = values.filterNot { listMemberInGroup.contains(it) }
                    LazyColumn(
                        contentPadding = PaddingValues(end = 16.sdp(), start = 16.sdp()),
                    ) {
                        itemsIndexed(listShow) { _, friend ->
                            if (isCreateDirectGroup) {
                                FriendListItem(friend, onFriendSelected = {
                                    onDirectFriendSelected(it)
                                })
                            } else {
                                FriendListItemSelectable(friend,
                                    selectedItem.contains(friend),
                                    onFriendSelected = { people, isAdd ->
                                        if (isAdd) selectedItem.add(people) else
                                            selectedItem.remove(people)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(16.sdp())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CKButton(
                    if (useCustomServerChecked.value && !isCreateDirectGroup) stringResource(id = R.string.btn_add)
                    else  stringResource(R.string.btn_next),
                    onClick = {
                        if (useCustomServerChecked.value) {
                            val people = getPeopleFromLink(urlOtherServer.value)
                            if (people?.userId != inviteGroupViewModel.getClientId()){
                                if (isCreateDirectGroup) {
                                    if (people != null) {
                                        inviteGroupViewModel.checkUserUrlValid(people.userId, people.domain)
                                    } else {
                                        addUserFromOtherServerError.value = "Profile link is incorrect."
                                    }
                                } else {
                                    if (people != null) {
                                        inviteGroupViewModel.checkUserUrlValid(people.userId, people.domain)
                                    } else {
                                        addUserFromOtherServerError.value = "Profile link is incorrect."
                                    }
                                }
                            } else {
                                addUserFromOtherServerError.value = "You can't create conversation with yourself"
                            }
                            urlOtherServer.value = ""
                            useCustomServerChecked.value = false
                        } else {
                            if (selectedItem.size > 0) {
                                onFriendSelected(selectedItem)
                            }
                        }
                    },
                    modifier = Modifier
                        .width(200.sdp())
                )
            }
        }
    }

    LaunchedEffect(textSearch) {
        snapshotFlow { textSearch.value }
            .distinctUntilChanged()
            .collect {
                inviteGroupViewModel.search(it)
            }
    }

    if (addUserFromOtherServerError.value.isNotBlank()) {
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = addUserFromOtherServerError.value,
            onDismissButtonClick = {
                addUserFromOtherServerError.value = ""
                inviteGroupViewModel.checkUserUrlResponse.value = null
            }
        )
    }

    if (checkUserUrlResponse.value?.status == Status.ERROR) {
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = checkUserUrlResponse.value!!.message,
            onDismissButtonClick = {
                inviteGroupViewModel.checkUserUrlResponse.value = null
            }
        )
    } else if (checkUserUrlResponse.value?.status == Status.SUCCESS) {
        val people = checkUserUrlResponse.value!!.data

        people?.let { user ->
            if (isCreateDirectGroup) {
                onDirectFriendSelected(user)
            } else {
                inviteGroupViewModel.insertFriend(user)
                if (selectedItem.find { people == it } == null)
                    selectedItem.add(user)
            }
        }
    }
}

@Composable
fun SelectedHorizontalBox(selectedItem: List<User>, unSelectItem: (people: User) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.sdp()),
    ) {
        itemsIndexed(selectedItem) { _, friend ->
            SelectedFriendBox(people = friend, onRemove = { unSelectItem(it) })
        }
    }
}

sealed class MemberUIType
object InviteMemberUIType : MemberUIType()
object AddMemberUIType : MemberUIType()
object RemoveMemberUIType : MemberUIType()
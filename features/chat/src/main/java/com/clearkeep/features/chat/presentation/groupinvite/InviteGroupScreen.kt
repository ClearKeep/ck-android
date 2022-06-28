package com.clearkeep.features.chat.presentation.groupinvite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LiveData
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.*
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.features.chat.presentation.composes.FriendListItem
import com.clearkeep.features.chat.presentation.composes.FriendListItemSelectable
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.common.utilities.defaultNonScalableTextSize
import com.clearkeep.common.utilities.isValidEmail
import com.clearkeep.common.utilities.sdp
import com.clearkeep.domain.model.User
import com.clearkeep.features.chat.presentation.utils.getPeopleFromLink
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InviteGroupScreen(
    managerMember: MemberUIType? = InviteMemberUIType,
    inviteGroupViewModel: InviteGroupViewModel,
    selectedItem: SnapshotStateList<User>,
    chatGroup: LiveData<com.clearkeep.domain.model.ChatGroup>,
    onFriendSelected: (List<User>) -> Unit,
    onDirectFriendSelected: (User) -> Unit,
    onBackPressed: () -> Unit,
    isCreateDirectGroup: Boolean = true
) {
    val friends = inviteGroupViewModel.filterFriends.observeAsState()
    val textSearch = rememberSaveable { mutableStateOf("") }
    val useCustomServerChecked = rememberSaveable { mutableStateOf(false) }
    val useFindByEmail = rememberSaveable { mutableStateOf(false) }
    val urlOtherServer = rememberSaveable { mutableStateOf("") }
    val emailFind = rememberSaveable { mutableStateOf("") }
    val addUserFromOtherServerError = rememberSaveable { mutableStateOf("") }
    val addUserByEmailError = rememberSaveable { mutableStateOf("") }
    val checkUserUrlResponse = inviteGroupViewModel.checkUserUrlResponse.observeAsState()
    val group = chatGroup.observeAsState()
    val isLoading = inviteGroupViewModel.isLoading.observeAsState()
    val context = LocalContext.current

    CKSimpleTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
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
                                    tint = LocalColorMapping.current.iconColorAlt
                                )
                            }
                            CKHeaderText(
                                text = when (managerMember) {
                                    InviteMemberUIType -> {
                                        if (isCreateDirectGroup) {
                                            stringResource(R.string.create_direct_message)
                                        } else {
                                            stringResource(R.string.create_group)
                                        }
                                    }
                                    AddMemberUIType -> stringResource(R.string.add_member)
                                    else -> stringResource(R.string.create_group)
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

                    friends.value?.let { values ->
                        val membersId =
                            group.value?.clientList?.filter { it.userState == com.clearkeep.domain.model.UserStateTypeInGroup.ACTIVE.value }
                                ?.map { it.userId } ?: emptyList()
                        val listShow = values.filterNot { membersId.contains(it.userId) }
                        LazyColumn {
                            item {
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
                                            if (useFindByEmail.value) useFindByEmail.value=!useFindByEmail.value
                                        }, verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = if (useCustomServerChecked.value) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
                                        ""
                                    )
                                    Spacer(modifier = Modifier.width(16.sdp()))
                                    Text(
                                        text = stringResource(R.string.add_user_other_server),
                                        modifier = Modifier.padding(vertical = 16.sdp()),
                                        style = MaterialTheme.typography.body1.copy(
                                            color = LocalColorMapping.current.bodyTextAlt,
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


                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.sdp())
                                        .clickable {
                                            useFindByEmail.value = !useFindByEmail.value
                                            if (useCustomServerChecked.value) useCustomServerChecked.value = !useCustomServerChecked.value
                                        }, verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = if (useFindByEmail.value) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
                                        ""
                                    )
                                    Spacer(modifier = Modifier.width(16.sdp()))
                                    Text(
                                        text = stringResource(R.string.add_user_other_server_email),
                                        modifier = Modifier.padding(vertical = 16.sdp()),
                                        style = MaterialTheme.typography.body1.copy(
                                            color = LocalColorMapping.current.bodyTextAlt,
                                            fontSize = defaultNonScalableTextSize(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Column(Modifier.padding(horizontal = 16.sdp())) {
                                    AnimatedVisibility(
                                        visible = useFindByEmail.value,
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
                                                        stringResource(R.string.invite_email_placeholder),
                                                        textValue = emailFind,
                                                        keyboardType = KeyboardType.Text,
                                                        singleLine = true,
                                                        onDone = {
                                                            if (useFindByEmail.value){
                                                                if (emailFind.value.trim().isValidEmail()) {
                                                                    inviteGroupViewModel.findEmail(emailFind.value.trim())
                                                                }else {
                                                                    addUserByEmailError.value =
                                                                        context.getString(R.string.invite_friend_profile_email_incorrect)
                                                                }
                                                            }                                                        }
                                                    )
                                                }
                                            }

                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.sdp()))
                            }
                            itemsIndexed(listShow) { _, friend ->
                                if (isCreateDirectGroup) {
                                    FriendListItem(Modifier.padding(horizontal = 16.sdp()), friend, onFriendSelected = {
                                        if (isLoading.value != true) {
                                            onDirectFriendSelected(it)
                                        }
                                    })
                                } else {
                                    FriendListItemSelectable(Modifier.padding(horizontal = 16.sdp()), friend,
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
                    if ((isCreateDirectGroup && useCustomServerChecked.value) || (!isCreateDirectGroup) || useFindByEmail.value) {
                        CKButton(
                            if (useCustomServerChecked.value && !isCreateDirectGroup && !useFindByEmail.value) stringResource(
                                id = R.string.btn_add
                            )
                            else if (useFindByEmail.value) stringResource(R.string.btn_find)
                            else stringResource(R.string.btn_next),
                            onClick = {
                                if (useFindByEmail.value){
                                    if (emailFind.value.trim().isValidEmail()) {
                                        inviteGroupViewModel.findEmail(emailFind.value.trim())
                                    }else {
                                        addUserFromOtherServerError.value =
                                            context.getString(R.string.invite_friend_profile_email_incorrect)
                                    }
                                }
                                else if (useCustomServerChecked.value) {
                                    val people = getPeopleFromLink(urlOtherServer.value.trim())
                                    if (people?.userId != inviteGroupViewModel.getClientId()) {
                                        if (isCreateDirectGroup) {
                                            if (people != null) {
                                                inviteGroupViewModel.checkUserUrlValid(
                                                    people.userId,
                                                    people.domain
                                                )
                                            } else {
                                                addUserFromOtherServerError.value =
                                                    context.getString(R.string.invite_friend_profile_link_incorrect)
                                            }
                                        } else {
                                            if (people != null) {
                                                inviteGroupViewModel.checkUserUrlValid(
                                                    people.userId,
                                                    people.domain
                                                )
                                            } else {
                                                addUserFromOtherServerError.value =
                                                    context.getString(R.string.invite_friend_profile_link_incorrect)
                                            }
                                        }
                                    } else {
                                        addUserFromOtherServerError.value =
                                            context.getString(R.string.invite_friend_profile_link_self)
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
                                .width(200.sdp()),
                            enabled = isLoading.value != true && ((isCreateDirectGroup && useCustomServerChecked.value && urlOtherServer.value.isNotBlank()) || (!isCreateDirectGroup && (selectedItem.isNotEmpty()
                                    || (useCustomServerChecked.value && urlOtherServer.value.isNotBlank()))) || (useFindByEmail.value && emailFind.value.trim().isValidEmail()))
                        )
                    }
                }
            }
            if (isLoading.value == true) {
                Box(Modifier.fillMaxSize()) {
                    CKCircularProgressIndicator(Modifier.align(Alignment.Center))
                }
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

    if (addUserByEmailError.value.isNotBlank()) {
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = addUserByEmailError.value,
            onDismissButtonClick = {
                addUserByEmailError.value = ""
            }
        )
    }


    if (checkUserUrlResponse.value?.status == com.clearkeep.common.utilities.network.Status.ERROR) {
        val error = checkUserUrlResponse.value!!.message?.split(",")
        val (errorTitle, errorText) = if (error?.size == 1) {
            stringResource(R.string.warning) to checkUserUrlResponse.value!!.message
        } else {
            error?.get(0) to error?.get(1)
        }

        CKAlertDialog(
            title = errorTitle,
            text = errorText,
            onDismissButtonClick = {
                inviteGroupViewModel.checkUserUrlResponse.value = null
            }
        )
    } else if (checkUserUrlResponse.value?.status == com.clearkeep.common.utilities.network.Status.SUCCESS) {
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
        inviteGroupViewModel.checkUserUrlResponse.value = null
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
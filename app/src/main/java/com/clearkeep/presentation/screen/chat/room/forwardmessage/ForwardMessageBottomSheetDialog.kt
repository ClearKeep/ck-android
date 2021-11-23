package com.clearkeep.presentation.screen.chat.room.forwardmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.presentation.components.*
import com.clearkeep.presentation.components.base.ButtonType
import com.clearkeep.presentation.components.base.CKButton
import com.clearkeep.presentation.components.base.CKSearchBox
import com.clearkeep.presentation.components.base.CKText
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.User
import com.clearkeep.presentation.screen.chat.composes.CircleAvatar
import com.clearkeep.presentation.screen.chat.composes.NewFriendListItem
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.isImageMessage
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp

@Composable
fun ForwardMessageBottomSheetDialog(
    message: com.clearkeep.domain.model.Message,
    forwardMessageResponse: Long?,
    allGroups: List<com.clearkeep.domain.model.ChatGroup>,
    peerUsersStatus: List<com.clearkeep.domain.model.User>,
    currentGroupUsersStatus: List<com.clearkeep.domain.model.User>,
    onForwardMessageGroup: (groupId: Long) -> Unit,
    onForwardMessagePeer: (receiver: com.clearkeep.domain.model.User, groupId: Long) -> Unit
) {
    val query = rememberSaveable { mutableStateOf("") }

    printlnCK("ForwardMessageBottomSheetDialog forwardMessageResponse $forwardMessageResponse")

    val groups = allGroups.filter { it.isGroup() && it.groupName.contains(query.value, true) }
    val users = allGroups.filter { !it.isGroup() && it.groupName.contains(query.value, true) }

    LazyColumn(Modifier.padding(horizontal = 16.sdp())) {
        item {
            Spacer(Modifier.height(40.sdp()))
            if (!isImageMessage(message.message)) {
                val user = currentGroupUsersStatus.find { it.userId == message.senderId }
                user?.let {
                    QuotedMessage(message, user = user)
                }
                Spacer(Modifier.height(16.sdp()))
            }
            CKText(
                "Forward to",
                color = LocalColorMapping.current.descriptionText,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.sdp()))
            CKSearchBox(
                query,
                placeholder = "Search for group or individuals",
                isDarkModeInvertedColor = true
            )
            Spacer(Modifier.height(8.sdp()))
        }
        if (groups.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.sdp()))
                CKText("Group", color = LocalColorMapping.current.descriptionText)
                Spacer(Modifier.height(8.sdp()))
            }
        }
        items(groups.size) { index ->
            val group = groups[index]
            val sent = forwardMessageResponse == group.groupId
            ForwardGroupItem(group, sent) {
                onForwardMessageGroup(group.groupId)
            }
        }
        if (users.isNotEmpty()) {
            item {
                CKText("User", color = LocalColorMapping.current.descriptionText)
                Spacer(Modifier.height(8.sdp()))
            }
        }
        items(users.size) { index ->
            val peerChatGroup = users[index]
            val otherUserId = peerChatGroup.clientList.find { it.userId != peerChatGroup.owner.clientId }?.userId ?: ""
            val user = peerUsersStatus.find { it.userId == otherUserId }
            val sent = forwardMessageResponse == peerChatGroup.groupId
            user?.let {
                ForwardPeerItem(peerChatGroup, sent, it) {
                    onForwardMessagePeer(it, peerChatGroup.groupId)
                }
            }
        }
    }
}

@Composable
fun QuotedMessage(message: com.clearkeep.domain.model.Message, user: com.clearkeep.domain.model.User) {
    Row {
        CircleAvatar(
            arrayListOf(user.avatar ?: ""),
            user.userName,
            size = 18.sdp()
        )
        Spacer(Modifier.width(8.sdp()))
        CKText(
            text = user.userName,
            style = MaterialTheme.typography.body2.copy(
                color = colorSuccessDefault,
                fontWeight = FontWeight.W600,
                fontSize = defaultNonScalableTextSize()
            ),
        )
    }
    Row(
        Modifier
            .background(
                if (LocalColorMapping.current.isDarkTheme) Color(0xFF9E9E9E) else grayscale5,
                RoundedCornerShape(topEnd = 24.sdp(), bottomEnd = 24.sdp())
            )
            .padding(end = 16.sdp())
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .height(IntrinsicSize.Max)
                .width(2.sdp())
                .background(grayscale1)
        )
        Spacer(Modifier.width(16.sdp()))
        CKText(
            message.message,
            modifier = Modifier.fillMaxWidth(),
            color = if (LocalColorMapping.current.isDarkTheme) grayscaleDarkModeDarkGrey2 else grayscale2
        )
    }
}

@Composable
private fun ForwardGroupItem(group: com.clearkeep.domain.model.ChatGroup, sent: Boolean, onClick: () -> Unit) {
    ConstraintLayout(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.sdp())
    ) {
        val (groupName, sendButton) = createRefs()
        val margin = 8.sdp()
        CKText(
            group.groupName,
            Modifier.constrainAs(groupName) {
                top.linkTo(sendButton.top)
                bottom.linkTo(sendButton.bottom)
                start.linkTo(parent.start)
                end.linkTo(sendButton.start, margin)
                width = Dimension.fillToConstraints
            },
            fontWeight = FontWeight.Bold,
            color = LocalColorMapping.current.bodyTextAlt
        )
        CKButton(
            if (sent) stringResource(R.string.btn_sent) else stringResource(R.string.btn_send),
            onClick = {
                onClick()
            },
            Modifier
                .constrainAs(sendButton) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
                .width(120.sdp()),
            buttonType = ButtonType.BorderGradient
        )
    }
}

@Composable
fun ForwardPeerItem(peer: com.clearkeep.domain.model.ChatGroup, sent: Boolean, userInfo: com.clearkeep.domain.model.User, onClick: () -> Unit) {
    val user =
        com.clearkeep.domain.model.User(
            "",
            peer.groupName,
            peer.ownerDomain,
            userInfo.userState,
            userInfo.userStatus,
            "",
            userInfo.avatar,
            ""
        )

    NewFriendListItem(
        Modifier.padding(bottom = 8.sdp()),
        user,
        action = {
            CKButton(
                if (sent) stringResource(R.string.btn_sent) else stringResource(R.string.btn_send),
                modifier = Modifier.width(120.sdp()),
                onClick = {
                    onClick()
                },
                buttonType = ButtonType.BorderGradient
            )
        }
    ) {

    }
}


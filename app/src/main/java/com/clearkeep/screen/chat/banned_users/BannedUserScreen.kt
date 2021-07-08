package com.clearkeep.screen.chat.main.banned_users

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.components.grayscale2
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.composes.FriendListItemAction
import com.clearkeep.screen.chat.contact_search.InputSearchBox

@Composable
fun BannedUserScreen() {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()) {
        HeaderBannedUser {}
        Spacer(modifier = Modifier.height(24.dp))
        InputSearchBox({}, {})
        Spacer(modifier = Modifier.height(16.dp))
        Text("User in Contact", color = grayscale2)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            item {
                FriendListItemAction("Unbanned", User(null, "", "Alex Mendes", "")) {}
                FriendListItemAction("Unbanned", User(null,"", "Alissa Baker", "")) {}
                FriendListItemAction("Unbanned", User(null,"", "Barbara Johnson", "")) {}
            }
        }
    }
}

@Composable
fun HeaderBannedUser(onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                alignment = Alignment.CenterStart
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        CKHeaderText("Banned User", headerTextType = HeaderTextType.Medium)
    }
}

@Preview
@Composable
fun BannedUserScreenPreview() {
    BannedUserScreen()
}

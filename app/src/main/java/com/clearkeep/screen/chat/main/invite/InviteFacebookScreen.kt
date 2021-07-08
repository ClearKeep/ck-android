package com.clearkeep.screen.chat.main.invite

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscale5
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.composes.FriendListItemSelectable

@Composable
fun InviteFacebookScreen() {
    val search = remember { mutableStateOf("") }

    Box {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()) {
            HeaderInviteFacebook {}
            Spacer(modifier = Modifier.height(24.dp))
            CKSearchBox(
                search,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(grayscale5)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                item {
                    FriendListItemSelectable(friend = People("", "test", ""), isSelected = false) { people: People, isAdd: Boolean -> }
                    FriendListItemSelectable(friend = People("", "Alex Mendes", ""), isSelected = false) { people: People, isAdd: Boolean -> }
                    FriendListItemSelectable(friend = People("", "Barbara Johnson", ""), isSelected = false) { people: People, isAdd: Boolean -> }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp, start = 16.dp, end = 16.dp)) {
            CKButton("Invite Selected", {})
        }
    }

}

@Composable
fun HeaderInviteFacebook(onCloseView: () -> Unit) {
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
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                alignment = Alignment.CenterStart
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        CKHeaderText("Invite user from Facebook", headerTextType = HeaderTextType.Medium)
    }
}

@Preview
@Composable
fun InviteFacebookScreenPreview() {
    InviteFacebookScreen()
}

package com.clearkeep.screen.chat.invite

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
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscale5
import com.clearkeep.utilities.sdp

@Composable
fun InviteFacebookScreen() {
    val search = remember { mutableStateOf("") }

    Box {
        Column(
            Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize()
        ) {
            HeaderInviteFacebook {}
            Spacer(modifier = Modifier.height(24.sdp()))
            CKSearchBox(
                search,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.sdp()))
                    .background(grayscale5)
            )
            Spacer(modifier = Modifier.height(16.sdp()))
            LazyColumn {
                item {
                    /*InviteFromFacebookItem(Modifier.padding(vertical = 8.dp), user = User("", "test", ""), isSelected = false) { people: User, isAdd: Boolean -> }
                    InviteFromFacebookItem(Modifier.padding(vertical = 8.dp), user = User("", "Alex Mendes", ""), isSelected = false) { people: User, isAdd: Boolean -> }
                    InviteFromFacebookItem(Modifier.padding(vertical = 8.dp), user = User("", "Barbara Johnson", ""), isSelected = false) { people: User, isAdd: Boolean -> }*/
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.sdp(), start = 16.sdp(), end = 16.sdp())
        ) {
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
        Spacer(Modifier.size(32.sdp()))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_chev_left),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                alignment = Alignment.CenterStart
            )
        }
        Spacer(modifier = Modifier.size(16.sdp()))
        CKHeaderText("Invite user from Facebook", headerTextType = HeaderTextType.Medium)
    }
}

package com.clearkeep.screen.chat.main.invite

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscale5
import com.clearkeep.components.pickledBlueWood
import com.clearkeep.components.primaryDefault

@Composable
fun InviteScreen(onCloseView: () -> Unit) {
    val email = remember { mutableStateOf("") }

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()) {
        HeaderInvite(onCloseView)
        Spacer(modifier = Modifier.height(24.dp))
        CKTextInputField(
            "Invite by email",
            email,
            keyboardType = KeyboardType.Email,
            singleLine = true,
            leadingIcon = {
                Image(
                    painterResource(R.drawable.ic_icon_mail),
                    contentDescription = null
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        CKButton("Invite", {}, enabled = email.value.isNotBlank())
        Spacer(modifier = Modifier.height(40.dp))

        InviteOption(text = "User from Facebook")
        Spacer(modifier = Modifier.height(8.dp))
        InviteOption(text = "User from Google")
        Spacer(modifier = Modifier.height(8.dp))
        InviteOption(text = "User from Microsoft")
    }
}

@Composable
fun HeaderInvite(onCloseView: () -> Unit) {
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
        CKHeaderText("Invite", headerTextType = HeaderTextType.Medium, color = primaryDefault,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun InviteOption(text: String) {
    Box(Modifier.background(grayscale5, RoundedCornerShape(16.dp)).padding(24.dp, 14.dp).fillMaxWidth()) {
        Text(text)
        Icon(painter = painterResource(R.drawable.ic_arrow_right), contentDescription = "", Modifier.align(
            Alignment.CenterEnd), tint = pickledBlueWood)
    }
}
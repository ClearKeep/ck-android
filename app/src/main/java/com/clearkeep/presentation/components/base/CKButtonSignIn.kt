package com.clearkeep.presentation.components.base


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.presentation.components.grayscaleOffWhite

private val height = 40.dp

@Composable
fun CKButtonSignIn(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: LoginType = LoginType.Google
) {
    val icon = when (buttonType) {
        LoginType.Google -> {
            painterResource(R.drawable.ic_icon_google)
        }
        LoginType.Microsoft -> {
            painterResource(R.drawable.ic_icon_office)
        }
        LoginType.Facebook -> {
            painterResource(R.drawable.ic_icons_facebook)
        }
    }

    Row(
        modifier = Modifier
            .background(
                grayscaleOffWhite, shape = RoundedCornerShape(50),
            )
            .clickable { onClick() }
            .size(dimensionResource(R.dimen._56sdp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Image(
                modifier = Modifier
                    .size(dimensionResource(R.dimen._24sdp)),
                painter = icon,
                contentDescription = null
            )
        }
    }
}

enum class LoginType {
    Google,
    Microsoft,
    Facebook,
}
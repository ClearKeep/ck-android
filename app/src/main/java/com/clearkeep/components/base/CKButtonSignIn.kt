package com.clearkeep.components.base


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.colorBlue1
import com.clearkeep.components.colorLightBlueFace
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.components.colorTiaMaria

private val height = 40.dp

@Composable
fun CKButtonSignIn(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: LoginType = LoginType.Google
) {
    val icon: Painter
    val textColor: Color
    when (buttonType) {
        LoginType.Google -> {
            icon = painterResource(R.drawable.ic_icon_google)
            textColor = colorBlue1
        }
        LoginType.Microsoft -> {
            icon = painterResource(R.drawable.ic_icon_office)
            textColor = colorTiaMaria
        }
        LoginType.Facebook->{
            icon = painterResource(R.drawable.ic_icons_facebook)
            textColor = colorLightBlueFace
        }

    }
    Row(
        modifier = Modifier.background(
            grayscaleOffWhite, shape = RoundedCornerShape(50),
        ).clickable { onClick() }.size(56.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Image(
                modifier = Modifier
                    .size(24.dp),
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

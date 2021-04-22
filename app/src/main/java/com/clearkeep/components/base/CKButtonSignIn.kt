package com.clearkeep.components.base


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
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
    OutlinedButton(
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = grayscaleOffWhite),
        shape = RoundedCornerShape(50),
        onClick = { onClick() },
        modifier = modifier.height(height),
        enabled = enabled,
    ) {
        Row {
            Text(
                text = label,
                color = textColor,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Image(
                modifier = Modifier.padding(start = 16.dp),
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

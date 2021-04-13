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
import com.clearkeep.components.grayscaleOffWhite

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
    if (buttonType == LoginType.Google) {
        icon = painterResource(R.drawable.ic_icon_google)
        textColor = Color.Blue
    } else {
        icon = painterResource(R.drawable.ic_icon_office)
        textColor = Color.Red
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
}

package com.clearkeep.utils

import android.app.Activity
import android.os.Handler
import android.util.Base64
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontWeight
import androidx.ui.unit.dp
import androidx.ui.unit.sp

// Models
data class Person(
    val name: String,
    val age: Int,
    val profilePictureUrl: String? = null
)


// Methods
fun getPersonList() = mutableListOf<Person>(
    Person("Grace Hopper", 25),
    Person("Ada Lovelace", 29)
)

@Composable
fun TitleComponent(title: String) {
    // Text is a predefined composable that does exactly what you'd expect it to - display text on
    // the screen. It allows you to customize its appearance using style, fontWeight, fontSize, etc.
    Text(title, style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W900,
        fontSize = 14.sp, color = Color.Black), modifier = Modifier.padding(16.dp) +
            Modifier.fillMaxWidth()
    )
}

@Composable
fun showMessage(activity: Activity, message: String, mainThreadHandler: Handler) {
    mainThreadHandler.post {
//        Toast.makeText(@App, message, Toast.LENGTH_SHORT).show()
    }
}

fun base64Encode(input: ByteArray): String {
    return Base64.encodeToString(input, Base64.DEFAULT)
}

fun base64Decode(input: String): ByteArray {
    return Base64.decode(input, Base64.DEFAULT)
}

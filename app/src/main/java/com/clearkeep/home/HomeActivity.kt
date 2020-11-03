package com.clearkeep.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.chat.single.SingleChatActivity
import com.clearkeep.ui.ButtonGeneral

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OurView()
        }
    }

    @Composable
    fun OurView() {
        Column (
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ButtonGeneral(
                "SingleChat",
                onClick = {
                    navigateToSingleChatActivity()
                })
        }
    }

    private fun navigateToSingleChatActivity() {
        val intent = Intent(this, SingleChatActivity::class.java)
        startActivity(intent)
    }

    @Preview
    @Composable
    fun PreviewApp() {
        OurView()
    }
}


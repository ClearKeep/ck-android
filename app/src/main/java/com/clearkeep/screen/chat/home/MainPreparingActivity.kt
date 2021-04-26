package com.clearkeep.screen.chat.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKCircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainPreparingActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val homePreparingViewModel: MainPreparingViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CKTheme {
                homePreparingViewModel.prepareState.observeAsState().value.let { prepareState ->
                    when (prepareState) {
                        PrepareSuccess -> {
                            navigateToHomeScreen()
                        }
                        PrepareProcessing -> {
                            LoadingComposable()
                        }
                        PrepareError -> {
                            ErrorComposable()
                        }
                    }
                }
            }
        }

        homePreparingViewModel.prepareChat()
    }

    @Composable
    private fun LoadingComposable() {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CKCircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorComposable() {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Please try again")
            Spacer(Modifier.height(20.dp))
            CKButton(
                "Try again",
                onClick = {
                    homePreparingViewModel.prepareChat()
                },
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    private fun navigateToHomeScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
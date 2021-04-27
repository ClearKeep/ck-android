package com.clearkeep.screen.main_antx

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.components.CKTheme
import com.clearkeep.components.grayscaleOffWhite
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LifecycleOwner {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val mainViewModel: MainViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CKTheme {
                AppContent()
            }
        }
    }

    @Composable
    fun AppContent() {
        Row(Modifier.fillMaxSize().background(grayscaleOffWhite)) {
            Box(Modifier.width(88.dp).background(Color.White)) {
                menuView(mainViewModel)
            }
            Box(
                Modifier
                    .fillMaxSize()) {
            }
        }

    }
}
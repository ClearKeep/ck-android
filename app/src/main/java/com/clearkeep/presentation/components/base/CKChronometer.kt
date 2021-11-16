package com.clearkeep.presentation.components.base

import android.os.SystemClock
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.clearkeep.presentation.components.grayscale5
import com.clearkeep.utilities.convertSecondsToHMmSs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlin.math.ceil

@Composable
fun CKChronometer(
    modifier: Modifier,
    base: Long
) {
    val value by remember { mutableStateOf(SystemClock.elapsedRealtime() - base) }
    val timer = remember { createTimer() }
    val tickSecond = timer.collectAsState(initial = 0)

    DisposableEffect(Unit) {
        onDispose {
        }
    }

    Text(
        getTimeAsString(ceil(value / 1000f).toLong() + tickSecond.value),
        style = MaterialTheme.typography.caption.copy(
            color = grayscale5,
        ),
        modifier = modifier,
    )
}

fun createTimer(): Flow<Int> {
    return (0 until Int.MAX_VALUE)
        .asFlow()
        .onEach {
            delay(1000)
        }
}

private fun getTimeAsString(seconds: Long): String {
    return convertSecondsToHMmSs(seconds)
}
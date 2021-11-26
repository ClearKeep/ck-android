package com.clearkeep.utilities

import android.content.Context
import android.content.Intent
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.features.splash.presentation.SplashActivity
import kotlin.system.exitProcess

fun restartToRoot(context: Context) {
    printlnCK("restartActivityToRoot")
    val intent = Intent(context, SplashActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
    exitProcess(2)
}
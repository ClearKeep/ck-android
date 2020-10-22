package com.clearkeep.application

import android.app.Application

class MyApplication : Application() {
    // AppContainer instance used by the rest of classes to obtain dependencies
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container =
            AppContainerImpl(applicationContext)
    }
}

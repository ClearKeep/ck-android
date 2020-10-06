package com.clearkeep.application

import android.app.Application
import com.clearkeep.data.AppContainer
import com.clearkeep.data.AppContainerImpl

class MyApplication : Application() {
    // AppContainer instance used by the rest of classes to obtain dependencies
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(applicationContext)
    }
}

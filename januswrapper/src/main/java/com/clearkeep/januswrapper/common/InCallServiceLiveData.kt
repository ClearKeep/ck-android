package com.clearkeep.januswrapper.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import com.clearkeep.januswrapper.AppCall
import com.clearkeep.januswrapper.utils.Constants.ACTION_CALL_SERVICE_AVAILABLE_STATE_CHANGED
import com.clearkeep.januswrapper.utils.Constants.EXTRA_SERVICE_IS_AVAILABLE

class InCallServiceLiveData(private val context: Context) : LiveData<Boolean>() {

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postValue(intent.getBooleanExtra(EXTRA_SERVICE_IS_AVAILABLE, false))
        }
    }

    override fun onActive() {
        super.onActive()
        postValue(AppCall.isCallAvailable(context))
        context.registerReceiver(
                networkReceiver,
                IntentFilter(ACTION_CALL_SERVICE_AVAILABLE_STATE_CHANGED)
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            context.unregisterReceiver(networkReceiver)
        } catch (e: Exception) {
        }
    }
}
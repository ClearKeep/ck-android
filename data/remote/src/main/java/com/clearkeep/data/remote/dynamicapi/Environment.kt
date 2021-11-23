package com.clearkeep.data.remote.dynamicapi

import com.clearkeep.domain.model.Server
import com.clearkeep.utilities.printlnCK
import java.lang.IllegalArgumentException
import javax.inject.Inject

class Environment @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider
) {
    private var server: com.clearkeep.domain.model.Server? = null

    //Temp server info - to get key for received messages
    private var tempServer: com.clearkeep.domain.model.Server? = null

    fun setUpDomain(server: com.clearkeep.domain.model.Server) {
        printlnCK("setUpDomain: $server")
        this.server = server
        dynamicAPIProvider.setUpDomain(server)
    }

    fun setUpTempDomain(server: com.clearkeep.domain.model.Server) {
        this.tempServer = server
    }

    fun getServer(): com.clearkeep.domain.model.Server {
        if (server == null) {
            printlnCK("getServer: server must be not NULL")
            throw IllegalArgumentException("getServer: server must be not NULL")
        }
        return server!!
    }

    fun getTempServer(): com.clearkeep.domain.model.Server {
        if (tempServer == null) {
            printlnCK("getTempServer null")
        }
        return tempServer ?: getServer()
    }

    fun getServerCanNull(): com.clearkeep.domain.model.Server? {
        return server
    }
}
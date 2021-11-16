package com.clearkeep.dynamicapi

import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.utilities.printlnCK
import java.lang.IllegalArgumentException
import javax.inject.Inject

class Environment @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider
) {
    private var server: Server? = null

    //Temp server info - to get key for received messages
    private var tempServer: Server? = null

    fun setUpDomain(server: Server) {
        printlnCK("setUpDomain: $server")
        this.server = server
        dynamicAPIProvider.setUpDomain(server)
    }

    fun setUpTempDomain(server: Server) {
        this.tempServer = server
    }

    fun getServer(): Server {
        if (server == null) {
            printlnCK("getServer: server must be not NULL")
            throw IllegalArgumentException("getServer: server must be not NULL")
        }
        return server!!
    }

    fun getTempServer(): Server {
        if (tempServer == null) {
            printlnCK("getTempServer null")
        }
        return tempServer ?: getServer()
    }

    fun getServerCanNull(): Server? {
        return server
    }
}
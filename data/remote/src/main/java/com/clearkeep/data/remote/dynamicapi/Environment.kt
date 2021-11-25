package com.clearkeep.data.remote.dynamicapi

import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.Environment
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Environment @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider
) : Environment {
    private var server: Server? = null

    //Temp server info - to get key for received messages
    private var tempServer: Server? = null

    override fun setUpDomain(server: Server) {
        printlnCK("setUpDomain: $server")
        this.server = server
        dynamicAPIProvider.setUpDomain(server)
    }

    override fun setUpTempDomain(server: Server) {
        this.tempServer = server
    }

    override fun getServer(): Server {
        if (server == null) {
            printlnCK("getServer: server must be not NULL")
            throw IllegalArgumentException("getServer: server must be not NULL")
        }
        return server!!
    }

    override fun getTempServer(): Server {
        if (tempServer == null) {
            printlnCK("getTempServer null")
        }
        return tempServer ?: getServer()
    }

    override fun getServerCanNull(): Server? {
        return server
    }
}
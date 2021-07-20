package com.clearkeep.dynamicapi

import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.utilities.printlnCK
import java.lang.IllegalArgumentException
import javax.inject.Inject

class Environment @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider
) {
    private var server: Server? = null

    fun setUpDomain(server: Server) {
        if (server == null) {
            throw IllegalArgumentException("account, server must be not null")
        }
        printlnCK("setUpDomain: $server")
        this.server = server
        dynamicAPIProvider.setUpDomain(server)
    }

    fun getServer() : Server {
        if (server == null) {
            printlnCK("getServer: server must be not NULL")
            throw IllegalArgumentException("getServer: server must be not NULL")
        }
        return server!!
    }

    fun getServerCanNull() : Server?{
        return server
    }
}
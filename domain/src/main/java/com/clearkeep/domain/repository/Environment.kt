package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Server

interface Environment {
    fun setUpDomain(server: Server)
    fun setUpTempDomain(server: Server)
    fun getServer(): Server
    fun getTempServer(): Server
    fun getServerCanNull(): Server?
}
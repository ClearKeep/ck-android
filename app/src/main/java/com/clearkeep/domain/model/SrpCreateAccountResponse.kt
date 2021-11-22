package com.clearkeep.domain.model

data class SrpCreateAccountResponse(val salt: String, val verificator: String)
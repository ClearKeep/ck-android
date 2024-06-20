package com.clearkeep.domain.model.response

data class SrpCreateAccountResponse(val salt: String, val verificator: String)
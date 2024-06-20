package com.clearkeep.domain.model.response

data class MfaAuthChallengeResponse(val salt: String, val publicChallengeB: String)
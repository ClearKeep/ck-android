package com.clearkeep.domain.model

data class MfaAuthChallengeResponse(val salt: String, val publicChallengeB: String)
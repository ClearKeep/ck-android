package com.clearkeep.domain.model

data class RequestChangePasswordRes(val salt: String, val publicChallengeB: String)
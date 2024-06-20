package com.clearkeep.domain.model.response

data class RequestChangePasswordRes(val salt: String, val publicChallengeB: String)
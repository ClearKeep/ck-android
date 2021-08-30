package com.clearkeep.db.clear_keep.model

data class LoginResponse(val accessToken: String, val otpHash: String, val sub: String, val hashKey: String, val errorCode: Int, val errorMessage: String)
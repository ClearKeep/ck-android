package com.clearkeep.model

import androidx.compose.Model

@Model
data class Message(
    val id: String,
    val message: String
)
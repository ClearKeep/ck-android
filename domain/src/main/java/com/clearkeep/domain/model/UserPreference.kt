package com.clearkeep.domain.model

data class UserPreference(
    val serverDomain: String,
    val userId: String,
    val showNotificationPreview: Boolean,
    val doNotDisturb: Boolean,
    val mfa: Boolean,
    val isSocialAccount: Boolean
) {
    companion object {
        fun getDefaultUserPreference(
            serverDomain: String,
            userId: String,
            isSocialAccount: Boolean
        ) = UserPreference(
            serverDomain,
            userId,
            showNotificationPreview = true,
            doNotDisturb = false,
            mfa = false,
            isSocialAccount = isSocialAccount
        )
    }
}
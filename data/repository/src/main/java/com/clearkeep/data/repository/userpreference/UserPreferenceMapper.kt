package com.clearkeep.data.repository.userpreference

import com.clearkeep.data.local.clearkeep.userpreference.UserPreferenceEntity
import com.clearkeep.domain.model.UserPreference

fun UserPreferenceEntity.toModel() = UserPreference(
    serverDomain,
    userId,
    showNotificationPreview,
    doNotDisturb,
    mfa,
    isSocialAccount
)

fun UserPreference.toEntity() = UserPreferenceEntity(
    serverDomain,
    userId,
    showNotificationPreview,
    doNotDisturb,
    mfa,
    isSocialAccount
)
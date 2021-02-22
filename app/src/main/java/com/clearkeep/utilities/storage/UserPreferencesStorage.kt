package com.clearkeep.utilities.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UserPreferencesStorage @Inject constructor(@ApplicationContext context: Context)
    : BaseSharedPreferencesStorage(context, "CK_SharedPreference_User")
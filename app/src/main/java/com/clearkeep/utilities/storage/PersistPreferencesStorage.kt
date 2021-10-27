package com.clearkeep.utilities.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PersistPreferencesStorage @Inject constructor(@ApplicationContext context: Context) :
    BaseSharedPreferencesStorage(context, "CK_SharedPreference_Persist")
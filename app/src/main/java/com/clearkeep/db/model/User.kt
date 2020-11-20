package com.clearkeep.db.model

import androidx.annotation.NonNull
import androidx.room.*

@Entity
data class User(
    @NonNull
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_name") val userName: String?
)
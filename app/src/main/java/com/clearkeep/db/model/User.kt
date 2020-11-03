package com.clearkeep.db.model

import androidx.room.*

@Entity
data class User(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_name") val userName: String?
)
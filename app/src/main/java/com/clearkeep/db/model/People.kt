package com.clearkeep.db.model

import androidx.room.*

@Entity
data class People(
    @ColumnInfo(name = "user_name") val userName: String
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}
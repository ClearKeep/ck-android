package com.clearkeep.model

import androidx.room.*

@Entity(tableName = "users")
class User {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id = 0

    @ColumnInfo(name = "first_name")
    var firstName: String? = null

    @ColumnInfo(name = "session")
    var session: String? = null

    @ColumnInfo(name = "security")
    var security: String? = null


}
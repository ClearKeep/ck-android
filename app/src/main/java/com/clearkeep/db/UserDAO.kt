package com.clearkeep.db

import androidx.room.*
import com.clearkeep.model.User

@Dao
interface UserDAO {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByUserId(userId: Int): User?

    @Query("SELECT * FROM users WHERE first_name LIKE :userName")
    fun getUserByName(userName: String?): User?

    @Query("SELECT * FROM users")
    fun allUser(): MutableList<User>?

    @Insert
    fun insertUser(users: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAllUser()

    @Update
    fun updateUser(users: User?)
}
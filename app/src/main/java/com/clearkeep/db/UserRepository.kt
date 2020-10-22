package com.clearkeep.db

import com.clearkeep.model.User


class UserRepository(private val mLocalDataSource: UserDataSource) :
    UserDataSource {

    companion object {
        private var sInstance: UserRepository? = null
        fun getInstance(localDataSource: UserDataSource?): UserRepository? {
            if (sInstance == null) {
                sInstance = localDataSource?.let { UserRepository(it) }
            }
            return sInstance
        }
    }


    override fun getUserByUserId(userId: Int): User? {
        return mLocalDataSource.getUserByUserId(userId)
    }

    override fun getUserByName(userName: String?): User? {
        return mLocalDataSource.getUserByName(userName)
    }

    override fun allUser(): MutableList<User>? {
        return mLocalDataSource.allUser()
    }

    override fun insertUser(users: User) {
        return mLocalDataSource.insertUser(users)
    }

    override fun deleteUser(user: User) {
        return mLocalDataSource.deleteUser(user)
    }

    override fun deleteAllUser() {
        return mLocalDataSource.deleteAllUser()
    }

    override fun updateUser(user: User) {
        return mLocalDataSource.updateUser(user)
    }
}
package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.dao.UserDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.User

class UserRepository(private val userDao: UserDao) {


    suspend fun insertUser(user: User): Result<Unit> {
        return try {
            userDao.insertUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deleteUserByEmail(email: String) {
        userDao.deleteUserByEmail(email)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }



    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }
    suspend fun loginUser(email: String, password: String): Boolean {
        val user = userDao.getUserByEmail(email)
        return user != null && user.password == password
    }
    suspend fun userExists(email: String): Boolean {
        val user = userDao.getUserByEmail(email)
        return user != null
    }

    
}
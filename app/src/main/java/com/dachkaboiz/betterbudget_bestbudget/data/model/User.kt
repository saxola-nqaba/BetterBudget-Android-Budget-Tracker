package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val email: String,
    val password: String,
    //fullName replaced with firstName and surname for error prevention purposes
    val firstName: String? = null,
    val surname: String? = null,
    //birthDate needs to use DateTime and must use the converter from B_b
    val birthDate: Long? = null,
    val age: Int? = null,
    val profilePicUri: String? = null
)
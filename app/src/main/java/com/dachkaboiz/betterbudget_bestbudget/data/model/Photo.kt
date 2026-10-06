package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [Index(value = ["userEmail"])],
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["email"],
        childColumns = ["userEmail"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class Photo (
    @PrimaryKey(autoGenerate = true)
    val photoID: Int = 0,
    val userEmail: String,
    val imageName: String,
    val imageDescription: String?,
    val imageUri: String,
    val dateCaptured: Long = System.currentTimeMillis()
)

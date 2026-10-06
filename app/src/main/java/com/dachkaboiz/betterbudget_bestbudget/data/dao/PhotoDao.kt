package com.dachkaboiz.betterbudget_bestbudget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.Photo

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo)

    @Delete
    suspend fun deletePhoto(photo: Photo)

    @Query("SELECT * FROM photos WHERE userEmail = :email")
    suspend fun getPhotosByUser(email: String): List<Photo>

    @Query("SELECT * FROM photos WHERE userEmail = :email AND (imageName LIKE '%' || :query || '%' OR imageDescription LIKE '%' || :query || '%')")
    suspend fun searchPhotos(email: String, query: String): List<Photo>

}


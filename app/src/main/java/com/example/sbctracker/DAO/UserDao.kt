package com.example.sbctracker.DAO

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sbctracker.models.Machine
import com.example.sbctracker.models.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT * FROM table_user WHERE id= (SELECT MAX(id) FROM table_user)")
    fun getUserDetails(): LiveData<User>

}
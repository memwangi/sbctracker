package com.example.sbctracker.DAO

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sbctracker.models.LastLocation

@Dao
interface LastLocationDao {

    @Query("SELECT * FROM location_cache")
    fun getLastLocations(): LiveData<List<LastLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(location: LastLocation)

    @Query("SELECT * FROM location_cache WHERE id= (SELECT MAX(id) FROM location_cache)")
    fun getRecentLocation():LiveData<LastLocation>

}
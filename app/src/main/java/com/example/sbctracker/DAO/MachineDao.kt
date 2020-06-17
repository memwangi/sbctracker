package com.example.sbctracker.DAO

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.sbctracker.models.Outlet

@Dao
interface MachineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outlet: Outlet)

    // Get items scanned in the last 24 hours
    @Query("SELECT * FROM table_machines WHERE NOT posted")
    fun getMachines(): LiveData<List<Outlet>>

    // Updated posted items
    @Query("UPDATE table_machines SET posted=:posted WHERE id=:id")
    fun setTrue(id: Long, posted: Boolean = true)
}
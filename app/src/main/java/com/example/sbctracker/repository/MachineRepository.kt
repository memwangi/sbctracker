package com.example.sbctracker.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.sbctracker.DAO.MachineDao
import com.example.sbctracker.models.Outlet


class MachineRepository (private val machineDao: MachineDao){
    val allMachines: LiveData<List<Outlet>> = machineDao.getMachines()

    suspend fun insert(outlet: Outlet) {
        machineDao.insert(outlet)
        Log.i("Inserting machine","Machine Repository, $outlet")
    }

    suspend fun setPosted(id: Long) {
        machineDao.setTrue(id)
        // Updated
    }
}
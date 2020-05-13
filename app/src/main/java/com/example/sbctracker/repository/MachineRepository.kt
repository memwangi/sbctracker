package com.example.sbctracker.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.sbctracker.DAO.MachineDao
import com.example.sbctracker.models.Machine


class MachineRepository (private val machineDao: MachineDao){
    val allMachines: LiveData<List<Machine>> = machineDao.getMachines()

    suspend fun insert(machine: Machine) {
        machineDao.insert(machine)
        Log.i("Inserting machine","Machine Repository, $machine")
    }

    suspend fun setPosted(id: Long) {
        machineDao.setTrue(id)
        // Updated
    }
}
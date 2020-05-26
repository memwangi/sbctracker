package com.example.sbctracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sbctracker.DAO.LastLocationDao
import com.example.sbctracker.DAO.MachineDao
import com.example.sbctracker.DAO.UserDao
import com.example.sbctracker.models.LastLocation
import com.example.sbctracker.models.Machine
import com.example.sbctracker.models.User

@Database(entities = [Machine::class, User::class, LastLocation::class], version = 2, exportSchema = true)
public abstract class SbcTrackerDatabase : RoomDatabase() {

    abstract fun machineDao() : MachineDao
    abstract fun UserDao() : UserDao
    abstract fun lastLocationDao() : LastLocationDao

    companion object {
        // Singleton to prevent multiple instances of the database
        @Volatile
        private var INSTANCE: SbcTrackerDatabase? = null

        fun getDatabase(context: Context): SbcTrackerDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
               val instance = Room.databaseBuilder(
                   context.applicationContext,
                   SbcTrackerDatabase::class.java,
                   "tracking_database"
               ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
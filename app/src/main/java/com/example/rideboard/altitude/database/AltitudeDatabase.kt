package com.example.rideboard.altitude.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.rideboard.altitude.AltitudeEntity

@Database(entities = [AltitudeEntity::class], version = 1, exportSchema = false)
abstract class AltitudeDatabase : RoomDatabase() {
    abstract fun altitudeDao(): AltitudeDao
}
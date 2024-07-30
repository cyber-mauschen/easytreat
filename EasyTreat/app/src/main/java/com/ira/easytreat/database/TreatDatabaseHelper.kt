package com.ira.easytreat.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper

class TreatDatabaseHelper(context:Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "treat_database.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db:SQLiteDatabase) {
        // Create tables here
        val createTableQuery = "CREATE TABLE records (id INTEGER PRIMARY KEY, name TEXT, imagePath TEXT, description TEXT, guidance TEXT, pets INTEGER, kids INTEGER, doping INTEGER)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades here
    }
}
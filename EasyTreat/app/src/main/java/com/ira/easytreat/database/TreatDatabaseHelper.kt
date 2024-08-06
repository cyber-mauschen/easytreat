package com.ira.easytreat.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper

class TreatDatabaseHelper(context:Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "treat_database.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "records"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_IMAGE_PATH = "image_path"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_INGREDIENTS = "ingredients"
        const val COLUMN_ALTERNATIVES = "alternatives"
        const val COLUMN_ALLERGENTS = "allergents"
        const val COLUMN_UPDATED = "updated"
    }

    override fun onCreate(db:SQLiteDatabase) {
        // Create tables here
        val createTableQuery = "CREATE TABLE records (" + COLUMN_ID + " INTEGER PRIMARY KEY, " + COLUMN_NAME + " TEXT, " + COLUMN_IMAGE_PATH + " TEXT, " + COLUMN_DESCRIPTION + " TEXT, " + COLUMN_INGREDIENTS + " TEXT, " + COLUMN_ALTERNATIVES + " TEXT, " + COLUMN_ALLERGENTS + " TEXT, " + COLUMN_UPDATED + " TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades here
    }
}
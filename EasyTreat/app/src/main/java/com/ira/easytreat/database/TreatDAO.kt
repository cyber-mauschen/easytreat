package com.ira.easytreat.database
import android.content.ContentValues
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date

// User data class
data class Record(val id: Int, val name: String, val imagePath: String, var description: String, var ingredients: String?,
                  var alternatives: String?, var allergens: String?, var updated: java.sql.Date?)
class TreatDAO(context: Context) {
    val dbHelper = TreatDatabaseHelper(context)

    fun stringToSqlDate(dateString: String): java.sql.Date? {
        try {
            val format = "yyyy-MM-dd"
            val simpleDateFormat = SimpleDateFormat(format)
            val utilDate = simpleDateFormat.parse(dateString)
            return java.sql.Date(utilDate.time)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun insertRecord(record: Record): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(TreatDatabaseHelper.COLUMN_ID, record.id)
            put(TreatDatabaseHelper.COLUMN_NAME, record.name)
            put(TreatDatabaseHelper.COLUMN_IMAGE_PATH, record.imagePath)
            put(TreatDatabaseHelper.COLUMN_DESCRIPTION, record.description)
            if (record.ingredients != null) {
                put(TreatDatabaseHelper.COLUMN_INGREDIENTS, record.ingredients)
            } else {
                put(TreatDatabaseHelper.COLUMN_INGREDIENTS, "")
            }
            if (record.alternatives != null) {
                put(TreatDatabaseHelper.COLUMN_ALTERNATIVES, record.alternatives)
            } else {
                put(TreatDatabaseHelper.COLUMN_ALTERNATIVES, "")
            }
            if (record.allergens != null) {
                put(TreatDatabaseHelper.COLUMN_ALLERGENTS, record.allergens)
            } else {
                put(TreatDatabaseHelper.COLUMN_ALLERGENTS, "")
            }
            put(TreatDatabaseHelper.COLUMN_UPDATED, java.sql.Date(System.currentTimeMillis()).toString())
        }
        return db.insert(TreatDatabaseHelper.TABLE_NAME, null, values)
    }

    fun updateRecord(record: Record): Any {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(TreatDatabaseHelper.COLUMN_ID, record.id)
            put(TreatDatabaseHelper.COLUMN_NAME, record.name)
            put(TreatDatabaseHelper.COLUMN_IMAGE_PATH, record.imagePath)
            put(TreatDatabaseHelper.COLUMN_DESCRIPTION, record.description)
            put(TreatDatabaseHelper.COLUMN_INGREDIENTS, record.ingredients)
            put(TreatDatabaseHelper.COLUMN_ALTERNATIVES, record.alternatives)
            put(TreatDatabaseHelper.COLUMN_ALLERGENTS, record.allergens)
            put(TreatDatabaseHelper.COLUMN_UPDATED, java.sql.Date(System.currentTimeMillis()).toString())
        }
        return db.update(TreatDatabaseHelper.TABLE_NAME, values, TreatDatabaseHelper.COLUMN_ID + " = ?", arrayOf(record.id.toString()))
    }

    fun deleteRecord(record: Record): Int {
        val db = dbHelper.writableDatabase
        val result = db.delete(TreatDatabaseHelper.TABLE_NAME, TreatDatabaseHelper.COLUMN_ID + " = ?", arrayOf(record.id.toString()))
        print(result)
        return result
    }

    fun getRecords(): ArrayList<Record> {
        val recordList = mutableListOf<Record>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(TreatDatabaseHelper.TABLE_NAME, null, null, null, null, null, TreatDatabaseHelper.COLUMN_UPDATED)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val imagePath = cursor.getString(2)
                val description = cursor.getString(3)
                val ingredients = cursor.getString(4)
                val alternatives = cursor.getString(5)
                val allergens = cursor.getString(6)
                val updated = cursor.getString(7)
                val record = Record(id, name, imagePath, description, ingredients, alternatives, allergens, stringToSqlDate(updated))
                recordList.add(record)
            } while (cursor.moveToNext())
        }
        cursor.close()
        var result = ArrayList<Record>(recordList)
        result.sortWith(compareBy { it.updated })
        return result
    }

    fun getRecord(recordId: Int): Record? {
        val db = dbHelper.readableDatabase
        try {
            val cursor =
                db.query(TreatDatabaseHelper.TABLE_NAME, null, TreatDatabaseHelper.COLUMN_ID + " = ?", arrayOf(recordId.toString()), null, null, null)
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val imagePath = cursor.getString(2)
                val description = cursor.getString(3)
                val ingredients = cursor.getString(4)
                val alternatives = cursor.getString(5)
                val allergens = cursor.getString(6)
                val updated = cursor.getString(7)
                val record = Record(id, name, imagePath, description, ingredients, alternatives, allergens, stringToSqlDate(updated))
                cursor.close()
                return record
            }
            cursor.close()
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
        return null
    }
}
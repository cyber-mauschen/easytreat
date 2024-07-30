package com.ira.easytreat.database
import android.content.ContentValues
import android.content.Context

// User data class
data class Record(val id: Int, val name: String, val imagePath: String, var description: String, var guidance: String?,
                  val pets: Boolean, val kids: Boolean, val doping: Boolean)
class TreatDAO(context: Context) {
    val dbHelper = TreatDatabaseHelper(context)
    fun insertRecord(record: Record): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", record.id)
            put("name", record.name)
            put("imagePath", record.imagePath)
            put("description", record.description)
            put("guidance", record.guidance)
            put("pets", record.pets)
            put("kids", record.kids)
            put("doping", record.doping)
        }
        return db.insert("records", null, values)
    }

    fun updateRecord(record: Record): Any {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", record.id)
            put("name", record.name)
            put("imagePath", record.imagePath)
            put("description", record.description)
            put("guidance", record.guidance)
            put("pets", record.pets)
            put("kids", record.kids)
            put("doping", record.doping)
        }
        return db.update("records", values, "id = ?", arrayOf(record.id.toString()))
    }

    fun deleteRecord(record: Record): Int {
        val db = dbHelper.writableDatabase
        val result = db.delete("records", "id = ?", arrayOf(record.id.toString()))
        print(result)
        return result
    }

    fun getRecords(): ArrayList<Record> {
        val recordList = mutableListOf<Record>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("records", null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val imagePath = cursor.getString(2)
                val description = cursor.getString(3)
                val guidance = cursor.getString(4)
                val pets = cursor.getInt(5) > 0
                val kids = cursor.getInt(6) > 0
                val doping = cursor.getInt(7) > 0
                val record = Record(id, name, imagePath, description, guidance, pets, kids, doping)
                recordList.add(record)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return ArrayList(recordList)
    }

    fun getRecord(recordId: Int): Record? {
        val db = dbHelper.readableDatabase
        try {
            val cursor =
                db.query("records", null, "id = ?", arrayOf(recordId.toString()), null, null, null)
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val imagePath = cursor.getString(2)
                val description = cursor.getString(3)
                val guidance = cursor.getString(4)
                val pets = cursor.getInt(5) > 0
                val kids = cursor.getInt(6) > 0
                val doping = cursor.getInt(7) > 0
                val record = Record(id, name, imagePath, description, guidance, pets, kids, doping)
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
package com.example.autosnap_inz

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast


class SqlLiteHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val DATABASE_NAME = "autosnap_testowy"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "car_collection"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_UID = "user_id"
        private const val COLUMN_MODEL = "car_model"
        private const val COLUMN_IMG = "car_picture"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val query = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_UID TEXT,
                $COLUMN_MODEL TEXT,
                $COLUMN_IMG BLOB
            );
        """.trimIndent()
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.setForeignKeyConstraintsEnabled(true)
        }
    }

    // Funkcja do wstawiania danych
    fun insertData(userId: String, model: String, image: ByteArray) {
        try {
            writableDatabase.use { db ->
                val values = ContentValues().apply {
                    put(COLUMN_UID, userId)
                    put(COLUMN_MODEL, model)
                    put(COLUMN_IMG, image)
                }

                val result = db.insert(TABLE_NAME, null, values)
                if (result != -1L) {
                    Toast.makeText(appContext, "Data inserted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(appContext, "Data insert failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("SqlLiteHelper", "Error inserting data: ${e.message}", e)
        }
    }


//    // Funkcja pobierająca wszystkie dane
//    fun getAllData(): Cursor? {
//        return try {
//            readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", null)
//        } catch (e: Exception) {
//            Log.e("SqlLiteHelper", "Error in getAllData(): ${e.message}", e)
//            null
//        }
//    }
//
//    // Funkcja do usuwania danych po ID
//    fun deleteData(id: Int): Boolean {
//        return try {
//            writableDatabase.use { db ->
//                val result = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
//                result > 0
//            }
//        } catch (e: Exception) {
//            Log.e("SqlLiteHelper", "Error in deleteData(): ${e.message}", e)
//            false
//        }
//    }

    // Funkcja do pobierania danych dla konkretnego użytkownika
    fun readUserData(userId: String): Cursor? {
        return try {
            readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_UID = ?",
                arrayOf(userId)
            )
        } catch (e: Exception) {
            Log.e("SqlLiteHelper", "Error in readUserData(): ${e.message}", e)
            null
        }
    }

    // Funkcja do usuwania danych po userId i modelu
    fun deleteCarByUserAndModel(userId: String, model: String): Boolean {
        return try {
            val db = writableDatabase
            val result = db.delete(
                TABLE_NAME,
                "$COLUMN_UID = ? AND $COLUMN_MODEL = ?",
                arrayOf(userId, model)
            )
            db.close()
            result > 0
        } catch (e: Exception) {
            Log.e("SqlLiteHelper", "Error in deleteCarByUserAndModel(): ${e.message}", e)
            false
        }
    }

}

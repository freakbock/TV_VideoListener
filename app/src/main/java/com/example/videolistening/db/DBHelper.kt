import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "VideoDatabase.db"
        private const val TABLE_NAME = "videos"
        private const val COLUMN_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "_id INTEGER PRIMARY KEY," +
                "$COLUMN_NAME TEXT)"
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertVideoName(videoName: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, videoName)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun deleteVideoName(videoName: String): Boolean {
        val db = this.writableDatabase
        val selection = "$COLUMN_NAME = ?"
        val selectionArgs = arrayOf(videoName)
        val deletedRows = db.delete(TABLE_NAME, selection, selectionArgs)
        db.close()
        return deletedRows > 0
    }

    fun getAllVideoNames(): List<String> {
        val videoNames = mutableListOf<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(COLUMN_NAME)
                if (columnIndex != -1) {
                    do {
                        val videoName = cursor.getString(columnIndex)
                        videoNames.add(videoName)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db.close()
        }
        return videoNames
    }


}

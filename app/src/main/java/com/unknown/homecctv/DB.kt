package com.unknown.homecctv

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log

object DB {
    private const val TAG = "DB"
    private const val dbTableName = "cctv"

    private lateinit var mContext: Context
    private val db: SQLiteDatabase by lazy { createDB() }

    private fun createDB(): SQLiteDatabase {
        val db = mContext.openOrCreateDatabase(C.DB_NAME, Context.MODE_PRIVATE, null)
        val sql = "create table if not exists $dbTableName (" +
                "num integer primary key autoincrement, " +
                "name text not null, " +
                "id text not null, " +
                "pw text, " +
                "isSaveChecked integer" +
                ");"
        db.execSQL(sql)
        return db
    }

    fun getCCTVItems(context: Context): ArrayList<CCTVItem> {
        mContext = context
        val arrCCTVItems = arrayListOf<CCTVItem>()

        val sql = "select * from $dbTableName"
        val cursor = db.rawQuery(sql, null)
        Log.e(TAG, "cursor position:${cursor.position}")
        if (cursor.count > 0){
            while (cursor.moveToNext()){
                arrCCTVItems.add(CCTVItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4)))
            }
        }
        cursor.close()

        return arrCCTVItems
    }

    fun addCCTVItem(context: Context, name: String, id: String): ArrayList<CCTVItem> {
        val contentValues = ContentValues()
        contentValues.put("name", name)
        contentValues.put("id", id)
        contentValues.put("pw", "")
        contentValues.put("isSaveChecked", 0)
        db.insert(dbTableName, null, contentValues)
        Log.e(TAG, "$name inserted..")

        return getCCTVItems(context)
    }

    fun editCCTVItem(context: Context, num: Int, name: String, id: String, pw: String, isSaveChecked: Int): ArrayList<CCTVItem> {
        val contentValues = ContentValues()
        contentValues.put("name", name)
        contentValues.put("id", id)
        contentValues.put("pw", pw)
        contentValues.put("isSaveChecked", isSaveChecked)
        db.update(dbTableName, contentValues, "num=?", arrayOf("$num"))
        Log.e(TAG, "$name updated..")

        return getCCTVItems(context)
    }

    fun deleteCCTVItem(context: Context, num: Int): ArrayList<CCTVItem> {
        db.delete(dbTableName, "num=?", arrayOf("$num"))
        Log.e(TAG, "$num deleted..")

        return getCCTVItems(context)
    }
}
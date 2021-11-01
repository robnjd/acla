package com.example.acla.backend

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log.d
import android.widget.Toast
import androidx.annotation.RequiresApi

class DatabaseHelper(val context: Context) : SQLiteOpenHelper(context, "aclaDB", null, 1) {
    val TAG = "DatabaseHelper"
    val mapColumns = mapOf("sessions" to listOf("id", "date", "workout", "start", "finish", "duration", "measure"))
    val dataTypes = mapOf("id" to "INT PRIMARY KEY")

    override fun onCreate(db: SQLiteDatabase?) {
        for(table in mapColumns.keys) {
            createTable(table, db)
        }
    }

    fun createTable(tablename: String, db: SQLiteDatabase?) {
        val lstColumns = mapColumns[tablename] ?: return
        var strColumns = ""
        for((c, col) in lstColumns.withIndex()) {
            strColumns += "$col ${dataTypes[col]?:"TEXT"}" + if(c != lstColumns.lastIndex) ", " else ""
        }
        val query = "CREATE TABLE ${tablename} ($strColumns)"
        db?.execSQL(query)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    fun <T> insertData(table: String, data: T) {
        val database = this.writableDatabase
        val contentValues = ContentValues()
        val mapData = when (table) {
            "sessions"   -> (data as Session).toMap()
            else        -> mutableMapOf()
        }
        d(TAG, "Insert: $mapData")
        for (field in mapColumns[table]!!) {
            contentValues.put(field, mapData[field]?:"")
        }

        val result = database.insert(table, null, contentValues)
        if (result == (0).toLong()) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun readSessions(where: String?=null) : MutableList<Session> {
        val lstSessions = mutableListOf<Session>()
        val db = this.readableDatabase

        val clause = if (where == null) "" else "WHERE $where"
        val query = "SELECT * FROM sessions $clause"
        val result = try {
            db.rawQuery(query, null)
        } catch (e: Exception) {
            createTable("sessions", db)
            db.rawQuery(query, null)
        }

        if (result.moveToFirst()) {
            do {
                val mapSesh = mutableMapOf<String, String>()
                for (col in mapColumns["sessions"]!!) {
                    val i = result.getColumnIndex(col)
                    mapSesh[col] = result.getString(i)
                }
                d(TAG, "Read: $mapSesh")
                if(mapSesh["date"]?:""!="") {
                    lstSessions.add(Session(mapSesh))
                }
            } while (result.moveToNext())
        }

        return lstSessions
    }

    fun updateSesion(original: Session, new: Session) : Boolean {
        val db = this.writableDatabase
        val mapOri = original.toMap()
        val mapNew = new.toMap()

        var set = "SET "
        for(k in mapOri.keys) {
            if(k == "index") continue
            if(mapOri[k] != mapNew[k]) {
                set += "$k = '${mapNew[k]}', "
            }
        }
        if(set == "SET ") return false
        set = set.substring(0, set.lastIndex-1)

        val query = "UPDATE sessions SET $set WHERE index = ${original.id}"

        val result = db.rawQuery(query, null)
        return result.moveToFirst()
    }

    fun deleteSession(sesh: Session) : Boolean {
        val db = this.writableDatabase
        val query = "DELETE sessions WHERE index = ${sesh.id}"
        val result = db.rawQuery(query, null)
        return result.moveToFirst()
    }

}
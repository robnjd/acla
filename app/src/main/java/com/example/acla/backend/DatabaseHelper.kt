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
    val mapColumns = mapOf( "sessions"      to listOf("id", "date", "workout", "start", "finish", "duration", "measure"),
                            "preferences"   to listOf("pref", "value"))
    val dataTypes = mapOf("id" to "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "pref" to "TEXT PRIMARY KEY")

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
            "sessions"      -> (data as Session).toMap()
            "preferences"   -> data as MutableMap<String, String>
            else            -> mutableMapOf()
        }
        d(TAG, "Insert: $mapData")
        for (field in mapColumns[table]!!) {
            contentValues.put(field, if(field=="id") null else mapData[field]?:"")
        }

        val result = database.insert(table, null, contentValues)
        if (result == (0).toLong()) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun readSessions(where: String?=null, order: String?="date DESC") : MutableList<Session> {
        val lstSessions = mutableListOf<Session>()
        val db = this.readableDatabase

        val clause = if (where == null) "" else "WHERE $where"
        val sort = if(order == null) "" else "ORDER BY $order"
        val query = "SELECT * FROM sessions $clause $sort"
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

    fun readPrefs(where: String?=null) : MutableMap<String, String> {
        val mapPrefs = mutableMapOf<String, String>()
        val db = this.readableDatabase

        val clause = if (where == null) "" else "WHERE $where"
        val query = "SELECT * FROM preferences $clause"
        val result = try {
            db.rawQuery(query, null)
        } catch (e: Exception) {
            createTable("preferences", db)
            db.rawQuery(query, null)
        }

        if (result.moveToFirst()) {
            do {
                mapPrefs[result.getString(0)] = result.getString(1)
            } while (result.moveToNext())
        }

        return mapPrefs
    }

    fun updatePrefs(mapPrefs: MutableMap<String, String>) {
        val db = this.writableDatabase
        d(TAG, "update prefs: $mapPrefs")

        for((pref, default) in mapPrefs) {
            val query = "UPDATE preferences SET value = '$default' WHERE pref = '$pref'"
            d(TAG, query)
            val result = db.rawQuery(query, null)
            if(!result.moveToFirst()) {
                val contentValues = ContentValues()
                contentValues.put("pref", pref)
                contentValues.put("value", default)
                db.insert("preferences", null, contentValues)
            } else {
                result.moveToFirst()
                d(TAG, result.toString())
            }
        }
    }

    fun updateSesion(original: Session, new: Session) : Boolean {
        val db = this.writableDatabase
        val mapOri = original.toMap()
        val mapNew = new.toMap()

        var set = ""
        for(k in mapOri.keys) {
            if(k == "id") continue
            if(mapOri[k] != mapNew[k]) {
                set += "$k = '${mapNew[k]}', "
            }
        }
        if(set == "") return false
        set = set.substring(0, set.lastIndex-1)

        val query = "UPDATE sessions SET $set WHERE id = ${original.id}"

        val result = db.rawQuery(query, null)
        return result.moveToFirst()
    }

    fun deleteSession(sesh: Session) : Boolean {
        val db = this.writableDatabase
        val query = "DELETE FROM sessions WHERE id = ${sesh.id}"
        val result = db.rawQuery(query, null)
        return result.moveToFirst()
    }

}
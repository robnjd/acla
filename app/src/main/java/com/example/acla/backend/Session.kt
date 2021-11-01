package com.example.acla.backend

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit
import javax.xml.datatype.DatatypeConstants.SECONDS

class Session(val load: MutableMap<String, String>?=null) {

    var id: Int? = null
    lateinit var date: LocalDate
    lateinit var workout: String
    lateinit var start: LocalTime
    lateinit var finish: LocalTime
    lateinit var duration: String
    lateinit var measure: String

    var seconds = 0
    var sets = 0
    var intervals = 0
    var distance = 0f
    var reps = 0
    var vars = 0

    val ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val hms = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        if(!load.isNullOrEmpty()) {
            id = load["index"]?.toInt()
            date = LocalDate.parse(load["date"], ymd)
            workout = load["workout"] ?: ""
            start = LocalTime.parse(load["start"], hms)
            finish = LocalTime.parse(load["finish"], hms)
            duration = load["duration"] ?: ""
            seconds = secondize()
            measure = load["measure"] ?: ""

            val spMeasure = measure.split(" X ")

            when(workout) {
                "Interval"  ->{ sets = spMeasure[0].toInt()
                                intervals = spMeasure[1].toInt() }

                "Run"       -> distance = measure.toFloat()

                "Routine"   ->{ reps = spMeasure[0].toInt()
                                sets = spMeasure[1].toInt()
                                vars = spMeasure[2].toInt() }
            }
        }
    }

    fun toMap() : MutableMap<String, String> {
        val out = mutableMapOf<String, String>()
        out["id"] = id?.toString() ?: "NULL"
        out["date"] = date.format(ymd)
        out["workout"] = workout
        out["start"] = start.format(hms)
        out["finish"] = finish.format(hms)
        out["duration"] = duration
        out["measure"] = measure
        return out
    }

    override fun toString() : String {
        return toMap().toString()
    }

    fun metricCount() : Float {
        when(workout) {
            "Interval"  ->{ val sp = measure.split(" X ")
                            val time = sp[1].split(":")
                            val mins = (time[0].toFloat()*60 + time[1].toFloat())/60
                            return sp[0].toFloat()*mins }

            "Run"       ->  return measure.toFloat()

            "Routine"   ->{ val sp = measure.split(" X ")
                            return sp[0].toFloat()*sp[1].toFloat()*sp[2].toFloat() }
        }
        return 0f
    }

    fun showMeasure() : String {
        return measure + if(workout == "Run") " km" else ""
    }

    fun secondize() : Int {
        val sp = duration.split(":")
        var sec = 0
        for((i, s) in sp.withIndex()) {
            sec += s.toInt()*(i - 2)*60
        }
        return sec
    }

    fun timeFormat(seconds: Float) : String {

        val calcH = Math.floor((seconds / 3600).toDouble())
        val calcM = Math.floor((seconds - calcH * 3600) / 60)
        val calcS = seconds - calcH*3600 - calcM*60

        val H = calcH.toString().split(".")[0].padStart(2, '0')
        val M = calcM.toString().split(".")[0].padStart(2, '0')
        val S = calcS.toString().split(".")[0].padStart(2, '0')

        return "$H:$M:$S"
    }


}


class Agg(val workout: String) {
    var count = 0
    var duration = 0
    var measure = 0f

    fun addSession(sesh: Session) {
        duration += sesh.seconds
        if(workout != "All") measure += sesh.metricCount()
    }
}
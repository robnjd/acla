package com.example.acla.ui.dashboard

import android.os.Bundle
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_graph.view.*
import kotlinx.android.synthetic.main.frag_time.view.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

class TimePager() : Fragment() {
    val TAG = "TimePager"
    lateinit var frag: View
    lateinit var cht: ChartHelper
    lateinit var com: CommonRoom
    lateinit var db: DatabaseHelper

    val vmDash: DashViewModel by viewModels({requireParentFragment()})
    val workoutColours = mapOf("All" to R.color.orange, "Interval" to R.color.blue, "Run" to R.color.green, "Routine" to R.color.purple)
    val dayColours = mapOf( "Monday" to R.color.red, "Tuesday" to R.color.orange, "Wednesday" to R.color.gold,
                            "Thursday" to R.color.green, "Friday" to R.color.teal, "Saturday" to R.color.blue, "Sunday" to R.color.purple )

    val lstMetrics = listOf("Sessions", "Duration", "Measures")
    val dayStats = mutableMapOf<String, MutableMap<String, Agg>>()
    val timeStats = mutableMapOf<String, MutableList<Agg>>()



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmDash.startDate.observe(viewLifecycleOwner, { start -> getSessions(start) } )
        return inflater.inflate(R.layout.frag_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        cht = ChartHelper(requireContext())
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())

        var workout = "All"
        var metric = "Sessions"

        for(w in workoutColours.keys) {
            frag.findViewById<ImageView>(com.viewId("tme${w}Button")).setOnClickListener {
                workout = pickWorkout(w)
                populateGraphs(workout, metric)
                frag.tmeMeasuresButton.visibility = if(workout=="All") View.GONE else View.VISIBLE
            }
        }
        for(m in lstMetrics) {
            frag.findViewById<ImageView>(com.viewId("tme${m}Button")).setOnClickListener {
                metric = pickMetric(m)
                populateGraphs(workout, metric)
            }
        }
    }

    fun getSessions(start: LocalDate) {
        val end = vmDash.endDate.value!!

        for(workout in workoutColours.keys) {
            dayStats[workout] = mutableMapOf()
            timeStats[workout] = mutableListOf()
            dayColours.keys.forEach { dayStats[workout]!![it] = Agg(workout) }
            (0..23).forEach { timeStats[workout]!!.add(Agg(workout)) }
        }
        for(sesh in db.readSessions("date BETWEEN '${start.format(com.ymdFormatter)}' AND '${end.format(com.ymdFormatter)}'")) {
            val d = sesh.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val h = sesh.start.hour
            dayStats[sesh.workout]!![d]!!.addSession(sesh)
            timeStats[sesh.workout]!![h].addSession(sesh)
            dayStats["All"]!![d]!!.addSession(sesh)
            timeStats["All"]!![h].addSession(sesh)
        }

        populateGraphs()
    }

    fun populateGraphs(workout: String="All", metric: String="Sessions") {
        val period = vmDash.period.value ?: "Monthly"

        val mapDayGraph = mutableMapOf<String, Float>()
        val mapTimeGraph = mutableMapOf<String, MutableMap<String, Float>>()

        for((d, agg) in dayStats[workout]!!) {
            val stat = when(metric) {
                "Sessions"  -> agg.count
                "Duration"  -> agg.duration.div(if(period=="Weekly") 60 else 360)
                "Measures"  -> agg.measure
                else        -> 0f
            }.toFloat()
            mapDayGraph[d] = stat
        }
        for((h, agg) in timeStats[workout]!!.withIndex()) {
            val stat = when(metric) {
                "Sessions"  -> agg.count
                "Duration"  -> agg.duration.div(if(period=="Weekly") 60 else 360)
                "Measures"  -> agg.measure
                else        -> 0f
            }.toFloat()

            val hr = if(h == 0) 12 else if(h < 13) h else h - 12
            val meridian = if(h < 13) "AM" else "PM"
            val mul = if(h < 13) -1 else 1

            if("$hr" !in mapTimeGraph.keys) mapTimeGraph["$hr"] = mutableMapOf("AM" to 0f, "PM" to 0f)
            mapTimeGraph["$hr"]!![meridian] = mul*stat
        }

        val meridianColours = mapOf("AM" to workoutColours[workout]!!, "PM" to workoutColours[workout]!!)

        cht.buildPieChart(frag.tmePieChart, mapDayGraph, dayColours)
        cht.buildHorizontalChart(frag.tmeTimeChart, mapTimeGraph, meridianColours)

        frag.tmeGraphMetric.text = when(metric) {
            "Sessions"  -> ""
            "Duration"  -> if(period=="Weekly") "mins" else "hrs"
            "Measures"  -> when(workout) {
                "Interval"  -> "x1:00"
                "Run"       -> "km"
                "Routine"   -> "reps"
                else        -> ""
            }
            else        -> ""
        }
    }

    fun pickWorkout(workout: String) : String {
        for((w, col) in workoutColours) {
            val colour = if(w == workout) col else R.color.greyLight
            com.tint(frag.findViewById<ImageView>(com.viewId("tme${w}Button")), colour, "fg")
        }
        return workout
    }

    fun pickMetric(metric: String) : String {
        for(m in lstMetrics) {
            val colour = if(m == metric) R.color.orange else R.color.greyLight
            com.tint(frag.findViewById<ImageView>(com.viewId("tme${m}Button")), colour, "fg")
        }
        return metric
    }
}
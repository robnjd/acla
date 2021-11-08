package com.example.acla.ui.dashboard

import android.os.Bundle
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_graph.view.*
import java.time.LocalDate

class GraphPager(val position: Int) : Fragment() {
    val TAG = "GraphPager"
    lateinit var frag: View
    lateinit var cht: ChartHelper
    lateinit var com: CommonRoom
    lateinit var db: DatabaseHelper
    val vmDash: DashViewModel by viewModels({requireParentFragment()})
    val workoutColours = mapOf("All" to R.color.orange, "Interval" to R.color.blue, "Run" to R.color.green, "Routine" to R.color.purple)
    val lstMetrics = listOf("Sessions", "Duration", "Measures")

    val stats = mutableMapOf<String, MutableMap<LocalDate, Agg>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmDash.startDate.observe(viewLifecycleOwner, { start -> getSessions(start) } )
        return inflater.inflate(R.layout.frag_graph, container, false)
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
            frag.findViewById<ImageView>(com.viewId("grf${w}Button")).setOnClickListener {
                workout = pickWorkout(w)
                populateGraph(workout, metric)
                frag.grfMeasuresButton.visibility = if(workout=="All") View.GONE else View.VISIBLE
            }
        }
        for(m in lstMetrics) {
            frag.findViewById<ImageView>(com.viewId("grf${m}Button")).setOnClickListener {
                metric = pickMetric(m)
                populateGraph(workout, metric)
            }
        }
    }

    fun getSessions(start: LocalDate) {
        val period = vmDash.period.value ?: "Monthly"
        val backdate = com.periodBucket(when(period) {
            "Weekly"    -> start.minusYears(1)
            "Monthly"   -> start.minusYears(2)
            else        -> LocalDate.parse("2017-01-01", com.ymdFormatter)
        }, period)!!
        val end = vmDash.endDate.value!!
        val span =  com.spanDates(backdate, end, period)

        for(workout in workoutColours.keys) {
            stats[workout] = mutableMapOf()
            span.forEach {
                stats[workout]!![it] = Agg(workout)
            }
        }
        for(sesh in db.readSessions("date BETWEEN '${backdate.format(com.ymdFormatter)}' AND '${end.format(com.ymdFormatter)}'")) {
            val p = com.periodBucket(sesh.date, period) ?: continue
            stats[sesh.workout]!![p]!!.addSession(sesh)
            stats["All"]!![p]!!.addSession(sesh)
        }

        populateGraph()
    }

    fun populateGraph(workout: String="All", metric: String="Sessions") {
        val mapGraph = mutableMapOf<String, Map<String, Float>>()
        val period = vmDash.period.value ?: "Monthly"

        for((d, agg) in stats[workout]!!) {
            val stat = when(metric) {
                "Sessions"  -> agg.count
                "Duration"  -> agg.duration.div(if(period=="Weekly") 60 else 360)
                "Measures"  -> agg.measure
                else        -> 0f
            }.toFloat()

            mapGraph[d.format(com.dmyFormatter)] = mapOf(workout to stat)
        }

        cht.buildLineChart(frag.grfLineChart, mapGraph, workoutColours, period)

        frag.grfGraphMetric.text = when(metric) {
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
            com.tint(frag.findViewById<ImageView>(com.viewId("grf${w}Button")), colour, "fg")
        }
        return workout
    }

    fun pickMetric(metric: String) : String {
        for(m in lstMetrics) {
            val colour = if(m == metric) R.color.orange else R.color.greyLight
            com.tint(frag.findViewById<ImageView>(com.viewId("grf${m}Button")), colour, "fg")
        }
        return metric
    }


}
package com.example.acla.ui.dashboard

import android.os.Bundle
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
import com.example.acla.DashViewModel
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_graph.view.*
import java.lang.Math.floor
import java.time.LocalDate
import kotlin.math.ceil

class TablePager() : Fragment() {

    lateinit var frag: View
    lateinit var cht: ChartHelper
    lateinit var com: CommonRoom
    lateinit var db: DatabaseHelper
    val vmDash: DashViewModel by viewModels({requireParentFragment()})
    val workoutColours = mapOf("All" to R.color.orange, "Interval" to R.color.blue, "Run" to R.color.green, "Routine" to R.color.purple)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmDash.stats.observe(viewLifecycleOwner, Observer {
            stats -> populateTable()
        } )

        return inflater.inflate(R.layout.frag_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        cht = ChartHelper(requireContext())
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())

        val allSessions = db.readSessions()
    }

    fun periodStats(sessions: MutableList<Session>, period: String) {

        val lstWorkouts = listOf("All", "Interval", "Run", "Routine")
        val aggPeriod = mutableMapOf<String, MutableMap<LocalDate, Agg>>()
        val current = mutableMapOf<String, MutableMap<String, Float>>()
        val lstMedians = mutableMapOf<String, MutableMap<String, MutableList<Float>>>()
        val medians = mutableMapOf<String, MutableMap<String, Float>>()

        for(workout in lstWorkouts) {
            aggPeriod[workout] = mutableMapOf()
            current[workout] = mutableMapOf("count" to 0f, "duration" to 0f, "measure" to 0f)
            lstMedians[workout] = mutableMapOf("count" to mutableListOf(), "duration" to mutableListOf(), "measure" to mutableListOf())
            medians[workout] = mutableMapOf("count" to 0f, "duration" to 0f, "measure" to 0f)
        }

        for(sesh in sessions) {
            val p = com.periodBucket(sesh.date, period) ?: continue
            if(p !in aggPeriod[sesh.workout]!!.keys) aggPeriod[sesh.workout]!![p] = Agg(sesh.workout)
            if(p !in aggPeriod["All"]!!.keys) aggPeriod["All"]!![p] = Agg("All")

            aggPeriod[sesh.workout]!![p]!!.addSession(sesh)
            aggPeriod["All"]!![p]!!.addSession(sesh)
        }

        for((workout, mapPeriod) in aggPeriod) {
            for((p, agg) in mapPeriod) {
                if(p == vmDash.searchFilter.value!!.startDate) {
                    lstMedians[workout]!!["count"]!!.plusAssign(agg.count.toFloat())
                    lstMedians[workout]!!["duration"]!!.plusAssign(agg.duration.toFloat())
                    lstMedians[workout]!!["measure"]!!.plusAssign(agg.measure)
                } else {
                    lstMedians[workout]!!["count"]!!.add(agg.count.toFloat())
                    lstMedians[workout]!!["duration"]!!.add(agg.duration.toFloat())
                    lstMedians[workout]!!["measure"]!!.add(agg.measure)
                }
            }
        }

        for((workout, mapAtts) in lstMedians) {
            for((att, lst) in mapAtts) {
                lst.sort()
                medians[workout]!![att] = if(lst.size % 2 == 0) {
                    ( lst[(lst.size/2)] + lst[(lst.size/2) + 1] )/2
                } else {
                    lst[lst.size/2]
                }
            }
        }

        val statRows = mutableListOf<MutableMap<String, String>>()
        for(workout in lstWorkouts) {
            val wmap = mutableMapOf<String, String>()
            wmap["workout"] = workout
            for(att in current[workout]!!.keys) {
                wmap["att-current"] = current[workout]!![att]!!.toString()
                wmap["att-delta"] = ((current[workout]!![att]!! - medians[workout]!![att]!!) / medians[workout]!![att]!!).toString()
            }
            statRows.add(wmap)
        }

    }


}
package com.example.acla.ui.dashboard

import android.os.Bundle
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_table.view.*
import java.time.LocalDate
import kotlin.math.abs

class TablePager() : Fragment() {
    val TAG = "TablePager"
    lateinit var frag: View
    lateinit var com: CommonRoom
    lateinit var db: DatabaseHelper
    val vmDash: DashViewModel by viewModels(ownerProducer={requireParentFragment()})

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmDash.startDate.observe(viewLifecycleOwner){ start -> fillTable(start) }
        return inflater.inflate(R.layout.frag_table, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())
    }

    fun fillTable(start: LocalDate) {
        val allSessions = db.readSessions()

        val period = vmDash.period.value

        frag.tabWeekTable.adapter = if(period == "Weekly") periodStats(allSessions, "Weekly") else averageStats(allSessions, "Weekly")
        frag.tabMonthTable.adapter = if(period == "Monthly") periodStats(allSessions, "Monthly") else averageStats(allSessions, "Monthly")
        frag.tabYearTable.adapter = periodStats(allSessions, "Yearly")

        frag.tabWeekTable.isExpanded = true
        frag.tabMonthTable.isExpanded = true
        frag.tabYearTable.isExpanded = true

        frag.tabWeekTitle.text = if(period=="Weekly") periodTitle(start, "Weekly") else "Average Week"
        frag.tabMonthTitle.text = if(period=="Yearly") "Average Month" else periodTitle(start, "Monthly")
        frag.tabYearTitle.text = periodTitle(start, "Yearly")
    }

    fun periodTitle(date: LocalDate, period: String) : String {
        val bucket = com.periodBucket(date, period)!!
        val today = LocalDate.now()

        if(bucket == com.periodBucket(today, period)) {
            return if(period == "Yearly") "Year To Date" else "Current ${period.replace("ly", "")}"
        }

        return when(period) {
            "Weekly"    -> "Week of ${bucket.format(com.dmonFormatter)} ${if(bucket.year != today.year) bucket.year else ""}"
            "Monthly"   -> "${bucket.format(com.monthFormatter)} ${if(bucket.year != today.year) bucket.year else ""}"
            "Yearly"    -> "${bucket.year}"
            else        -> ""
        }


    }

    fun periodStats(sessions: MutableList<Session>, period: String) : TableAdapter {

        val lstWorkouts = listOf("Interval", "Run", "Routine")
        val aggPeriod = mutableMapOf<String, MutableMap<LocalDate, Agg>>()
        val current = mutableMapOf<String, MutableMap<String, Float>>()
        val lstMedians = mutableMapOf<String, MutableMap<String, MutableList<Float>>>()
        val medians = mutableMapOf<String, MutableMap<String, Float>>()

        val ytd = vmDash.period.value!! == "Yearly" && vmDash.startDate.value!!.year == LocalDate.now().year

        for(workout in lstWorkouts) {
            aggPeriod[workout] = mutableMapOf()
            current[workout] = mutableMapOf("Sessions" to 0f, "Duration" to 0f, "Measures" to 0f)
            lstMedians[workout] = mutableMapOf("Sessions" to mutableListOf(), "Duration" to mutableListOf(), "Measures" to mutableListOf())
            medians[workout] = mutableMapOf("Sessions" to 0f, "Duration" to 0f, "Measures" to 0f)
        }

        for(sesh in sessions) {
            val p = com.periodBucket(sesh.date, period) ?: continue

            if(ytd && p.month.value > LocalDate.now().month.value) continue

            if(p !in aggPeriod[sesh.workout]!!.keys) aggPeriod[sesh.workout]!![p] = Agg(sesh.workout)

            aggPeriod[sesh.workout]!![p] = aggPeriod[sesh.workout]!![p]!!.addSession(sesh)
        }

        val periodStart = com.periodBucket(vmDash.startDate.value!!, period)

        for((workout, mapPeriod) in aggPeriod) {
            for((p, agg) in mapPeriod) {
                if(p == periodStart) {
                    current[workout]!!["Sessions"] = current[workout]!!["Sessions"]!! + agg.count.toFloat()
                    current[workout]!!["Duration"] = current[workout]!!["Duration"]!! + agg.duration.toFloat()
                    current[workout]!!["Measures"] = current[workout]!!["Measures"]!! + agg.measure
                } else {
                    lstMedians[workout]!!["Sessions"]!!.add(agg.count.toFloat())
                    lstMedians[workout]!!["Duration"]!!.add(agg.duration.toFloat())
                    lstMedians[workout]!!["Measures"]!!.add(agg.measure)
                }
            }
        }

        for((workout, mapAtts) in lstMedians) {
            for((att, lst) in mapAtts) {
                if(lst.isNullOrEmpty()) continue
                lst.sort()
                medians[workout]!![att] = if(lst.size % 2 == 0) {
                    ( lst[(lst.size/2)-1] + lst[(lst.size/2)] )/2
                } else {
                    lst[lst.size/2]
                }
            }
        }

        val statRows = mutableListOf<MutableMap<String, String>>()
        for(workout in lstWorkouts) {
            val wmap = mutableMapOf<String, String>()
            wmap["workout"] = workout
            for((att, v) in current[workout]!!) {
                wmap["${att}-current"] = v.toString()
                val delta = (v - medians[workout]!![att]!!) / medians[workout]!![att]!!
                wmap["${att}-delta"] = abs(delta).toString()
                wmap["${att}-direction"] = if(delta > 0.001) "+" else if(delta < -0.001) "-" else "0"
            }
            if(wmap["Sessions-current"]?.toFloat() != 0f) statRows.add(wmap)

        }

        return TableAdapter(requireContext(), statRows, "dashboard")

    }

    fun averageStats(sessions: MutableList<Session>, period: String)  : TableAdapter  {
        val lstWorkouts = listOf("Interval", "Run", "Routine")
        val aggPeriod = mutableMapOf<String, MutableMap<LocalDate, Agg>>()
        val average = mutableMapOf<String, MutableMap<String, Float>>()
        val lstMedians = mutableMapOf<String, MutableMap<String, MutableList<Float>>>()
        val medians = mutableMapOf<String, MutableMap<String, Float>>()

        for(workout in lstWorkouts) {
            aggPeriod[workout] = mutableMapOf()
            average[workout] = mutableMapOf("Sessions" to 0f, "Duration" to 0f, "Measures" to 0f)
            lstMedians[workout] = mutableMapOf("Sessions" to mutableListOf(), "Duration" to mutableListOf(), "Measures" to mutableListOf())
            medians[workout] = mutableMapOf("Sessions" to 0f, "Duration" to 0f, "Measures" to 0f)
        }

        for(sesh in sessions) {
            val p = com.periodBucket(sesh.date, period) ?: continue

            if(p !in aggPeriod[sesh.workout]!!.keys) aggPeriod[sesh.workout]!![p] = Agg(sesh.workout)

            aggPeriod[sesh.workout]!![p] = aggPeriod[sesh.workout]!![p]!!.addSession(sesh)
        }

        val periodStart = com.periodBucket(vmDash.startDate.value!!, period)
        val periodEnd = com.periodBucket(vmDash.endDate.value!!, period)


        for((workout, mapPeriod) in aggPeriod) {
            for((p, agg) in mapPeriod) {
                if(p >= periodStart && p <= periodEnd) {
                    average[workout]!!["Sessions"] = average[workout]!!["Sessions"]!! + agg.count.toFloat()
                    average[workout]!!["Duration"] = average[workout]!!["Duration"]!! + agg.duration.toFloat()
                    average[workout]!!["Measures"] = average[workout]!!["Measures"]!! + agg.measure
                } else {
                    lstMedians[workout]!!["Sessions"]!!.add(agg.count.toFloat())
                    lstMedians[workout]!!["Duration"]!!.add(agg.duration.toFloat())
                    lstMedians[workout]!!["Measures"]!!.add(agg.measure)
                }
            }
        }

        for((workout, mapAtts) in lstMedians) {
            for((att, lst) in mapAtts) {
                if(lst.isNullOrEmpty()) continue
                lst.sort()
                medians[workout]!![att] = if(lst.size % 2 == 0) {
                    ( lst[(lst.size/2)-1] + lst[(lst.size/2)] )/2
                } else {
                    lst[lst.size/2]
                }
            }
        }

        val div = averageDivisor(period)

        val statRows = mutableListOf<MutableMap<String, String>>()
        for(workout in lstWorkouts) {
            val wmap = mutableMapOf<String, String>()
            wmap["workout"] = workout
            for((att, v) in average[workout]!!) {
                wmap["${att}-current"] = (v/div).toString()
                val delta = (v/div - medians[workout]!![att]!!) / medians[workout]!![att]!!
                wmap["${att}-delta"] = abs(delta).toString()
                wmap["${att}-direction"] = if(delta > 0.001) "+" else if(delta < -0.001) "-" else "0"
            }
            if(wmap["Sessions-current"]?.toFloat() != 0f) statRows.add(wmap)
        }

        return TableAdapter(requireContext(), statRows, "dashboard")

    }

    private fun averageDivisor(period: String) : Float {
        val today = LocalDate.now()
        val start = vmDash.startDate.value!!
        return when(period) {
            "Weekly"    -> if(start.year == today.year && start.month == today.month &&
                              start.dayOfMonth >= today.minusDays(today.dayOfWeek.value.toLong()).dayOfMonth &&
                              start.dayOfMonth < today.plusDays((8-start.dayOfWeek.value).toLong()).dayOfMonth){
                                (if(vmDash.period.value == "Yearly") start.dayOfYear else start.dayOfMonth)/7f
                            } else {
                                val end = start.withDayOfMonth(start.lengthOfMonth())
                                (if(vmDash.period.value == "Yearly") end.dayOfYear else end.dayOfMonth)/7f
                            }
            "Monthly"   -> if(start.year == today.year && start.monthValue == today.monthValue){
                                (start.monthValue - 1 + start.dayOfMonth/start.lengthOfMonth()).toFloat()
                            } else {
                                start.monthValue.toFloat()
                            }
            else        -> 1f
        }

    }


}
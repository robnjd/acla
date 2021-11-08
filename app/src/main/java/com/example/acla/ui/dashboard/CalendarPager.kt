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
import kotlinx.android.synthetic.main.frag_calendar.view.*
import java.time.LocalDate

class CalendarPager() : Fragment() {
    val TAG = "CalendarPager"
    lateinit var frag: View
    lateinit var com: CommonRoom
    lateinit var db: DatabaseHelper
    val vmDash: DashViewModel by viewModels(ownerProducer={requireParentFragment()})

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmDash.startDate.observe(viewLifecycleOwner){ start -> newDate(start) }
        return inflater.inflate(R.layout.frag_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())
    }

    fun newDate(start: LocalDate) {
        val period = vmDash.period.value!!

        frag.calWeekTitle.text = if(period=="Weekly") "Week of ${start.format(com.dmonFormatter)}" else "Cumulative Week"
        frag.calMonthTitle.text = if(period=="Yearly") "Cumulative Month" else start.format(com.monthyrFormatter)
        frag.calYearTitle.text = start.year.toString()

        frag.calMonthHeader.visibility = if(period == "Yearly") View.GONE else View.VISIBLE

        frag.calWeekTable.adapter = fillCalendar(start, "Weekly", period)
        frag.calMonthTable.adapter = fillCalendar(start, "Monthly", period)
        frag.calYearTable.adapter = fillCalendar(start, "Yearly", period)
    }

    fun generateQuery(searchDate: LocalDate, table: String, period: String) : String {
        val start = when(table) {
            "Weekly"    -> when(period) {
                "Weekly"    -> searchDate.minusDays(searchDate.dayOfWeek.value - 1L)
                "Monthly"   -> searchDate.withDayOfMonth(1)
                else        -> searchDate.withDayOfYear(1)
            }
            "Monthly"   -> when(period) {
                "Yearly"    -> searchDate.withDayOfYear(1)
                else        -> searchDate.withDayOfMonth(1)
            }
            else        -> searchDate.withDayOfYear(1)  }
        val end = when(table) {
            "Weekly"    -> when(period) {
                "Weekly"    -> start.plusDays(7)
                "Monthly"   -> start.withDayOfMonth(start.lengthOfMonth())
                else        -> start.withDayOfYear(start.lengthOfYear())
            }
            "Monthly"   -> when(period) {
                "Yearly"    -> searchDate.withDayOfYear(start.lengthOfYear())
                else        -> searchDate.withDayOfMonth(start.lengthOfMonth())
            }
            else        -> searchDate.withDayOfYear(start.lengthOfYear())  }

        d(TAG, "$table, $period : $start -> $end")

        return "date BETWEEN '${start.format(com.ymdFormatter)}' AND '${end.format(com.ymdFormatter)}'"
    }

    fun fillCalendar(searchDate: LocalDate, table: String, period: String) : TableAdapter {

        val mapDates = mutableMapOf<String, CalDate>()
        val lstCalendar = mutableListOf<MutableList<CalDate>>()
        val start = when(table) {
                        "Weekly"    -> searchDate.minusDays(searchDate.dayOfWeek.value - 1L)
                        "Monthly"   -> searchDate.withDayOfMonth(1)
                        else        -> searchDate.withDayOfYear(1)  }

        val subPeriod = when(table) {
                        "Weekly"    -> "Weekdaily"
                        "Monthly"   -> "Daily"
                        else        -> "Monthly" }

        val rowLength = if(table == "Yearly") 5 else 6
        val lstSessions = db.readSessions(generateQuery(searchDate, table, period))
        val lstDates = com.periodSubdiv(start, table)

        if(table == "Monthly" && period != "Yearly") {
            val startDay = lstDates[0].dayOfWeek.value
            for(d in 1 until startDay) {
                lstDates.add(0, LocalDate.parse("2000-01-01", com.ymdFormatter))
            }
        }

        lstDates.forEach {
            mapDates[com.strPeriodBucket(it, subPeriod)!!] = CalDate(it, subPeriod)
        }

        for(sesh in lstSessions) {
            val sub = com.strPeriodBucket(sesh.date, subPeriod)
            if(sub in mapDates.keys) {
                mapDates[sub]!!.addSession(sesh)
            }
        }

        var d = 0
        while(d < mapDates.keys.size) {
            val lstRow = mutableListOf<CalDate>()
            for(c in 0..rowLength) {
                if(d > lstDates.lastIndex || lstDates[d] == LocalDate.parse("2000-01-01", com.ymdFormatter)){
                    lstRow.add( CalDate() )
                } else {
                    val date = com.strPeriodBucket(lstDates[d], subPeriod)
                    lstRow.add(mapDates[date]!!)
                }
                d += 1
            }
            lstCalendar.add(lstRow)
        }

        return TableAdapter(requireContext(), lstCalendar, "calendar")
    }


}
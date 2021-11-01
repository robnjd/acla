package com.example.acla.ui.dashboard

import android.os.Bundle
import android.util.Log
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
import kotlinx.android.synthetic.main.frag_calendar.view.*
import kotlinx.android.synthetic.main.frag_graph.view.*
import java.time.LocalDate

class CalendarPager() : Fragment() {

    lateinit var frag: View
    lateinit var cht: ChartHelper
    lateinit var com: CommonRoom
    val vmDash: DashViewModel by viewModels({requireParentFragment()})

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmDash.stats.observe(viewLifecycleOwner, Observer {
            stats ->
                frag.calMonthTable.adapter = fillCalendar("Monthly")
                frag.calYearTable.adapter = fillCalendar("Yearly")
        } )

        return inflater.inflate(R.layout.frag_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        cht = ChartHelper(requireContext())
        com = CommonRoom(requireContext())

        frag.calMonthTable.adapter = fillCalendar("Monthly")
        frag.calYearTable.adapter = fillCalendar("Yearly")
    }

    fun fillCalendar(period: String) : TableAdapter {

        val searchFilter = vmDash.searchFilter.value

        val lstDates = com.periodSubdiv(searchFilter!!.startDate, searchFilter!!.period)

        if(period == "Monthly"){
            val startDay =lstDates[0].dayOfWeek.value
            for(d in 1 until startDay) {
                lstDates.add(0, LocalDate.parse("2000-01-01", com.ymdFormatter))
            }
        }

        val mapDates = mutableMapOf<LocalDate, MutableMap<String, Int>>()
        lstDates.forEach {
            mapDates[it] = mutableMapOf("Interval" to 0, "Run" to 0, "Routine" to 0)
        }

        val subPeriod = if(searchFilter.period == "Yearly") "Monthly" else "Daily"

        for(sesh in vmDash.lstSessions.value!!){
            val sub = com.periodBucket(sesh.date, subPeriod)
            if(sub in lstDates) {
                mapDates[sub]!![sesh.workout] = (mapDates[sub]!![sesh.workout]?:0) + 1
            }
        }

        val lstCalendar = mutableListOf<MutableList<List<String>>>()
        val rowLength = if(searchFilter.period == "Yearly") 5 else 6
        var d = 0
        while(d < lstDates.size) {
            var lstRow = mutableListOf<List<String>>()
            for(c in 0..rowLength) {
                if(d > lstDates.lastIndex || lstDates[d] == LocalDate.parse("2000-01-01", com.ymdFormatter)){
                    lstRow.add( listOf("", "0", "0", "0") )
                } else {
                    val row = mutableListOf<String>()
                    row.add(com.formatSubdiv(lstDates[d], searchFilter.period))
                    row.add(mapDates[lstDates[d]]!!["Interval"].toString())
                    row.add(mapDates[lstDates[d]]!!["Run"].toString())
                    row.add(mapDates[lstDates[d]]!!["Routine"].toString())
                    lstRow.add( row )
                }
                d += 1
            }
            lstCalendar.add(lstRow)
        }

        return TableAdapter(requireContext(), lstCalendar, "calendar")

    }


}
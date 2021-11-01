package com.example.acla.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.acla.DashViewModel
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_dashboard.*
import kotlinx.android.synthetic.main.frag_dashboard.view.*
import java.time.LocalDate
import java.util.*

class DashboardFragment : Fragment() {

    lateinit var frag: View
    lateinit var vmDash: DashViewModel
    lateinit var searchFilter: SearchFilter
    lateinit var db: DatabaseHelper
    lateinit var com: CommonRoom

    val pages = 3
    val lstPages = mutableListOf<Boolean>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.frag_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())
        vmDash = ViewModelProvider(this)[DashViewModel::class.java]

        val today = LocalDate.now()

        searchFilter = SearchFilter(requireContext(), dshSearchLayout)
        searchFilter.period = "Monthly"
        searchFilter.metric = "count"
        searchFilter.startDate = today.withDayOfMonth(1)
        searchFilter.endDate = today.withDayOfMonth(today.lengthOfMonth())

        val workoutColours = mapOf("All" to R.color.orange, "Interval" to R.color.blue, "Run" to R.color.green, "Routine" to R.color.purple)

        dshSearchIcon.setOnClickListener {
            if(searchFilter.open){
                vmDash.searchFilter.value = searchFilter
                vmDash.lstSessions.value = getSessions()
                vmDash.stats.value = getStats(vmDash.lstSessions.value!!)
            }
            searchFilter.openFilter()
        }

        frag.dshPager.adapter = PagerAdapter(this, pages)

        frag.dshPager.registerOnPageChangeCallback( object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position : Int) {
                super.onPageSelected(position)
                changePage(position)
            } } )

    }

    fun changePage(page: Int) {
        (0..lstPages.lastIndex).forEach { lstPages[it] = it == page }
        frag.dshPages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        frag.dshPages.adapter = ListRecycler(requireContext(), lstPages, R.layout.item_page, "page")
    }

    fun getSessions() : MutableList<Session> {
        val where = "date BETWEEN '${searchFilter.startDate}' AND '${searchFilter.endDate}'"
        return db.readSessions(where)
    }

    fun getStats(lstSessions: MutableList<Session>) : MutableMap<String, MutableMap<LocalDate, Float>> {
        val lstAtts = listOf("count", "duration", "metric")

        val stats = mutableMapOf<String, MutableMap<LocalDate, Float> >()
        lstAtts.forEach { stats[it] = mutableMapOf() }

        for(sesh in lstSessions) {
            val period = com.periodBucket(sesh.date, searchFilter.period) ?: continue
            if(period !in stats["count"]!!.keys) {
                lstAtts.forEach { stats[it] = mutableMapOf(period to 0f) }
            }
            stats["count"]!![period] = stats["count"]!![period]!! + 1
            stats["duration"]!![period] = stats["duration"]!![period]!! + sesh.seconds
            stats["metric"]!![period] = stats["metric"]!![period]!! + sesh.metricCount()
        }

        return stats
    }

}
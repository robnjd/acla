package com.example.acla.ui.dashboard

import android.os.Bundle
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_dashboard.*
import kotlinx.android.synthetic.main.frag_dashboard.view.*
import java.time.LocalDate

class DashboardFragment : Fragment() {
    val TAG = "DashboardFragment"
    lateinit var frag: View
    lateinit var vmDash: DashViewModel
    lateinit var searchFilter: SearchFilter
    lateinit var db: DatabaseHelper
    lateinit var com: CommonRoom

    val pages = 4

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

        dshSearchIcon.setOnClickListener {
            if(searchFilter.open){
                vmDash.search(searchFilter)
            }
            searchFilter.openFilter()
        }

        frag.dshPager.adapter = PagerAdapter(this, pages)

        frag.dshPager.registerOnPageChangeCallback( object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position : Int) {
                super.onPageSelected(position)
                changePage(position)
            } } )

        vmDash.search(searchFilter)
        changePage(0)
    }

    fun changePage(page: Int) {
        val lstPages = mutableListOf<Boolean>()
        (0 until pages).forEach { lstPages.add(it == page) }
        d(TAG, lstPages.toString())
        frag.dshPages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        frag.dshPages.adapter = ListRecycler(requireContext(), lstPages, R.layout.item_page, "page")
    }

    fun getSessions() : MutableList<Session> {
        val where = "date BETWEEN '${searchFilter.startDate}' AND '${searchFilter.endDate}'"
        return db.readSessions(where)
    }

    fun getStats(lstSessions: MutableList<Session>) : MutableMap<LocalDate, MutableMap<String, Float>> {
        val lstAtts = listOf("count", "duration", "measure")

        val stats = mutableMapOf<LocalDate, MutableMap<String, Float> >()
        //lstAtts.forEach { stats[it] = mutableMapOf() }

        for(sesh in lstSessions) {
            val period = com.periodBucket(sesh.date, searchFilter.period) ?: continue
            if(period !in stats.keys) {
                lstAtts.forEach { stats[period] = mutableMapOf("count" to 0f, "duration" to 0f, "measure" to 0f) }
            }
            stats[period]!!["count"] = stats[period]!!["count"]!! + 1
            stats[period]!!["duration"] = stats[period]!!["duration"]!! + sesh.seconds
            stats[period]!!["measure"] = stats[period]!!["measure"]!! + sesh.metricCount()
        }

        return stats
    }

}
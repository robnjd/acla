package com.example.acla.backend

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.acla.R
import com.example.acla.ui.dashboard.*
import kotlinx.android.synthetic.main.item_import.view.*
import kotlinx.android.synthetic.main.item_history.view.*
import kotlinx.android.synthetic.main.item_page.view.*
import java.time.LocalDate

class TableAdapter(private val context: Context,
                   private val dataSource: MutableList<*>,
                   private val type: String,
                   private val clicker: ((Any)->Unit)? = null) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    lateinit var rowView: View
    val com = CommonRoom(context)

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position] as Any
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        when (type) {
            "history"   -> HistoryRow(position, parent)
            "legend"    -> LegendRow(position, parent)
            "calendar"  -> CalendarRow(position, parent)
            "images"    -> ImageRow(position, parent)
            "import"    -> ImportRow(position, parent)
        }

        return rowView
    }

    private fun ImportRow(position: Int, parent: ViewGroup) {
        rowView = inflater.inflate(R.layout.item_import, parent, false)
        val rBackup = getItem(position) as String
        rowView.iimpFilename.text = rBackup
    }

    private fun CalendarRow(position: Int, parent: ViewGroup) {
        rowView = inflater.inflate(R.layout.item_calendar, parent, false)
        val rCalendar = getItem(position) as List<List<String>>

        for(d in 0..rCalendar.lastIndex){

            val vw = rowView.findViewById<TextView>(viewId("tcEntry${d+1}"))!!
            val vBottom = rowView.findViewById<TextView>(viewId("tcBottomNum${d+1}"))!!
            val vTop = rowView.findViewById<TextView>(viewId("tcTopNum${d+1}"))!!
            val vCircle = rowView.findViewById<ImageView>(viewId("tcCircle${d+1}"))
            val vHalf = rowView.findViewById<ImageView>(viewId("tcHalf${d+1}"))

            vw.text = rCalendar[d][0]

            val counts = mapOf("Interval" to rCalendar[d][1].toInt(), "Run" to rCalendar[d][2].toInt(), "Routine" to rCalendar[d][3].toInt())

            var nonzero = 0
            for (w in counts.keys){
                if(counts[w]?:0 > 0) {
                    nonzero += 1
                }
            }

            when(nonzero){
                0   -> {vw.setTextColor(ContextCompat.getColor(context, R.color.textBlack))
                        vCircle.visibility = View.GONE
                        vHalf.visibility = View.GONE
                        vBottom.visibility = View.GONE }

                1   -> {var col = R.color.white
                        vCircle.visibility = View.VISIBLE
                        vHalf.visibility = View.GONE
                        for(w in counts.keys){
                            if(counts[w]?:0 > 0){
                                col = com.workoutIcon(w)["col"]!!
                                if(counts[w]!! > 1) {
                                    vBottom.visibility = View.VISIBLE
                                    vBottom.setTextColor(ContextCompat.getColor(context, col))
                                    vBottom.text = counts[w].toString()
                                }
                                break
                            } }
                        com.tint(vCircle, col, "bg")
                        vw.setTextColor(ContextCompat.getColor(context, R.color.white)) }

                2   -> {vw.setTextColor(ContextCompat.getColor(context, R.color.white))
                        val topbot = listOf(vTop, vBottom)
                        val circles = listOf(vCircle, vHalf)
                        var pos = 0
                        for(w in counts.keys){
                            if(counts[w]?:0 > 0){
                                val vTopBot = topbot[pos]
                                val vSplit = circles[pos]
                                val col = com.workoutIcon(w)["col"]!!

                                vTopBot.visibility = View.VISIBLE
                                vSplit.visibility = View.VISIBLE

                                vTopBot.text = counts[w].toString()
                                vTopBot.setTextColor(ContextCompat.getColor(context, col))
                         
                                com.tint(vSplit, col, "bg")

                                pos += 1
                            }
                        } }

                3   -> {}
            }
        }

        for(d in rCalendar.size..6){
            val vw = rowView.findViewById<ConstraintLayout>(viewId("tcDate${d+1}"))!!
            val sp = rowView.findViewById<Space>(viewId("tcSpace${d}"))

            vw.visibility= View.GONE
            sp.visibility = View.GONE
        }
    }

    private fun HistoryRow(position: Int, parent: ViewGroup) {

        rowView = inflater.inflate(R.layout.item_history, parent, false)
        val iSesh = getItem(position) as Session

        rowView.thDate.text = iSesh.date.format(com.dmyFormatter)
        rowView.thStart.text = iSesh.start.format(iSesh.hms)
        rowView.thFinish.text =  iSesh.finish.format(iSesh.hms)
        rowView.thDuration.text = iSesh.duration
        rowView.thMeasure.text = iSesh.showMeasure()

        com.workoutIcon(iSesh.workout, rowView.thIcon)
    }

    private fun LegendRow(position: Int, parent: ViewGroup) {

        rowView = inflater.inflate(R.layout.item_legend, parent, false)
        val startIndex = position*3

        for(i in 0..2) {

            if(startIndex+i >= getCount()){ break }

            val vColour = rowView.findViewById<ImageView>(viewId("tlColour$i"))
            val vText = rowView.findViewById<TextView>(viewId("tlText$i"))

            val entry = getItem(startIndex + i) as List<String>
            val colour = ContextCompat.getColor(context, entry[0].toInt())

            vColour.setColorFilter(colour)
            vText.text = entry[1]
        }
    }

    private fun ImageRow(position: Int, parent: ViewGroup) {
        rowView = inflater.inflate(R.layout.item_calendar, parent, false)
        val rCalendar = getItem(position) as List<List<String>>

        for(d in 0..rCalendar.lastIndex){
            val vw = rowView.findViewById<TextView>(viewId("tcEntry${d+1}"))!!
            val vCircle = rowView.findViewById<ImageView>(viewId("tcCircle${d+1}"))

            val date = LocalDate.parse(rCalendar[d][0], com.ymdFormatter)
            val img = com.boolize(rCalendar[d][1])

            vw.text = date.dayOfMonth.toString()
            var col = R.color.textBlack

            if(img){
                vCircle.visibility = View.VISIBLE
                col = R.color.white
                com.tint(vCircle, R.color.orange, "bg")
                vCircle.setOnClickListener {
                    clicker!!(date)
                }
            } else {
                vCircle.visibility = View.GONE
            }
            vw.setTextColor(ContextCompat.getColor(context, col))
        }
    }

    fun viewId(viewName: String) : Int {

        val res = context.getResources()
        val id = res.getIdentifier(viewName, "id", context.getPackageName())

        return id
    }

}

class PagerAdapter(activity: DashboardFragment, val cntItems: Int) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return cntItems
    }

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0       -> CalendarPager()
            1       -> TablePager()
            2       -> GraphPager(position)
            3       -> TimePager()
            else    -> GraphPager(position)
        }
    }

}

class ListRecycler(val context: Context, var list: MutableList<*>,  val layout: Int, val type: String,
                   val mapColour: MutableMap<String, Int>?=null, val clicked: ((Any, View, String)->Unit)? = null) : RecyclerView.Adapter<ListRecycler.ViewHolder>(){

    val TAG = "ListRecycler"
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    lateinit var com: CommonRoom

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val view = view
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflater.inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        com = CommonRoom(context)
        if(clicked != null) holder.view.setOnClickListener { clicked!!(list[position]!!, holder.view, "clicked") }

        when(type){
            "page"          -> rowPage(holder.view, position)
        }
    }

    fun rowPage(row: View, position: Int) {
        val iPage = list[position] as Boolean

        if(!iPage){
            com.tint(row.ipgIcon, R.color.grey, "fg")
            com.tint(row.ipgIcon, R.color.blank, "bg")
        }
    }
}
package com.example.acla.backend

import android.content.Context
import android.graphics.PorterDuff
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
import kotlinx.android.synthetic.main.item_aggregate.view.*
import kotlinx.android.synthetic.main.item_dropdown.view.*
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
            "dashboard" -> DashboardRow(position, parent)
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
        val rCalendar = getItem(position) as List<CalDate>

        for(d in 0..rCalendar.lastIndex){

            val vw = rowView.findViewById<TextView>(viewId("tcEntry${d+1}"))!!
            val vBottom = rowView.findViewById<TextView>(viewId("tcBottomNum${d+1}"))!!
            val vTop = rowView.findViewById<TextView>(viewId("tcTopNum${d+1}"))!!
            val vCircle = rowView.findViewById<ImageView>(viewId("tcCircle${d+1}"))
            val vHalf = rowView.findViewById<ImageView>(viewId("tcHalf${d+1}"))

            vw.text = rCalendar[d].subdiv

            val counts = mapOf("Interval" to rCalendar[d].interval, "Run" to rCalendar[d].run, "Routine" to rCalendar[d].routine)

            var nonzero = 0
            for (w in counts.keys){
                if(counts[w]?:0f >= 0.1) {
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
                            if(counts[w]?:0f > 0.1){
                                col = com.workoutIcon(w)["col"]!!
                                if(counts[w]!! != 1f) {
                                    vBottom.visibility = View.VISIBLE
                                    vBottom.setTextColor(ContextCompat.getColor(context, col))
                                    vBottom.text = com.numberFormat(counts[w]?:0f, 1)
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
                            if(counts[w]?:0f >= 0.1){
                                val vTopBot = topbot[pos]
                                val vSplit = circles[pos]
                                val col = com.workoutIcon(w)["col"]!!

                                vTopBot.visibility = View.VISIBLE
                                vSplit.visibility = View.VISIBLE

                                vTopBot.text = com.numberFormat(counts[w]?:0f,1)
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

    private fun DashboardRow(position: Int, parent: ViewGroup) {
        rowView = inflater.inflate(R.layout.item_aggregate, parent, false)
        val iDash = getItem(position) as Map<String, String>

        com.workoutIcon(iDash["workout"]!!, rowView.iaggWorkoutIcon)

        for(att in listOf("Sessions", "Duration", "Measures")) {
            val form = when(att){
                "Sessions"  -> com.numberFormat(iDash["$att-current"]!!.toFloat(), 1)
                "Duration"  -> com.timeFormat(iDash["$att-current"]!!.toFloat())
                "Measures"   -> com.measureFormat(iDash["$att-current"]!!.toFloat(), iDash["workout"]!!)
                else        -> ""
            }
            val delta = rowView.findViewById<TextView>(com.viewId("iagg${att}Delta"))
            val img = rowView.findViewById<ImageView>(com.viewId("iagg${att}DeltaIcon"))
            val icn = when(iDash["$att-direction"]) {
                "+"     -> R.drawable.ic_trendingup
                "-"     -> R.drawable.ic_trendingdown
                else    -> R.drawable.ic_trendingflat
            }
            val col = when(iDash["$att-direction"]) {
                "+"     -> R.color.green
                "-"     -> R.color.red
                else    -> R.color.greyLight
            }

            rowView.findViewById<TextView>(com.viewId("iagg${att}Value")).text = form
            delta.text = com.percentFormat(iDash["$att-delta"]!!)
            delta.setTextColor(ContextCompat.getColor(context, col))
            img.setImageResource(icn)
            com.tint(img, col, "fg")
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
            0       -> TablePager()
            1       -> CalendarPager()
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

class IconDropdown(val context: Context, val list: List<Map<String, Any>>) : BaseAdapter() {

    val TAG = "IncomeDropdown"

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_dropdown, parent, false)

        val iIncome = getItem(position)

        view.idrpIcon.setImageResource(("${iIncome["icon"]}".toInt()))
        tint(view.idrpIcon, "${iIncome["colour"]}".toInt(), "fg")
        if("text" in iIncome.keys) view.idrpText.text = "${iIncome["text"]}" else view.idrpText.visibility = View.GONE

        return view
    }

    override fun getItem(position: Int) : Map<String, Any> {
        return list[position]
    }

    override fun getItemId(position: Int) : Long {
        return position.toLong()
    }

    override fun getCount() : Int {
        return list.size
    }

    fun <V : View> tint(view : V, colour : Int, foreback : String = "fg") {
        val col = ContextCompat.getColor(context, colour)

        when (foreback) {
            "fg", "fore", "foreground" -> { view as ImageView
                                            view.setColorFilter(col, PorterDuff.Mode.SRC_IN) }
            "bg", "back", "background" ->   view.background.setColorFilter(col, PorterDuff.Mode.SRC_IN)
        }
    }




}
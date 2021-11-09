package com.example.acla.backend

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.acla.R
import java.time.LocalDate


class SearchFilter(private val context: Context, private val searchLayout: ConstraintLayout) {

    val com = CommonRoom(context)
    val res = context.getResources()

    lateinit var layFilter : ConstraintLayout
    lateinit var btnPeriod : TextView
    lateinit var btnSearch : ImageView
    lateinit var btnDate : TextView
    lateinit var btnBack : ImageView
    lateinit var btnNext : ImageView

    var period = "Monthly"
    var metric = "count"
    lateinit var startDate : LocalDate
    lateinit var endDate : LocalDate

    var open = false

    init {

        setButtons(searchLayout)

        val mapIcons = mapOf("period" to mapOf("Weekly" to "W", "Monthly" to "M", "Yearly" to "Y"),
                             "metric" to mapOf("count" to R.drawable.ic_whistle, "duration" to R.drawable.ic_timer, "measure" to R.drawable.ic_measure))

        val periodColours = mapOf("Weekly" to R.color.blue, "Monthly" to R.color.purple, "Yearly" to R.color.green)

        btnDate.text = "Current Month"

        btnPeriod.setOnClickListener {
            period = com.switch(period, listOf("Weekly", "Monthly", "Yearly"))
            btnPeriod.text = mapIcons["period"]!![period]!!.toString()
            btnPeriod.setTextColor(ContextCompat.getColor(context, periodColours[period]!!))
            changeTimeFrame(period, 0)
        }

        searchLayout.setOnTouchListener(object: OnSwipeTouchListener(context){
            override fun onSwipeRight() : Boolean {
                if(open) openFilter()
                return true
            }
            override fun onSwipeLeft() : Boolean {
                if(!open) openFilter()
                return true
            }
        } )

        btnDate.setOnTouchListener(object: OnSwipeTouchListener(context){
            override fun onSwipeTop() : Boolean {
                changeTimeFrame(period, 1)
                return true
            }
            override fun onSwipeBottom() : Boolean {
                changeTimeFrame(period, -1)
                return true
            }
            override fun onSwipeRight() : Boolean {
                openFilter()
                return true
            }
        } )

        btnBack.setOnClickListener {
            changeTimeFrame(period, 1)
        }

        btnNext.setOnClickListener {
            changeTimeFrame(period, -1)
        }

        btnDate.setOnClickListener {
            btnDate.text = convertDate(btnDate.text.toString()).format(com.dmyFormatter)
            com.showCalendar(btnDate)
        }

    }

    private fun setButtons(lay: ConstraintLayout){

        for (i in 0 until lay.childCount) {

            val vw = lay.getChildAt(i)
            val vwName = res.getResourceName(vw.id)

            if (vwName.contains("FilterPeriod")) {
                btnPeriod = vw as TextView
            } else if (vwName.contains("SearchIcon")) {
                btnSearch = vw as ImageView
            } else if (vwName.contains("FilterDate")) {
                btnDate = vw as TextView
            } else if (vwName.contains("FilterLayout")) {
                layFilter = vw as ConstraintLayout
            } else if (vwName.contains("Back")) {
                btnBack = vw as ImageView
            } else if (vwName.contains("Next")) {
                btnNext = vw as ImageView
            }

            if (vw is ConstraintLayout) {
                setButtons(vw)
            }
        }
    }

    private fun changeNumber(oldNum: String, period: String, updown: Int) : String {
        val lstPast = listOf("Current", "Last")
        val num = try{
            oldNum.toInt()
        } catch (E : Exception) {
            lstPast.indexOf(oldNum)
        }

        var newNum = num + updown
        if(newNum < 0) { newNum = 0 }

        val strNum = if(newNum < 2){
            lstPast[newNum]
        } else {
            "$newNum"
        }

        return "$strNum ${period.replace("ly", "")}${if(newNum > 1)"s Ago" else ""}"
    }

    private fun changeTimeFrame(period: String, updown: Int) {

        val currentTimeFrame = btnDate.text.toString()
        val spCurrent = currentTimeFrame.split(" ")
        val pName = period.replace("ly", "")

        val timeFrame = if(spCurrent.size > 1){
            if(spCurrent[1].replace("s","") == pName) {
                changeNumber(spCurrent[0], pName, updown)
            } else {
                "${spCurrent[0]} $pName" + if(spCurrent.size > 2) "s Ago" else ""
            }
        } else {
            "Current $pName"
        }

        btnDate.text = timeFrame
        setStartEnd(convertDate(timeFrame), period)
    }

    private fun convertDate(strDate : String) : LocalDate {

        return if(strDate.contains("Ago") || strDate.contains("Current") || strDate.contains("Last")){
            val curlast = listOf("Current", "Last")
            val spDate = strDate.split(" ")
            val num = curlast.indexOf(spDate[0])
            dateDelta(if(num == -1) spDate[0].toLong() else num.toLong(), spDate[1])
        } else {
            if(strDate.contains("/")){
                val d = strDate.substring(strDate.lastIndex-9,strDate.length)
                LocalDate.parse(d, com.dmyFormatter)
            } else if(strDate.contains(" ")){
                val d = "01 $strDate"
                LocalDate.parse(d, com.monFormatter)
            } else {
                val d = "01/01/$strDate"
                LocalDate.parse(d, com.dmyFormatter)
            }
        }
    }

    private fun dateDelta(number : Long, period : String, startDate : LocalDate = LocalDate.now(), past : Boolean = true) : LocalDate {
        var parsePeriod = period.toLowerCase()
        parsePeriod = parsePeriod.replace("s", "")
        parsePeriod = parsePeriod.replace("ly", "")

        return when(parsePeriod){
            "day"   -> if(past) startDate.minusDays(number)     else startDate.plusDays(number)
            "week"  -> if(past) startDate.minusWeeks(number)    else startDate.plusWeeks(number)
            "month" -> if(past) startDate.minusMonths(number)   else startDate.plusMonths(number)
            "year"  -> if(past) startDate.minusYears(number)    else startDate.plusYears(number)
            else    -> startDate
        }
    }

    fun openFilter(){

        btnSearch.setImageResource(R.color.orange)

        open = !open

        val icon = when(open){
            true    ->{ layFilter.visibility = View.VISIBLE
                        R.drawable.ic_send }
            false   ->{ layFilter.visibility = View.GONE
                        R.drawable.ic_search }
        }

        btnSearch.setImageResource(icon)
    }

    fun getFilters() {

        val inputDate = convertDate(btnDate.text.toString())

        startDate = when(period) {
            "Weekly"    -> inputDate.minusDays(inputDate.dayOfWeek.value.toLong()-1)
            "Monthly"   -> inputDate.withDayOfMonth(1)
            "Yearly"    -> inputDate.withDayOfYear(1)
            else        -> inputDate
        }

        endDate = when(period){
            "Weekly"  -> startDate.plusWeeks(1)
            "Monthly" -> startDate.plusMonths(1)
            "Yearly"  -> startDate.plusYears(1)
            else    -> startDate.plusDays(1)
        }.minusDays(1)

    }

    private fun setStartEnd(date: LocalDate, period: String) {
        startDate = when(period){
            "Weekly"    -> date.minusDays((if(date.dayOfWeek.value == 0) 7 else date.dayOfWeek.value - 1).toLong())
            "Monthly"   -> date.minusDays(date.dayOfMonth.toLong()-1)
            "Yearly"    -> date.minusDays(date.dayOfYear.toLong()-1)
            else        -> date
        }
        endDate = dateDelta(1, period, startDate, false).minusDays(1L)
    }

}


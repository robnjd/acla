package com.example.acla.backend

import android.content.Context
import android.util.Log.d
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.acla.R
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ChartHelper(private val context: Context) {
    val TAG = "ChartHelper"
    private val com = CommonRoom(context)

    private val touch = mutableMapOf<String, Float>()
    private val touchListener = object : View.OnTouchListener {
        override fun onTouch(v : View, event : MotionEvent) : Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touch["x"] = if(event.x < 300) -500f else if(event.x > 700) 500f else 0f
                touch["y"] = event.y - 200f
            }
            return false
        } }


    fun axisDate(date: String, datePeriod: String, suppressYear: Boolean = false) : String {
        
        if(datePeriod == "Weekdaily"){ return date }

        val d = LocalDate.parse(date, com.dmyFormatter)

        val form: DateTimeFormatter

        if(suppressYear) {

            form = when(datePeriod) {
                "Daily"     -> if (d.dayOfMonth == 1) com.dmFormatter else com.dFormatter
                "Weekly"    ->  com.dmonFormatter
                "Monthly"   -> com.monFormatter
                else        -> return d.year.toString()
            }

        } else {

            form = when (datePeriod) {
                "Daily" -> if (d.dayOfYear == 1) com.dmyFormatter else if (d.dayOfMonth == 1) com.dmFormatter else com.dFormatter
                "Weekly" -> if (d.dayOfYear < 7) com.dmonyFormatter else com.dmonFormatter
                "Monthly" -> if (d.dayOfYear == 1) com.monyFormatter else com.monFormatter
                else -> return d.year.toString()
            }
        }
        return d.format(form)
    }
    
    fun buildComboChart(chartView : CombinedChart, mapData : Map<String, Map<String, Float>>, setTypes : Map<String, String>, mapColours : Map<String, Int>) {

        val dataLine = LineData()
        val dataBar = BarData()
        val dataComb = CombinedData()

        val xRange = mutableListOf<String>()
        xRange.addAll(mapData.keys)
        val setNames = com.subKeys(mapData)

        for(set in setNames) {
            when(setTypes[set]){
                "Line"  ->{ val lstSet = mutableListOf<Entry>()
                    for((i, x) in xRange.withIndex()) {
                        lstSet.add(Entry(i.toFloat(), com.getSubEl(mapData, x, set)?:0f))
                    }
                    val dataSet = LineDataSet(lstSet, set)
                    formatLine(dataSet, mapColours[set]?:0)

                    dataLine.addDataSet(dataSet) }

                "Bar"   ->{ val lstSet = mutableListOf<BarEntry>()
                    for((i, x) in xRange.withIndex()) {
                        lstSet.add(BarEntry(i.toFloat(), com.getSubEl(mapData, x, set)?:0f))
                    }
                    val dataSet = BarDataSet(lstSet, set)
                    dataSet.setColors(ContextCompat.getColor(context, mapColours[set]!!))
                    dataSet.setDrawValues(false)

                    dataBar.addDataSet(dataSet) }
            }
        }

        dataComb.setData(dataLine)
        dataComb.setData(dataBar)

        formatAxes(chartView, xRange, "combo")
        chartView.data = dataComb
        chartView.invalidate()

        chartView.axisLeft.addLimitLine(drawLimitLine(1f, "Target"))
    }

    fun buildHorizontalChart(chartView : HorizontalBarChart, mapData : Map<String, Map<String, Float>>, colours : Map<String, Int>) {

        val chData = BarData()

        val xRange = mutableListOf<String>()
        for(i in 12 downTo 1){ xRange.add("$i") }

        for(bar in com.subKeys(mapData)) {
            val lstSet = mutableListOf<BarEntry>()

            for((i, x) in xRange.withIndex()) {
                lstSet.add(BarEntry(i.toFloat(), com.getSubEl(mapData, x, bar)?:0f))
            }

            val dataSet = BarDataSet(lstSet, bar)
            dataSet.setColors(ContextCompat.getColor(context, colours[bar]!!))
            dataSet.setDrawValues(false)

            chData.addDataSet(dataSet)
        }

        chData.barWidth = 0.5f

        formatAxes(chartView, xRange, "horizontal", "" , false, null)
        chartView.axisLeft.addLimitLine(drawLimitLine(0f, "PM", R.color.black, colours["PM"]!!, "RT"))
        chartView.axisLeft.addLimitLine(drawLimitLine(0f, "AM", R.color.blank, colours["AM"]!!, "LT"))

        chartView.data = chData
        chartView.invalidate()
    }

    fun buildLineChart(chartView : LineChart, mapData : Map<String, Map<String, Float>>, mapColours : Map<String, Int>, periodType: String="Daily", rightAxis : List<String>? = null, suppressYear : Boolean = false) {

        val chData = LineData()

        val xRange = mutableListOf<String>()
        xRange.addAll(mapData.keys)

        val lineNames = com.subKeys(mapData)
        val lstMarkers = mutableListOf<MutableList<Map<String, String>>>()

        for (line in lineNames) {

            val lstLine = mutableListOf<Entry>()

            for((i, x) in xRange.withIndex()) {
                lstLine.add(Entry(i.toFloat(), com.getSubEl(mapData, x, line)?:0f))
            }
            lstMarkers.add(mutableListOf(mapOf("colour" to (mapColours[line]?:0).toString())))

            val dataSet = LineDataSet(lstLine, line)

            if(rightAxis != null && line in rightAxis){ dataSet.axisDependency = YAxis.AxisDependency.RIGHT }
            formatLine(dataSet, mapColours[line]?:0, false, mapColours[line])

            chData.addDataSet(dataSet)
        }

        formatAxes(chartView, xRange, "line", periodType, rightAxis != null, suppressYear=suppressYear)
        chartView.data = chData
        chartView.invalidate()

        //chartView.marker = MarkerHelper(context, R.layout.dialog_marker, lstMarkers)
    }

    fun buildPieChart(chartView : PieChart, mapData : Map<String, Float>, mapColours : Map<String, Int> ) {

        val chData = PieData()

        val xRange = mutableListOf<String>()
        val pieValues = mutableListOf<PieEntry>()
        val pieColours = mutableListOf<Int>()
        val pieLabels = mutableListOf<String>()
        val lstMarkers = mutableListOf<MutableList<Map<String, String>>>(mutableListOf())

        xRange.addAll(mapData.keys)

        for (slice in mapData.keys){

            val sliceValue = mapData[slice]?:0f
            if(sliceValue > 0f) {
                pieLabels.add(slice)
                pieValues.add(PieEntry(sliceValue, slice))
                pieColours.add(ContextCompat.getColor(context, mapColours[slice]?:0))
                lstMarkers[0].add(mapOf("colour" to (mapColours[slice]?:0).toString()))
            }
        }

        val dataSet = PieDataSet(pieValues, "")
        dataSet.setColors(pieColours)
        dataSet.setValueFormatter(PercentFormatter(chartView))
        chartView.setEntryLabelColor(com.themeColour(R.attr.textPrimary))

        chData.addDataSet(dataSet)
        chData.setValueTextSize(0f)

        chartView.data = chData

        chartView.description.text = ""
        chartView.isDrawHoleEnabled = false
        chartView.legend.isEnabled = false
        chartView.setUsePercentValues(true)
        chartView.setDrawEntryLabels(true)

        chartView.invalidate()

       // chartView.marker = MarkerHelper(context, R.layout.dialog_marker, lstMarkers, "pie")
/*
        val pieTotal = com.dictionarySum(mapData as MutableMap<String, Float>)

        chartView.setOnTouchListener(touchListener)

        chartView.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e : Entry, h : Highlight) {
                val sliceName = pieLabels[h.x.toInt()]
                val sliceValue = setup.prefCur + " " + com.priceString(h.y)
                val perc = com.percentString(com.toFloat(h.y) / pieTotal)

                showDataBox(sliceName, null, "$sliceValue  |  $perc", mapColours[sliceName]?:0, touch)
            }
            override fun onNothingSelected() { }
        } )
*/
    }

    fun buildStackedChart(chartView : BarChart, mapData : Map<String, Map<String, Float>>, mapColours : Map<String, Int>, stacking : String = "Absolute") {

        val chData = BarData()

        val xRange = mutableListOf<String>()
        xRange.addAll(mapData.keys)

        val blockNames = com.subKeys(mapData)

        for ((i, x) in xRange.withIndex()) {

            val div = if(stacking  == "Percent") com.mapSum(mapData[x]!! as MutableMap) else 1f

            var dataBlocks = floatArrayOf()
            var blockColours = mutableListOf<Int>()

            for (block in blockNames) {
                dataBlocks += (com.getSubEl(mapData, x, block)?:0f)/div
                blockColours.add(ContextCompat.getColor(context, mapColours[block]?:0))
            }
            val dataBar = BarDataSet(listOf(BarEntry(i.toFloat(), dataBlocks)), "")
            dataBar.setColors(blockColours)
            dataBar.setDrawValues(false)

            chData.addDataSet(dataBar)
        }

        formatAxes(chartView, xRange, "stacked")
        chartView.data = chData
        chartView.invalidate()
/*
        chartView.setOnTouchListener(touchListener)

        chartView.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {

                val xValue = xRange[h!!.x.toInt()]
                val fullDate = getYear(h!!.x.toInt(), xRange)
                val block = mapData[xValue]!!.keys.toList()[h!!.stackIndex]
                val yValue = com.getSubEl(mapData, xValue, block)?:0f
                val total = com.mapSum(mapData[xValue]!! as MutableMap)
                val perc = yValue/total

                showDataBox(block, fullDate, "${setup.prefCur} ${com.priceString(yValue)}  |  ${com.percentString(perc)}",
                    mapColours[block]?:0, touch, "Date Total", "${setup.prefCur} ${com.priceString(total)}")
            }
            override fun onNothingSelected() { }
        } )

 */
    }

    fun <T> formatAxes(chart : T, xValues : MutableList<String>, type : String, periodType: String="Daily", rightAxis : Boolean = false, yMin : Float? = 0f, suppressYear: Boolean = false) {

        val tChart = when(type) { "bar"         -> chart as BarChart
                                  "stacked"     -> chart as BarChart
                                  "horizontal"  -> chart as HorizontalBarChart
                                  "combo"       -> chart as CombinedChart
                                  else          -> chart as LineChart }

        val xAxis = tChart.getXAxis()
        val rightYAxis = tChart.getAxisRight()
        val leftYAxis = tChart.getAxisLeft()

        // Format x-axis values
        val axForm = object: IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if(value.toInt() >= xValues.size || value.toInt() < 0){ return "" }
                if(type=="horizontal"){ return xValues[value.toInt()] }
                return axisDate(xValues[value.toInt()], periodType, suppressYear)
            }
        }
        xAxis.setGranularity(1f)
        xAxis.setValueFormatter(axForm)
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
        xAxis.setDrawGridLines(false)
        //xAxis.setLabelCount(12, false)
        xAxis.yOffset = 0f
        xAxis.axisLineColor = com.themeColour(R.attr.textPrimary)
        xAxis.textColor = com.themeColour(R.attr.textPrimary)

        // Format y-axis
        leftYAxis.granularity = 1f
        leftYAxis.setDrawGridLines(true)
        leftYAxis.xOffset = 0f
        leftYAxis.axisLineColor = com.themeColour(R.attr.textPrimary)
        leftYAxis.textColor = com.themeColour(R.attr.textPrimary)

        rightYAxis.granularity = 1f
        rightYAxis.setEnabled(rightAxis)
        rightYAxis.setDrawGridLines(false)
        if(yMin != null) {
            leftYAxis.mAxisMinimum = yMin
            leftYAxis.axisMinimum = yMin
            rightYAxis.mAxisMinimum = yMin
            rightYAxis.axisMinimum = yMin
        }


        // Format descriptions
        tChart.legend.isEnabled = false
        tChart.getDescription().isEnabled = false
    }

    fun formatLine(dsLine : LineDataSet, colour : Int, dataPoints : Boolean = true, fill : Int? = null) {

        dsLine.lineWidth = 2f
        dsLine.fillAlpha = 90
        dsLine.circleHoleRadius = 4f

        //dsLine.circleHoleColor = ContextCompat.getColor(context, mapColours[incType]!!)
        dsLine.color = ContextCompat.getColor(context, colour)
        dsLine.setCircleColor(ContextCompat.getColor(context, colour))
        dsLine.setDrawCircles(dataPoints)
        if(fill != null){
            dsLine.setDrawFilled(true)
            dsLine.fillColor = ContextCompat.getColor(context, fill)
            dsLine.fillAlpha = 100
        }

        dsLine.setDrawValues(false)
    }

    fun drawLimitLine(lineAt : Float, label : String, colour : Int = R.color.black,
                      colText : Int = R.color.black, limPosition: String = "RT") : LimitLine {

        val lim = LimitLine(lineAt)

        lim.setLineColor(com.themeColour(R.attr.textPrimary))
        lim.setLineWidth(1f)

        lim.setLabel(label)
        lim.textColor = ContextCompat.getColor(context, colText)
        lim.labelPosition = when(limPosition){
            "RT"    -> LimitLine.LimitLabelPosition.RIGHT_TOP
            "LT"    -> LimitLine.LimitLabelPosition.LEFT_TOP
            "RB"    -> LimitLine.LimitLabelPosition.RIGHT_BOTTOM
            "LB"    -> LimitLine.LimitLabelPosition.LEFT_BOTTOM
            else    -> LimitLine.LimitLabelPosition.RIGHT_TOP
        }

        return lim
    }
/*
    fun showDataBox(dataName : String, valueText : String?, dataValue : String, dataColour : Int = 0,
                    location : Map<String, Float>? = null, valueText2 : String? = null, dataValue2 : String? = null)
    {

        val dialog = Dialog(context)

        val width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        val height = ConstraintLayout.LayoutParams.WRAP_CONTENT

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_chartpopup)
        dialog.window.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window.setLayout(width, height)

        if(location != null){
            //    dialog.window.attributes.gravity = Gravity.CENTER
            dialog.window.attributes.x = location["x"]!!.toInt()
            dialog.window.attributes.y = location["y"]!!.toInt()
        }

        dialog.dcpTitle.text = dataName
        dialog.dcpTitle.setTextColor(ContextCompat.getColor(context, dataColour))
        dialog.dcpValueText.text = (if(valueText == null) "Value" else valueText) + ":"
        dialog.dcpValue.text = dataValue

        if(valueText2 != null || dataValue2 != null){
            dialog.dcpValueText2.visibility = View.VISIBLE
            dialog.dcpValue2.visibility = View.VISIBLE
            dialog.dcpValueText2.text = valueText2 + ":"
            dialog.dcpValue2.text = dataValue2
        }

        dialog.show()

    }

 */

    fun getYear(xIndex : Int, xRange : List<String>) : String {

        val xValue = xRange[xIndex]

        if(xValue.length == 4 || xValue.split(" ").size == 2 || xValue.split("/").size == 3){
            return xRange[xIndex]
        }

        var i = xIndex - 1
        while(i >= 0){
            val week = xRange[i].split("/")
            val month = xRange[i].split(" ")
            if(week.size == 3){ return "$xValue/${week[2]}" }
            if(month.size == 2){ return "$xValue ${month[1]}" }
            i--
        }

        return ""

    }

}
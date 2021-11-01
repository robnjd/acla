package com.example.acla.backend

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.acla.R
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class CommonRoom(val context: Context) {

    val dFormatter = DateTimeFormatter.ofPattern("d")
    val dmFormatter = DateTimeFormatter.ofPattern("d MMM")
    val dmyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val monFormatter = DateTimeFormatter.ofPattern("MMM")
    val dmonFormatter = DateTimeFormatter.ofPattern("dd MMM")
    val dmonyFormatter = DateTimeFormatter.ofPattern("dd MMM YYYY")
    val monyFormatter = DateTimeFormatter.ofPattern("MMM YYYY")
    val monthyrFormatter = DateTimeFormatter.ofPattern("MMMM YYYY")
    val dmonthyrFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
/*
    fun addData(oldValue: Float, entry: List<String>, aggregation: String) : Float {

        when(aggregation) {
            "count"             -> return oldValue + 1
            "duration"          -> return oldValue + secondsFormat(entry[setup!!.mapCols[aggregation]!!])
            "measure"           -> when(entry[setup!!.mapCols["workout"]!!]){
                "Interval"  -> return oldValue + convertInterval(entry[setup.mapCols[aggregation]!!])
                "Run"       -> return oldValue + toFloat(entry[setup.mapCols[aggregation]!!])
                "Routine"   -> return oldValue + convertReps(entry[setup.mapCols[aggregation]!!])
            }
        }
        return -1f
    }
 */

    fun <K, A> appendMapList(map: MutableMap<K, MutableList<A>>, key: K, add: A) {
        if(key !in map){
            map[key] = mutableListOf<A>()
        }
        map[key]!!.add(add)
    }

    fun <T> arrayColumn(array: List<List<T>>, colNumber: Int) : List<T> {
        val lstColumn = mutableListOf<T>()
        array.forEach {
            lstColumn.add(it[colNumber])
        }
        return lstColumn
    }

    fun avgFormat(num: Float, decimals: Int = 1) : String {
        val list = num.toString().split(".")
        var dec = ""
        if(list.size == 2){
            dec += "."
            if(list[1].length < decimals) {
                dec += list[1]
            } else {
                dec += list[1].substring(0, decimals)
            }
        }

        if(decimals == 0 || dec.replace(".","").toInt() == 0){
            dec = ""
        }

        return list[0] + dec
    }

    fun boolize(str: String) : Boolean {

        if(str.toLowerCase() in listOf("true", "t")) {
            return true
        }
        return false
    }

    fun convertInterval(defInterval: String) : Float {

        // Gives Interval count in minutes

        val spDef = defInterval.split(" X ")
        val count = toFloat(spDef[0])
        val interval = spDef[1]

        return count*secondsFormat(interval,"M")
    }

    fun convertReps(defRoutine: String) : Float {
        val spDef = defRoutine.split(" X ")
        val reps = toFloat(spDef[0])
        val sets = toFloat(spDef[1])
        val vars = toFloat(if(spDef.lastIndex >= 2) spDef[2] else 1)

        return reps*sets*vars
    }

    fun countPeriods(startDate: LocalDate, endDate: LocalDate, timePeriod: String) : Int {
        var count = 0
        var testDate = LocalDate.parse(strPeriodBucket(startDate, timePeriod), dmyFormatter)
        val endPeriod = LocalDate.parse(strPeriodBucket(endDate, timePeriod), dmyFormatter)

        while(testDate.compareTo(endPeriod) < 0){
            count++
            testDate = when(timePeriod) {
                "Daily"     -> testDate.plusDays(1)
                "Weekly"    -> testDate.plusDays(7)
                "Monthly"   -> testDate.plusMonths(1)
                "Yearly"    -> testDate.plusYears(1)
                else        -> testDate
            }
            if(count>1000){ return -1 }
        }
        return count
    }

    fun <A, B, C> createSubMap(map: MutableMap<A, MutableMap<B, C>>, key1: A) :  MutableMap<A, MutableMap<B, C>> {

        if(key1 !in map.keys){ map[key1] = mutableMapOf() }

        return map
    }

    fun <A, B, C, D> createSubMaps(map: MutableMap<A, MutableMap<B, MutableMap<C, D>>>, key1: A, key2: B) :  MutableMap<A, MutableMap<B, MutableMap<C, D>>> {

        if(key1 !in map.keys){ map[key1] = mutableMapOf() }

        if(key2 !in map[key1]!!.keys){ map[key1]!![key2] = mutableMapOf() }

        return map
    }

    fun datePosition(item: LocalDate , l: List<LocalDate>) : Int {

        var pos = l.size

        for((i, ll) in l.withIndex()) {
            if(item < ll){
                pos = i
                break
            }
        }

        return pos

    }

    fun dateSort(unsorted : MutableList<List<String>>, sortColumn : Int = 0, reversed: Boolean = false) : MutableList<List<String>> {

        // Sorts the input data by date order (newest first) for the date in sortColumn

        var sorted = mutableListOf<List<String>>()

        for (uRow in unsorted) {

            val uDate = LocalDate.parse(uRow[sortColumn], dmyFormatter)
            var addIndex = sorted.size

            for ((i, sRow) in sorted.withIndex()) {
                val sDate = LocalDate.parse(sRow[sortColumn], dmyFormatter)
                if (uDate.compareTo(sDate) < 0) {
                    addIndex = i
                    break
                }
            }
            sorted.add(addIndex, uRow)
        }

        if(reversed){ sorted = sorted.reversed() as MutableList<List<String>> }

        return sorted
    }

    fun dateList(unsorted: Set<String>) : MutableList<String> {

        var list = mutableListOf<List<String>>()
        var outList = mutableListOf<String>()

        unsorted.forEach{ list.add(listOf(it)) }

        dateSort(list).forEach{ outList.add(it[0]) }

        return outList
    }

    fun dateRange(startDate: LocalDate, endDate: LocalDate, periodType: String) : List<String> {

        var date = LocalDate.parse(strPeriodBucket(startDate, periodType), dmyFormatter)
        val endPeriod = LocalDate.parse(strPeriodBucket(endDate, periodType), dmyFormatter)
        val lstDates = mutableListOf<String>()

        while (date.compareTo(endPeriod) <= 0) {
            lstDates.add(date.format(dmyFormatter))
            date = when(periodType) {
                "Daily"     -> date.plusDays(1)
                "Weekly"    -> date.plusDays(7)
                "Monthly"   -> date.plusMonths(1)
                "Yearly"    -> date.plusYears(1)
                else        -> date.plusDays(1)
            }
        }
        return lstDates
    }

    fun decap(str: String) : String {
        val start = str.substring(0,1)
        val rest = str.substring(1, str.length)
        return start + rest.toLowerCase()
    }

    fun formatSubdiv(date: LocalDate, period: String) : String {

        var form = when(period) {
            "Weekly"  -> date.dayOfWeek
            "Monthly" -> date.dayOfMonth
            "Yearly"  -> date.month.name
            else -> ""
        }.toString()

        if(form.length > 3){
            form =  form.substring(0,3)
        }
        return form
    }

    fun formatTime(timeView: EditText) : String? {

        if(timeView.text == null || timeView.text.toString() == ""){ return null }

        var spTime = timeView.text.toString().split(":") as MutableList<String>
        val maxVals = listOf(24f,60f,60f)
        var newTime = ""

        when(spTime.size) {
            1 -> spTime.addAll(listOf("00", "00"))
            2 -> spTime.add("00")
            3 -> spTime = spTime
            4 -> return null
        }

        for((i, t) in spTime.withIndex()) {
            if(t.length > 2){ return null }
            if(t.toFloat() > maxVals[i]){ return null }
            newTime += lead("$t:")
        }
        return newTime.substring(0,newTime.length-1)
    }

    fun <A, B, C> getSubEl(map : Map<A, Map<B, C>>, key1 : A, key2 : B) : C? {
        if(key1 in map.keys){
            if(key2 in map[key1]!!.keys){
                return map[key1]!![key2]!!
            }
        }
        return null
    }

    fun <A, B, C, D> getSubSubEl(map : Map<A, Map<B, Map<C, D>>>, key1 : A, key2 : B, key3: C) : D? {
        if(key1 in map.keys){
            if(key2 in map[key1]!!.keys){
                if(key3 in map[key1]!![key2]!!.keys) {
                    return map[key1]!![key2]!![key3]
                }
            }
        }
        return null
    }

    fun getHour(time: String) : Int {
        return time.split(":")[0].toInt()
    }
/*
    fun groupData(rawData: MutableList<List<String>>, dateGrouping:  String,
                  otherGrouping: String, aggregation: String) : Map<String, MutableMap<String, Float>> {

        val colDate = setup!!.mapCols["date"]!!
        val colOther = setup!!.mapCols[otherGrouping]!!

        val mapData = mutableMapOf<String, MutableMap<String, Float>>()

        for(entry in rawData){

            val date = periodBucket(LocalDate.parse(entry[colDate], dmyFormatter), dateGrouping)!!

            val other = entry[colOther]

            if(mapData[date] == null) {
                mapData[date] = mutableMapOf(other to addData(0f, entry, aggregation))
            } else {
                mapData[date]!![other] = addData(getSubEl(mapData, date, other)?:0f, entry, aggregation)
            }
        }

        return mapData
    }
 */

    fun inPeriod(date: String, startPeriod: LocalDate, endPeriod: LocalDate) : Boolean {

        val dateTest = LocalDate.parse(date, dmyFormatter)
        val after: Boolean
        val before: Boolean

        after = startPeriod.compareTo(dateTest) <= 0
        before = endPeriod.compareTo(dateTest) >= 0

        return after && before
    }

    fun inPrevYears(date: String, startDate: LocalDate, endDate: LocalDate, earliestDate: LocalDate, allTime: Boolean = false) : Long {

        var yrs = 0L
        val start =  if(allTime) endDate.withDayOfYear(1) else startDate
        val end = if(allTime) endDate.withDayOfYear(endDate.lengthOfYear()) else endDate

        var prevStart = start
        var prevEnd = end

        while(prevEnd >= earliestDate){
            //d("dash", "$date : $prevStart : $prevEnd : ${inPeriod(date, prevStart, prevEnd)}")
            if(inPeriod(date, prevStart, prevEnd)){ return yrs }
            yrs++
            prevStart = start.minusYears(yrs)
            prevEnd = end.minusYears(yrs)
        }
        return -1
    }

    fun <A, B, C> invertMap(map: Map<A, Map<B, C>>) : Map<B, Map<A, C>> {

        var inverted = mutableMapOf<B, MutableMap<A, C>>()

        for(subK in subKeys(map)){
            inverted[subK] = mutableMapOf()
            for(k in map.keys){
                val el = getSubEl(map, k, subK)
                if(el != null) { inverted[subK]!![k] = el }
            }
        }
        return inverted
    }

    fun <T> lead(number: T, leadCount: Int = 2, leadDigit: Char = '0') : String {
        return number.toString().padStart(leadCount, leadDigit)
    }

    fun listString(list: List<String>, sep: String = ",") : String {
        var outString = ""
        for((i,l) in list.withIndex()){
            outString += l + if(i != l.lastIndex) sep else ""
        }
        return  outString
    }

    fun <K> mapSum(map : Map<K,*>?, lstMinusKeys : List<K>? = null, subKey: String? = null) : Float {

        var total = 0f
        if(map == null){ return total }

        for ((k, v) in map) {
            val incr = toFloat(if(v is Map<*,*>) v[subKey] else v)
            val sgn = if(lstMinusKeys == null) 1 else if(k in lstMinusKeys) -1 else 1
            total += sgn*incr
        }
        return total
    }

    fun nullHint(edit: EditText) : String {
        val text = edit.text.toString()
        if(text == ""){
            return edit.hint.toString()
        }
        return text
    }

    fun periodBucket(d : LocalDate, type : String) : LocalDate? {

        // Gives the start date of the period bucket a given date is in.
        // e.g. for weekly report it will give the date of the Sunday before a specified date

        return when (type) {
            "Weekdaily" -> d
            "Daily"     -> d
            "Weekly"    -> d.minusDays((d.dayOfWeek.value - 1).toLong())
            "Monthly"   -> d.withDayOfMonth(1)
            "Yearly"    -> d.withDayOfYear(1)
            else        -> null
        }
    }

    fun strPeriodBucket(d : LocalDate, type : String) : String? {
        return if (type == "Weekdaily") {
            d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        } else {
            periodBucket(d, type)?.format(dmyFormatter)
        }
    }

    fun periodSubdiv(startDate : LocalDate, period : String) : MutableList<LocalDate> {

        // Returns list of subdivision of a given date period

        val lstDates = mutableListOf<LocalDate>()

        var addDate = startDate
        val endDate = when(period) {
            "Weekly"    -> startDate.plusWeeks(1L)
            "Monthly"   -> startDate.plusMonths(1L)
            "Yearly"    -> startDate.plusYears(1L)
            else        -> startDate
        }

        while(addDate < endDate){
            lstDates.add(addDate)
            addDate = when(period){
                "Weekly"  -> { addDate.plusDays(1L) }
                "Monthly" -> { addDate.plusDays(1L) }
                "Yearly"  -> { addDate.plusMonths(1L) }
                else    -> { addDate.plusDays(1L) }
            }
        }
        return lstDates
    }
/*
    fun readFromFile(): MutableList<List<String>> {

        val file = File(setup!!.filePath, setup.fileName)

        val inputStream = FileInputStream(file)
        val bufferedReader = BufferedReader(
            InputStreamReader(inputStream)
        )

        val lstFile = mutableListOf<List<String>>()
        bufferedReader.forEachLine {
            lstFile.add(0,it.split("|"))
        }

        inputStream.close()

        return lstFile
    }
 */

    fun showCalendar(DateView : TextView) {

        val dateString = DateView.text.toString()
        val openDate = LocalDate.parse(dateString, dmyFormatter)

        val dpd = DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                DateView.text = "${lead(dayOfMonth)}/${lead(monthOfYear + 1)}/$year"
            },
            openDate.year,
            openDate.month.value - 1,
            openDate.dayOfMonth
        )

        dpd.show()
    }

    fun showClock(timeView: TextView) {

        val spTime = timeView.text.toString().split(":")
        val hr = if(spTime[0] == "") 0 else spTime[0].toInt()
        val min = if(spTime.size < 2 || spTime[1] == "") 0 else spTime[1].toInt()

        val clock = TimePickerDialog(context, object : TimePickerDialog.OnTimeSetListener {
            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                timeView.text = String.format("%d:%d", hourOfDay, minute) + ":00"
            }
        }, hr, min, true)
        clock.show()
    }


    fun spinnerFormat(spinner : Spinner, list : MutableList<String>, options : Map<String, Int>, layDropdown : Int = android.R.layout.simple_list_item_1) {

        val ddList = list as List<String?>

        spinner.adapter = object : ArrayAdapter<String?>(context, layDropdown, ddList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val vw = super.getView(position, convertView, parent)
                val vwText = vw.findViewById<TextView>(android.R.id.text1)

                if("gravity" in options){
                    vwText.gravity = options["gravity"]!!
                }
                if("textSize" in options){
                    vwText.textSize = options["textSize"]!!.toFloat()
                }
                if("textColour" in options){
                    vwText.setTextColor(ContextCompat.getColor(context, options["textColour"]!!))
                }
                return vw
            }
        }
    }

    fun <A, B, C> subKeys(map : Map<A, Map<B, C>>) : List<B> {

        var lstKeys = mutableListOf<B>()

        for(k in map.keys){
            for (subK in map[k]!!.keys) {
                if (subK in lstKeys) { continue }
                lstKeys.add(subK)
            }
        }
        return lstKeys
    }

    fun <T> switch(input:T, list: List<T>) : T {
        var i = list.indexOf(input) + 1

        if(i == list.size){ i = 0 }

        return list[i]
    }

    fun timeCalc(t1: String, t2: String, calculation: String="difference") : Float {

        val T1 = secondsFormat(t1)
        val T2 = secondsFormat(t2)

        val calc = when(calculation) {
            "difference"    -> T2 - T1
            "sum"           -> T1 + T2
            "average"       -> (T1 + T2)/2
            else            -> 0f
        }

        return calc
    }

    fun timeSum(times: Map<String, Map<String, String>>, subKey: String) : Float {
        var total = 0f
        for((k, v) in times){
            total += secondsFormat(v[subKey]?:"")
        }
        return total
    }

    fun <V : View> tint(view : V, colour : Int, foreback : String = "fg") {

        val col = ContextCompat.getColor(context, colour)

        when (foreback) {
            "fg", "fore", "foreground"  ->{ view as ImageView
                view.setColorFilter(col, PorterDuff.Mode.SRC_IN) }
            "bg", "back", "background"  ->  view.background.setColorFilter(col, PorterDuff.Mode.SRC_IN)
        }

    }

    fun secondsFormat(time: String, firstPeriod: String="H") : Float {

        val spTime = time.split(":")

        val H = if(firstPeriod == "H") toFloat(spTime[0])*3600 else 0f
        val M = when(firstPeriod) {
            "H"     -> if(spTime.size > 1) toFloat(spTime[1]) * 60 else 0f
            "M"     -> toFloat(spTime[0]) * 60
            "S"     -> if(spTime.size > 1) return -1f else 0f
            else    -> 0f
        }
        val S = when(firstPeriod) {
            "H"     -> if(spTime.size > 2) toFloat(spTime[2]) else 0f
            "M"     -> if(spTime.size > 1) toFloat(spTime[1]) else 0f
            "S"     -> if(spTime.size > 1) return -1f else toFloat(spTime[0])
            else    -> 0f
        }

        return H + M + S
    }

    fun timeFormat(seconds: Float) : String {

        val calcH = Math.floor((seconds / 3600).toDouble())
        val calcM = Math.floor((seconds - calcH * 3600) / 60)
        val calcS = seconds - calcH*3600 - calcM*60

        val H = calcH.toString().split(".")[0].padStart(2, '0')
        val M = calcM.toString().split(".")[0].padStart(2, '0')
        val S = calcS.toString().split(".")[0].padStart(2, '0')

        return "$H:$M:$S"
    }

    fun <T> toFloat(num: T) : Float {
        if(num == null || num.toString() == ""){ return 0f }
        return num.toString().replace(",", "").toFloat()
    }

    fun <T> toLong(num: T) : Long {
        if(num == null || num.toString() == ""){ return 0L }
        return num.toString().replace(",", "").toLong()
    }

    fun viewId(viewName: String) : Int {

        val res = context.getResources();
        val id = res.getIdentifier(viewName, "id", context.getPackageName())

        return id
    }

    fun workoutIcon(workout: String, iconView: ImageView? = null) : Map<String, Int> {

        var icon = mutableMapOf("col" to 0, "img" to 0)

        when(workout){
            "Interval"  ->{ icon["col"] = R.color.blue
                icon["img"] = R.drawable.ic_interval
            }
            "Run"       ->{ icon["col"] = R.color.green
                icon["img"] = R.drawable.ic_run
            }
            "Routine"   ->{ icon["col"] = R.color.purple
                icon["img"] = R.drawable.ic_rep
            }
        }

        if(iconView != null) {
            iconView.setImageResource(icon["img"]!!)
            tint(iconView, icon["col"]!!, "fg")
            tint(iconView, icon["col"]!!, "bg")
        }

        return icon
    }
/*
    fun writeToFile(list: MutableList<List<String>>, overwrite: Boolean = false) {

        val file = File(setup!!.filePath, setup.fileName)

        val ordList = dateSort(list,0)

        try {
            if(overwrite) {
                file.printWriter().use { out ->
                    ordList.forEach{ out.println(listString(it, "|")) }
                }
            } else {
                FileOutputStream(file, true).bufferedWriter().use { out ->
                    ordList.forEach{ out.appendln(listString(it, "|")) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Write Failure: $e", Toast.LENGTH_LONG).show()
        }
    }

}
 */
}
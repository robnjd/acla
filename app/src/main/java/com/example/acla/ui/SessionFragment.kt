package com.example.acla.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import com.example.acla.backend.DatabaseHelper
import com.example.acla.backend.Session
import kotlinx.android.synthetic.main.frag_entry.view.*
import kotlinx.android.synthetic.main.frag_session.view.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class SessionFragment : Fragment() {

    val vmMain: MainViewModel by activityViewModels()
    val dmyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    lateinit var com: CommonRoom
    lateinit var frag: View
    lateinit var tone : ToneGenerator
    lateinit var db: DatabaseHelper

    var interval = "rest"
    var workCounter = 0
    var mainStop = 0L
    var splitStop = 0L
    var mainTimer = "stopped"
    var splitTimer = "stopped"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.frag_session, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())
        frag = view
        
        val today = LocalDate.now()

        var workout = "Routine"
        var showSwitch = false

        // Initialize
        frag.sesDate.text = today.format(dmyFormatter)
        frag.sesChrono.typeface = ResourcesCompat.getFont(requireContext(), R.font.black_ops_one)
        frag.sesChrono.textSize = 80f
        frag.sesChronoSplit.typeface = ResourcesCompat.getFont(requireContext(), R.font.black_ops_one)
        frag.sesChronoSplit.textSize = 60f
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        frag.sesChronoSplit.base = SystemClock.elapsedRealtime()

        // Setup Listeners
        frag.sesLayoutWorkout.setOnClickListener {

            workout = com.switch(workout, listOf("Routine", "Interval", "Run"))
            frag.sesWorkoutTitle.text = workout
            val icon = com.workoutIcon(workout)
            frag.sesWorkoutIcon.setImageResource(icon["img"]!!)
            com.tint(frag.sesLayoutWorkout, icon["col"]?:0, "bg")

            when(workout) {
                "Interval" -> {
                    frag.sesIntervalLayout.visibility = View.VISIBLE
                    frag.sesRunLayout.visibility = View.GONE
                    frag.sesRoutineLayout.visibility = View.GONE
                    if(showSwitch) frag.sesLaySplit.visibility = View.VISIBLE
                }
                "Run"       -> {
                    frag.sesIntervalLayout.visibility = View.GONE
                    frag.sesRunLayout.visibility = View.VISIBLE
                    frag.sesRoutineLayout.visibility = View.GONE
                    frag.sesLaySplit.visibility = View.GONE
                }
                "Routine"   -> {
                    frag.sesRoutineLayout.visibility = View.VISIBLE
                    frag.sesIntervalLayout.visibility = View.GONE
                    frag.sesRunLayout.visibility = View.GONE
                    frag.sesLaySplit.visibility = View.GONE
                }
            }
        }

        frag.sesSave.setOnClickListener {

            val newSession = Session()
            newSession.date = LocalDate.parse(frag.sesDate.text.toString(), com.dmyFormatter)
            newSession.workout = workout
            newSession.start = LocalTime.parse(frag.sesStartTime.text.toString(), newSession.hms)
            newSession.finish = LocalTime.parse(frag.sesFinishTime.text.toString(), newSession.hms)
            newSession.duration = frag.sesDuration.text.toString()
            newSession.measure = when(workout) {
                "Interval"  -> "${com.nullHint(frag.sesCount)} X ${com.nullHint(frag.sesIntervalMins)}:${com.nullHint(frag.sesIntervalSecs).padStart(2,'0')}"
                "Run"       -> com.nullHint(frag.sesDistance)
                "Routine"   -> "${com.nullHint(frag.sesReps)} X ${com.nullHint(frag.sesSets)} X ${com.nullHint(frag.sesVars)}"
                else        -> ""
            }

            db.insertData("sessions", newSession)
            Toast.makeText(requireContext(), "Session Saved.", Toast.LENGTH_LONG).show()
            frag.sesReset.callOnClick()
        }

        frag.sesReset.setOnClickListener {
            // Reset Main Timer
            if(mainTimer == "running"){
                mainTimer = timerSwitch(mainTimer)
            }
            frag.sesChrono.base = SystemClock.elapsedRealtime()
            mainStop = 0
            frag.sesStartTime.text = getString(R.string.zero_time)
            frag.sesFinishTime.text = getString(R.string.zero_time)
            frag.sesDuration.text = getString(R.string.zero_time)
            frag.sesDistance.text.clear()

            // Reset Split Timer
            if(splitTimer == "running"){
                splitTimer = splitSwitch(splitTimer)
            }
            frag.sesChronoSplit.base = SystemClock.elapsedRealtime()
            interval = "rest"
            splitStop = 0L
            workCounter = 0

            // Reset FABs
            frag.sesReset.visibility = View.INVISIBLE
            frag.sesSave.visibility = View.INVISIBLE
        }

        frag.sesSplitPlay.setOnClickListener {
            if(mainTimer == "running"){
                splitTimer = splitSwitch(splitTimer)
            }
        }

        frag.sesSplitRestart.setOnClickListener {
            val startTime = when(interval){ "work" -> com.toLong(com.nullHint(frag.sesIntervalMins))*60 + com.toLong(com.nullHint(frag.sesIntervalSecs))
                "rest" -> 10L
                else   -> 0L }
            frag.sesChronoSplit.base = SystemClock.elapsedRealtime() + startTime*(1000L)
        }

        frag.sesChronoSplit.setOnChronometerTickListener {

            if(frag.sesChronoSplit.base < SystemClock.elapsedRealtime()) {
                val sound = when(interval){
                    "work"  -> ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
                    "rest"  -> ToneGenerator.TONE_CDMA_HIGH_L
                    else    -> ToneGenerator.TONE_CDMA_ONE_MIN_BEEP
                }
                tone.startTone(sound, 500)
                changeInterval()
            }
        }

        frag.sesSplitSwitch.setOnClickListener {
            showSwitch = !showSwitch
            if(showSwitch){
                frag.sesLaySplit.visibility = View.VISIBLE
                frag.sesSplitSwitch.setImageResource(R.drawable.ic_bell_off)
            } else {
                frag.sesLaySplit.visibility = View.GONE
                frag.sesSplitSwitch.setImageResource(R.drawable.ic_bell)
                if(splitTimer == "running"){
                    splitTimer = splitSwitch(splitTimer)
                }
            }
        }

    }

    fun clickFAB() {
        mainTimer = timerSwitch(mainTimer)
        if(mainTimer != splitTimer){
            splitTimer = splitSwitch(splitTimer)
        }
    }

    fun timerSwitch(state: String) : String {

        val time = LocalDateTime.now().toLocalTime().toString()
        val newState = com.switch(state, listOf("stopped", "running"))

        when (newState) {
            "running" -> {
                frag.sesChrono.base = SystemClock.elapsedRealtime() + mainStop
                frag.sesChrono.start()

                frag.sesSplitPlay.callOnClick()

                frag.sesStartTime.text = time.split(".")[0]

                //fabTimer.setImageResource(R.drawable.ic_pause)
                vmMain.fabFunction.value = "pause"
                frag.sesReset.visibility = View.INVISIBLE
                frag.sesSave.visibility = View.INVISIBLE
            }
            "stopped" -> {
                frag.sesChrono.stop()
                mainStop = frag.sesChrono.base - SystemClock.elapsedRealtime()

                frag.sesFinishTime.text = time.split(".")[0]
                frag.sesDuration.text = com.timeFormat(com.timeCalc(frag.sesStartTime.text.toString(),frag.sesFinishTime.text.toString()))

                //fabTimer.setImageResource(R.drawable.ic_timer)
                vmMain.fabFunction.value = "start"
                frag.sesReset.visibility = View.VISIBLE
                frag.sesSave.visibility = View.VISIBLE
            }
        }

        return newState
    }

    fun splitSwitch(state: String) : String {

        val newState = com.switch(state, listOf("stopped", "running"))

        when (newState) {
            "stopped" -> {
                frag.sesChronoSplit.stop()
                splitStop = frag.sesChronoSplit.base - SystemClock.elapsedRealtime()
                com.tint(frag.sesLaySplit, R.color.textGrey, "bg")
                com.tint(frag.sesSplitPlay, R.color.textGrey, "fg")
                com.tint(frag.sesSplitRestart, R.color.textGrey, "fg")
                frag.sesSplitText.text = "STOP"
                frag.sesSplitPlay.setImageResource(R.drawable.ic_play)
            }

            "running" -> {
                changeInterval(splitStop, false)
                splitStop = 0L
                frag.sesSplitPlay.setImageResource(R.drawable.ic_pause)
            }
        }

        return newState
    }

    fun changeInterval(start: Long = 0L, flip: Boolean = true) {

        interval = if(flip) com.switch(interval, listOf("work", "rest")) else interval

        var col = R.color.grey
        var msg = ""
        var startTime = 0L

        when(interval) {
            "work" -> {
                col = R.color.green
                if(flip) { workCounter++ }
                msg = "WORK: $workCounter"
                startTime = com.toLong(com.nullHint(frag.sesIntervalMins))*60 + com.toLong(com.nullHint(frag.sesIntervalSecs))
            }

            "rest" -> {
                col = R.color.blue
                msg = "REST"
                startTime = 10L
            }
        }

        com.tint(frag.sesLaySplit, col, "bg")
        com.tint(frag.sesSplitPlay, col, "fg")
        com.tint(frag.sesSplitRestart, col, "fg")
        frag.sesSplitText.text = msg

        frag.sesChronoSplit.base = SystemClock.elapsedRealtime() + if(start == 0L) startTime*(1000L) else start
        frag.sesChronoSplit.isCountDown = true
        frag.sesChronoSplit.start()
    }

}
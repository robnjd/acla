package com.example.acla.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log.d
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import com.example.acla.backend.DatabaseHelper
import com.example.acla.backend.Session
import kotlinx.android.synthetic.main.frag_entry.view.*
import java.time.LocalDate
import java.time.LocalTime

class EntryDialog(val sesh: Session?=null) : DialogFragment() {

    val TAG = "EntryDialog"

    lateinit var com: CommonRoom
    lateinit var frag: View
    lateinit var db: DatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog?.show()
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)

        return inflater.inflate(R.layout.frag_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())
        frag = view

        val calcDuration = object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val start = frag.entStart.text.toString()
                val finish = frag.entFinish.text.toString()
                frag.entDuration.text = com.timeFormat(com.timeCalc(start, finish, "difference"))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        }

        var workout = "Interval"

        if(sesh != null) {
            workout = sesh.workout
            frag.entDate.text = sesh.date.format(com.daydmonyFormatter)
            frag.entStart.text = sesh.start.format(sesh.hms)
            frag.entFinish.text = sesh.finish.format(sesh.hms)
            frag.entDuration.text = sesh.duration

            when (sesh.workout) {
                "Interval"  ->{ val interval = sesh.measure.split(" X ")
                                frag.entCount.setText(interval[0])
                                if (interval.size > 1) {
                                    val time = interval[1].split(":")
                                    frag.entMins.setText(time[0])
                                    if (time.size > 1) {
                                        frag.entSecs.setText(time[1])
                                    }
                                } }
                "Run"       -> frag.entDistance.setText(sesh.measure)
                "Routine"   ->{ val repset = sesh.measure.split(" X ")
                                frag.entReps.setText(repset[0])
                                frag.entSets.setText(repset[1])
                                frag.entVars.setText(if(repset.lastIndex >= 2) repset[2] else "1" )}
            }
        } else {
            frag.entDate.text = LocalDate.now().format(com.daydmonyFormatter)
        }

        changeWorkout(workout, false)

        frag.entIcon.setOnClickListener {
            workout = changeWorkout(workout)
        }

        frag.entDate.setOnClickListener { com.showCalendar(it as TextView, com.daydmonyFormatter) }

        frag.entStart.setOnClickListener {
            com.showClock(frag.entStart)
        }

        frag.entFinish.setOnClickListener {
             com.showClock(frag.entFinish)
        }

        frag.entStart.addTextChangedListener(calcDuration)

        frag.entFinish.addTextChangedListener(calcDuration)

        frag.entSave.setOnClickListener {
            val newSession = Session()
            newSession.workout = workout
            newSession.date = LocalDate.parse(frag.entDate.text.toString(), com.daydmonyFormatter)
            newSession.start = LocalTime.parse(frag.entStart.text.toString(), newSession.hms)
            newSession.finish = LocalTime.parse(frag.entFinish.text.toString(), newSession.hms)
            newSession.duration = frag.entDuration.text.toString()
            newSession.measure = when(workout) {
                "Interval"  -> "${com.nullHint(frag.entCount)} X ${com.nullHint(frag.entMins)}:${com.nullHint(frag.entSecs).padStart(2,'0')}"
                "Run"       -> com.nullHint(frag.entDistance)
                "Routine"   -> "${com.nullHint(frag.entReps)} X ${com.nullHint(frag.entSets)} X ${com.nullHint(frag.entVars)}"
                else        -> ""
            }
            d(TAG, newSession.toString())

            if(sesh == null) {
                db.insertData("sessions", newSession)
            } else {
                db.updateSesion(sesh, newSession)
            }
            Toast.makeText(requireContext(), "Session Saved.", Toast.LENGTH_LONG).show()
            targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, null)
            dialog?.dismiss()
        }
        
    }

    private fun changeWorkout(workout: String, flip: Boolean = true) : String {

        val newWorkout = if(flip) com.switch(workout, listOf("Interval", "Run", "Routine")) else workout
        com.workoutIcon(newWorkout, frag.entIcon)

        when(newWorkout) {
            "Interval"  ->{ frag.entIntervalLayout.visibility = View.VISIBLE
                            frag.entRunLayout.visibility = View.GONE
                            frag.entRoutineLayout.visibility = View.GONE }
            "Run"       ->{ frag.entIntervalLayout.visibility = View.GONE
                            frag.entRunLayout.visibility = View.VISIBLE
                            frag.entRoutineLayout.visibility = View.GONE }
            "Routine"   ->{ frag.entIntervalLayout.visibility = View.GONE
                            frag.entRunLayout.visibility = View.GONE
                            frag.entRoutineLayout.visibility = View.VISIBLE }
        }
        return newWorkout
    }

}
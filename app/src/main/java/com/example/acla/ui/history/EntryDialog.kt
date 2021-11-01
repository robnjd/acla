package com.example.acla.ui.history

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import com.example.acla.backend.DatabaseHelper
import com.example.acla.backend.Session
import com.example.acla.backend.TableAdapter
import kotlinx.android.synthetic.main.frag_befter.view.*
import kotlinx.android.synthetic.main.frag_entry.view.*
import kotlinx.android.synthetic.main.frag_tracker.view.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class EntryDialog(val sesh: Session?=null) : DialogFragment() {

    val TAG = "EntyDialog"

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
                val start = frag.dhStart.text.toString()
                val finish = frag.dhFinish.text.toString()
                frag.dhDuration.text = com.timeFormat(com.timeCalc(start, finish, "difference"))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        }

        var workout = "Interval"

        if(sesh != null) {
            workout = sesh.workout
            frag.dhDate.text = sesh.date.format(com.dmyFormatter)
            frag.dhStart.text = sesh.start.format(sesh.hms)
            frag.dhFinish.text = sesh.finish.format(sesh.hms)
            frag.dhDuration.text = sesh.duration

            when (sesh.workout) {
                "Interval"  ->{ val interval = sesh.measure.split(" X ")
                                frag.dhCount.setText(interval[0])
                                if (interval.size > 1) {
                                    val time = interval[1].split(":")
                                    frag.dhMins.setText(time[0])
                                    if (time.size > 1) {
                                        frag.dhSecs.setText(time[1])
                                    }
                                } }
                "Run"       -> frag.dhDistance.setText(sesh.measure)
                "Routine"   ->{ val repset = sesh.measure.split(" X ")
                                frag.dhReps.setText(repset[0])
                                frag.dhSets.setText(repset[1])
                                frag.dhVars.setText(if(repset.lastIndex >= 2) repset[2] else "1" )}
            }
        } else {
            frag.dhDate.text = LocalDate.now().format(com.dmyFormatter)
        }

        changeWorkout(workout, false)

        frag.dhIcon.setOnClickListener {
            workout = changeWorkout(workout)
        }

        frag.dhDate.setOnClickListener { com.showCalendar(it as TextView) }

        frag.dhStart.setOnClickListener {
            com.showClock(frag.dhStart)
        }

        frag.dhFinish.setOnClickListener {
             com.showClock(frag.dhFinish)
        }

        frag.dhStart.addTextChangedListener(calcDuration)

        frag.dhFinish.addTextChangedListener(calcDuration)

        frag.dhSave.setOnClickListener {
            val newSession = Session()
            newSession.date = LocalDate.parse(frag.dhDate.text.toString(), com.dmyFormatter)
            newSession.start = LocalTime.parse(frag.dhStart.text.toString(), newSession.hms)
            newSession.finish = LocalTime.parse(frag.dhFinish.text.toString(), newSession.hms)
            newSession.duration = frag.dhDuration.text.toString()
            newSession.measure = when(workout) {
                "Interval"  -> "${com.nullHint(frag.dhCount)} X ${com.nullHint(frag.dhMins)}:${com.nullHint(frag.dhSecs).padStart(2,'0')}"
                "Run"       -> com.nullHint(frag.dhDistance)
                "Routine"   -> "${com.nullHint(frag.dhReps)} X ${com.nullHint(frag.dhSets)} X ${com.nullHint(frag.dhVars)}"
                else        -> ""
            }

            if(sesh == null) {
                db.insertData("session", newSession)
            } else {
                db.updateSesion(sesh, newSession)
            }
            Toast.makeText(requireContext(), "Session Saved.", Toast.LENGTH_LONG).show()
            targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, null)
        }
        
    }

    private fun changeWorkout(workout: String, flip: Boolean = true) : String {

        val newWorkout = if(flip) com.switch(workout, listOf("Interval", "Run", "Routine")) else workout
        com.workoutIcon(newWorkout, frag.dhIcon)

        when(newWorkout) {
            "Interval"  ->{ frag.dhIntervalLayout.visibility = View.VISIBLE
                            frag.dhRunLayout.visibility = View.GONE
                            frag.dhRoutineLayout.visibility = View.GONE }
            "Run"       ->{ frag.dhIntervalLayout.visibility = View.GONE
                            frag.dhRunLayout.visibility = View.VISIBLE
                            frag.dhRoutineLayout.visibility = View.GONE }
            "Routine"   ->{ frag.dhIntervalLayout.visibility = View.GONE
                            frag.dhRunLayout.visibility = View.GONE
                            frag.dhRoutineLayout.visibility = View.VISIBLE }
        }
        return newWorkout
    }

}
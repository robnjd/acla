package com.example.acla.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_settings.view.*
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class SettingsFragment : Fragment() {
    val TAG = "SettingsFragment"
    lateinit var com: CommonRoom
    lateinit var frag: View
    lateinit var db: DatabaseHelper

    var filePath = File("")
    var import = ""
    var prefs = mutableMapOf<String, String>()

    val lstWorkouts = listOf(mapOf("text" to "Interval", "icon" to R.drawable.ic_interval, "colour" to R.color.blue),
                             mapOf("text" to "Run", "icon" to R.drawable.ic_run, "colour" to R.color.green),
                             mapOf("text" to "Routine", "icon" to R.drawable.ic_rep, "colour" to R.color.purple) )

    val IMPORT = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        com = CommonRoom(requireContext())
        db = DatabaseHelper(requireContext())
        filePath = requireContext().getExternalFilesDir(null)!!

        prefs = db.readPrefs()

        val lstImport = getBackupFiles()

        frag.setWorkout.adapter = IconDropdown(requireContext(), lstWorkouts)

        frag.setImportTable.adapter = TableAdapter(requireContext(), lstImport, "import")

        frag.setImportTable.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            import = lstImport[position]
            val dialogConfirm = ConfirmDialog("Import sessions from $import?")
            dialogConfirm.setTargetFragment(this, IMPORT)
            dialogConfirm.show(parentFragmentManager, "Confirm")
        }

        frag.setOnClickListener {
            savePrefs()
        }

        d(TAG, prefs.toString())
        setPrefs()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data == null || resultCode != AppCompatActivity.RESULT_OK) return

        when(requestCode) {
            IMPORT  -> if(data.extras?.getString("reply")?:"no" == "yes") handleImport()
        }
    }

    private fun getBackupFiles() : MutableList<String> {
        val lstBackups = mutableListOf<String>()
        d(TAG, filePath.toString())
        filePath.listFiles().forEach{
            if(it.isFile){
                d(TAG, it.nameWithoutExtension)
                val fileLocal = it.name
                lstBackups.add(fileLocal)
            }
        }
        return lstBackups
    }

    private fun handleImport() {
        var inserts = 0

        for(line in readFromFile(import)) {
            val sesh = Session()
            sesh.date = LocalDate.parse(line[0], com.dmyFormatter)
            sesh.workout = if(line.isNotEmpty()) line[1] else continue
            sesh.start = LocalTime.parse(if(line.size > 1) line[2] else "00:00:00", sesh.hms)
            sesh.finish = LocalTime.parse(if(line.size > 2) line[3] else "00:00:00", sesh.hms)
            sesh.duration = if(line.size > 3) line[4] else "00:00:00"
            sesh.measure = if(line.size > 4) line[5] else ""
            val exists = db.readSessions("date = '${sesh.date}' AND start = '${sesh.start}'")
            if(exists.isNullOrEmpty()) {
                db.insertData("sessions", sesh)
                inserts += 1
            }

        }
        Toast.makeText(requireContext(), "Imported $inserts sessions from $import", Toast.LENGTH_LONG).show()
        import = ""
    }

    private fun readFromFile(filename: String): MutableList<List<String>> {

        val file = File(filePath, "/$filename")

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

    private fun savePrefs() {
        val newPrefs = mutableMapOf<String, String>()
        newPrefs["workout"] = (frag.setWorkout.selectedItem as Map<String, String>)["text"]!!

        newPrefs["measureInterval"] = "${frag.setIntervalSets.text} X ${frag.setIntervalMins.text}:${frag.setIntervalSecs.text}"
        newPrefs["measureRun"] = "${frag.setRunDistance.text}"
        newPrefs["measureRoutine"] = "${frag.setRoutineReps.text} X ${frag.setRoutineSets.text} X ${frag.setRoutineForms.text}"

        db.updatePrefs(newPrefs)
        Toast.makeText(requireContext(), "Preferences saved.", Toast.LENGTH_SHORT).show()
    }

    private fun setPrefs() {
        val workouts = listOf("Interval", "Run", "Routine")
        frag.setWorkout.setSelection(workouts.indexOf(prefs["workout"]?:"Interval"))

        val mInterval = prefs["measureInterval"]?.split(" X ")
        val mRun = prefs["measureRun"]
        val mRoutine = prefs["measureRoutine"]?.split(" X ")

        if(mInterval?.size ?: 0 > 1) {
            frag.setIntervalSets.setText(mInterval!![0])
            frag.setIntervalMins.setText(mInterval[1].split(":")[0])
            frag.setIntervalSecs.setText(mInterval[1].split(":")[1])
        }
        frag.setRunDistance.setText(mRun)
        if(mRoutine?.size ?: 0 > 2) {
            frag.setRoutineReps.setText(mRoutine!![0])
            frag.setRoutineSets.setText(mRoutine!![1])
            frag.setRoutineForms.setText(mRoutine!![2])
        }

    }

}
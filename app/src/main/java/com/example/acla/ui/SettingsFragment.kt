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
import com.example.acla.backend.CommonRoom
import com.example.acla.backend.DatabaseHelper
import com.example.acla.backend.Session
import com.example.acla.backend.TableAdapter
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
    lateinit var db: DatabaseHelper
    lateinit var frag: View

    var filePath = File("")
    var import = ""

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

        val lstImport = getBackupFiles()

        frag.setImportTable.adapter = TableAdapter(requireContext(), lstImport, "import")

        frag.setImportTable.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            import = lstImport[position]
            val dialogConfirm = ConfirmDialog("Import sessions from $import?")
            dialogConfirm.setTargetFragment(this, IMPORT)
            dialogConfirm.show(parentFragmentManager, "Confirm")
        }

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
            sesh.date = LocalDate.parse(line[0], sesh.ymd)
            sesh.workout = if(line.isNotEmpty()) line[1] else continue
            sesh.start = LocalTime.parse(if(line.size > 1) line[2] else "00:00:00", sesh.hms)
            sesh.finish = LocalTime.parse(if(line.size > 2) line[3] else "00:00:00", sesh.hms)
            sesh.duration = if(line.size > 3) line[4] else "00:00:00"
            sesh.measure = if(line.size > 4) line[5] else ""
            db.insertData("sessions", sesh)
            inserts += 1
        }
        Toast.makeText(requireContext(), "Imported $inserts sessions from $import", Toast.LENGTH_LONG).show()
        import = ""
    }

    fun readFromFile(filename: String): MutableList<List<String>> {

        val file = File(filePath, "/$filename.txt")

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

}
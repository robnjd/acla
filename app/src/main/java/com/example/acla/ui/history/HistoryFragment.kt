package com.example.acla.ui.history

import android.content.Intent
import android.os.Bundle
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
import com.example.acla.ui.ConfirmDialog
import kotlinx.android.synthetic.main.frag_history.view.*

class HistoryFragment : Fragment() {

    lateinit var com: CommonRoom
    lateinit var db: DatabaseHelper
    lateinit var frag: View

    var delete: Session?= null

    val ENTRY = 0
    val CONFIRM = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.frag_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        frag = view
        com = CommonRoom(requireActivity())
        db = DatabaseHelper(requireContext())

        val lstHistory = db.readSessions()
        frag.hstTable.adapter = TableAdapter(requireContext(), lstHistory, "history")

        // Setup Listeners
        frag.hstTable.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val dialogEntry = EntryDialog(lstHistory[position])
            dialogEntry.setTargetFragment(this, ENTRY)
            dialogEntry.show(parentFragmentManager, "Entry")
        }

        frag.hstTable.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            delete = lstHistory[position]
            val dialogConfirm = ConfirmDialog("Delete workout from ${delete?.date?.format(com.dmyFormatter)}?")
            dialogConfirm.setTargetFragment(this, CONFIRM)
            dialogConfirm.show(parentFragmentManager, "Confirm")
            true
        }

    }

    fun clickFAB() {
        val dialogEntry = EntryDialog()
        dialogEntry.setTargetFragment(this, ENTRY)
        dialogEntry.show(parentFragmentManager, "Entry")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data == null || resultCode != AppCompatActivity.RESULT_OK) return

        when(requestCode) {
            CONFIRM -> if(data.extras?.getString("reply")?:"no" == "yes") handleDelete()
        }
    }

    fun handleDelete() {
        if(delete == null) {
            Toast.makeText(requireContext(), "No session selected.", Toast.LENGTH_SHORT).show()
            return
        }
        db.deleteSession(delete!!)
    }

}
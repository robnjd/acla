package com.example.acla.ui.tracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import kotlinx.android.synthetic.main.frag_befter.view.*
import kotlinx.android.synthetic.main.frag_tracker.view.*
import java.time.LocalDate

class BefterDialog(val ordered: MutableList<LocalDate>) : DialogFragment() {

    val TAG = "BefterDialog"

    val vmMain: MainViewModel by activityViewModels()
    lateinit var com: CommonRoom
    lateinit var frag: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog?.show()
        val width = WindowManager.LayoutParams.WRAP_CONTENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)

        return inflater.inflate(R.layout.frag_befter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        com = CommonRoom(requireContext())
        frag = view
        val ordForm = mutableListOf<String>()

        ordered.forEach {
            ordForm.add(it.format(com.dmyFormatter))
        }

        val spinOpts = mapOf("gravity" to Gravity.CENTER, "textSize" to 16, "textColour" to R.color.white)

        com.spinnerFormat(frag.dbBefore, ordForm, spinOpts)
        com.spinnerFormat(frag.dbAfter, ordForm, spinOpts)

        frag.dbBefore.setSelection(ordForm.indexOf(frag.trkBeforeDate.text.toString()))
        frag.dbAfter.setSelection(ordForm.indexOf(frag.trkAfterDate.text.toString()))

        frag.dbGo.setOnClickListener {

            val before = LocalDate.parse(frag.dbBefore.selectedItem.toString(), com.dmyFormatter)
            val after = LocalDate.parse(frag.dbAfter.selectedItem.toString(), com.dmyFormatter)

            if(after < before){
                Toast.makeText(requireContext(), "Before date must be earlier than After date.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val intent = Intent()
            intent.putExtra("before", before.format(com.ymdFormatter))
            intent.putExtra("after", after.format(com.ymdFormatter))
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            dialog?.dismiss()

        }
    }

}
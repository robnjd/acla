package com.example.acla.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import kotlinx.android.synthetic.main.frag_befter.view.*
import kotlinx.android.synthetic.main.frag_confirm.view.*
import kotlinx.android.synthetic.main.frag_image.view.*
import kotlinx.android.synthetic.main.frag_tracker.view.*
import java.io.File
import java.time.LocalDate

class ConfirmDialog(val message: String) : DialogFragment() {

    val TAG = "ConfirmDialog"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog?.show()
        val width = WindowManager.LayoutParams.WRAP_CONTENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)

        return inflater.inflate(R.layout.frag_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val frag = view
        val intent = Intent()

        frag.dcText.text = message

        frag.dcYes.setOnClickListener {
            intent.putExtra("reply", "yes")
            targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, intent)
            dialog?.dismiss()
        }

        frag.dcNo.setOnClickListener {
            intent.putExtra("reply", "no")
            targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, intent)
            dialog?.dismiss()
        }
    }

}
package com.example.acla.ui.tracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log.d
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import kotlinx.android.synthetic.main.frag_befter.view.*
import kotlinx.android.synthetic.main.frag_load.view.*
import kotlinx.android.synthetic.main.frag_tracker.view.*
import java.time.LocalDate

class LoadingDialog(val size: Int) : DialogFragment() {

    val TAG = "LoadingDialog"

    lateinit var com: CommonRoom
    lateinit var frag: View
    val vmMain: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        vmMain.load.observe(viewLifecycleOwner, { load ->
            frag.lodProgress.progress = load
            d(TAG, "Loading: ${load}")
            //if(load >= size) dialog?.dismiss()
        } )

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog?.show()
        val width = WindowManager.LayoutParams.WRAP_CONTENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)

        return inflater.inflate(R.layout.frag_load, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        com = CommonRoom(requireContext())
        frag = view
        d(TAG, "$size")
        frag.lodProgress.setMax(size)
        frag.lodProgress.progress = 0
    }

}
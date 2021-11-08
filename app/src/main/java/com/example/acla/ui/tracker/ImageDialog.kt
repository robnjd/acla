package com.example.acla.ui.tracker

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import com.example.acla.ui.ConfirmDialog
import kotlinx.android.synthetic.main.frag_image.view.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ImageDialog(val date: LocalDate) : DialogFragment() {

    val TAG = "ImageDialog"
    val CONFIRM = 0

    val vmMain: MainViewModel by activityViewModels()
    lateinit var com: CommonRoom
    lateinit var frag: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog?.show()
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)

        return inflater.inflate(R.layout.frag_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        com = CommonRoom(requireContext())
        frag = view

        frag.imgTitle.text = date.format(com.daydmonyFormatter)
       // if(date !in vmMain.mapPictures.value!!) vmMain.fileImage(date)
        frag.imgImage.setImageBitmap(vmMain.mapPictures.value!![date])

        frag.imgDelete.setOnClickListener {
            val dialogConfirm = ConfirmDialog("Delete image: /$date.png?")
            dialogConfirm.setTargetFragment(this, CONFIRM)
            dialogConfirm.show(parentFragmentManager, "Confirm")
        }

        frag.imgImage.setOnClickListener {
            val intent = Intent()
            intent.putExtra("function", "camera")
            intent.putExtra("date", date.format(com.ymdFormatter))
            targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, intent)
            dialog?.dismiss()
        }

        frag.imgBefore.setOnClickListener {
            setAs("before")
        }

        frag.imgAfter.setOnClickListener {
            setAs("after")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode != AppCompatActivity.RESULT_OK) return

        when(requestCode){
            CONFIRM ->{ val reply = data?.extras?.getString("reply") ?: "no"
                        if(reply == "no") return
                        File(vmMain.locPictures.value!!.absolutePath + "/$date.png").delete()
                        Toast.makeText(requireContext(), "File deleted.", Toast.LENGTH_SHORT).show()
                        targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, null)
                        dialog?.dismiss() }
        }
    }

    fun setAs(befter: String) {
        val intent = Intent()
        intent.putExtra("function", befter)
        intent.putExtra("date", date.format(com.ymdFormatter))
        targetFragment?.onActivityResult(targetRequestCode, AppCompatActivity.RESULT_OK, intent)
        dialog?.dismiss()
    }
}
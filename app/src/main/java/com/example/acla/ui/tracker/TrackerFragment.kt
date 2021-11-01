package com.example.acla.ui.tracker

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log.d
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.example.acla.BuildConfig
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.CommonRoom
import com.example.acla.backend.TableAdapter
import kotlinx.android.synthetic.main.frag_tracker.view.*
import java.io.File
import java.time.LocalDate
import java.util.*

class TrackerFragment : Fragment() {

    val TAG = "TrackerFragment"
    val vmMain: MainViewModel by activityViewModels()
    lateinit var com: CommonRoom
    lateinit var frag: View
    lateinit var fileName : String
    var ordered = mutableListOf<LocalDate>()
    val today = LocalDate.now()

    val BEFTER = 0
    val IMAGE = 1
    val CAMERA_REQUEST = 1888
    val MY_CAMERA_PERMISSION_CODE = 1000


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_tracker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup
        com = CommonRoom(requireContext())
        frag = view
        vmMain.locPictures.value = requireContext().getExternalFilesDir("/images")!!

        frag.trkYear.text = today.year.toString()
        frag.trkMonth.text = today.month.name

        handleRefresh()

        // Listeners
        frag.trkDateButton.setOnClickListener {
            val dialogBefter = BefterDialog(ordered)
            dialogBefter.setTargetFragment(this, BEFTER)
            dialogBefter.show(parentFragmentManager, "Befter")
        }

        frag.trkPlayButton.setOnClickListener {
            val from = LocalDate.parse(frag.trkBeforeDate.text, com.dmyFormatter)
            val to = LocalDate.parse(frag.trkAfterDate.text, com.dmyFormatter)
            playGIF(from, to)
        }

        frag.trkPrevYear.setOnClickListener {
            changeCalendar("year", "prev")
        }

        frag.trkNextYear.setOnClickListener {
            changeCalendar("year", "next")
        }

        frag.trkPrevMonth.setOnClickListener {
            changeCalendar("month", "prev")
        }

        frag.trkNextMonth.setOnClickListener {
            changeCalendar("month", "next")
        }

    }


    private fun handleBefter(data: Intent?) {
        val before = LocalDate.parse(data?.extras?.getString("before"), com.ymdFormatter)
        val after = LocalDate.parse(data?.extras?.getString("after"), com.ymdFormatter)
        getBefter(before, after)
    }

    fun getImages() {

        // Get an ordered list of files from the /images folder
        ordered.clear()
        vmMain.mapExtensions.value?.clear()
        vmMain.mapPictures.value?.clear()

        vmMain.locPictures.value!!.listFiles().forEach{

            if(it.isFile && it.extension in listOf("jpg", "png")){
                val date = try {
                    LocalDate.parse(it.nameWithoutExtension.substring(0, 10), com.ymdFormatter)
                } catch(e:Exception){
                    null
                }

                if(date != null) {
                    ordered.add(com.datePosition(date, ordered), date)
                    vmMain.mapExtensions.value!![date] = it.extension
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "camera permission granted", Toast.LENGTH_LONG).show()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                createPhotoFile(cameraIntent, "$fileName.png")
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            } else {
                Toast.makeText(requireContext(), "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data == null || resultCode != AppCompatActivity.RESULT_OK) return

        when(requestCode) {
            CAMERA_REQUEST  -> handleRefresh()
            BEFTER          -> handleBefter(data)
            IMAGE           ->{ if(data.extras?.getString("camera") != null) {
                openCamera(data.extras!!.getString("camera")!!)
            }
                handleRefresh() }
        }
    }

    fun handleRefresh() {
        if(ordered.isEmpty()) return
        getImages()
        getBefter(ordered[0], ordered.last())
        getCalendar()
    }

    fun clickFAB() {
        openCamera(today.format(com.ymdFormatter))
    }


    fun getBefter(before: LocalDate, after: LocalDate) {

        // Place images into Before/After image views

        if(before !in vmMain.mapPictures.value!!.keys) {
            vmMain.fileImage(before)
        }

        if(after !in vmMain.mapPictures.value!!.keys) {
            vmMain.fileImage(after)
        }

        frag.trkBeforeImage.setImageBitmap(vmMain.mapPictures.value!![before])
        frag.trkBeforeDate.text = before.format(com.dmyFormatter)

        frag.trkAfterImage.setImageBitmap(vmMain.mapPictures.value!![after])
        frag.trkAfterDate.text = after.format(com.dmyFormatter)
    }

    fun changeCalendar(period: String, prevnext: String) {

        // Change the month shown in the Calendar table

        val cal = LocalDate.parse("01 ${com.decap(frag.trkMonth.text.toString())} ${frag.trkYear.text}", com.dmonthyrFormatter)
        val pm = (if(prevnext == "prev") -1 else 1).toLong()

        if(cal.withDayOfMonth(1) > LocalDate.now()){
            return
        }

        when(period){
            "year"  -> frag.trkYear.text = (cal.year + pm).toString()
            "month" ->{ frag.trkMonth.text = cal.plusMonths(pm).month.name
                frag.trkYear.text = cal.plusMonths(pm).year.toString() }
        }

        getCalendar()
    }

    fun openCamera(saveAs: String) {
        d(TAG, "openCamera ${ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)}")
        // Open the camera app
        fileName = saveAs

        if(ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            d(TAG, "requestPermission")
            //activityResultLauncher.launch(Manifest.permission.CAMERA)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), MY_CAMERA_PERMISSION_CODE)
        } else {
            d(TAG, "cameraIntent")
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            createPhotoFile(cameraIntent, "$fileName.png")
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    val activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted -> if(isGranted) openCamera("") }

    fun createPhotoFile(intent: Intent, filename: String) {
        d(TAG, "craetePhotoFile")
        val file = File(vmMain.locPictures.value!!.absolutePath + "/" + filename)
        val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID+".provider", file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
    }

    fun playGIF(from: LocalDate, to: LocalDate) {

        // Cycle the after image through dates between from and to

        val start = ordered.indexOf(from)
        val end = ordered.indexOf(to)

        for(d in start..end) {
            if(ordered[d] !in vmMain.mapPictures.value!!.keys) vmMain.fileImage(ordered[d])
        }

        frag.trkBeforeImage.setImageBitmap(vmMain.mapPictures.value!![from])
        frag.trkBeforeDate.text = from.format(com.dmyFormatter)
        frag.trkAfterImage.setImageBitmap(vmMain.mapPictures.value!![from])
        frag.trkAfterDate.text = from.format(com.dmyFormatter)

        val gifHandler = Handler()
        for(i in start..end) {
            gifHandler.postDelayed({
                frag.trkAfterImage.setImageBitmap(vmMain.mapPictures.value!![ordered[i]])
                frag.trkAfterDate.text = ordered[i].format(com.dmyFormatter)
            }, i*(500L-i/10))
        }


    }

    fun getCalendar(){

        val yr = frag.trkYear.text
        val mon = frag.trkMonth.text

        val startDate = LocalDate.parse("01 ${com.decap(mon.toString())} $yr", com.dmonthyrFormatter)

        val lstCalendar = mutableListOf<List<List<String>>>()
        var week = mutableListOf<List<String>>()

        for(d in 0..startDate.lengthOfMonth()){
            val date = startDate.plusDays(d.toLong())
            week.add(listOf(date.toString(), (date in ordered).toString()))

            if(week.size == 7 || d == startDate.lengthOfMonth() - 1){
                lstCalendar.add(week)
                week = mutableListOf()
            }
        }

        frag.trkImageTable.adapter = TableAdapter(requireContext(), lstCalendar, "images"){ date -> openImage(date as LocalDate) }

        for(d in 1 until startDate.lengthOfMonth()) {
            val week: Int = d/7
            frag.trkImageTable.adapter.getView(week, null, frag.trkImageTable).findViewById<ImageView>(com.viewId("tcCircle${d-(week*7)+1}"))
        }

    }

    fun openImage(date: LocalDate) {
        val dialogImage = ImageDialog(date)
        dialogImage.setTargetFragment(this, IMAGE)
        dialogImage.show(parentFragmentManager, "Image")
    }
}
package com.example.acla

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acla.backend.Session
import java.io.File
import java.time.LocalDate

class MainViewModel: ViewModel() {

    val TAG = "MainViewModel"

    val fabFunction = MutableLiveData<String>()
    val mapPictures = MutableLiveData<MutableMap<LocalDate, Bitmap>>()
    val mapExtensions = MutableLiveData<MutableMap<LocalDate, String>>()
    val locPictures = MutableLiveData<File>()



    init {
        fabFunction.value = "start"
        mapPictures.value = mutableMapOf()
        mapExtensions.value = mutableMapOf()
        locPictures.value = File("")
    }

    fun fileImage(date: LocalDate) {
        val path = locPictures.value!!.absolutePath + "/$date.${mapExtensions.value!![date]}"
        val img = BitmapFactory.decodeStream(File(path).inputStream())
        val orImg = getRotation(img, path)

        if(img != null) mapPictures.value!![date] = orImg!!
    }

    fun getRotation(img: Bitmap, path: String) : Bitmap? {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        return when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90     -> orientImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180    -> orientImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270    -> orientImage(img, 270)
            else                                    -> img
        }
    }

    fun orientImage(img: Bitmap?, angle: Int) : Bitmap? {
        if(img == null) return img
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

}
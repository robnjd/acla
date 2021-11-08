package com.example.acla.ui.dashboard

import android.util.Log.d
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acla.backend.SearchFilter
import com.example.acla.backend.Session
import java.time.LocalDate

class DashViewModel: ViewModel() {
    val TAG = "DashViewModel"
    val startDate = MutableLiveData<LocalDate>()
    val endDate = MutableLiveData<LocalDate>()
    val period = MutableLiveData<String>()
    val stats = MutableLiveData<MutableMap<LocalDate, MutableMap<String, Float>>>()
    val lstSessions = MutableLiveData<MutableList<Session>>()

    init {
        stats.value = mutableMapOf()
        lstSessions.value = mutableListOf()
    }

    fun search(searchFilter: SearchFilter) {
        period.value = searchFilter.period
        endDate.value = searchFilter.endDate
        startDate.value = searchFilter.startDate
        d(TAG, "run Search")
    }

}
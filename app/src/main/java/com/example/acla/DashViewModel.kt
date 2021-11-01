package com.example.acla

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acla.backend.SearchFilter
import com.example.acla.backend.Session
import java.time.LocalDate

class DashViewModel: ViewModel() {

    val searchFilter = MutableLiveData<SearchFilter>()
    val stats = MutableLiveData<MutableMap<String, MutableMap<LocalDate, Float>>>()
    val lstSessions = MutableLiveData<MutableList<Session>>()

}
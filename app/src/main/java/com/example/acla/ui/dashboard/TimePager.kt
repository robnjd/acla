package com.example.acla.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.acla.MainViewModel
import com.example.acla.R
import com.example.acla.backend.*
import kotlinx.android.synthetic.main.frag_graph.view.*
import java.time.LocalDate

class TimePager(val position: Int) : Fragment() {

    lateinit var frag: View
    lateinit var cht: ChartHelper
    lateinit var com: CommonRoom
    val vmMain: MainViewModel by activityViewModels()
    val workoutColours = mapOf("All" to R.color.orange, "Interval" to R.color.blue, "Run" to R.color.green, "Routine" to R.color.purple)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vmMain.stats.observe(viewLifecycleOwner, Observer {
            stats -> populateGraph()
        } )

        return inflater.inflate(R.layout.frag_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag = view
        cht = ChartHelper(requireContext())
        com = CommonRoom(requireContext())


    }


}
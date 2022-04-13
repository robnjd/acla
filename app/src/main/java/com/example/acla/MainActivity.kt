package com.example.acla

import android.os.Bundle
import android.util.Log.d
import android.view.View
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.android.synthetic.main.app_bar_main.*
import androidx.lifecycle.ViewModelProvider
import com.example.acla.backend.DatabaseHelper
import com.example.acla.ui.SessionFragment
import com.example.acla.ui.history.HistoryFragment
import com.example.acla.ui.tracker.TrackerFragment

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController : NavController
    lateinit var vmMain: MainViewModel
    var screen = "Session"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Setup Navigation _____________________________________________________________
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val setMenu = setOf(R.id.nav_session,
                            R.id.nav_history,
                            R.id.nav_dashboard,
                            R.id.nav_tracker,
                            R.id.nav_settings)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setMenu, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        vmMain = ViewModelProvider(this)[MainViewModel::class.java]

        vmMain.fabFunction.observe(this) { function ->
            val icon = when (function) {
                "start"     -> R.drawable.ic_whistle
                "pause"     -> R.drawable.ic_pause
                "camera"    -> R.drawable.ic_camera
                "add"       -> R.drawable.ic_add
                else        -> 0
            }
            fab.setImageResource(icon)
        }

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            screen = destination.label.toString()
            vmMain.fabFunction.value = when(screen) {
                "Session"   -> "start"
                "Tracker"   -> "camera"
                "History"   -> "add"
                else        -> ""
            }
            fab.visibility = when(screen) {
                "Session", "Tracker", "History"     -> View.VISIBLE
                else                                -> View.GONE
            }
        }

        fab.setOnClickListener {
            val nav = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            when(screen) {
                "Session"   -> (nav.childFragmentManager.fragments[0] as SessionFragment).clickFAB()
                "Tracker"   -> (nav.childFragmentManager.fragments[0] as TrackerFragment).clickFAB()
                "History"   -> (nav.childFragmentManager.fragments[0] as HistoryFragment).clickFAB()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
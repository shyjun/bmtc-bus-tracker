package com.bmtc.bustracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bmtc.bustracker.ui.BusTrackerScreen
import com.bmtc.bustracker.ui.theme.BmtcBusTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BmtcBusTrackerTheme {
                BusTrackerScreen()
            }
        }
    }
}

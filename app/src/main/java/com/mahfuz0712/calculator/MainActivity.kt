package com.mahfuz0712.calculator

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mahfuz0712.calculator.ui.CalculatorApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val prefs = getSharedPreferences("calc", MODE_PRIVATE)
        setContent {
            var amber by remember { mutableStateOf(prefs.getBoolean("amber", false)) }
            CalculatorApp(amber = amber) {
                amber = !amber
                prefs.edit().putBoolean("amber", amber).apply()
            }
        }
    }
}

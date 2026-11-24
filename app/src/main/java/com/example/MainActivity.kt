package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.configureAppCheck
import com.example.core.navigation.ApnaAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val deps = remember { ApnaDependencies(applicationContext) }
            ApnaAppRoot(deps = deps)
        }
    }
}

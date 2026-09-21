package org.openreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import org.openreader.app.ui.theme.OpenReaderTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OpenReaderApplication).container
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            OpenReaderTheme {
                OpenReaderApp(
                    container = container,
                    widthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}

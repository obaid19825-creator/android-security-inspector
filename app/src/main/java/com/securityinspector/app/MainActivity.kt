package com.securityinspector.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.securityinspector.app.presentation.navigation.SecurityInspectorNavGraph
import com.securityinspector.app.presentation.theme.SecurityInspectorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurityInspectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecurityInspectorNavGraph()
                }
            }
        }
    }
}

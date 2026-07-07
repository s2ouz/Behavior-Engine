package com.behaviorengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.behaviorengine.navigation.BehaviorEngineNavGraph
import com.behaviorengine.ui.theme.BehaviorEngineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BehaviorEngineRoot()
        }
    }
}

@Composable
private fun BehaviorEngineRoot() {
    BehaviorEngineTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BehaviorEngineNavGraph()
        }
    }
}

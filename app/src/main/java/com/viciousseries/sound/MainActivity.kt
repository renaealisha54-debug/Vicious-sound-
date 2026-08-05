package com.viciousseries.sound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val watchdog = Watchdog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        watchdog.start()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SelfTestScreen(watchdog)
                }
            }
        }
    }

    override fun onDestroy() {
        watchdog.stop()
        super.onDestroy()
    }
}

@Composable
fun SelfTestScreen(watchdog: Watchdog) {
    var status by remember { mutableStateOf("Healthy") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Vicious Sound", style = MaterialTheme.typography.headlineMedium)
        Text("Current state: $status")

        Button(onClick = { watchdog.beat(loopSpeedMs = 10.0); status = "Healthy" }) {
            Text("Healthy")
        }
        Button(onClick = { watchdog.beat(loopSpeedMs = 75.0); status = "Warning" }) {
            Text("Warning")
        }
        Button(onClick = { watchdog.beat(loopSpeedMs = 250.0); status = "Critical" }) {
            Text("Critical")
        }
        Button(onClick = { watchdog.exception("manual trigger"); status = "Crashed" }) {
            Text("Crashed")
        }
        Button(onClick = { watchdog.reset(); status = "Healthy (reset)" }) {
            Text("Reset")
        }

        Divider()

        Button(onClick = {
            scope.launch {
                status = "Running full self-test..."
                watchdog.beat(10.0); delay(3000)
                watchdog.beat(75.0); status = "Warning"; delay(3000)
                watchdog.beat(250.0); status = "Critical"; delay(3000)
                watchdog.exception("self-test crash"); status = "Crashed"; delay(3000)
                watchdog.reset(); status = "Healthy (self-test complete)"
            }
        }) {
            Text("Run Full Self-Test")
        }
    }
}

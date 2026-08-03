package de.ctrlxs.workday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Indigo = Color(0xFF818CF8)
val Violet = Color(0xFFA78BFA)
val Cyan = Color(0xFF22D3EE)
val BgTop = Color(0xFF0B1120)
val BgBottom = Color(0xFF111C33)
val CardColor = Color(0xFF16223A)
val TrackColor = Color(0xFF1E2C48)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF8FA3C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WorkdayApp() }
    }
}

@Composable
fun WorkdayApp() {
    val context = LocalContext.current
    val store = remember { ScheduleStore(context) }
    var week by remember { mutableStateOf(store.load()) }
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Indigo,
            background = BgTop,
            surface = CardColor,
            onPrimary = Color.White,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        ) {
            AnimatedContent(
                targetState = showSettings,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "screens"
            ) { settings ->
                if (settings) {
                    ScheduleScreen(
                        week = week,
                        onChange = { updated ->
                            week = updated
                            store.save(updated)
                        },
                        onBack = { showSettings = false }
                    )
                } else {
                    TodayScreen(week = week, onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}

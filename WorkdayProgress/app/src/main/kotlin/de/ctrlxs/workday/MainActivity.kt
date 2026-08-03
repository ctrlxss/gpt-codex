package de.ctrlxs.workday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Indigo = Color(0xFF818CF8)
val Violet = Color(0xFFA78BFA)
val Cyan = Color(0xFF22D3EE)
val BgTop = Color(0xFF0B1120)
val BgBottom = Color(0xFF111C33)
val CardColor = Color(0xFF16223A)
val TrackColor = Color(0xFF1E2C48)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF8FA3C0)

enum class Screen { Home, Planner }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CoreSystemApp() }
    }
}

@Composable
fun CoreSystemApp() {
    val context = LocalContext.current
    val store = remember { ScheduleStore(context) }
    var week by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var menuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = menuOpen || screen != Screen.Home) {
        if (menuOpen) menuOpen = false else screen = Screen.Home
    }

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
                targetState = screen,
                transitionSpec = {
                    if (targetState == Screen.Planner) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "screens"
            ) { current ->
                when (current) {
                    Screen.Home -> TodayScreen(week = week, onOpenMenu = { menuOpen = true })
                    Screen.Planner -> PlannerScreen(
                        week = week,
                        onChange = { updated ->
                            week = updated
                            store.save(updated)
                        },
                        onBack = { screen = Screen.Home }
                    )
                }
            }

            AnimatedVisibility(visible = menuOpen, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { menuOpen = false }
                )
            }
            AnimatedVisibility(
                visible = menuOpen,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                SideMenu(
                    onOpenPlanner = {
                        menuOpen = false
                        screen = Screen.Planner
                    }
                )
            }
        }
    }
}

@Composable
private fun SideMenu(onOpenPlanner: () -> Unit) {
    Column(
        Modifier
            .fillMaxHeight()
            .width(290.dp)
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(CardColor)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            "CORE SYSTEM",
            color = Cyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Text(
            "your life, one place",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(28.dp))

        MenuItem("📅", "Work schedule", "week planner & patient slots", enabled = true, onClick = onOpenPlanner)
        MenuItem("⏰", "Reminders", "coming soon", enabled = false)
        MenuItem("✅", "Tasks", "coming soon", enabled = false)
        MenuItem("💡", "Plans & ideas", "coming soon", enabled = false)

        Spacer(Modifier.weight(1f))

        Text(
            "More modules will land here —\none app for everything.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun MenuItem(
    emoji: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) TrackColor else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

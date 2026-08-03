package de.ctrlxs.workday

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    week: WeekPlan,
    record: DayRecord,
    onToggleSick: () -> Unit,
    onToggleClockOut: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            today = LocalDate.now()
            delay(1000)
        }
    }

    val planned = week.days[today.dayOfWeek] ?: emptyList()
    val blocks = effectiveBlocks(planned, record.clockOut)
    val status = computeStatus(blocks, now, week.minutesPerPatient)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "CORE SYSTEM",
                    color = Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )
                Text(
                    today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    today.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        when {
            record.sick -> SickCard()
            planned.isEmpty() -> OffCard()
            blocks.isEmpty() -> WentHomeCard(record.clockOut ?: 0)
            else -> ProgressSection(status, blocks)
        }

        if (planned.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    emoji = "🤒",
                    label = if (record.sick) "Sick today ✓" else "I'm sick today",
                    sub = if (record.sick) "tap to undo" else "marks today as a sick day",
                    active = record.sick,
                    accent = Color(0xFFF87171),
                    onClick = onToggleSick
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    emoji = "🏠",
                    label = record.clockOut?.let { "Out at ${formatMinutes(it)}" } ?: "Clock out",
                    sub = if (record.clockOut != null) "tap to undo" else "going home now",
                    active = record.clockOut != null,
                    accent = Color(0xFFFBBF24),
                    enabled = !record.sick,
                    onClick = onToggleClockOut
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProgressSection(status: DayStatus, blocks: List<TimeBlock>) {
    val animatedProgress by animateFloatAsState(
        targetValue = status.progress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

        PhaseBadge(status)

        Spacer(Modifier.height(28.dp))

        Text(
            "${(animatedProgress * 100).toInt()}%",
            color = TextPrimary,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text("of your workday done", color = TextSecondary, fontSize = 14.sp)

        Spacer(Modifier.height(24.dp))

        ShimmerProgressBar(
            progress = animatedProgress,
            active = status.phase == DayPhase.WORKING,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(36.dp))

        when (status.phase) {
            DayPhase.BEFORE -> {
                Text("work starts in", color = TextSecondary, fontSize = 14.sp)
                CountdownText(status.secondsUntilStart)
            }
            DayPhase.BREAK -> {
                Text("break — next block in", color = TextSecondary, fontSize = 14.sp)
                CountdownText(status.secondsUntilNextBlock)
            }
            DayPhase.DONE -> {
                Text("🎉", fontSize = 40.sp)
                Text(
                    "Done for today!",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> {
                Text("time until finished", color = TextSecondary, fontSize = 14.sp)
                CountdownText(status.secondsUntilEnd)
            }
        }

        Spacer(Modifier.height(36.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "patients left",
                accent = Cyan
            ) {
                AnimatedCount(status.patientsLeft)
                Text(
                    "of ${status.totalPatients} today",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            StatCard(
                modifier = Modifier.weight(1f),
                label = "finish time",
                accent = Violet
            ) {
                Text(
                    formatMinutes(blocks.last().end),
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${blocks.size} block${if (blocks.size == 1) "" else "s"} · " +
                        formatDuration(status.totalWorkSeconds) + " work",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PhaseBadge(status: DayStatus) {
    val (text, color) = when (status.phase) {
        DayPhase.WORKING -> "Working" to Cyan
        DayPhase.BREAK -> "On break ☕" to Violet
        DayPhase.BEFORE -> "Not started yet" to TextSecondary
        DayPhase.DONE -> "Finished" to Color(0xFF34D399)
        DayPhase.OFF -> "Day off" to TextSecondary
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CardColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(if (status.phase == DayPhase.WORKING) dotAlpha else 1f)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ShimmerProgressBar(progress: Float, active: Boolean, modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "shimmerX"
    )

    BoxWithConstraints(
        modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(TrackColor)
    ) {
        val barWidth = maxWidth
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.horizontalGradient(listOf(Indigo, Violet, Cyan)))
        ) {
            if (active) {
                val sweepWidth = 90.dp
                Box(
                    Modifier
                        .offset(x = (barWidth + sweepWidth * 2) * shimmerX - sweepWidth * 2)
                        .width(sweepWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun CountdownText(seconds: Long) {
    Text(
        formatCountdown(seconds),
        color = TextPrimary,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun AnimatedCount(value: Int) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (targetState < initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith
                    (slideOutVertically { -it } + fadeOut())
            } else {
                (slideInVertically { -it } + fadeIn()) togetherWith
                    (slideOutVertically { it } + fadeOut())
            }
        },
        label = "count"
    ) { v ->
        Text(
            "$v",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label.uppercase(),
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    sub: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) accent.copy(alpha = 0.16f) else CardColor)
            .then(
                if (active) Modifier.border(1.5.dp, accent, RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (active) accent else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(sub, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SickCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardColor)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤒", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Sick today",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text("Rest up and get well soon ❤️", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun WentHomeCard(clockOut: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardColor)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏠", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Went home at ${formatMinutes(clockOut)}",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text("The rest of the day is yours.", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun OffCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardColor)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏖️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "No work today",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text("Enjoy your day off!", color = TextSecondary, fontSize = 14.sp)
    }
}

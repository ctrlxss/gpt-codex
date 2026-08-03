package de.ctrlxs.workday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Amber = Color(0xFFFBBF24)
private val Red = Color(0xFFF87171)
private val Green = Color(0xFF34D399)

@Composable
fun StatsScreen(week: WeekPlan, store: AttendanceStore, onBack: () -> Unit) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val now = LocalTime.now()
    val records = remember(ym) { store.month(ym) }
    val stats = remember(ym, records, week) { computeMonthStats(ym, week, records, today, now) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
            Text(
                "Hours & attendance",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            NavArrow("‹") { ym = ym.minusMonths(1) }
            Text(
                "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            NavArrow("›") { ym = ym.plusMonths(1) }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(
                Modifier.weight(1f),
                label = "hours worked",
                value = formatDuration(stats.workedSeconds),
                sub = "of ${formatDuration(stats.plannedSeconds)} planned",
                accent = Cyan
            )
            StatBox(
                Modifier.weight(1f),
                label = "days worked",
                value = "${stats.workedDays}",
                sub = "this month",
                accent = Violet
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(
                Modifier.weight(1f),
                label = "sick days",
                value = "${stats.sickDays.size}",
                sub = if (stats.sickDays.isEmpty()) "none 💪" else "get well soon",
                accent = Red
            )
            StatBox(
                Modifier.weight(1f),
                label = "left early",
                value = "${stats.earlyDays.size}",
                sub = if (stats.earlyDays.isEmpty()) "never" else "days",
                accent = Amber
            )
        }

        Spacer(Modifier.height(24.dp))

        val events = buildList {
            stats.sickDays.forEach { add(Triple(it, "Sick", Red)) }
            stats.earlyDays.forEach { (date, minute) ->
                add(Triple(date, "Left early at ${formatMinutes(minute)}", Amber))
            }
        }.sortedBy { it.first }

        Text(
            "SPECIAL DAYS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        if (events.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardColor)
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✨", fontSize = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Full attendance — no sick days,\nno early leaves this month.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            events.forEach { (date, label, color) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        label,
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NavArrow(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Cyan,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp)
    )
}

@Composable
private fun StatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    accent: Color,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardColor)
            .padding(16.dp)
    ) {
        Text(
            label.uppercase(),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(value, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = TextSecondary, fontSize = 11.sp)
    }
}

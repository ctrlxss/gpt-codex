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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private data class PickerTarget(val day: DayOfWeek, val field: String, val minutes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(week: WeekSchedule, onChange: (WeekSchedule) -> Unit, onBack: () -> Unit) {
    var picker by remember { mutableStateOf<PickerTarget?>(null) }

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
                "My schedule",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        PatientSlotCard(week, onChange)

        Spacer(Modifier.height(16.dp))

        week.days.keys.sortedBy { it.value }.forEach { dow ->
            val day = week.days.getValue(dow)
            DayCard(
                dow = dow,
                day = day,
                onToggle = { enabled ->
                    onChange(week.copy(days = week.days + (dow to day.copy(enabled = enabled))))
                },
                onPick = { field, minutes -> picker = PickerTarget(dow, field, minutes) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
    }

    picker?.let { target ->
        val state = rememberTimePickerState(
            initialHour = target.minutes / 60,
            initialMinute = target.minutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { picker = null },
            containerColor = CardColor,
            confirmButton = {
                TextButton(onClick = {
                    val minutes = state.hour * 60 + state.minute
                    val day = week.days.getValue(target.day)
                    val updated = when (target.field) {
                        "workStart" -> day.copy(workStart = minutes)
                        "workEnd" -> day.copy(workEnd = minutes)
                        "breakStart" -> day.copy(breakStart = minutes)
                        else -> day.copy(breakEnd = minutes)
                    }
                    onChange(week.copy(days = week.days + (target.day to updated)))
                    picker = null
                }) { Text("OK", color = Cyan) }
            },
            dismissButton = {
                TextButton(onClick = { picker = null }) { Text("Cancel", color = TextSecondary) }
            },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
private fun PatientSlotCard(week: WeekSchedule, onChange: (WeekSchedule) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Minutes per patient", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("used to count patients left", color = TextSecondary, fontSize = 12.sp)
        }
        StepperButton("−") {
            if (week.minutesPerPatient > 5) onChange(week.copy(minutesPerPatient = week.minutesPerPatient - 5))
        }
        Text(
            "${week.minutesPerPatient}",
            color = Cyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        StepperButton("+") {
            if (week.minutesPerPatient < 120) onChange(week.copy(minutesPerPatient = week.minutesPerPatient + 5))
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TrackColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun DayCard(
    dow: DayOfWeek,
    day: DaySchedule,
    onToggle: (Boolean) -> Unit,
    onPick: (field: String, minutes: Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dow.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = day.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Indigo,
                    uncheckedTrackColor = TrackColor
                )
            )
        }
        if (day.enabled) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeChip("Start", day.workStart, Cyan, Modifier.weight(1f)) { onPick("workStart", day.workStart) }
                TimeChip("End", day.workEnd, Cyan, Modifier.weight(1f)) { onPick("workEnd", day.workEnd) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeChip("Break from", day.breakStart, Violet, Modifier.weight(1f)) { onPick("breakStart", day.breakStart) }
                TimeChip("Break until", day.breakEnd, Violet, Modifier.weight(1f)) { onPick("breakEnd", day.breakEnd) }
            }
        }
    }
}

@Composable
private fun TimeChip(
    label: String,
    minutes: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TrackColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(formatMinutes(minutes), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

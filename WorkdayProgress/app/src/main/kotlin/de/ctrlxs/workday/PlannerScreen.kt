package de.ctrlxs.workday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val AXIS_START = 5 * 60
private const val AXIS_END = 22 * 60
private const val SNAP = 15
private val HOUR_HEIGHT = 56.dp
private val AXIS_WIDTH = 40.dp
private val GRID_HEIGHT = HOUR_HEIGHT * ((AXIS_END - AXIS_START) / 60)
private val GridLine = Color(0xFF223051)
private val NowRed = Color(0xFFF87171)

private fun minutesToDp(minutes: Int) = HOUR_HEIGHT * ((minutes - AXIS_START) / 60f)

private data class EditTarget(
    val dow: DayOfWeek,
    val index: Int?,
    val startText: String,
    val endText: String,
)

@Composable
fun PlannerScreen(week: WeekPlan, onChange: (WeekPlan) -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(30_000)
        }
    }
    val days = week.days.keys.sortedBy { it.value }
    val today = LocalDate.now().dayOfWeek

    fun setBlocks(dow: DayOfWeek, blocks: List<TimeBlock>) {
        onChange(week.copy(days = week.days + (dow to blocks)))
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
            Column(Modifier.weight(1f)) {
                Text("Work schedule", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "tap a block to edit · tap free space to add",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            MiniStepper(
                value = week.minutesPerPatient,
                label = "min/pat",
                onMinus = { if (week.minutesPerPatient > 5) onChange(week.copy(minutesPerPatient = week.minutesPerPatient - 5)) },
                onPlus = { if (week.minutesPerPatient < 120) onChange(week.copy(minutesPerPatient = week.minutesPerPatient + 5)) },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Spacer(Modifier.width(AXIS_WIDTH))
            days.forEach { dow ->
                val blocks = week.days.getValue(dow)
                val totalMin = blocks.sumOf { it.duration }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        color = if (dow == today) Cyan else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (totalMin > 0) formatDuration(totalMin * 60L) else "off",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                TimeAxis()
                days.forEach { dow ->
                    DayColumn(
                        dow = dow,
                        blocks = week.days.getValue(dow),
                        isToday = dow == today,
                        minutesPerPatient = week.minutesPerPatient,
                        onEdit = { i, b ->
                            editing = EditTarget(dow, i, formatMinutes(b.start), formatMinutes(b.end))
                        },
                        onAdd = { minute ->
                            val start = ((minute - 30).coerceAtLeast(AXIS_START) / SNAP) * SNAP
                            editing = EditTarget(
                                dow, null,
                                formatMinutes(start),
                                formatMinutes(minOf(start + 60, AXIS_END))
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // "now" line across the whole week
            val nowMin = now.toSecondOfDay() / 60
            if (nowMin in AXIS_START..AXIS_END) {
                Box(
                    Modifier
                        .offset(y = minutesToDp(nowMin) - 1.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(NowRed.copy(alpha = 0.85f))
                )
                Text(
                    formatMinutes(nowMin),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(y = minutesToDp(nowMin) - 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NowRed)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }

    editing?.let { target ->
        BlockDialog(
            target = target,
            dayBlocks = week.days.getValue(target.dow),
            onSave = { block ->
                val list = week.days.getValue(target.dow)
                val base = if (target.index != null) {
                    list.filterIndexed { i, _ -> i != target.index }
                } else list
                setBlocks(target.dow, (base + block).sortedBy { it.start })
                editing = null
            },
            onDelete = {
                if (target.index != null) {
                    setBlocks(
                        target.dow,
                        week.days.getValue(target.dow).filterIndexed { i, _ -> i != target.index }
                    )
                }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun BlockDialog(
    target: EditTarget,
    dayBlocks: List<TimeBlock>,
    onSave: (TimeBlock) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var startText by remember { mutableStateOf(target.startText) }
    var endText by remember { mutableStateOf(target.endText) }
    var error by remember { mutableStateOf<String?>(null) }
    val dayName = target.dow.getDisplayName(TextStyle.FULL, Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = {
            Text(
                if (target.index == null) "New block · $dayName" else "Edit block · $dayName",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField("Start", startText, Modifier.weight(1f)) { startText = it; error = null }
                    TimeField("End", endText, Modifier.weight(1f)) { endText = it; error = null }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: "e.g. 8:00, 0800 or 14:30",
                    color = if (error != null) NowRed else TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = parseTimeInput(startText)
                val e = parseTimeInput(endText)
                val others = dayBlocks.filterIndexed { i, _ -> i != target.index }
                when {
                    s == null || e == null -> error = "Enter times like 8:00 or 0800"
                    e <= s -> error = "End must be after start"
                    s < AXIS_START || e > AXIS_END ->
                        error = "Keep it between ${formatMinutes(AXIS_START)} and ${formatMinutes(AXIS_END)}"
                    others.any { s < it.end && e > it.start } -> error = "Overlaps another block"
                    else -> onSave(TimeBlock(s, e))
                }
            }) { Text("Save", color = Cyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (target.index != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = NowRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
            }
        }
    )
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 5) onChange(it) },
        label = { Text(label, color = TextSecondary) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun TimeAxis() {
    Box(Modifier.width(AXIS_WIDTH).height(GRID_HEIGHT)) {
        for (h in (AXIS_START / 60)..(AXIS_END / 60)) {
            Text(
                "%02d".format(h),
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .offset(y = minutesToDp(h * 60) - 7.dp)
                    .width(AXIS_WIDTH - 8.dp)
            )
        }
    }
}

@Composable
private fun DayColumn(
    dow: DayOfWeek,
    blocks: List<TimeBlock>,
    isToday: Boolean,
    minutesPerPatient: Int,
    onEdit: (Int, TimeBlock) -> Unit,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(GRID_HEIGHT)
            .background(if (isToday) Color(0x0A22D3EE) else Color.Transparent)
            .drawBehind {
                val hours = (AXIS_END - AXIS_START) / 60
                for (i in 0..hours) {
                    val y = i * size.height / hours
                    drawLine(GridLine, Offset(0f, y), Offset(size.width, y), 1f)
                }
                drawLine(GridLine, Offset(0f, 0f), Offset(0f, size.height), 1f)
                drawLine(GridLine, Offset(size.width, 0f), Offset(size.width, size.height), 1f)
            }
            .pointerInput(dow, blocks) {
                detectTapGestures { offset ->
                    val minute = AXIS_START + (offset.y / (HOUR_HEIGHT.toPx() / 60f)).toInt()
                    if (blocks.none { minute >= it.start && minute < it.end }) onAdd(minute)
                }
            }
    ) {
        blocks.forEachIndexed { i, block ->
            BlockView(
                block = block,
                minutesPerPatient = minutesPerPatient,
                onClick = { onEdit(i, block) },
            )
        }
    }
}

@Composable
private fun BlockView(
    block: TimeBlock,
    minutesPerPatient: Int,
    onClick: () -> Unit,
) {
    val height = HOUR_HEIGHT * (block.duration / 60f)
    Box(
        Modifier
            .offset(y = minutesToDp(block.start))
            .padding(horizontal = 2.dp)
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Indigo.copy(alpha = 0.8f), Violet.copy(alpha = 0.8f))))
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                formatMinutes(block.start),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (block.duration >= 45) {
                Text(
                    formatMinutes(block.end),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            if (block.duration >= 90 && minutesPerPatient > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "${block.duration / minutesPerPatient} pat.",
                    color = Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MiniStepper(value: Int, label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperKey("−", onMinus)
            Text(
                "$value",
                color = Cyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            StepperKey("+", onPlus)
        }
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun StepperKey(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TrackColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

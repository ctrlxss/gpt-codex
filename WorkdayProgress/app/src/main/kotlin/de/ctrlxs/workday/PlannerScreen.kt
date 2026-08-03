package de.ctrlxs.workday

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val AXIS_START = 6 * 60
private const val AXIS_END = 21 * 60
private const val SNAP = 15
private val HOUR_HEIGHT = 56.dp
private val AXIS_WIDTH = 40.dp
private val GRID_HEIGHT = HOUR_HEIGHT * ((AXIS_END - AXIS_START) / 60)
private val GridLine = Color(0xFF223051)

private fun minutesToDp(minutes: Int) = HOUR_HEIGHT * ((minutes - AXIS_START) / 60f)

@Composable
fun PlannerScreen(week: WeekPlan, onChange: (WeekPlan) -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<Pair<DayOfWeek, Int>?>(null) }
    val days = week.days.keys.sortedBy { it.value }
    val today = LocalDate.now().dayOfWeek

    fun setBlocks(dow: DayOfWeek, blocks: List<TimeBlock>) {
        onChange(week.copy(days = week.days + (dow to blocks)))
    }

    fun moveBlock(dow: DayOfWeek, index: Int, delta: Int) {
        val list = week.days.getValue(dow)
        val b = list.getOrNull(index) ?: return
        val lo = list.getOrNull(index - 1)?.end ?: AXIS_START
        val hi = list.getOrNull(index + 1)?.start ?: AXIS_END
        if (hi - lo < b.duration) return
        val newStart = (b.start + delta).coerceIn(lo, hi - b.duration)
        setBlocks(dow, list.toMutableList().also { it[index] = TimeBlock(newStart, newStart + b.duration) })
    }

    fun resizeBlock(dow: DayOfWeek, index: Int, topEdge: Boolean, delta: Int) {
        val list = week.days.getValue(dow)
        val b = list.getOrNull(index) ?: return
        val updated = if (topEdge) {
            val lo = list.getOrNull(index - 1)?.end ?: AXIS_START
            b.copy(start = (b.start + delta).coerceIn(lo, b.end - SNAP))
        } else {
            val hi = list.getOrNull(index + 1)?.start ?: AXIS_END
            b.copy(end = (b.end + delta).coerceIn(b.start + SNAP, hi))
        }
        setBlocks(dow, list.toMutableList().also { it[index] = updated })
    }

    fun addBlock(dow: DayOfWeek, minute: Int) {
        val list = week.days.getValue(dow)
        var gapStart = AXIS_START
        var gapEnd = AXIS_END
        for (b in list) {
            if (minute < b.start) { gapEnd = b.start; break }
            if (minute < b.end) return
            gapStart = b.end
        }
        if (gapEnd - gapStart < SNAP) return
        var start = (minute / SNAP) * SNAP
        start = start.coerceIn(gapStart, maxOf(gapStart, gapEnd - 60))
        val end = minOf(start + 60, gapEnd)
        val newList = (list + TimeBlock(start, end)).sortedBy { it.start }
        setBlocks(dow, newList)
        selected = dow to newList.indexOfFirst { it.start == start && it.end == end }
    }

    fun deleteSelected() {
        val (dow, i) = selected ?: return
        val list = week.days.getValue(dow)
        if (i in list.indices) setBlocks(dow, list.filterIndexed { idx, _ -> idx != i })
        selected = null
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
                    "tap free space to add · drag to move · handles to resize",
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

        Box(Modifier.weight(1f)) {
            Row(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                TimeAxis()
                days.forEach { dow ->
                    DayColumn(
                        dow = dow,
                        blocks = week.days.getValue(dow),
                        isToday = dow == today,
                        selectedIndex = selected?.takeIf { it.first == dow }?.second,
                        minutesPerPatient = week.minutesPerPatient,
                        onSelect = { i -> selected = if (i == null) null else dow to i },
                        onAdd = { minute -> addBlock(dow, minute) },
                        onMove = { i, d -> moveBlock(dow, i, d) },
                        onResize = { i, top, d -> resizeBlock(dow, i, top, d) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = selected != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val sel = selected
                val block = sel?.let { week.days.getValue(it.first).getOrNull(it.second) }
                Row(
                    Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardColor)
                        .border(1.dp, GridLine, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sel != null && block != null) {
                        Text(
                            "${sel.first.getDisplayName(TextStyle.SHORT, Locale.getDefault())} " +
                                "${formatMinutes(block.start)}–${formatMinutes(block.end)}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Delete",
                            color = Color(0xFFF87171),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { deleteSelected() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Text(
                            "Done",
                            color = Cyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selected = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
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
    selectedIndex: Int?,
    minutesPerPatient: Int,
    onSelect: (Int?) -> Unit,
    onAdd: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onResize: (Int, Boolean, Int) -> Unit,
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
            .pointerInput(dow) {
                detectTapGestures { offset ->
                    val minute = AXIS_START +
                        (offset.y / (HOUR_HEIGHT.toPx() / 60f)).toInt()
                    onAdd(minute)
                }
            }
    ) {
        blocks.forEachIndexed { i, block ->
            BlockView(
                block = block,
                isSelected = i == selectedIndex,
                minutesPerPatient = minutesPerPatient,
                onSelect = { onSelect(i) },
                onMove = { d -> onMove(i, d) },
                onResize = { top, d -> onResize(i, top, d) },
            )
        }
    }
}

@Composable
private fun BlockView(
    block: TimeBlock,
    isSelected: Boolean,
    minutesPerPatient: Int,
    onSelect: () -> Unit,
    onMove: (Int) -> Unit,
    onResize: (Boolean, Int) -> Unit,
) {
    val height = HOUR_HEIGHT * (block.duration / 60f)
    Box(
        Modifier
            .offset(y = minutesToDp(block.start))
            .padding(horizontal = 2.dp)
            .fillMaxWidth()
            .height(height)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        if (isSelected) listOf(Indigo, Violet)
                        else listOf(Indigo.copy(alpha = 0.55f), Violet.copy(alpha = 0.55f))
                    )
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, Cyan, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .pointerInput(block) {
                    detectTapGestures { onSelect() }
                }
                .pointerInput(block) {
                    val stepPx = HOUR_HEIGHT.toPx() * SNAP / 60f
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { onSelect(); acc = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            acc += amount.y
                            val steps = (acc / stepPx).toInt()
                            if (steps != 0) {
                                acc -= steps * stepPx
                                onMove(steps * SNAP)
                            }
                        }
                    )
                }
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

        if (isSelected) {
            ResizeHandle(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-7).dp),
                onDragSteps = { d -> onResize(true, d) }
            )
            ResizeHandle(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 7.dp),
                onDragSteps = { d -> onResize(false, d) }
            )
        }
    }
}

@Composable
private fun ResizeHandle(modifier: Modifier, onDragSteps: (Int) -> Unit) {
    Box(
        modifier
            .size(width = 34.dp, height = 14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Cyan)
            .pointerInput(Unit) {
                val stepPx = HOUR_HEIGHT.toPx() * SNAP / 60f
                var acc = 0f
                detectDragGestures(
                    onDragStart = { acc = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        acc += amount.y
                        val steps = (acc / stepPx).toInt()
                        if (steps != 0) {
                            acc -= steps * stepPx
                            onDragSteps(steps * SNAP)
                        }
                    }
                )
            }
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = 14.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BgTop)
        )
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

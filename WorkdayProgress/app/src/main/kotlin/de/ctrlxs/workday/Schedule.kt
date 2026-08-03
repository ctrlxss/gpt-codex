package de.ctrlxs.workday

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.math.ceil

/** A continuous stretch of work. Times are minutes since midnight. */
data class TimeBlock(val start: Int, val end: Int) {
    val duration: Int get() = end - start
}

data class WeekPlan(
    val days: Map<DayOfWeek, List<TimeBlock>> = DayOfWeek.entries
        .filter { it.value in 1..5 }
        .associateWith { DEFAULT_BLOCKS },
    val minutesPerPatient: Int = 20,
) {
    companion object {
        val DEFAULT_BLOCKS = listOf(TimeBlock(8 * 60, 12 * 60), TimeBlock(12 * 60 + 30, 17 * 60))
    }
}

enum class DayPhase { OFF, BEFORE, WORKING, BREAK, DONE }

data class DayStatus(
    val phase: DayPhase,
    val progress: Float,
    val secondsUntilStart: Long,
    val secondsUntilEnd: Long,
    val secondsUntilNextBlock: Long,
    val remainingWorkSeconds: Long,
    val totalWorkSeconds: Long,
    val patientsLeft: Int,
    val totalPatients: Int,
)

/** Blocks must be sorted and non-overlapping — the planner guarantees both. */
fun computeStatus(blocks: List<TimeBlock>, now: LocalTime, minutesPerPatient: Int): DayStatus {
    val off = DayStatus(DayPhase.OFF, 0f, 0, 0, 0, 0, 0, 0, 0)
    if (blocks.isEmpty()) return off

    val t = now.toSecondOfDay().toLong()
    val total = blocks.sumOf { it.duration * 60L }
    if (total <= 0) return off

    var worked = 0L
    for (b in blocks) {
        worked += t.coerceIn(b.start * 60L, b.end * 60L) - b.start * 60L
    }
    val remaining = (total - worked).coerceAtLeast(0)

    val firstStart = blocks.first().start * 60L
    val lastEnd = blocks.last().end * 60L
    val phase = when {
        t < firstStart -> DayPhase.BEFORE
        t >= lastEnd -> DayPhase.DONE
        blocks.any { t >= it.start * 60L && t < it.end * 60L } -> DayPhase.WORKING
        else -> DayPhase.BREAK
    }
    val nextStart = blocks.firstOrNull { it.start * 60L > t }?.let { it.start * 60L } ?: lastEnd

    val slot = minutesPerPatient * 60L
    return DayStatus(
        phase = phase,
        progress = (worked.toFloat() / total).coerceIn(0f, 1f),
        secondsUntilStart = (firstStart - t).coerceAtLeast(0),
        secondsUntilEnd = (lastEnd - t).coerceAtLeast(0),
        secondsUntilNextBlock = (nextStart - t).coerceAtLeast(0),
        remainingWorkSeconds = remaining,
        totalWorkSeconds = total,
        patientsLeft = if (slot > 0) ceil(remaining / slot.toDouble()).toInt() else 0,
        totalPatients = if (slot > 0) ceil(total / slot.toDouble()).toInt() else 0,
    )
}

class ScheduleStore(context: Context) {
    private val prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE)

    fun load(): WeekPlan {
        val days = WeekPlan().days.mapValues { (dow, defaults) ->
            val raw = prefs.getString("blocks_${dow.value}", null)
            when {
                raw != null -> parseBlocks(raw)
                else -> migrateLegacy(dow) ?: defaults
            }
        }
        return WeekPlan(days, prefs.getInt("minutes_per_patient", 20))
    }

    fun save(week: WeekPlan) {
        val editor = prefs.edit()
        week.days.forEach { (dow, blocks) ->
            editor.putString(
                "blocks_${dow.value}",
                blocks.joinToString(",") { "${it.start}-${it.end}" }
            )
        }
        editor.putInt("minutes_per_patient", week.minutesPerPatient)
        editor.apply()
    }

    private fun parseBlocks(raw: String): List<TimeBlock> =
        raw.split(",")
            .mapNotNull { part ->
                val p = part.split("-")
                if (p.size != 2) return@mapNotNull null
                val s = p[0].toIntOrNull() ?: return@mapNotNull null
                val e = p[1].toIntOrNull() ?: return@mapNotNull null
                if (e > s) TimeBlock(s, e) else null
            }
            .sortedBy { it.start }

    /** v1 stored "enabled|workStart|workEnd|breakStart|breakEnd" per day. */
    private fun migrateLegacy(dow: DayOfWeek): List<TimeBlock>? {
        val raw = prefs.getString("day_${dow.value}", null) ?: return null
        val p = raw.split("|")
        if (p.size != 5) return null
        if (p[0] != "1") return emptyList()
        val ws = p[1].toIntOrNull() ?: return null
        val we = p[2].toIntOrNull() ?: return null
        val bs = (p[3].toIntOrNull() ?: return null).coerceIn(ws, we)
        val be = (p[4].toIntOrNull() ?: return null).coerceIn(bs, we)
        return listOf(TimeBlock(ws, bs), TimeBlock(be, we)).filter { it.duration > 0 }
    }
}

fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

fun formatCountdown(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%d:%02d:%02d".format(h, m, s)
}

fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

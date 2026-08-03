package de.ctrlxs.workday

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.math.ceil

/** Times are stored as minutes since midnight. */
data class DaySchedule(
    val enabled: Boolean = true,
    val workStart: Int = 8 * 60,
    val workEnd: Int = 17 * 60,
    val breakStart: Int = 12 * 60,
    val breakEnd: Int = 12 * 60 + 30,
)

data class WeekSchedule(
    val days: Map<DayOfWeek, DaySchedule> = DayOfWeek.entries
        .filter { it.value in 1..5 }
        .associateWith { DaySchedule() },
    val minutesPerPatient: Int = 20,
)

enum class DayPhase { OFF, BEFORE, WORKING, BREAK, DONE }

data class DayStatus(
    val phase: DayPhase,
    val progress: Float,
    val secondsUntilStart: Long,
    val secondsUntilEnd: Long,
    val secondsUntilBreakEnd: Long,
    val remainingWorkSeconds: Long,
    val totalWorkSeconds: Long,
    val patientsLeft: Int,
    val totalPatients: Int,
)

fun computeStatus(day: DaySchedule?, now: LocalTime, minutesPerPatient: Int): DayStatus {
    val off = DayStatus(DayPhase.OFF, 0f, 0, 0, 0, 0, 0, 0, 0)
    if (day == null || !day.enabled) return off

    val t = now.toSecondOfDay().toLong()
    val ws = day.workStart * 60L
    val we = day.workEnd * 60L
    if (we <= ws) return off

    // Clamp the break inside the work window so a misconfigured break can't break the math.
    val bs = (day.breakStart * 60L).coerceIn(ws, we)
    val be = (day.breakEnd * 60L).coerceIn(bs, we)

    val totalWork = (we - ws) - (be - bs)
    val worked = ((t.coerceIn(ws, we) - ws) - (t.coerceIn(bs, be) - bs)).coerceAtLeast(0)
    val remaining = (totalWork - worked).coerceAtLeast(0)

    val phase = when {
        t < ws -> DayPhase.BEFORE
        t >= we -> DayPhase.DONE
        t in bs until be -> DayPhase.BREAK
        else -> DayPhase.WORKING
    }

    val slot = minutesPerPatient * 60L
    return DayStatus(
        phase = phase,
        progress = if (totalWork > 0) (worked.toFloat() / totalWork).coerceIn(0f, 1f) else 0f,
        secondsUntilStart = (ws - t).coerceAtLeast(0),
        secondsUntilEnd = (we - t).coerceAtLeast(0),
        secondsUntilBreakEnd = (be - t).coerceAtLeast(0),
        remainingWorkSeconds = remaining,
        totalWorkSeconds = totalWork,
        patientsLeft = if (slot > 0) ceil(remaining / slot.toDouble()).toInt() else 0,
        totalPatients = if (slot > 0) ceil(totalWork / slot.toDouble()).toInt() else 0,
    )
}

class ScheduleStore(context: Context) {
    private val prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE)

    fun load(): WeekSchedule {
        val defaults = WeekSchedule()
        val days = defaults.days.mapValues { (dow, def) ->
            val raw = prefs.getString("day_${dow.value}", null) ?: return@mapValues def
            val p = raw.split("|")
            if (p.size != 5) def else DaySchedule(
                enabled = p[0] == "1",
                workStart = p[1].toIntOrNull() ?: def.workStart,
                workEnd = p[2].toIntOrNull() ?: def.workEnd,
                breakStart = p[3].toIntOrNull() ?: def.breakStart,
                breakEnd = p[4].toIntOrNull() ?: def.breakEnd,
            )
        }
        return WeekSchedule(days, prefs.getInt("minutes_per_patient", 20))
    }

    fun save(week: WeekSchedule) {
        val editor = prefs.edit()
        week.days.forEach { (dow, d) ->
            val e = if (d.enabled) "1" else "0"
            editor.putString("day_${dow.value}", "$e|${d.workStart}|${d.workEnd}|${d.breakStart}|${d.breakEnd}")
        }
        editor.putInt("minutes_per_patient", week.minutesPerPatient)
        editor.apply()
    }
}

fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

fun formatCountdown(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%d:%02d:%02d".format(h, m, s)
}

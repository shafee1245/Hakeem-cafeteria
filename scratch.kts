import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

fun parseTime(t: String): java.time.LocalTime {
    val clean = t.trim().uppercase()
    return try {
        java.time.LocalTime.parse(clean)
    } catch (e: Exception) {
        val fmt = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("h:mm a")
            .toFormatter(Locale.US)
        java.time.LocalTime.parse(clean, fmt)
    }
}

println("09:00 AM -> ${parseTime("09:00 AM")}")
println("22:00 -> ${parseTime("22:00")}")

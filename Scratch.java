import java.time.LocalTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class Scratch {
    public static LocalTime parseTime(String t) {
        String clean = t.trim().toUpperCase();
        try {
            return LocalTime.parse(clean);
        } catch (Exception e) {
            java.time.format.DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("h:mm a")
                .toFormatter(Locale.US);
            return LocalTime.parse(clean, fmt);
        }
    }

    public static void main(String[] args) {
        System.out.println("09:00 AM -> " + parseTime("09:00 AM"));
        System.out.println("22:00 -> " + parseTime("22:00"));
    }
}

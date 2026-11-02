package util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Định dạng hiển thị: tiền, ngày theo kiểu Việt Nam. */
public final class Fmt {
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fmt() {}

    /** 112400000 -> "112.400.000 ₫" */
    public static String money(long v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", s).format(v) + " ₫";
    }

    public static String date(LocalDate d) { return d == null ? "" : d.format(DMY); }

    /** "Thứ Bảy, 31/10/2026" */
    public static String weekdayDate(LocalDate d) {
        return weekday(d.getDayOfWeek()) + ", " + date(d);
    }

    public static String weekday(DayOfWeek w) {
        return switch (w) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }
}

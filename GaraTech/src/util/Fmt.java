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

    public static String money(double v) { return money(Math.round(v)); }

    /** 112400000 -> "112.400.000 ₫" */
    public static String money(long v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", s).format(v) + " ₫";
    }

    public static String date(LocalDate d) { return d == null ? "" : d.format(DMY); }

    /** "dd/MM/yyyy HH:mm" (rỗng nếu null). */
    public static String dateTime(java.time.LocalDateTime t) {
        return t == null ? "" : t.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** 18600000 -> "18,6 tr" (dùng cho số tròn triệu trên biểu đồ / ô thống kê). */
    public static String trieu(long v) {
        return String.format(Locale.ROOT, "%.1f", v / 1_000_000.0).replace('.', ',') + " tr";
    }

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

    /** Đọc số tiền bằng chữ tiếng Việt (VD: 1.144.000 -> "Một triệu một trăm bốn mươi bốn nghìn đồng chẵn.") */
    public static String moneyInWords(long amount) {
        if (amount == 0) return "Không đồng";
        if (amount < 0) return "Âm " + moneyInWords(-amount);

        final String[] chuSo = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        final String[] donVi = {"", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"};

        long temp = amount;
        java.util.List<Integer> blocks = new java.util.ArrayList<>();
        while (temp > 0) {
            blocks.add((int) (temp % 1000));
            temp /= 1000;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            int b = blocks.get(i);
            if (b == 0) continue;

            int tram = b / 100;
            int chuc = (b % 100) / 10;
            int donViNum = b % 10;

            StringBuilder bText = new StringBuilder();
            if (tram > 0 || (i < blocks.size() - 1 && (chuc > 0 || donViNum > 0))) {
                bText.append(chuSo[tram]).append(" trăm ");
            }

            if (chuc > 1) {
                bText.append(chuSo[chuc]).append(" mươi ");
                if (donViNum == 1) {
                    bText.append("mốt ");
                } else if (donViNum == 4) {
                    bText.append("tư ");
                } else if (donViNum == 5) {
                    bText.append("lăm ");
                } else if (donViNum > 0) {
                    bText.append(chuSo[donViNum]).append(" ");
                }
            } else if (chuc == 1) {
                bText.append("mười ");
                if (donViNum == 5) {
                    bText.append("lăm ");
                } else if (donViNum > 0) {
                    bText.append(chuSo[donViNum]).append(" ");
                }
            } else if (chuc == 0 && donViNum > 0) {
                if (tram > 0 || i < blocks.size() - 1) {
                    bText.append("lẻ ");
                }
                bText.append(chuSo[donViNum]).append(" ");
            }

            if (!bText.isEmpty()) {
                sb.append(bText.toString().trim()).append(" ").append(donVi[i]).append(" ");
            }
        }

        String res = sb.toString().trim().replaceAll("\\s+", " ");
        if (res.isEmpty()) return "Không đồng";
        return Character.toUpperCase(res.charAt(0)) + res.substring(1) + " đồng chẵn.";
    }
}

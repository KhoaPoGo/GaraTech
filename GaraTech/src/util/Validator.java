package util;

import java.util.regex.Pattern;

/** Các kiểm tra dữ liệu theo "Ràng buộc miền giá trị" trong báo cáo. Trả về true nếu hợp lệ. */
public final class Validator {
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validator() {}

    public static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    /** Số điện thoại: đúng 10 chữ số, bắt đầu bằng 0. */
    public static boolean phone(String s) { return s != null && PHONE.matcher(s.trim()).matches(); }

    public static boolean email(String s) { return s != null && EMAIL.matcher(s.trim()).matches(); }

    /** Tiền, số lượng, số km: >= 0. */
    public static boolean nonNegative(double v) { return v >= 0; }

    /** heSoLuong phải nằm trong [1.84, 6.5]. */
    public static boolean heSoLuong(double v) { return v >= 1.84 && v <= 6.5; }

    /** Biển số xe VN (đơn giản): 2 số + 1-2 chữ + (dấu - tùy chọn) + 4-5 số, ví dụ 51H-12345, 60A-24680. */
    public static boolean bienSo(String s) {
        return s != null && s.trim().toUpperCase().matches("^\\d{2}[A-Z]{1,2}\\d?-?\\d{4,5}(\\.\\d{2})?$");
    }
}

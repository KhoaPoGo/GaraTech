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

    /**
     * heSoLuong phải nằm trong [1.0, 5.0] (CK_NV_heso trong sql/01_TaoBang.sql).
     * Lưu ý: báo cáo ghi [1.84, 6.5], nhóm cần chốt một khoảng rồi sửa đồng thời SQL và hàm này.
     */
    public static boolean heSoLuong(double v) { return v >= 1.0 && v <= 5.0; }

    /** Biển số xe VN: 2 số + 1-2 chữ (+ 1 số cho xe máy) + dấu - tùy chọn + 4 số, 5 số hoặc dạng 123.45. Ví dụ 51A-123.45, 51H-12345, 60A-24680. */
    public static boolean bienSo(String s) {
        return s != null && s.trim().toUpperCase().matches("^\\d{2}[A-Z]{1,2}\\d?-?(\\d{3}\\.\\d{2}|\\d{4,5})$");
    }
}

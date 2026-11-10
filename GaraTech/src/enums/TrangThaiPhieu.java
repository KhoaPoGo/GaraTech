package enums;

/**
 * Enum 4 trạng thái chuẩn của Phiếu tiếp nhận - sửa chữa (PhieuTiepNhanSuaChua):
 * 1. Chờ sửa chữa (CHO_SUA)
 * 2. Đang sửa chữa (DANG_SUA)
 * 3. Chờ linh kiện (CHO_LINH_KIEN)
 * 4. Hoàn tất (HOAN_TAT)
 * 
 * Luồng chuyển đổi hợp lệ:
 * CHO_SUA -> DANG_SUA <-> CHO_LINH_KIEN -> HOAN_TAT
 */
public enum TrangThaiPhieu {
    CHO_SUA("Chờ sửa chữa"),
    DANG_SUA("Đang sửa chữa"),
    CHO_LINH_KIEN("Chờ linh kiện"),
    HOAN_TAT("Hoàn tất");

    private final String label;

    TrangThaiPhieu(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isLocked() {
        return this == HOAN_TAT;
    }

    /**
     * Chuyển đổi an toàn từ chuỗi trong cơ sở dữ liệu (hỗ trợ cả mã code, label mới và dữ liệu cũ).
     */
    public static TrangThaiPhieu fromDb(String text) {
        if (text == null || text.isBlank()) return CHO_SUA;
        String trimmed = text.trim();

        // 1. Khớp theo name hoặc label chính thức
        for (TrangThaiPhieu tt : values()) {
            if (tt.name().equalsIgnoreCase(trimmed) || tt.label.equalsIgnoreCase(trimmed)) {
                return tt;
            }
        }

        // 2. Tương thích ánh xạ dữ liệu cũ nếu database còn lưu chuỗi cũ
        String upper = trimmed.toUpperCase();
        if (upper.contains("CHO_LINH_KIEN") || upper.contains("CHỜ LINH KIỆN") || upper.contains("CHO LINH KIEN")) {
            return CHO_LINH_KIEN;
        }
        if (upper.contains("DANG") || upper.contains("ĐANG") || upper.contains("KIEM_TRA") || upper.contains("KIỂM TRA")) {
            return DANG_SUA;
        }
        if (upper.contains("HOAN") || upper.contains("HOÀN") || upper.contains("BAN_GIAO") || upper.contains("BÀN GIAO")) {
            return HOAN_TAT;
        }

        return CHO_SUA;
    }

    @Override
    public String toString() {
        return label;
    }
}

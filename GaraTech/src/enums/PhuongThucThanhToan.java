package enums;

/**
 * Phương thức thanh toán: TIEN_MAT (Tiền mặt), CHUYEN_KHOAN (Chuyển khoản).
 */
public enum PhuongThucThanhToan {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản");

    private final String label;

    PhuongThucThanhToan(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static PhuongThucThanhToan fromString(String s) {
        if (s == null) return null;
        for (PhuongThucThanhToan p : values()) {
            if (p.name().equalsIgnoreCase(s.trim()) || p.label.equalsIgnoreCase(s.trim())) {
                return p;
            }
        }
        return null;
    }

    public static PhuongThucThanhToan fromLabel(String s) {
        return fromString(s);
    }
}

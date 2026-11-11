package enums;

/**
 * Trạng thái thanh toán của hóa đơn: CHUA_THANH_TOAN (Chưa thanh toán), DA_THANH_TOAN (Đã thanh toán).
 */
public enum TrangThaiThanhToan {
    CHUA_THANH_TOAN("Chưa thanh toán"),
    DA_THANH_TOAN("Đã thanh toán");

    private final String label;

    TrangThaiThanhToan(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static TrangThaiThanhToan fromString(String s) {
        if (s == null) return null;
        for (TrangThaiThanhToan t : values()) {
            if (t.name().equalsIgnoreCase(s.trim()) || t.label.equalsIgnoreCase(s.trim())) {
                return t;
            }
        }
        return null;
    }

    public static TrangThaiThanhToan fromLabel(String s) {
        return fromString(s);
    }
}

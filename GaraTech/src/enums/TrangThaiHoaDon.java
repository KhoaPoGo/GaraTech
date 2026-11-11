package enums;

/**
 * Trạng thái hóa đơn: CHUA_THANH_TOAN ("Chưa thanh toán"), DA_THANH_TOAN ("Đã thanh toán").
 */
public enum TrangThaiHoaDon {
    CHUA_THANH_TOAN("Chưa thanh toán"),
    DA_THANH_TOAN("Đã thanh toán");

    private final String label;

    TrangThaiHoaDon(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiHoaDon fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiHoaDon t : values()) {
            if (t.name().equalsIgnoreCase(s.trim()) || t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }
}

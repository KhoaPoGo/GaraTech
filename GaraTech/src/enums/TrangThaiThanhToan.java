package enums;

/** Trạng thái GiaoDichThanhToan (class spec 2.9). Chuỗi label phải khớp đúng giá trị lưu trong CSDL. */
public enum TrangThaiThanhToan {
    CHO_THANH_TOAN("Chờ thanh toán"),
    DA_THANH_TOAN("Đã thanh toán"),
    HUY("Hủy");

    private final String label;

    TrangThaiThanhToan(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiThanhToan fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiThanhToan t : values()) {
            if (t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }
}

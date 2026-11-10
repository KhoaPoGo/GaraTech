package enums;

/**
 * Trạng thái HoaDon (ràng buộc CHECK CK_HD_tt trong sql/01_TaoBang.sql).
 * Chuỗi label khớp chính xác với giá trị lưu trữ trong CSDL SQL Server:
 * - CHUA_THANH_TOAN: "Chưa thanh toán"
 * - THANH_TOAN_MOT_PHAN: "Đã thanh toán một phần"
 * - DA_THANH_TOAN: "Đã thanh toán"
 * - DA_HUY: "Đã hủy"
 */
public enum TrangThaiHoaDon {
    CHUA_THANH_TOAN("Chưa thanh toán"),
    THANH_TOAN_MOT_PHAN("Đã thanh toán một phần"),
    DA_THANH_TOAN("Đã thanh toán"),
    DA_HUY("Đã hủy");

    private final String label;

    TrangThaiHoaDon(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiHoaDon fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiHoaDon t : values()) {
            if (t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }
}

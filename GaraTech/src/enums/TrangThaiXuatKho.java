package enums;

/** Trạng thái PhieuXuatKho (class spec 2.13). Chuỗi label phải khớp đúng giá trị lưu trong CSDL. */
public enum TrangThaiXuatKho {
    CHO_XAC_NHAN("Chờ xác nhận"),
    DA_XUAT_KHO("Đã xuất kho"),
    DA_HUY("Đã hủy");

    private final String label;

    TrangThaiXuatKho(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiXuatKho fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiXuatKho t : values()) {
            if (t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }
}

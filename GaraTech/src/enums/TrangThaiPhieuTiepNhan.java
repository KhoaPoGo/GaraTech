package enums;

/** Trạng thái PhieuTiepNhan (3 trạng thái). Chuỗi label khớp đúng giá trị lưu trong CSDL. */
public enum TrangThaiPhieuTiepNhan {
    CHO_KHACH_HANG_XAC_NHAN("Chờ khách hàng xác nhận"),
    DA_XAC_NHAN("Đã xác nhận"),
    DA_HUY("Đã hủy");

    private final String label;

    TrangThaiPhieuTiepNhan(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiPhieuTiepNhan fromLabel(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.equalsIgnoreCase("Chờ khách xác nhận") || trimmed.equalsIgnoreCase("Chờ khách hàng xác nhận")) {
            return CHO_KHACH_HANG_XAC_NHAN;
        }
        for (TrangThaiPhieuTiepNhan t : values()) {
            if (t.label.equalsIgnoreCase(trimmed)) return t;
        }
        return null;
    }
}

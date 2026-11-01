package enums;

/** Trạng thái PhieuTiepNhan (class spec 2.5). Chuỗi label phải khớp đúng giá trị lưu trong CSDL. */
public enum TrangThaiPhieuTiepNhan {
    CHO_XU_LY("Chờ xử lý"),
    DANG_KIEM_TRA("Đang kiểm tra"),
    CHO_KHACH_XAC_NHAN("Chờ khách xác nhận"),
    DA_HUY("Đã hủy");

    private final String label;

    TrangThaiPhieuTiepNhan(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiPhieuTiepNhan fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiPhieuTiepNhan t : values()) {
            if (t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }
}

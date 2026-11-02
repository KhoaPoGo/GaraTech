package enums;

/** Trạng thái HangMucSuaChua (class spec 2.7). Chuỗi label phải khớp đúng giá trị lưu trong CSDL. */
public enum TrangThaiHangMuc {
    CHO_DUYET("Chờ duyệt"),
    DA_XAC_NHAN("Đã xác nhận"),
    TU_CHOI("Từ chối"),
    HOAN_THANH("Hoàn thành");

    private final String label;

    TrangThaiHangMuc(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiHangMuc fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiHangMuc t : values()) {
            if (t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }
}

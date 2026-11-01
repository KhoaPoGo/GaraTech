package enums;

/** Trạng thái PhieuSuaChua (class spec 2.6). Chuỗi label phải khớp đúng giá trị lưu trong CSDL. */
public enum TrangThaiPhieuSuaChua {
    DA_XAC_NHAN("Đã xác nhận"),
    DANG_SUA_CHUA("Đang sửa chữa"),
    CHO_KIEM_TRA("Chờ kiểm tra"),
    HOAN_THANH("Hoàn thành"),
    DA_BAN_GIAO("Đã bàn giao"),
    DA_HUY("Đã hủy");

    private final String label;

    TrangThaiPhieuSuaChua(String label) { this.label = label; }

    public String label() { return label; }

    @Override public String toString() { return label; }

    public static TrangThaiPhieuSuaChua fromLabel(String s) {
        if (s == null) return null;
        for (TrangThaiPhieuSuaChua t : values()) {
            if (t.label.equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }

    /**
     * Luồng chuyển trạng thái hợp lệ (báo cáo, mục 3 & yêu cầu phi chức năng "Tính nhất quán nghiệp vụ"):
     * Đã xác nhận -> Đang sửa chữa -> Chờ kiểm tra -> Hoàn thành -> Đã bàn giao; Đã hủy chỉ từ Đã xác nhận.
     * Service luôn gọi hàm này trước khi cập nhật trạng thái.
     */
    public boolean canMoveTo(TrangThaiPhieuSuaChua next) {
        return switch (this) {
            case DA_XAC_NHAN -> next == DANG_SUA_CHUA || next == DA_HUY;
            case DANG_SUA_CHUA -> next == CHO_KIEM_TRA;
            case CHO_KIEM_TRA -> next == HOAN_THANH || next == DANG_SUA_CHUA; // quản lý có thể trả về làm lại
            case HOAN_THANH -> next == DA_BAN_GIAO;
            case DA_BAN_GIAO, DA_HUY -> false;
        };
    }
}

package enums;

import java.text.Normalizer;

/** 4 vai trò trong hệ thống. dbValue khớp đúng ràng buộc CK_NV_vaitro của cột NhanVien.vaiTro (sql/01_TaoBang.sql). */
public enum Role {
    QUAN_LY("Quản lý", "Quản lý"),
    TIEP_NHAN("Nhân viên tiếp nhận", "Nhân viên tiếp nhận"),
    KY_THUAT_VIEN("Kỹ thuật viên", "Kỹ thuật viên"),
    KHO("Nhân viên kho", "Nhân viên kho");

    private final String dbValue;
    private final String displayName;

    Role(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() { return dbValue; }
    public String displayName() { return displayName; }

    /** Đổi giá trị cột vaiTro thành Role; trả về null nếu không khớp. Chuẩn hóa Unicode để chữ có dấu gõ kiểu nào cũng khớp. */
    public static Role fromDb(String s) {
        if (s == null) return null;
        String x = Normalizer.normalize(s.trim(), Normalizer.Form.NFC);
        for (Role r : values()) {
            if (Normalizer.normalize(r.dbValue, Normalizer.Form.NFC).equalsIgnoreCase(x)) return r;
        }
        return null;
    }
}

package enums;

/** 4 vai trò trong hệ thống. dbValue khớp ràng buộc CHECK của cột NhanVien.vaiTro. */
public enum Role {
    QUAN_LY("Quản Lý", "Quản lý"),
    TIEP_NHAN("Tiếp Nhận", "Nhân viên tiếp nhận"),
    KY_THUAT_VIEN("Kỹ Thuật Viên", "Kỹ thuật viên"),
    KHO("Kho", "Nhân viên kho");

    private final String dbValue;
    private final String displayName;

    Role(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() { return dbValue; }
    public String displayName() { return displayName; }

    public static Role fromDb(String s) {
        if (s == null) return null;
        for (Role r : values()) {
            if (r.dbValue.equalsIgnoreCase(s.trim())) return r;
        }
        return null;
    }
}

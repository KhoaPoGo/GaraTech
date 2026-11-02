package dao;

import connectDB.ConnectDB;
import enums.Role;
import enums.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Truy vấn đăng nhập trên TaiKhoan(tenDangNhap, matKhau, trangThaiDangNhap, maNV)
 * JOIN NhanVien(maNV, hoTen, vaiTro, dangLam) theo sql/01_TaoBang.sql.
 * Nếu đổi tên bảng / cột trong DDL thì CHỈ sửa file này.
 */
public final class TaiKhoanDao {
    /** Giá trị cột trangThaiDangNhap (khớp CHECK CK_TK_tt). */
    public static final String TT_HOAT_DONG = "Hoạt động";
    public static final String TT_DANG_KHOA = "Đang khóa";

    private TaiKhoanDao() {}

    /** Một dòng tài khoản dùng cho đăng nhập: người dùng + chuỗi hash mật khẩu + trạng thái. */
    public record LoginRow(User user, String matKhau, String trangThaiDangNhap, boolean dangLam) {
        /** Tài khoản đang được phép đăng nhập (trangThaiDangNhap = 'Hoạt động'). */
        public boolean hoatDong() { return TT_HOAT_DONG.equalsIgnoreCase(trangThaiDangNhap); }
    }

    /**
     * Tìm tài khoản theo tên đăng nhập. KHÔNG so sánh mật khẩu ở đây: mật khẩu lưu dạng PBKDF2 có salt riêng,
     * việc kiểm tra do {@code util.PasswordUtil.verify} làm ở tầng bus.
     *
     * @return null nếu không có tài khoản này
     */
    public static LoginRow findByUsername(String tenDangNhap) throws SQLException {
        String sql = """
                SELECT tk.tenDangNhap, tk.matKhau, tk.trangThaiDangNhap,
                       nv.maNV, nv.hoTen, nv.vaiTro, nv.dangLam
                FROM TaiKhoan tk
                JOIN NhanVien nv ON nv.maNV = tk.maNV
                WHERE tk.tenDangNhap = ?
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = new User(rs.getString("maNV"), rs.getString("hoTen"),
                        Role.fromDb(rs.getString("vaiTro")), rs.getString("tenDangNhap"));
                return new LoginRow(u, rs.getString("matKhau"),
                        rs.getString("trangThaiDangNhap"), rs.getBoolean("dangLam"));
            }
        }
    }
}

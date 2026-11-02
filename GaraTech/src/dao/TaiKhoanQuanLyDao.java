package dao;

import connectDB.ConnectDB;
import entity.NhanVien;
import enums.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý tài khoản (cấp mới, đặt lại mật khẩu, khóa / mở khóa) cho màn hình của quản lý.
 * Tách khỏi {@link TaiKhoanDao} (chỉ phục vụ đăng nhập) để hai chức năng không sửa chung một file.
 */
public class TaiKhoanQuanLyDao {

    /** Một dòng trong tab Tài khoản: tài khoản + thông tin nhân viên sở hữu. */
    public record TaiKhoanRow(String tenDangNhap, String trangThai, String maNV, String hoTen, Role vaiTro, boolean dangLam) {}

    public List<TaiKhoanRow> findAll() throws SQLException {
        String sql = "SELECT tk.tenDangNhap, tk.trangThaiDangNhap, tk.maNV, nv.hoTen, nv.vaiTro, nv.dangLam"
                + " FROM TaiKhoan tk JOIN NhanVien nv ON nv.maNV = tk.maNV ORDER BY tk.maNV";
        List<TaiKhoanRow> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new TaiKhoanRow(rs.getString("tenDangNhap"), rs.getString("trangThaiDangNhap"),
                        rs.getString("maNV"), rs.getString("hoTen"), Role.fromDb(rs.getString("vaiTro")),
                        rs.getBoolean("dangLam")));
            }
        }
        return out;
    }

    /** Nhân viên đang làm nhưng chưa có tài khoản (để cấp tài khoản mới). */
    public List<NhanVien> findNhanVienChuaCoTaiKhoan() throws SQLException {
        String sql = "SELECT nv.maNV, nv.hoTen, nv.soDienThoai, nv.email, nv.diaChi, nv.vaiTro, nv.heSoLuong, nv.luongCoBan, nv.dangLam"
                + " FROM NhanVien nv WHERE nv.dangLam = 1 AND NOT EXISTS (SELECT 1 FROM TaiKhoan tk WHERE tk.maNV = nv.maNV)"
                + " ORDER BY nv.maNV";
        List<NhanVien> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new NhanVien(rs.getString("maNV"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                        rs.getString("email"), rs.getString("diaChi"), Role.fromDb(rs.getString("vaiTro")),
                        rs.getBigDecimal("heSoLuong").doubleValue(), rs.getLong("luongCoBan"), rs.getBoolean("dangLam")));
            }
        }
        return out;
    }

    public boolean existsTenDangNhap(String ten) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM TaiKhoan WHERE LOWER(tenDangNhap) = LOWER(?)")) {
            ps.setString(1, ten);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void insert(String tenDangNhap, String matKhauHash, String maNV) throws SQLException {
        String sql = "INSERT INTO TaiKhoan(tenDangNhap, matKhau, trangThaiDangNhap, maNV) VALUES (?,?,N'Hoạt động',?)";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, matKhauHash);
            ps.setString(3, maNV);
            ps.executeUpdate();
        }
    }

    public int updateMatKhau(String tenDangNhap, String matKhauHash) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE TaiKhoan SET matKhau = ? WHERE tenDangNhap = ?")) {
            ps.setString(1, matKhauHash);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate();
        }
    }

    public int updateTrangThai(String tenDangNhap, String trangThai) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE TaiKhoan SET trangThaiDangNhap = ? WHERE tenDangNhap = ?")) {
            ps.setNString(1, trangThai);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate();
        }
    }
}

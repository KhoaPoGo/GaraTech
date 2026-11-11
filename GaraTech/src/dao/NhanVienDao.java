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

/** Truy vấn bảng NhanVien (màn Dịch vụ & nhân sự của quản lý). */
public class NhanVienDao {
    private static final String COLS = "maNV, hoTen, soDienThoai, email, diaChi, vaiTro, heSoLuong, luongCoBan, dangLam";

    private static NhanVien map(ResultSet rs) throws SQLException {
        return new NhanVien(rs.getString("maNV"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                rs.getString("email"), rs.getString("diaChi"), rs.getString("vaiTro"),
                rs.getBigDecimal("heSoLuong").doubleValue(), rs.getInt("luongCoBan"), rs.getBoolean("dangLam"));
    }

    public List<NhanVien> findAll() throws SQLException {
        List<NhanVien> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM NhanVien ORDER BY maNV");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<NhanVien> findKyThuatVien() throws SQLException {
        List<NhanVien> out = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM NhanVien WHERE vaiTro = N'Kỹ thuật viên' AND dangLam = 1 ORDER BY hoTen";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public NhanVien findById(String maNV) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM NhanVien WHERE maNV = ?")) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Đã có nhân viên khác (khác maNV) dùng số điện thoại / email này chưa. */
    public String findDuplicate(String sdt, String email, String exceptMaNV) throws SQLException {
        String sql = "SELECT soDienThoai, email FROM NhanVien WHERE maNV <> ? AND (soDienThoai = ? OR LOWER(email) = LOWER(?))";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, exceptMaNV == null ? "" : exceptMaNV);
            ps.setString(2, sdt);
            ps.setString(3, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return sdt.equals(rs.getString("soDienThoai")) ? "Số điện thoại " + sdt : "Email " + email;
            }
        }
    }

    public void insert(Connection c, NhanVien n) throws SQLException {
        String sql = "INSERT INTO NhanVien(" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getMaNV());
            ps.setNString(2, n.getHoTen());
            ps.setString(3, n.getSoDienThoai());
            ps.setString(4, n.getEmail());
            ps.setNString(5, n.getDiaChi());
            ps.setNString(6, n.getVaiTro());
            ps.setBigDecimal(7, java.math.BigDecimal.valueOf(n.getHeSoLuong()));
            ps.setInt(8, n.getLuongCoBan());
            ps.setBoolean(9, n.isDangLam());
            ps.executeUpdate();
        }
    }

    /** Sửa thông tin (không đổi mã, không đổi trạng thái làm việc). */
    public int update(NhanVien n) throws SQLException {
        String sql = "UPDATE NhanVien SET hoTen=?, soDienThoai=?, email=?, diaChi=?, vaiTro=?, heSoLuong=?, luongCoBan=?"
                + " WHERE maNV=?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setNString(1, n.getHoTen());
            ps.setString(2, n.getSoDienThoai());
            ps.setString(3, n.getEmail());
            ps.setNString(4, n.getDiaChi());
            ps.setNString(5, n.getVaiTro());
            ps.setBigDecimal(6, java.math.BigDecimal.valueOf(n.getHeSoLuong()));
            ps.setInt(7, n.getLuongCoBan());
            ps.setString(8, n.getMaNV());
            return ps.executeUpdate();
        }
    }

    public int setDangLam(String maNV, boolean dangLam) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE NhanVien SET dangLam=? WHERE maNV=?")) {
            ps.setBoolean(1, dangLam);
            ps.setString(2, maNV);
            return ps.executeUpdate();
        }
    }

    /** Số phiếu thống nhất chưa hoàn tất đang gắn kỹ thuật viên này. */
    public int demPhieuDangGiao(String maKTV) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE maNVKyThuat = ?"
                + " AND trangThai IN ('CHO_SUA','DANG_SUA','CHO_LINH_KIEN')";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}

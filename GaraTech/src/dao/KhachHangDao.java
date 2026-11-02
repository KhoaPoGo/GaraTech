package dao;

import connectDB.ConnectDB;
import entity.KhachHang;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho KhachHang (bảng KhachHang trong SQL Server).
 */
public final class KhachHangDao {

    private KhachHangDao() {}

    public static List<KhachHang> getAll() throws SQLException {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT maKH, hoTen, soDienThoai, diaChi, email, diemTichLuy FROM KhachHang ORDER BY maKH DESC";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(fromResultSet(rs));
            }
        }
        return list;
    }

    public static KhachHang findById(String maKH) throws SQLException {
        String sql = "SELECT maKH, hoTen, soDienThoai, diaChi, email, diemTichLuy FROM KhachHang WHERE maKH = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        }
        return null;
    }

    public static KhachHang findByPhone(String soDienThoai) throws SQLException {
        String sql = "SELECT maKH, hoTen, soDienThoai, diaChi, email, diemTichLuy FROM KhachHang WHERE soDienThoai = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, soDienThoai);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        }
        return null;
    }

    public static boolean existsPhone(String soDienThoai, String excludeMaKH) throws SQLException {
        String sql = "SELECT 1 FROM KhachHang WHERE soDienThoai = ?" + (excludeMaKH != null ? " AND maKH <> ?" : "");
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, soDienThoai);
            if (excludeMaKH != null) {
                ps.setString(2, excludeMaKH);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean insert(KhachHang kh) throws SQLException {
        try (Connection c = ConnectDB.getConnection()) {
            return insert(c, kh);
        }
    }

    public static boolean insert(Connection c, KhachHang kh) throws SQLException {
        String sql = "INSERT INTO KhachHang (maKH, hoTen, soDienThoai, diaChi, email, diemTichLuy) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, kh.getMaKH());
            ps.setString(2, kh.getHoTen());
            ps.setString(3, kh.getSoDienThoai());
            ps.setString(4, kh.getDiaChi());
            ps.setString(5, kh.getEmail());
            ps.setInt(6, kh.getDiemTichLuy());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean update(KhachHang kh) throws SQLException {
        String sql = "UPDATE KhachHang SET hoTen = ?, soDienThoai = ?, diaChi = ?, email = ?, diemTichLuy = ? WHERE maKH = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSoDienThoai());
            ps.setString(3, kh.getDiaChi());
            ps.setString(4, kh.getEmail());
            ps.setInt(5, kh.getDiemTichLuy());
            ps.setString(6, kh.getMaKH());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean delete(String maKH) throws SQLException {
        String sql = "DELETE FROM KhachHang WHERE maKH = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKH);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean hasTransactions(String maKH) throws SQLException {
        String sql = "SELECT 1 WHERE EXISTS (SELECT 1 FROM PhieuTiepNhan WHERE maKH = ?) " +
                     "OR EXISTS (SELECT 1 FROM Xe WHERE maKH = ?)";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKH);
            ps.setString(2, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM KhachHang";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static KhachHang fromResultSet(ResultSet rs) throws SQLException {
        return new KhachHang(
                rs.getString("maKH"),
                rs.getString("hoTen"),
                rs.getString("soDienThoai"),
                rs.getString("diaChi"),
                rs.getString("email"),
                rs.getInt("diemTichLuy")
        );
    }
}

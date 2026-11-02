package dao;

import connectDB.ConnectDB;
import entity.PhuTung;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PhuTungDao {
    private static final String COLS = "maPhuTung, tenPhuTung, loaiPhuTung, donViTinh, donGiaNhap, donGiaBan, soLuongTon, tonKhoToiThieu, viTriLuuKho, thoiGianBaoHanh";

    private static PhuTung map(ResultSet rs) throws SQLException {
        return new PhuTung(
                rs.getString("maPhuTung"),
                rs.getString("tenPhuTung"),
                rs.getString("loaiPhuTung"),
                rs.getString("donViTinh"),
                rs.getDouble("donGiaNhap"),
                rs.getDouble("donGiaBan"),
                rs.getInt("soLuongTon"),
                rs.getInt("tonKhoToiThieu"),
                rs.getString("viTriLuuKho"),
                rs.getInt("thoiGianBaoHanh")
        );
    }

    public List<PhuTung> findAll() throws SQLException {
        List<PhuTung> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM PhuTung ORDER BY tenPhuTung";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<PhuTung> findAvailable() throws SQLException {
        List<PhuTung> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM PhuTung WHERE soLuongTon > 0 ORDER BY tenPhuTung";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public PhuTung findById(String maPhuTung) throws SQLException {
        String sql = "SELECT " + COLS + " FROM PhuTung WHERE maPhuTung = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhuTung);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<PhuTung> search(String keyword) throws SQLException {
        List<PhuTung> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM PhuTung WHERE maPhuTung LIKE ? OR LOWER(tenPhuTung) LIKE ? OR LOWER(loaiPhuTung) LIKE ? ORDER BY tenPhuTung";
        String kw = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }
}

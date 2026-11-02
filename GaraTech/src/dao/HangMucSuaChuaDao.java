package dao;

import connectDB.ConnectDB;
import entity.HangMucSuaChua;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class HangMucSuaChuaDao {
    private static final String COLS = """
            maHangMuc, maPhieuSC, loaiHangMuc, maDichVu, maPhuTung,
            tenHangMuc, soLuong, donGia, mucGiamGia, thanhTien,
            laPhatSinh, thoiHanBaoHanh, trangThai, ghiChu
            """;

    private static HangMucSuaChua map(ResultSet rs) throws SQLException {
        Timestamp tsBh = rs.getTimestamp("thoiHanBaoHanh");
        return new HangMucSuaChua(
                rs.getString("maHangMuc"),
                rs.getString("maPhieuSC"),
                rs.getString("loaiHangMuc"),
                rs.getString("maDichVu"),
                rs.getString("maPhuTung"),
                rs.getString("tenHangMuc"),
                rs.getInt("soLuong"),
                rs.getDouble("donGia"),
                rs.getDouble("mucGiamGia"),
                rs.getDouble("thanhTien"),
                rs.getBoolean("laPhatSinh"),
                tsBh != null ? tsBh.toLocalDateTime() : null,
                rs.getString("trangThai"),
                rs.getString("ghiChu")
        );
    }

    public List<HangMucSuaChua> findByPhieuSC(String maPhieuSC) throws SQLException {
        List<HangMucSuaChua> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM HangMucSuaChua WHERE maPhieuSC = ? ORDER BY laPhatSinh, maHangMuc";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuSC);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Thêm 1 hạng mục mới (dùng khi phát sinh việc/phụ tùng trong quá trình sửa chữa).
     */
    public String insertSingle(HangMucSuaChua hm) throws SQLException {
        String sql = """
                INSERT INTO HangMucSuaChua(maHangMuc, maPhieuSC, loaiHangMuc, maDichVu, maPhuTung,
                                           tenHangMuc, soLuong, donGia, mucGiamGia, laPhatSinh,
                                           thoiHanBaoHanh, trangThai, ghiChu)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = ConnectDB.getConnection()) {
            String maHM = IdGenerator.next(c, IdGenerator.Kind.HANG_MUC);
            hm.setMaHangMuc(maHM);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, maHM);
                ps.setString(2, hm.getMaPhieuSC());
                ps.setString(3, hm.getLoaiHangMuc());
                ps.setString(4, hm.getMaDichVu());
                ps.setString(5, hm.getMaPhuTung());
                ps.setString(6, hm.getTenHangMuc());
                ps.setInt(7, hm.getSoLuong());
                ps.setDouble(8, hm.getDonGia());
                ps.setDouble(9, hm.getMucGiamGia());
                ps.setBoolean(10, hm.isLaPhatSinh());
                ps.setTimestamp(11, hm.getThoiHanBaoHanh() != null ? Timestamp.valueOf(hm.getThoiHanBaoHanh()) : null);
                ps.setString(12, hm.getTrangThai() != null ? hm.getTrangThai() : "Chờ duyệt");
                ps.setString(13, hm.getGhiChu());
                ps.executeUpdate();
            }
            return maHM;
        }
    }

    public boolean updateTrangThai(String maHangMuc, String trangThai) throws SQLException {
        String sql = "UPDATE HangMucSuaChua SET trangThai = ? WHERE maHangMuc = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trangThai);
            ps.setString(2, maHangMuc);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(String maHangMuc) throws SQLException {
        String sql = "DELETE FROM HangMucSuaChua WHERE maHangMuc = ? AND (trangThai = N'Chờ duyệt' OR trangThai = N'Từ chối')";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maHangMuc);
            return ps.executeUpdate() > 0;
        }
    }
}

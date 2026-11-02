package dao;

import connectDB.ConnectDB;
import entity.KhachHang;
import entity.Xe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Xe (bảng Xe trong SQL Server).
 */
public final class XeDao {

    public record LichSuSuaChuaRow(
            String ngay,
            String bienSo,
            String maPhieuSC,
            String noiDung,
            String hoaDon
    ) {}

    private XeDao() {}

    public static List<Xe> getAll() throws SQLException {
        List<Xe> list = new ArrayList<>();
        String sql = """
                SELECT x.bienSo, x.hangXe, x.model, x.namSanXuat, x.mauXe, x.soMay, x.soKmHienTai, x.ngayDangKy,
                       k.maKH, k.hoTen, k.soDienThoai, k.diaChi, k.email, k.diemTichLuy
                FROM Xe x
                JOIN KhachHang k ON x.maKH = k.maKH
                ORDER BY x.ngayDangKy DESC
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(fromResultSet(rs));
            }
        }
        return list;
    }

    public static Xe findByPlate(String bienSo) throws SQLException {
        String sql = """
                SELECT x.bienSo, x.hangXe, x.model, x.namSanXuat, x.mauXe, x.soMay, x.soKmHienTai, x.ngayDangKy,
                       k.maKH, k.hoTen, k.soDienThoai, k.diaChi, k.email, k.diemTichLuy
                FROM Xe x
                JOIN KhachHang k ON x.maKH = k.maKH
                WHERE x.bienSo = ?
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        }
        return null;
    }

    public static List<Xe> findByMaKH(String maKH) throws SQLException {
        List<Xe> list = new ArrayList<>();
        String sql = """
                SELECT x.bienSo, x.hangXe, x.model, x.namSanXuat, x.mauXe, x.soMay, x.soKmHienTai, x.ngayDangKy,
                       k.maKH, k.hoTen, k.soDienThoai, k.diaChi, k.email, k.diemTichLuy
                FROM Xe x
                JOIN KhachHang k ON x.maKH = k.maKH
                WHERE x.maKH = ?
                ORDER BY x.ngayDangKy DESC
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(fromResultSet(rs));
                }
            }
        }
        return list;
    }

    public static boolean existsPlate(String bienSo) throws SQLException {
        String sql = "SELECT 1 FROM Xe WHERE bienSo = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean insert(Xe xe) throws SQLException {
        try (Connection c = ConnectDB.getConnection()) {
            return insert(c, xe);
        }
    }

    public static boolean insert(Connection c, Xe xe) throws SQLException {
        String sql = """
                INSERT INTO Xe (bienSo, maKH, hangXe, model, namSanXuat, mauXe, soMay, soKmHienTai, ngayDangKy)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, xe.getBienSo().trim().toUpperCase());
            ps.setString(2, xe.getKhachHang().getMaKH());
            ps.setString(3, xe.getHangXe());
            ps.setString(4, xe.getModel());
            ps.setInt(5, xe.getNamSanXuat());
            ps.setString(6, xe.getMauXe());
            ps.setString(7, xe.getSoMay());
            ps.setFloat(8, xe.getSoKmHienTai());
            LocalDateTime ndk = xe.getNgayDangKy() != null ? xe.getNgayDangKy() : LocalDateTime.now();
            ps.setTimestamp(9, Timestamp.valueOf(ndk));
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean update(Xe xe) throws SQLException {
        String sql = """
                UPDATE Xe
                SET hangXe = ?, model = ?, namSanXuat = ?, mauXe = ?, soMay = ?, soKmHienTai = ?, ngayDangKy = ?
                WHERE bienSo = ?
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, xe.getHangXe());
            ps.setString(2, xe.getModel());
            ps.setInt(3, xe.getNamSanXuat());
            ps.setString(4, xe.getMauXe());
            ps.setString(5, xe.getSoMay());
            ps.setFloat(6, xe.getSoKmHienTai());
            LocalDateTime ndk = xe.getNgayDangKy() != null ? xe.getNgayDangKy() : LocalDateTime.now();
            ps.setTimestamp(7, Timestamp.valueOf(ndk));
            ps.setString(8, xe.getBienSo().trim().toUpperCase());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean delete(String bienSo) throws SQLException {
        String sql = "DELETE FROM Xe WHERE bienSo = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim().toUpperCase());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean hasTickets(String bienSo) throws SQLException {
        String sql = "SELECT 1 FROM PhieuTiepNhan WHERE bienSo = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static List<LichSuSuaChuaRow> getLichSuSuaChua(String bienSo) throws SQLException {
        List<LichSuSuaChuaRow> list = new ArrayList<>();
        DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String sql = """
                SELECT ptn.thoiGianTiepNhan, ptn.bienSo, psc.maPhieuSC, ptn.yeuCauKhachHang, hd.maHoaDon
                FROM PhieuTiepNhan ptn
                JOIN PhieuSuaChua psc ON psc.maPhieuTN = ptn.maPhieuTN
                LEFT JOIN HoaDon hd ON hd.maPhieuSC = psc.maPhieuSC
                WHERE ptn.bienSo = ?
                ORDER BY ptn.thoiGianTiepNhan DESC
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("thoiGianTiepNhan");
                    String ngayStr = ts != null ? ts.toLocalDateTime().format(dmy) : "";
                    String bs = rs.getString("bienSo");
                    String psc = rs.getString("maPhieuSC");
                    String yc = rs.getString("yeuCauKhachHang");
                    String hd = rs.getString("maHoaDon");
                    list.add(new LichSuSuaChuaRow(
                            ngayStr,
                            bs != null ? bs : bienSo,
                            psc != null ? psc : "---",
                            yc != null ? yc : "---",
                            hd != null ? hd : "Chưa phát hành"
                    ));
                }
            }
        }
        return list;
    }

    private static Xe fromResultSet(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang(
                rs.getString("maKH"),
                rs.getString("hoTen"),
                rs.getString("soDienThoai"),
                rs.getString("diaChi"),
                rs.getString("email"),
                rs.getInt("diemTichLuy")
        );
        Timestamp ts = rs.getTimestamp("ngayDangKy");
        LocalDateTime ndk = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Xe(
                rs.getString("bienSo"),
                kh,
                rs.getString("hangXe"),
                rs.getString("model"),
                rs.getInt("namSanXuat"),
                rs.getString("mauXe"),
                rs.getString("soMay"),
                rs.getFloat("soKmHienTai"),
                ndk
        );
    }
}

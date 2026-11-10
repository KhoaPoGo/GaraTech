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
import java.util.Locale;

/**
 * Data Access Object cho Xe (bảng Xe trong SQL Server).
 */
public final class XeDao {

    public record LichSuSuaChuaRow(
            String ngay,
            String bienSo,
            String maPhieu,
            String noiDung,
            String hoaDon,
            String trangThai,
            String kyThuatVien,
            String hanHoanThanh,
            String ghiChuKyThuat,
            String chungTuLienQuan
    ) {
        public String maPhieuSC() {
            return maPhieu;
        }

        public LichSuSuaChuaRow(String ngay, String bienSo, String maPhieu, String noiDung, String hoaDon) {
            this(ngay, bienSo, maPhieu, noiDung, hoaDon,
                    "Đã hoàn tất", "Chưa phân công", ngay + " · 17:00",
                    noiDung, (maPhieu != null ? maPhieu : "---") + " · " + (hoaDon != null && !hoaDon.isEmpty() ? hoaDon : "Chưa lập hóa đơn"));
        }
    }

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
        String sql = "SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSo = ?";
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
        DateTimeFormatter dmyTime = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
        String sql = """
                SELECT p.ngayGioTiepNhan, p.bienSo, p.maPhieu, p.yeuCauBanDau, p.tinhTrangXe,
                       p.trangThai AS trangThaiPhieu, p.ghiChuKyThuat, p.thoiGianHenTra, p.thoiGianHoanThanh,
                       p.soKmTiepNhan,
                       nvkt.hoTen AS tenKTV,
                       hd.maHoaDon, hd.trangThai AS trangThaiHD
                FROM PhieuTiepNhanSuaChua p
                LEFT JOIN NhanVien nvkt ON p.maNVKyThuat = nvkt.maNV
                LEFT JOIN HoaDon hd ON hd.maPhieu = p.maPhieu
                WHERE p.bienSo = ?
                ORDER BY p.ngayGioTiepNhan DESC
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("ngayGioTiepNhan");
                    String ngayStr = ts != null ? ts.toLocalDateTime().format(dmy) : "";
                    String bs = rs.getString("bienSo");
                    String maPhieu = rs.getString("maPhieu");
                    String yc = rs.getString("yeuCauBanDau");
                    String tinhTrang = rs.getString("tinhTrangXe");
                    String ttPhieu = rs.getString("trangThaiPhieu");
                    String ghiChu = rs.getString("ghiChuKyThuat");
                    Timestamp tsHenTra = rs.getTimestamp("thoiGianHenTra");
                    Timestamp tsHoanThanh = rs.getTimestamp("thoiGianHoanThanh");
                    String tenKTV = rs.getString("tenKTV");
                    String maHD = rs.getString("maHoaDon");
                    String ttHD = rs.getString("trangThaiHD");

                    String noiDung = (yc != null && !yc.isBlank()) ? yc : ((tinhTrang != null && !tinhTrang.isBlank()) ? tinhTrang : "Bảo dưỡng & sửa chữa");
                    String trangThaiHienThi = formatTrangThaiPhieu(ttPhieu, ttHD);

                    String hanHT;
                    if (tsHoanThanh != null) {
                        hanHT = tsHoanThanh.toLocalDateTime().format(dmyTime);
                    } else if (tsHenTra != null) {
                        hanHT = tsHenTra.toLocalDateTime().format(dmyTime);
                    } else {
                        hanHT = !ngayStr.isEmpty() ? ngayStr + " · 17:00" : "---";
                    }

                    String hdHienThi = (maHD != null && !maHD.isBlank()) ? maHD : "Chưa lập hóa đơn";
                    String docs = (maPhieu != null ? maPhieu : "---") + " · " + hdHienThi;

                    String ghiChuHienThi;
                    if (ghiChu != null && !ghiChu.isBlank()) {
                        ghiChuHienThi = ghiChu;
                    } else if (tinhTrang != null && !tinhTrang.isBlank()) {
                        ghiChuHienThi = tinhTrang;
                    } else {
                        ghiChuHienThi = noiDung;
                    }

                    list.add(new LichSuSuaChuaRow(
                            ngayStr,
                            bs != null ? bs : bienSo,
                            maPhieu != null ? maPhieu : "---",
                            noiDung,
                            hdHienThi,
                            trangThaiHienThi,
                            tenKTV != null && !tenKTV.isBlank() ? tenKTV : "Chưa phân công",
                            hanHT,
                            ghiChuHienThi,
                            docs
                    ));
                }
            }
        }
        return list;
    }

    private static String formatTrangThaiPhieu(String rawStatus, String hdStatus) {
        if (rawStatus == null) return "Chờ sửa chữa";
        return switch (rawStatus.toUpperCase(Locale.ROOT)) {
            case "CHO_SUA" -> "Chờ sửa chữa";
            case "DANG_SUA" -> "Đang sửa chữa";
            case "CHO_LINH_KIEN" -> "Chờ linh kiện";
            case "HOAN_TAT" -> {
                if ("Đã thanh toán".equalsIgnoreCase(hdStatus)) {
                    yield "Đã thanh toán";
                }
                yield "Đã hoàn tất";
            }
            default -> rawStatus;
        };
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

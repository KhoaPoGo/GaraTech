package dao;

import connectDB.ConnectDB;
import entity.PhieuTiepNhan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PhieuTiepNhanDao {

    public record PhieuTiepNhanRow(
            String maPhieuTN,
            String bienSo,
            String maKH,
            String tenKhachHang,
            String soDienThoai,
            String thongTinXe,
            LocalDateTime thoiGianTiepNhan,
            LocalDateTime thoiGianHenTra,
            float soKmTiepNhan,
            float mucNhienLieu,
            String tinhTrangBanDau,
            String yeuCauKhachHang,
            String maNV,
            String tenNhanVien,
            String trangThai
    ) {
    }

    public static boolean insert(PhieuTiepNhan ptn) throws SQLException {
        String sqlPtn = """
                INSERT INTO PhieuTiepNhan(maPhieuTN, bienSo, maKH, maNV, thoiGianTiepNhan, thoiGianHenTra,
                                          tinhTrangBanDau, yeuCauKhachHang, mucNhienLieu, soKmTiepNhan, trangThai)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String sqlUpdateXe = "UPDATE Xe SET soKmHienTai = ? WHERE bienSo = ? AND soKmHienTai < ?";

        try (Connection c = ConnectDB.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sqlPtn)) {
                    ps.setString(1, ptn.getMaPhieuTN());
                    ps.setString(2, ptn.getXe() != null ? ptn.getXe().getBienSo() : null);
                    ps.setString(3, ptn.getKhachHang() != null ? ptn.getKhachHang().getMaKH() : null);
                    ps.setString(4, ptn.getNhanVien() != null ? ptn.getNhanVien().getMaNV() : null);
                    ps.setTimestamp(5, ptn.getThoiGianTiepNhan() != null
                            ? Timestamp.valueOf(ptn.getThoiGianTiepNhan())
                            : Timestamp.valueOf(LocalDateTime.now()));
                    ps.setTimestamp(6, ptn.getThoiGianHenTra() != null
                            ? Timestamp.valueOf(ptn.getThoiGianHenTra())
                            : null);
                    ps.setString(7, ptn.getTinhTrangBanDau());
                    ps.setString(8, ptn.getYeuCauKhachHang());
                    ps.setFloat(9, ptn.getMucNhienLieu());
                    ps.setFloat(10, ptn.getSoKmTiepNhan());
                    ps.setString(11, ptn.getTrangThai() != null ? ptn.getTrangThai() : "Chờ khách hàng xác nhận");
                    ps.executeUpdate();
                }

                // Cập nhật ODO của xe nếu ODO tiếp nhận lớn hơn
                if (ptn.getXe() != null && ptn.getSoKmTiepNhan() > 0) {
                    try (PreparedStatement psXe = c.prepareStatement(sqlUpdateXe)) {
                        psXe.setFloat(1, ptn.getSoKmTiepNhan());
                        psXe.setString(2, ptn.getXe().getBienSo());
                        psXe.setFloat(3, ptn.getSoKmTiepNhan());
                        psXe.executeUpdate();
                    }
                }

                c.commit();
                return true;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Lấy chi tiết phiếu tiếp nhận theo mã phiếu.
     */
    public static PhieuTiepNhanRow findById(String maPhieuTN) throws SQLException {
        String sql = """
                SELECT ptn.maPhieuTN, ptn.bienSo, ptn.maKH, ptn.maNV,
                       ptn.thoiGianTiepNhan, ptn.thoiGianHenTra,
                       ptn.tinhTrangBanDau, ptn.yeuCauKhachHang,
                       ptn.mucNhienLieu, ptn.soKmTiepNhan, ptn.trangThai,
                       kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                       x.hangXe, x.model, x.namSanXuat, x.mauXe,
                       nv.hoTen AS tenNV
                FROM PhieuTiepNhan ptn
                JOIN KhachHang kh ON ptn.maKH = kh.maKH
                JOIN Xe x ON ptn.bienSo = x.bienSo
                JOIN NhanVien nv ON ptn.maNV = nv.maNV
                WHERE ptn.maPhieuTN = ?
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuTN);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm kiếm danh sách phiếu tiếp nhận có bộ lọc ngày, từ khóa, trạng thái.
     */
    public static List<PhieuTiepNhanRow> search(LocalDate fromDate, LocalDate toDate, String keyword, String trangThai) throws SQLException {
        List<PhieuTiepNhanRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT ptn.maPhieuTN, ptn.bienSo, ptn.maKH, ptn.maNV,
                       ptn.thoiGianTiepNhan, ptn.thoiGianHenTra,
                       ptn.tinhTrangBanDau, ptn.yeuCauKhachHang,
                       ptn.mucNhienLieu, ptn.soKmTiepNhan, ptn.trangThai,
                       kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                       x.hangXe, x.model, x.namSanXuat, x.mauXe,
                       nv.hoTen AS tenNV
                FROM PhieuTiepNhan ptn
                JOIN KhachHang kh ON ptn.maKH = kh.maKH
                JOIN Xe x ON ptn.bienSo = x.bienSo
                JOIN NhanVien nv ON ptn.maNV = nv.maNV
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND ptn.thoiGianTiepNhan >= ?");
            params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND ptn.thoiGianTiepNhan <= ?");
            params.add(Timestamp.valueOf(toDate.atTime(23, 59, 59)));
        }
        if (trangThai != null && !trangThai.isBlank() && !trangThai.equalsIgnoreCase("Tất cả")) {
            sql.append(" AND ptn.trangThai = ?");
            params.add(trangThai.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            sql.append(" AND (ptn.maPhieuTN LIKE ? OR ptn.bienSo LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?)");
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY ptn.thoiGianTiepNhan DESC");

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Tìm danh sách phiếu tiếp nhận hợp lệ để lập phiếu sửa chữa
     * (trạng thái Đã xác nhận và chưa được gắn vào phiếu sửa chữa chưa hủy nào).
     */
    public static List<PhieuTiepNhanRow> findEligibleForRepair(String keyword) throws SQLException {
        List<PhieuTiepNhanRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT ptn.maPhieuTN, ptn.bienSo, ptn.maKH, ptn.maNV,
                       ptn.thoiGianTiepNhan, ptn.thoiGianHenTra,
                       ptn.tinhTrangBanDau, ptn.yeuCauKhachHang,
                       ptn.mucNhienLieu, ptn.soKmTiepNhan, ptn.trangThai,
                       kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                       x.hangXe, x.model, x.namSanXuat, x.mauXe,
                       nv.hoTen AS tenNV
                FROM PhieuTiepNhan ptn
                JOIN KhachHang kh ON ptn.maKH = kh.maKH
                JOIN Xe x ON ptn.bienSo = x.bienSo
                JOIN NhanVien nv ON ptn.maNV = nv.maNV
                WHERE ptn.trangThai IN (N'Đã xác nhận', N'Chờ khách hàng xác nhận', N'Chờ khách xác nhận')
                  AND ptn.maPhieuTN NOT IN (SELECT maPhieuTN FROM PhieuSuaChua WHERE trangThai <> N'Đã hủy')
                """);

        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            sql.append(" AND (ptn.maPhieuTN LIKE ? OR ptn.bienSo LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?)");
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY ptn.thoiGianTiepNhan DESC");

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public static boolean updateTrangThai(String maPhieuTN, String trangThaiMoi) throws SQLException {
        String sql = "UPDATE PhieuTiepNhan SET trangThai = ? WHERE maPhieuTN = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trangThaiMoi);
            ps.setString(2, maPhieuTN);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lấy số ODO gần nhất đã ghi nhận của xe (từ các phiếu tiếp nhận trước hoặc từ bảng Xe).
     */
    public static float getLatestOdo(String bienSo) throws SQLException {
        String sql = """
                SELECT MAX(odo) AS maxOdo FROM (
                    SELECT soKmTiepNhan AS odo FROM PhieuTiepNhan WHERE bienSo = ?
                    UNION ALL
                    SELECT soKmHienTai AS odo FROM Xe WHERE bienSo = ?
                ) t
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo);
            ps.setString(2, bienSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getFloat("maxOdo");
                }
            }
        }
        return 0;
    }

    private static PhieuTiepNhanRow mapRow(ResultSet rs) throws SQLException {
        Timestamp tsTn = rs.getTimestamp("thoiGianTiepNhan");
        Timestamp tsHt = rs.getTimestamp("thoiGianHenTra");

        String hangXe = rs.getString("hangXe");
        String model = rs.getString("model");
        int nam = rs.getInt("namSanXuat");
        String mau = rs.getString("mauXe");
        String thongTinXe = String.format("%s %s %d · %s", hangXe, model, nam, mau);

        return new PhieuTiepNhanRow(
                rs.getString("maPhieuTN"),
                rs.getString("bienSo"),
                rs.getString("maKH"),
                rs.getString("tenKH"),
                rs.getString("sdtKH"),
                thongTinXe,
                tsTn != null ? tsTn.toLocalDateTime() : null,
                tsHt != null ? tsHt.toLocalDateTime() : null,
                rs.getFloat("soKmTiepNhan"),
                rs.getFloat("mucNhienLieu"),
                rs.getString("tinhTrangBanDau"),
                rs.getString("yeuCauKhachHang"),
                rs.getString("maNV"),
                rs.getString("tenNV"),
                rs.getString("trangThai")
        );
    }
}

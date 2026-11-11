package dao;

import connectDB.ConnectDB;
import enums.TrangThaiPhieu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn phiếu tiếp nhận - sửa chữa phục vụ quản lý: Điều phối & tiến độ, Kiểm tra chất lượng, Dashboard.
 */
public class DieuPhoiDao {

    /** Một phiếu sửa chữa kèm thông tin xe, khách, kỹ thuật viên và tiến độ hạng mục. */
    public record PhieuCard(String maPhieuSC, TrangThaiPhieu trangThai, String bienSo, String tenXe,
                            String tenKhach, String maKTV, String tenKTV, LocalDateTime duKien,
                            LocalDateTime batDau, String yeuCau, int tongHangMuc, int hangMucXong) {
        /** Tiến độ 0–100 theo số hạng mục đã hoàn thành. */
        public int tienDo() {
            if (trangThai == TrangThaiPhieu.HOAN_TAT) return 100;
            return tongHangMuc == 0 ? 0 : hangMucXong * 100 / tongHangMuc;
        }

        /** Đang làm dở mà đã quá hạn dự kiến. */
        public boolean quaHan(LocalDateTime now) {
            return duKien != null && duKien.isBefore(now)
                    && (trangThai == TrangThaiPhieu.CHO_SUA || trangThai == TrangThaiPhieu.DANG_SUA || trangThai == TrangThaiPhieu.CHO_LINH_KIEN);
        }
    }

    /** Kỹ thuật viên kèm số phiếu đang giữ (để hiện Rảnh / Bận khi phân công). */
    public record KtvLoad(String maNV, String hoTen, int soPhieuDangGiu) {
        public boolean ranh() { return soPhieuDangGiu == 0; }
    }

    /** Một hạng mục của phiếu (màn kiểm tra chất lượng). */
    public record HangMuc(String ten, String loai, int soLuong, String trangThai, boolean phatSinh) {}

    private static final String SELECT = """
            SELECT p.maPhieu AS maPhieuSC, p.trangThai, p.maNVKyThuat AS maKTV, kt.hoTen AS tenKTV,
                   p.thoiGianHenTra AS thoiGianDuKien, p.thoiGianBatDau,
                   p.bienSo, x.hangXe, x.model, kh.hoTen AS tenKH, p.yeuCauBanDau AS yeuCauKhachHang,
                   (SELECT COUNT(*) FROM ChiTietDichVu dv WHERE dv.maPhieu = p.maPhieu) +
                   (SELECT COUNT(*) FROM ChiTietLinhKien lk WHERE lk.maPhieu = p.maPhieu) AS tongHM,
                   CASE WHEN p.trangThai = 'HOAN_TAT' THEN
                        (SELECT COUNT(*) FROM ChiTietDichVu dv WHERE dv.maPhieu = p.maPhieu) +
                        (SELECT COUNT(*) FROM ChiTietLinhKien lk WHERE lk.maPhieu = p.maPhieu)
                   ELSE 0 END AS xongHM
            FROM PhieuTiepNhanSuaChua p
            JOIN Xe x ON x.bienSo = p.bienSo
            JOIN KhachHang kh ON kh.maKH = x.maKH
            LEFT JOIN NhanVien kt ON kt.maNV = p.maNVKyThuat
            """;

    private static final String ORDER = " ORDER BY CASE WHEN p.thoiGianHenTra IS NULL THEN 1 ELSE 0 END, p.thoiGianHenTra, p.maPhieu";

    private static LocalDateTime ldt(Timestamp t) { return t == null ? null : t.toLocalDateTime(); }

    private static PhieuCard map(ResultSet rs) throws SQLException {
        return new PhieuCard(rs.getString("maPhieuSC"), TrangThaiPhieu.fromDb(rs.getString("trangThai")),
                rs.getString("bienSo"), rs.getString("hangXe") + " " + rs.getString("model"), rs.getString("tenKH"),
                rs.getString("maKTV"), rs.getString("tenKTV"), ldt(rs.getTimestamp("thoiGianDuKien")),
                ldt(rs.getTimestamp("thoiGianBatDau")), rs.getString("yeuCauKhachHang"),
                rs.getInt("tongHM"), rs.getInt("xongHM"));
    }

    private static String inList(int n) {
        return "(" + "?,".repeat(n - 1) + "?)";
    }

    /** Các phiếu có trạng thái nằm trong danh sách, đã sắp theo hạn hoàn thành; limit <= 0 là lấy hết. */
    public List<PhieuCard> findByTrangThai(int limit, TrangThaiPhieu... trangThai) throws SQLException {
        String sql = (limit > 0 ? SELECT.replaceFirst("SELECT ", "SELECT TOP " + limit + " ") : SELECT)
                + " WHERE p.trangThai IN " + inList(trangThai.length) + ORDER;
        List<PhieuCard> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < trangThai.length; i++) ps.setString(i + 1, trangThai[i].name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public PhieuCard findById(String maPhieu) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.maPhieu = ?")) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<KtvLoad> findKtvVaTaiTrong() throws SQLException {
        String sql = """
                SELECT nv.maNV, nv.hoTen,
                       (SELECT COUNT(*) FROM PhieuTiepNhanSuaChua p WHERE p.maNVKyThuat = nv.maNV
                          AND p.trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN')) AS soPhieu
                FROM NhanVien nv
                WHERE nv.vaiTro = N'Kỹ thuật viên' AND nv.dangLam = 1
                ORDER BY nv.hoTen
                """;
        List<KtvLoad> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new KtvLoad(rs.getString("maNV"), rs.getString("hoTen"), rs.getInt("soPhieu")));
        }
        return out;
    }

    public boolean laKtvDangLam(String maNV) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM NhanVien WHERE maNV = ? AND vaiTro = N'Kỹ thuật viên' AND dangLam = 1")) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<HangMuc> findHangMuc(String maPhieu) throws SQLException {
        String sql = """
                SELECT dv.tenDichVu AS tenHangMuc, N'Dịch vụ' AS loaiHangMuc, cdv.soLuong,
                       N'Hoàn thành' AS trangThai,
                       CAST(0 AS BIT) AS laPhatSinh
                FROM ChiTietDichVu cdv
                JOIN DichVu dv ON dv.maDichVu = cdv.maDichVu
                WHERE cdv.maPhieu = ?
                UNION ALL
                SELECT pt.tenPhuTung AS tenHangMuc, N'Phụ tùng' AS loaiHangMuc, clk.soLuong,
                       N'Hoàn thành' AS trangThai,
                       CAST(0 AS BIT) AS laPhatSinh
                FROM ChiTietLinhKien clk
                JOIN PhuTung pt ON pt.maPhuTung = clk.maPhuTung
                WHERE clk.maPhieu = ?
                """;
        List<HangMuc> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new HangMuc(rs.getString("tenHangMuc"), rs.getString("loaiHangMuc"),
                        rs.getInt("soLuong"), rs.getString("trangThai"), rs.getBoolean("laPhatSinh")));
            }
        }
        return out;
    }

    /** Số hạng mục chưa xong. */
    public int demHangMucChuaXong(String maPhieu) throws SQLException {
        String sql = "SELECT CASE WHEN trangThai = 'HOAN_TAT' THEN 0 ELSE 0 END FROM PhieuTiepNhanSuaChua WHERE maPhieu = ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Phụ tùng đã xuất kho cho phiếu này chưa. */
    public boolean daXuatKho(String maPhieu) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PhieuXuatKho WHERE maPhieu = ? AND trangThai = N'Đã xuất kho'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public String findGhiChuKyThuat(String maPhieu) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT ghiChuKyThuat FROM PhieuTiepNhanSuaChua WHERE maPhieu = ?")) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ---------------------------------------------------------------- cập nhật (luôn kèm điều kiện trạng thái cũ)

    /** Phân công kỹ thuật viên + đặt hạn. Trả về 0 nếu phiếu đã hoàn tất hoặc không tồn tại. */
    public int phanCong(String maPhieu, String maKTV, LocalDateTime duKien) throws SQLException {
        String sql = "UPDATE PhieuTiepNhanSuaChua SET maNVKyThuat = ?, thoiGianHenTra = ? WHERE maPhieu = ?"
                + " AND trangThai <> 'HOAN_TAT'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV);
            ps.setTimestamp(2, Timestamp.valueOf(duKien));
            ps.setString(3, maPhieu);
            return ps.executeUpdate();
        }
    }

    public int batDauSua(String maPhieu) throws SQLException {
        String sql = "UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA', thoiGianBatDau = COALESCE(thoiGianBatDau, SYSDATETIME())"
                + " WHERE maPhieu = ? AND trangThai = 'CHO_SUA' AND maNVKyThuat IS NOT NULL";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            return ps.executeUpdate();
        }
    }

    public int huy(String maPhieu, TrangThaiPhieu tuTrangThai, String lyDo) throws SQLException {
        String sql = "UPDATE PhieuTiepNhanSuaChua SET ghiChuKyThuat = ? WHERE maPhieu = ? AND trangThai <> 'HOAN_TAT'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setNString(1, "Hủy: " + lyDo);
            ps.setString(2, maPhieu);
            return ps.executeUpdate();
        }
    }

    /** Chờ kiểm tra / Đang sửa -> Hoàn tất, ghi thời điểm hoàn thành và ghi chú. */
    public int hoanThanh(String maPhieu, String ghiChuMoi) throws SQLException {
        String sql = "UPDATE PhieuTiepNhanSuaChua SET trangThai = 'HOAN_TAT', ghiChuKyThuat = ?,"
                + " thoiGianHoanThanh = CASE WHEN thoiGianBatDau IS NOT NULL AND thoiGianBatDau > SYSDATETIME() THEN thoiGianBatDau ELSE SYSDATETIME() END"
                + " WHERE maPhieu = ? AND trangThai IN ('DANG_SUA', 'CHO_SUA', 'CHO_LINH_KIEN')";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (ghiChuMoi == null) ps.setNull(1, java.sql.Types.NVARCHAR); else ps.setNString(1, ghiChuMoi);
            ps.setString(2, maPhieu);
            return ps.executeUpdate();
        }
    }

    /** Trả về làm lại, ghi lý do vào ghi chú kỹ thuật. */
    public int traVeLamLai(String maPhieu, String ghiChuMoi) throws SQLException {
        String sql = "UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA', ghiChuKyThuat = ?, thoiGianHoanThanh = NULL"
                + " WHERE maPhieu = ? AND trangThai <> 'HOAN_TAT'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setNString(1, ghiChuMoi);
            ps.setString(2, maPhieu);
            return ps.executeUpdate();
        }
    }
}

package dao;

import connectDB.ConnectDB;
import enums.TrangThaiPhieuSuaChua;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn phiếu sửa chữa phục vụ quản lý: Điều phối & tiến độ, Kiểm tra chất lượng, Dashboard.
 * (Tách khỏi PhieuSuaChuaDao của nhóm tiếp nhận để hai bên không sửa chung một file.)
 */
public class DieuPhoiDao {

    /** Một phiếu sửa chữa kèm thông tin xe, khách, kỹ thuật viên và tiến độ hạng mục. */
    public record PhieuCard(String maPhieuSC, TrangThaiPhieuSuaChua trangThai, String bienSo, String tenXe,
                            String tenKhach, String maKTV, String tenKTV, LocalDateTime duKien,
                            LocalDateTime batDau, String yeuCau, int tongHangMuc, int hangMucXong) {
        /** Tiến độ 0–100 theo số hạng mục đã hoàn thành. */
        public int tienDo() {
            if (trangThai == TrangThaiPhieuSuaChua.CHO_KIEM_TRA || trangThai == TrangThaiPhieuSuaChua.HOAN_THANH
                    || trangThai == TrangThaiPhieuSuaChua.DA_BAN_GIAO) return 100;
            return tongHangMuc == 0 ? 0 : hangMucXong * 100 / tongHangMuc;
        }

        /** Đang làm dở mà đã quá hạn dự kiến. */
        public boolean quaHan(LocalDateTime now) {
            return duKien != null && duKien.isBefore(now)
                    && (trangThai == TrangThaiPhieuSuaChua.DA_XAC_NHAN || trangThai == TrangThaiPhieuSuaChua.DANG_SUA_CHUA);
        }
    }

    /** Kỹ thuật viên kèm số phiếu đang giữ (để hiện Rảnh / Bận khi phân công). */
    public record KtvLoad(String maNV, String hoTen, int soPhieuDangGiu) {
        public boolean ranh() { return soPhieuDangGiu == 0; }
    }

    /** Một hạng mục của phiếu (màn kiểm tra chất lượng). */
    public record HangMuc(String ten, String loai, int soLuong, String trangThai, boolean phatSinh) {}

    private static final String SELECT = """
            SELECT p.maPhieuSC, p.trangThai, p.maKTV, kt.hoTen AS tenKTV, p.thoiGianDuKien, p.thoiGianBatDau,
                   tn.bienSo, x.hangXe, x.model, kh.hoTen AS tenKH, tn.yeuCauKhachHang,
                   (SELECT COUNT(*) FROM HangMucSuaChua h WHERE h.maPhieuSC = p.maPhieuSC AND h.trangThai <> N'Từ chối') AS tongHM,
                   (SELECT COUNT(*) FROM HangMucSuaChua h WHERE h.maPhieuSC = p.maPhieuSC AND h.trangThai = N'Hoàn thành') AS xongHM
            FROM PhieuSuaChua p
            JOIN PhieuTiepNhan tn ON tn.maPhieuTN = p.maPhieuTN
            JOIN Xe x ON x.bienSo = tn.bienSo
            JOIN KhachHang kh ON kh.maKH = tn.maKH
            LEFT JOIN NhanVien kt ON kt.maNV = p.maKTV
            """;

    private static final String ORDER = " ORDER BY CASE WHEN p.thoiGianDuKien IS NULL THEN 1 ELSE 0 END, p.thoiGianDuKien, p.maPhieuSC";

    private static LocalDateTime ldt(Timestamp t) { return t == null ? null : t.toLocalDateTime(); }

    private static PhieuCard map(ResultSet rs) throws SQLException {
        return new PhieuCard(rs.getString("maPhieuSC"), TrangThaiPhieuSuaChua.fromLabel(rs.getString("trangThai")),
                rs.getString("bienSo"), rs.getString("hangXe") + " " + rs.getString("model"), rs.getString("tenKH"),
                rs.getString("maKTV"), rs.getString("tenKTV"), ldt(rs.getTimestamp("thoiGianDuKien")),
                ldt(rs.getTimestamp("thoiGianBatDau")), rs.getString("yeuCauKhachHang"),
                rs.getInt("tongHM"), rs.getInt("xongHM"));
    }

    private static String inList(int n) {
        return "(" + "?,".repeat(n - 1) + "?)";
    }

    /** Các phiếu có trạng thái nằm trong danh sách, đã sắp theo hạn hoàn thành; limit <= 0 là lấy hết. */
    public List<PhieuCard> findByTrangThai(int limit, TrangThaiPhieuSuaChua... trangThai) throws SQLException {
        String sql = (limit > 0 ? SELECT.replaceFirst("SELECT ", "SELECT TOP " + limit + " ") : SELECT)
                + " WHERE p.trangThai IN " + inList(trangThai.length) + ORDER;
        List<PhieuCard> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < trangThai.length; i++) ps.setNString(i + 1, trangThai[i].label());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public PhieuCard findById(String maPhieu) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.maPhieuSC = ?")) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<KtvLoad> findKtvVaTaiTrong() throws SQLException {
        String sql = """
                SELECT nv.maNV, nv.hoTen,
                       (SELECT COUNT(*) FROM PhieuSuaChua p WHERE p.maKTV = nv.maNV
                          AND p.trangThai IN (N'Đã xác nhận', N'Đang sửa chữa')) AS soPhieu
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
        String sql = "SELECT tenHangMuc, loaiHangMuc, soLuong, trangThai, laPhatSinh FROM HangMucSuaChua"
                + " WHERE maPhieuSC = ? ORDER BY maHangMuc";
        List<HangMuc> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new HangMuc(rs.getString("tenHangMuc"), rs.getString("loaiHangMuc"),
                        rs.getInt("soLuong"), rs.getString("trangThai"), rs.getBoolean("laPhatSinh")));
            }
        }
        return out;
    }

    /** Số hạng mục chưa xong (không tính hạng mục bị từ chối). */
    public int demHangMucChuaXong(String maPhieu) throws SQLException {
        String sql = "SELECT COUNT(*) FROM HangMucSuaChua WHERE maPhieuSC = ? AND trangThai NOT IN (N'Hoàn thành', N'Từ chối')";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Phụ tùng đã xuất kho cho phiếu này chưa (nếu rồi thì không cho hủy phiếu). */
    public boolean daXuatKho(String maPhieu) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PhieuXuatKho WHERE maPhieuSC = ? AND trangThai = N'Đã xuất kho'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public String findGhiChuKyThuat(String maPhieu) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT ghiChuKyThuat FROM PhieuSuaChua WHERE maPhieuSC = ?")) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ---------------------------------------------------------------- cập nhật (luôn kèm điều kiện trạng thái cũ)

    /** Phân công kỹ thuật viên + đặt hạn. Trả về 0 nếu phiếu đã đổi trạng thái trong lúc đó. */
    public int phanCong(String maPhieu, String maKTV, LocalDateTime duKien) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET maKTV = ?, thoiGianDuKien = ? WHERE maPhieuSC = ?"
                + " AND trangThai IN (N'Đã xác nhận', N'Đang sửa chữa')";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV);
            ps.setTimestamp(2, Timestamp.valueOf(duKien));
            ps.setString(3, maPhieu);
            return ps.executeUpdate();
        }
    }

    public int batDauSua(String maPhieu) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET trangThai = N'Đang sửa chữa', thoiGianBatDau = SYSDATETIME()"
                + " WHERE maPhieuSC = ? AND trangThai = N'Đã xác nhận' AND maKTV IS NOT NULL";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            return ps.executeUpdate();
        }
    }

    public int huy(String maPhieu, TrangThaiPhieuSuaChua tuTrangThai, String lyDo) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET trangThai = N'Đã hủy', lyDoHuy = ? WHERE maPhieuSC = ? AND trangThai = ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setNString(1, lyDo);
            ps.setString(2, maPhieu);
            ps.setNString(3, tuTrangThai.label());
            return ps.executeUpdate();
        }
    }

    /** Chờ kiểm tra -> Hoàn thành, ghi thời điểm hoàn thành (không sớm hơn thời điểm bắt đầu) và ghi chú. */
    public int hoanThanh(String maPhieu, String ghiChuMoi) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET trangThai = N'Hoàn thành', ghiChuKyThuat = ?,"
                + " thoiGianHoanThanh = CASE WHEN thoiGianBatDau > SYSDATETIME() THEN thoiGianBatDau ELSE SYSDATETIME() END"
                + " WHERE maPhieuSC = ? AND trangThai = N'Chờ kiểm tra'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (ghiChuMoi == null) ps.setNull(1, java.sql.Types.NVARCHAR); else ps.setNString(1, ghiChuMoi);
            ps.setString(2, maPhieu);
            return ps.executeUpdate();
        }
    }

    /** Chờ kiểm tra -> Đang sửa chữa (trả về làm lại), ghi lý do vào ghi chú kỹ thuật. */
    public int traVeLamLai(String maPhieu, String ghiChuMoi) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET trangThai = N'Đang sửa chữa', ghiChuKyThuat = ?, thoiGianHoanThanh = NULL"
                + " WHERE maPhieuSC = ? AND trangThai = N'Chờ kiểm tra'";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setNString(1, ghiChuMoi);
            ps.setString(2, maPhieu);
            return ps.executeUpdate();
        }
    }
}

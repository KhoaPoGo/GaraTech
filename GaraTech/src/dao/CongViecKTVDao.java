package dao;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho màn hình công việc kỹ thuật viên.
 * Tương thích schema mới sử dụng bảng PhieuTiepNhanSuaChua.
 *
 * Lưu ý thiết kế: schema mới chỉ lưu trạng thái ở cấp phiếu, không có trạng thái
 * độc lập cho từng dịch vụ/linh kiện. WorkItemRow chỉ phục vụ hiển thị danh sách
 * dịch vụ và linh kiện, không được cập nhật trạng thái riêng.
 */
public final class CongViecKTVDao {

    public record TicketRow(
            String maPhieuSC,
            String maKTV,
            String trangThai,
            String bienSo,
            String thongTinXe,
            String tenKhach,
            String soDienThoai,
            double odo,
            String tinhTrangBanDau,
            String yeuCauKhachHang,
            String maPhieuTN,
            LocalDateTime thoiGianTiepNhan,
            LocalDateTime thoiGianDuKien,
            LocalDateTime thoiGianBatDau,
            LocalDateTime thoiGianHoanThanh,
            String ghiChuKyThuat,
            int tongHangMuc,
            int hangMucHoanThanh
    ) {
        public int tienDo() {
            return tongHangMuc <= 0 ? 0
                    : Math.min(100, hangMucHoanThanh * 100 / tongHangMuc);
        }
    }

    /** Dòng dịch vụ/linh kiện để hiển thị; không có trạng thái riêng trong DB mới. */
    public record WorkItemRow(
            String maHangMuc,
            String tenHangMuc,
            String loaiHangMuc,
            int soLuong,
            String trangThai,
            boolean phatSinh,
            String ghiChu
    ) { }

    public record PartRow(
            String maPhieuXK,
            String maPhuTung,
            String tenPhuTung,
            String donViTinh,
            int soLuongYeuCau,
            int soLuongDaXuat,
            String trangThaiPhieu,
            String ghiChu
    ) { }

    public record HistoryRow(
            String maPhieuSC,
            String trangThai,
            LocalDateTime thoiGian,
            String yeuCau
    ) { }

    private static final String SELECT_TICKETS = """
            SELECT
                p.maPhieu AS maPhieuSC,
                p.maNVKyThuat AS maKTV,
                CASE p.trangThai
                    WHEN 'CHO_SUA' THEN N'Chờ sửa chữa'
                    WHEN 'DANG_SUA' THEN N'Đang sửa chữa'
                    WHEN 'CHO_LINH_KIEN' THEN N'Chờ linh kiện'
                    WHEN 'HOAN_TAT' THEN N'Hoàn thành'
                    ELSE p.trangThai
                END AS trangThai,
                x.bienSo,
                CONCAT(x.hangXe, N' ', x.model, N' ', x.namSanXuat,
                       N' · ', x.mauXe) AS thongTinXe,
                kh.hoTen AS tenKhach,
                kh.soDienThoai,
                p.soKmTiepNhan,
                p.tinhTrangXe AS tinhTrangBanDau,
                p.yeuCauBanDau AS yeuCauKhachHang,
                p.maPhieu AS maPhieuTN,
                p.ngayGioTiepNhan AS thoiGianTiepNhan,
                p.thoiGianHenTra AS thoiGianDuKien,
                p.thoiGianBatDau,
                p.thoiGianHoanThanh,
                p.ghiChuKyThuat,
                (
                    SELECT COUNT(*)
                    FROM ChiTietDichVu dv
                    WHERE dv.maPhieu = p.maPhieu
                ) + (
                    SELECT COUNT(*)
                    FROM ChiTietLinhKien lk
                    WHERE lk.maPhieu = p.maPhieu
                ) AS tongHangMuc,
                CASE WHEN p.trangThai = 'HOAN_TAT' THEN
                    (
                        SELECT COUNT(*)
                        FROM ChiTietDichVu dv
                        WHERE dv.maPhieu = p.maPhieu
                    ) + (
                        SELECT COUNT(*)
                        FROM ChiTietLinhKien lk
                        WHERE lk.maPhieu = p.maPhieu
                    )
                ELSE 0 END AS hangMucHoanThanh
            FROM PhieuTiepNhanSuaChua p
            INNER JOIN Xe x ON x.bienSo = p.bienSo
            INNER JOIN KhachHang kh ON kh.maKH = x.maKH
            WHERE p.maNVKyThuat = ?
            ORDER BY
                CASE WHEN p.thoiGianHenTra IS NULL THEN 1 ELSE 0 END,
                p.thoiGianHenTra,
                p.ngayGioTiepNhan DESC
            """;

    public List<TicketRow> findByTechnician(String maKTV) throws SQLException {
        requireText(maKTV, "Mã kỹ thuật viên");
        List<TicketRow> result = new ArrayList<>();

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_TICKETS)) {
            ps.setString(1, maKTV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
        }
        return result;
    }

    public TicketRow findTicketById(String maPhieu, String maKTV) throws SQLException {
        requireText(maPhieu, "Mã phiếu");
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = SELECT_TICKETS.replace(
                "WHERE p.maNVKyThuat = ?",
                "WHERE p.maNVKyThuat = ? AND p.maPhieu = ?"
        );

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV.trim());
            ps.setString(2, maPhieu.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTicket(rs) : null;
            }
        }
    }

    /**
     * Đọc các dịch vụ và linh kiện đã gắn vào phiếu.
     * Vì schema mới không có cột trạng thái chi tiết, tất cả dòng chỉ mang
     * trạng thái hiển thị tổng hợp theo phiếu: Hoàn thành hoặc Theo dõi theo phiếu.
     */
    public List<WorkItemRow> findWorkItems(String maPhieu) throws SQLException {
        requireText(maPhieu, "Mã phiếu");

        String sql = """
                SELECT
                    CONCAT('DV:', dv.maDichVu) AS maHangMuc,
                    dv.tenDichVu AS tenHangMuc,
                    N'Dịch vụ' AS loaiHangMuc,
                    ctdv.soLuong,
                    CASE WHEN p.trangThai = 'HOAN_TAT'
                         THEN N'Hoàn thành' ELSE N'Theo dõi theo phiếu' END AS trangThai,
                    CAST(0 AS bit) AS laPhatSinh,
                    ctdv.ghiChu
                FROM ChiTietDichVu ctdv
                INNER JOIN DichVu dv ON dv.maDichVu = ctdv.maDichVu
                INNER JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = ctdv.maPhieu
                WHERE ctdv.maPhieu = ?

                UNION ALL

                SELECT
                    CONCAT('PT:', lk.maPhuTung) AS maHangMuc,
                    pt.tenPhuTung AS tenHangMuc,
                    N'Phụ tùng' AS loaiHangMuc,
                    lk.soLuong,
                    CASE WHEN p.trangThai = 'HOAN_TAT'
                         THEN N'Hoàn thành' ELSE N'Theo dõi theo phiếu' END AS trangThai,
                    CAST(0 AS bit) AS laPhatSinh,
                    lk.ghiChu
                FROM ChiTietLinhKien lk
                INNER JOIN PhuTung pt ON pt.maPhuTung = lk.maPhuTung
                INNER JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = lk.maPhieu
                WHERE lk.maPhieu = ?

                ORDER BY loaiHangMuc, maHangMuc
                """;

        List<WorkItemRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu.trim());
            ps.setString(2, maPhieu.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new WorkItemRow(
                            rs.getString("maHangMuc"),
                            rs.getString("tenHangMuc"),
                            rs.getString("loaiHangMuc"),
                            rs.getInt("soLuong"),
                            rs.getString("trangThai"),
                            rs.getBoolean("laPhatSinh"),
                            rs.getString("ghiChu")
                    ));
                }
            }
        }
        return result;
    }

    /** Đọc các yêu cầu xuất kho và số lượng đã được kho duyệt xuất. */
    public List<PartRow> findPartsByRepair(String maPhieu) throws SQLException {
        requireText(maPhieu, "Mã phiếu");

        String sql = """
                SELECT
                    px.maPhieuXK,
                    ct.maPhuTung,
                    pt.tenPhuTung,
                    pt.donViTinh,
                    ct.soLuongXuat AS soLuongYeuCau,
                    CASE WHEN px.trangThai = N'Đã xuất kho'
                         THEN ct.soLuongXuat ELSE 0 END AS soLuongDaXuat,
                    px.trangThai AS trangThaiPhieu,
                    ct.ghiChu
                FROM PhieuXuatKho px
                INNER JOIN ChiTietXuatKho ct ON ct.maPhieuXK = px.maPhieuXK
                INNER JOIN PhuTung pt ON pt.maPhuTung = ct.maPhuTung
                WHERE px.maPhieu = ?
                ORDER BY px.ngayYeuCau DESC, pt.tenPhuTung
                """;

        List<PartRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PartRow(
                            rs.getString("maPhieuXK"),
                            rs.getString("maPhuTung"),
                            rs.getString("tenPhuTung"),
                            rs.getString("donViTinh"),
                            rs.getInt("soLuongYeuCau"),
                            rs.getInt("soLuongDaXuat"),
                            rs.getString("trangThaiPhieu"),
                            rs.getString("ghiChu")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Schema mới không có trạng thái riêng cho ChiTietDichVu/ChiTietLinhKien.
     * Không cập nhật một dòng chi tiết thành Hoàn thành/Chờ linh kiện được.
     */
    public boolean updateWorkItemStatus(
            String maHangMuc,
            String maPhieu,
            String maKTV,
            String trangThaiMoi
    ) throws SQLException {
        throw new SQLException(
                "Database hiện tại không lưu trạng thái từng hạng mục. "
                + "Hãy cập nhật trạng thái chung trên PhieuTiepNhanSuaChua."
        );
    }

    public boolean saveTechnicalNote(String maPhieu, String maKTV, String ghiChu)
            throws SQLException {
        requireText(maPhieu, "Mã phiếu");
        requireText(maKTV, "Mã kỹ thuật viên");

        String note = ghiChu == null ? null : ghiChu.trim();
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException(
                    "Ghi chú kỹ thuật không được vượt quá 500 ký tự.");
        }

        String sql = """
                UPDATE PhieuTiepNhanSuaChua
                SET ghiChuKyThuat = ?
                WHERE maPhieu = ?
                  AND maNVKyThuat = ?
                  AND trangThai IN ('DANG_SUA', 'CHO_LINH_KIEN')
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (note == null || note.isBlank()) {
                ps.setNull(1, Types.NVARCHAR);
            } else {
                ps.setNString(1, note);
            }
            ps.setString(2, maPhieu.trim());
            ps.setString(3, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    /** Bắt đầu thi công hoặc tiếp tục thi công sau khi đã chờ linh kiện. */
    public boolean startWork(String maPhieu, String maKTV) throws SQLException {
        requireText(maPhieu, "Mã phiếu");
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = """
                UPDATE PhieuTiepNhanSuaChua
                SET trangThai = 'DANG_SUA',
                    thoiGianBatDau = COALESCE(thoiGianBatDau, SYSDATETIME())
                WHERE maPhieu = ?
                  AND maNVKyThuat = ?
                  AND trangThai IN ('CHO_SUA', 'CHO_LINH_KIEN')
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu.trim());
            ps.setString(2, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    /** Chuyển toàn bộ phiếu sang trạng thái chờ linh kiện. */
    public boolean markWaitingForParts(String maPhieu, String maKTV) throws SQLException {
        requireText(maPhieu, "Mã phiếu");
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = """
                UPDATE PhieuTiepNhanSuaChua
                SET trangThai = 'CHO_LINH_KIEN'
                WHERE maPhieu = ?
                  AND maNVKyThuat = ?
                  AND trangThai = 'DANG_SUA'
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu.trim());
            ps.setString(2, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Tương thích tên phương thức cũ của GUI.
     * Với schema mới, hoàn tất thi công chuyển trực tiếp sang HOAN_TAT
     * (không tồn tại trạng thái trung gian Chờ kiểm tra).
     */
    public boolean submitForInspection(String maPhieu, String maKTV, String ghiChu)
            throws SQLException {
        requireText(maPhieu, "Mã phiếu");
        requireText(maKTV, "Mã kỹ thuật viên");

        String note = ghiChu == null ? null : ghiChu.trim();
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException(
                    "Ghi chú kỹ thuật không được vượt quá 500 ký tự.");
        }

        String sql = """
                UPDATE p
                SET p.trangThai = 'HOAN_TAT',
                    p.thoiGianHoanThanh = SYSDATETIME(),
                    p.ghiChuKyThuat = ?
                FROM PhieuTiepNhanSuaChua p
                WHERE p.maPhieu = ?
                  AND p.maNVKyThuat = ?
                  AND p.trangThai = 'DANG_SUA'
                  AND (
                      EXISTS (SELECT 1 FROM ChiTietDichVu dv WHERE dv.maPhieu = p.maPhieu)
                      OR EXISTS (SELECT 1 FROM ChiTietLinhKien lk WHERE lk.maPhieu = p.maPhieu)
                  )
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (note == null || note.isBlank()) {
                ps.setNull(1, Types.NVARCHAR);
            } else {
                ps.setNString(1, note);
            }
            ps.setString(2, maPhieu.trim());
            ps.setString(3, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    public List<HistoryRow> findVehicleHistory(String bienSo, String maPhieuHienTai)
            throws SQLException {
        requireText(bienSo, "Biển số xe");

        String sql = """
                SELECT TOP (10)
                    p.maPhieu AS maPhieuSC,
                    CASE p.trangThai
                        WHEN 'CHO_SUA' THEN N'Chờ sửa chữa'
                        WHEN 'DANG_SUA' THEN N'Đang sửa chữa'
                        WHEN 'CHO_LINH_KIEN' THEN N'Chờ linh kiện'
                        WHEN 'HOAN_TAT' THEN N'Hoàn thành'
                        ELSE p.trangThai
                    END AS trangThai,
                    COALESCE(p.thoiGianHoanThanh,
                             p.thoiGianBatDau,
                             p.ngayGioTiepNhan) AS thoiGian,
                    p.yeuCauBanDau AS yeuCau
                FROM PhieuTiepNhanSuaChua p
                WHERE p.bienSo = ?
                  AND p.maPhieu <> ?
                ORDER BY COALESCE(p.thoiGianHoanThanh,
                                  p.thoiGianBatDau,
                                  p.ngayGioTiepNhan) DESC
                """;

        List<HistoryRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim());
            ps.setString(2, maPhieuHienTai == null ? "" : maPhieuHienTai.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new HistoryRow(
                            rs.getString("maPhieuSC"),
                            rs.getString("trangThai"),
                            toLocalDateTime(rs.getTimestamp("thoiGian")),
                            rs.getString("yeuCau")
                    ));
                }
            }
        }
        return result;
    }

    private static TicketRow mapTicket(ResultSet rs) throws SQLException {
        return new TicketRow(
                rs.getString("maPhieuSC"),
                rs.getString("maKTV"),
                rs.getString("trangThai"),
                rs.getString("bienSo"),
                rs.getString("thongTinXe"),
                rs.getString("tenKhach"),
                rs.getString("soDienThoai"),
                rs.getDouble("soKmTiepNhan"),
                rs.getString("tinhTrangBanDau"),
                rs.getString("yeuCauKhachHang"),
                rs.getString("maPhieuTN"),
                toLocalDateTime(rs.getTimestamp("thoiGianTiepNhan")),
                toLocalDateTime(rs.getTimestamp("thoiGianDuKien")),
                toLocalDateTime(rs.getTimestamp("thoiGianBatDau")),
                toLocalDateTime(rs.getTimestamp("thoiGianHoanThanh")),
                rs.getString("ghiChuKyThuat"),
                rs.getInt("tongHangMuc"),
                rs.getInt("hangMucHoanThanh")
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " không được để trống.");
        }
    }
}

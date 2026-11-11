package dao;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** DAO phục vụ riêng màn hình Công việc kỹ thuật viên. */
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
            return tongHangMuc <= 0 ? 0 : Math.min(100, hangMucHoanThanh * 100 / tongHangMuc);
        }
    }

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
            SELECT p.maPhieuSC,
                   p.maKTV,
                   p.trangThai,
                   tn.bienSo,
                   CONCAT(x.hangXe, N' ', x.model, N' ', x.namSanXuat, N' · ', x.mauXe) AS thongTinXe,
                   kh.hoTen AS tenKhach,
                   kh.soDienThoai,
                   tn.soKmTiepNhan,
                   tn.tinhTrangBanDau,
                   tn.yeuCauKhachHang,
                   tn.maPhieuTN,
                   tn.thoiGianTiepNhan,
                   p.thoiGianDuKien,
                   p.thoiGianBatDau,
                   p.thoiGianHoanThanh,
                   p.ghiChuKyThuat,
                   (SELECT COUNT(*)
                    FROM HangMucSuaChua hm
                    WHERE hm.maPhieuSC = p.maPhieuSC
                      AND hm.trangThai <> N'Từ chối') AS tongHangMuc,
                   (SELECT COUNT(*)
                    FROM HangMucSuaChua hm
                    WHERE hm.maPhieuSC = p.maPhieuSC
                      AND hm.trangThai = N'Hoàn thành') AS hangMucHoanThanh
            FROM PhieuSuaChua p
            INNER JOIN PhieuTiepNhan tn ON tn.maPhieuTN = p.maPhieuTN
            INNER JOIN Xe x ON x.bienSo = tn.bienSo
            INNER JOIN KhachHang kh ON kh.maKH = tn.maKH
            WHERE p.maKTV = ?
              AND p.trangThai IN (N'Đã xác nhận', N'Đang sửa chữa', N'Chờ kiểm tra')
            ORDER BY CASE WHEN p.thoiGianDuKien IS NULL THEN 1 ELSE 0 END,
                     p.thoiGianDuKien,
                     tn.thoiGianTiepNhan DESC
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

    public TicketRow findTicketById(String maPhieuSC, String maKTV) throws SQLException {
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        requireText(maKTV, "Mã kỹ thuật viên");
        String sql = SELECT_TICKETS.replace(
                "WHERE p.maKTV = ?\n              AND p.trangThai IN (N'Đã xác nhận', N'Đang sửa chữa', N'Chờ kiểm tra')",
                "WHERE p.maKTV = ? AND p.maPhieuSC = ?\n              AND p.trangThai IN (N'Đã xác nhận', N'Đang sửa chữa', N'Chờ kiểm tra')"
        );
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV.trim());
            ps.setString(2, maPhieuSC.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTicket(rs) : null;
            }
        }
    }

    public List<WorkItemRow> findWorkItems(String maPhieuSC) throws SQLException {
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        String sql = """
                SELECT maHangMuc, tenHangMuc, loaiHangMuc, soLuong,
                       trangThai, laPhatSinh, ghiChu
                FROM HangMucSuaChua
                WHERE maPhieuSC = ?
                ORDER BY laPhatSinh, maHangMuc
                """;
        List<WorkItemRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuSC.trim());
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

    public List<PartRow> findPartsByRepair(String maPhieuSC) throws SQLException {
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        String sql = """
                SELECT px.maPhieuXK,
                       ct.maPhuTung,
                       pt.tenPhuTung,
                       pt.donViTinh,
                       ct.soLuongXuat,
                       px.trangThai,
                       ct.ghiChu
                FROM PhieuXuatKho px
                INNER JOIN ChiTietXuatKho ct ON ct.maPhieuXK = px.maPhieuXK
                INNER JOIN PhuTung pt ON pt.maPhuTung = ct.maPhuTung
                WHERE px.maPhieuSC = ?
                ORDER BY px.ngayYeuCau DESC, pt.tenPhuTung
                """;
        List<PartRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuSC.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("trangThai");
                    int requested = rs.getInt("soLuongXuat");
                    int issued = "Đã xuất kho".equals(status) ? requested : 0;
                    result.add(new PartRow(
                            rs.getString("maPhieuXK"),
                            rs.getString("maPhuTung"),
                            rs.getString("tenPhuTung"),
                            rs.getString("donViTinh"),
                            requested,
                            issued,
                            status,
                            rs.getString("ghiChu")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Chuyển trạng thái checklist. Chỉ cho KTV được phân công cập nhật hạng mục
     * của phiếu đang sửa; chỉ cho phép chuyển giữa Đã xác nhận và Hoàn thành.
     */
    public boolean updateWorkItemStatus(
            String maHangMuc,
            String maPhieuSC,
            String maKTV,
            String trangThaiMoi
    ) throws SQLException {
        requireText(maHangMuc, "Mã hạng mục");
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        requireText(maKTV, "Mã kỹ thuật viên");
        if (!"Đã xác nhận".equals(trangThaiMoi) && !"Hoàn thành".equals(trangThaiMoi)) {
            throw new IllegalArgumentException("Trạng thái hạng mục không hợp lệ.");
        }

        String sql = """
                UPDATE hm
                SET trangThai = ?
                FROM HangMucSuaChua hm
                INNER JOIN PhieuSuaChua p ON p.maPhieuSC = hm.maPhieuSC
                WHERE hm.maHangMuc = ?
                  AND hm.maPhieuSC = ?
                  AND p.maKTV = ?
                  AND p.trangThai = N'Đang sửa chữa'
                  AND hm.trangThai IN (N'Đã xác nhận', N'Hoàn thành')
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trangThaiMoi);
            ps.setString(2, maHangMuc.trim());
            ps.setString(3, maPhieuSC.trim());
            ps.setString(4, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean saveTechnicalNote(String maPhieuSC, String maKTV, String ghiChu) throws SQLException {
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        requireText(maKTV, "Mã kỹ thuật viên");
        String note = ghiChu == null ? null : ghiChu.trim();
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException("Ghi chú kỹ thuật không được vượt quá 500 ký tự.");
        }
        String sql = """
                UPDATE PhieuSuaChua
                SET ghiChuKyThuat = ?
                WHERE maPhieuSC = ?
                  AND maKTV = ?
                  AND trangThai = N'Đang sửa chữa'
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (note == null || note.isBlank()) ps.setNull(1, java.sql.Types.NVARCHAR);
            else ps.setNString(1, note);
            ps.setString(2, maPhieuSC.trim());
            ps.setString(3, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean startWork(String maPhieuSC, String maKTV) throws SQLException {
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        requireText(maKTV, "Mã kỹ thuật viên");
        String sql = """
                UPDATE PhieuSuaChua
                SET trangThai = N'Đang sửa chữa', thoiGianBatDau = SYSDATETIME()
                WHERE maPhieuSC = ? AND maKTV = ? AND trangThai = N'Đã xác nhận'
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuSC.trim());
            ps.setString(2, maKTV.trim());
            return ps.executeUpdate() == 1;
        }
    }

    /** Chỉ chuyển sang Chờ kiểm tra khi tất cả hạng mục không bị từ chối đã hoàn thành. */
    public boolean submitForInspection(String maPhieuSC, String maKTV, String ghiChu) throws SQLException {
        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        requireText(maKTV, "Mã kỹ thuật viên");
        String note = ghiChu == null ? null : ghiChu.trim();
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException("Ghi chú kỹ thuật không được vượt quá 500 ký tự.");
        }

        return ConnectDB.inTransaction(c -> {
            String sqlCount = """
                    SELECT COUNT(*) AS tong,
                           SUM(CASE WHEN trangThai NOT IN (N'Hoàn thành', N'Từ chối') THEN 1 ELSE 0 END) AS chuaXong
                    FROM HangMucSuaChua WITH (UPDLOCK, HOLDLOCK)
                    WHERE maPhieuSC = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlCount)) {
                ps.setString(1, maPhieuSC.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("tong") == 0) {
                        throw new SQLException("Phiếu sửa chữa chưa có hạng mục để hoàn tất.");
                    }
                    if (rs.getInt("chuaXong") > 0) {
                        throw new SQLException("Vẫn còn hạng mục chưa hoàn thành hoặc đang chờ duyệt.");
                    }
                }
            }

            String sqlUpdate = """
                    UPDATE PhieuSuaChua
                    SET trangThai = N'Chờ kiểm tra',
                        ghiChuKyThuat = ?
                    WHERE maPhieuSC = ?
                      AND maKTV = ?
                      AND trangThai = N'Đang sửa chữa'
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                if (note == null || note.isBlank()) ps.setNull(1, java.sql.Types.NVARCHAR);
                else ps.setNString(1, note);
                ps.setString(2, maPhieuSC.trim());
                ps.setString(3, maKTV.trim());
                return ps.executeUpdate() == 1;
            }
        });
    }

    public List<HistoryRow> findVehicleHistory(String bienSo, String maPhieuHienTai) throws SQLException {
        requireText(bienSo, "Biển số xe");
        String sql = """
                SELECT TOP 10 p.maPhieuSC, p.trangThai,
                       COALESCE(p.thoiGianHoanThanh, p.thoiGianBatDau, tn.thoiGianTiepNhan) AS thoiGian,
                       tn.yeuCauKhachHang
                FROM PhieuSuaChua p
                INNER JOIN PhieuTiepNhan tn ON tn.maPhieuTN = p.maPhieuTN
                WHERE tn.bienSo = ? AND p.maPhieuSC <> ?
                ORDER BY COALESCE(p.thoiGianHoanThanh, p.thoiGianBatDau, tn.thoiGianTiepNhan) DESC
                """;
        List<HistoryRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bienSo.trim());
            ps.setString(2, maPhieuHienTai == null ? "" : maPhieuHienTai.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("thoiGian");
                    result.add(new HistoryRow(
                            rs.getString("maPhieuSC"),
                            rs.getString("trangThai"),
                            ts == null ? null : ts.toLocalDateTime(),
                            rs.getString("yeuCauKhachHang")
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

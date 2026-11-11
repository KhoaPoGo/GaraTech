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

/**
 * DAO cho màn hình yêu cầu phụ tùng của kỹ thuật viên.
 * Dùng schema mới: PhieuTiepNhanSuaChua, PhieuXuatKho, Xe, NhanVien.
 */
public final class YeuCauPhuTungDao {

    /** Phiếu được phân công cho KTV; maPhieuSC là tên alias tương thích GUI cũ. */
    public record RepairTicketRow(
            String maPhieuSC,
            String trangThai,
            String bienSo,
            String thongTinXe
    ) {
        public boolean duocYeuCauPhuTung() {
            return "Đang sửa chữa".equals(trangThai)
                    || "Chờ linh kiện".equals(trangThai);
        }
    }

    /** Lịch sử yêu cầu xuất kho của KTV. */
    public record RequestRow(
            String maPhieuXK,
            String maPhieuSC,
            String bienSo,
            String thongTinXe,
            LocalDateTime ngayYeuCau,
            LocalDateTime ngayXuat,
            String lyDoXuat,
            String lyDoTuChoi,
            String tenNVKho,
            String trangThai
    ) { }

    /**
     * Tìm các phiếu chưa hoàn tất đang được phân công cho KTV.
     * Database lưu trạng thái dạng mã; DAO chuyển sang nhãn tiếng Việt để GUI dùng.
     * Có cả CHO_LINH_KIEN vì KTV cần gửi yêu cầu vật tư khi phiếu đang chờ linh kiện.
     */
    public List<RepairTicketRow> findAssignedRepairs(String maKTV) throws SQLException {
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = """
                SELECT p.maPhieu AS maPhieuSC,
                       CASE p.trangThai
                           WHEN 'CHO_SUA' THEN N'Chờ sửa chữa'
                           WHEN 'DANG_SUA' THEN N'Đang sửa chữa'
                           WHEN 'CHO_LINH_KIEN' THEN N'Chờ linh kiện'
                           WHEN 'HOAN_TAT' THEN N'Hoàn tất'
                           ELSE p.trangThai
                       END AS trangThaiHienThi,
                       p.bienSo,
                       CONCAT(x.hangXe, N' ', x.model, N' ', x.namSanXuat,
                              N' · ', x.mauXe) AS thongTinXe
                FROM dbo.PhieuTiepNhanSuaChua p
                INNER JOIN dbo.Xe x ON x.bienSo = p.bienSo
                WHERE p.maNVKyThuat = ?
                  AND p.trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN')
                ORDER BY CASE p.trangThai
                             WHEN 'CHO_LINH_KIEN' THEN 0
                             WHEN 'DANG_SUA' THEN 1
                             WHEN 'CHO_SUA' THEN 2
                             ELSE 3
                         END,
                         CASE WHEN p.thoiGianHenTra IS NULL THEN 1 ELSE 0 END,
                         p.thoiGianHenTra,
                         p.ngayGioTiepNhan DESC
                """;

        List<RepairTicketRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new RepairTicketRow(
                            rs.getString("maPhieuSC"),
                            rs.getString("trangThaiHienThi"),
                            rs.getString("bienSo"),
                            rs.getString("thongTinXe")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Đọc lịch sử yêu cầu kho theo kỹ thuật viên.
     * PhieuXuatKho.maPhieuSC là computed column, nhưng truy vấn dùng maPhieu gốc
     * để thể hiện đúng cấu trúc schema mới.
     */
    public List<RequestRow> findRequestsByTechnician(String maKTV) throws SQLException {
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = """
                SELECT px.maPhieuXK,
                       p.maPhieu AS maPhieuSC,
                       p.bienSo,
                       CONCAT(x.hangXe, N' ', x.model, N' ', x.namSanXuat,
                              N' · ', x.mauXe) AS thongTinXe,
                       px.ngayYeuCau,
                       px.ngayXuat,
                       px.lyDoXuat,
                       px.lyDoTuChoi,
                       nv.hoTen AS tenNVKho,
                       px.trangThai
                FROM dbo.PhieuXuatKho px
                INNER JOIN dbo.PhieuTiepNhanSuaChua p ON p.maPhieu = px.maPhieu
                INNER JOIN dbo.Xe x ON x.bienSo = p.bienSo
                LEFT JOIN dbo.NhanVien nv ON nv.maNV = px.maNVKho
                WHERE px.maKTVYeuCau = ?
                ORDER BY px.ngayYeuCau DESC, px.maPhieuXK DESC
                """;

        List<RequestRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp created = rs.getTimestamp("ngayYeuCau");
                    Timestamp issued = rs.getTimestamp("ngayXuat");
                    result.add(new RequestRow(
                            rs.getString("maPhieuXK"),
                            rs.getString("maPhieuSC"),
                            rs.getString("bienSo"),
                            rs.getString("thongTinXe"),
                            toLocalDateTime(created),
                            toLocalDateTime(issued),
                            rs.getString("lyDoXuat"),
                            rs.getString("lyDoTuChoi"),
                            rs.getString("tenNVKho"),
                            rs.getString("trangThai")
                    ));
                }
            }
        }
        return result;
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }
}

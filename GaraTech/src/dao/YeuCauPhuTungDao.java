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

/** DAO truy vấn danh sách phiếu sửa chữa được phân công và lịch sử yêu cầu phụ tùng của KTV. */
public final class YeuCauPhuTungDao {

    /** Phiếu sửa chữa mà KTV có thể chọn để lập yêu cầu phụ tùng. */
    public record RepairTicketRow(
            String maPhieuSC,
            String trangThai,
            String bienSo,
            String thongTinXe
    ) {
        public boolean dangSuaChua() {
            return "Đang sửa chữa".equals(trangThai);
        }
    }

    /** Thông tin phiếu yêu cầu xuất kho kèm thông tin xe để hiển thị lịch sử. */
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
     * Danh sách phiếu đang được phân công cho kỹ thuật viên.
     * Chỉ đưa vào lựa chọn các phiếu Đã xác nhận hoặc Đang sửa chữa.
     * Việc gửi yêu cầu chỉ được bật khi phiếu ở trạng thái Đang sửa chữa.
     */
    public List<RepairTicketRow> findAssignedRepairs(String maKTV) throws SQLException {
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = """
                SELECT p.maPhieuSC,
                       p.trangThai,
                       tn.bienSo,
                       CONCAT(x.hangXe, N' ', x.model, N' ', x.namSanXuat, N' · ', x.mauXe) AS thongTinXe
                FROM PhieuSuaChua p
                INNER JOIN PhieuTiepNhan tn ON tn.maPhieuTN = p.maPhieuTN
                INNER JOIN Xe x ON x.bienSo = tn.bienSo
                WHERE p.maKTV = ?
                  AND p.trangThai IN (N'Đã xác nhận', N'Đang sửa chữa')
                ORDER BY CASE WHEN p.trangThai = N'Đang sửa chữa' THEN 0 ELSE 1 END,
                         p.thoiGianDuKien,
                         p.maPhieuSC DESC
                """;

        List<RepairTicketRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new RepairTicketRow(
                            rs.getString("maPhieuSC"),
                            rs.getString("trangThai"),
                            rs.getString("bienSo"),
                            rs.getString("thongTinXe")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Lấy lịch sử mọi yêu cầu phụ tùng do kỹ thuật viên hiện tại gửi.
     * JOIN qua Phiếu sửa chữa để vẫn hiển thị được xe của các phiếu đã hoàn tất.
     */
    public List<RequestRow> findRequestsByTechnician(String maKTV) throws SQLException {
        requireText(maKTV, "Mã kỹ thuật viên");

        String sql = """
                SELECT px.maPhieuXK,
                       px.maPhieuSC,
                       tn.bienSo,
                       CONCAT(x.hangXe, N' ', x.model, N' ', x.namSanXuat, N' · ', x.mauXe) AS thongTinXe,
                       px.ngayYeuCau,
                       px.ngayXuat,
                       px.lyDoXuat,
                       px.lyDoTuChoi,
                       nv.hoTen AS tenNVKho,
                       px.trangThai
                FROM PhieuXuatKho px
                INNER JOIN PhieuSuaChua p ON p.maPhieuSC = px.maPhieuSC
                INNER JOIN PhieuTiepNhan tn ON tn.maPhieuTN = p.maPhieuTN
                INNER JOIN Xe x ON x.bienSo = tn.bienSo
                LEFT JOIN NhanVien nv ON nv.maNV = px.maNVKho
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

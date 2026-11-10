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
 * Truy vấn dữ liệu thống kê tức thời cho Bảng điều hành Tiếp nhận viên (Dashboard Tiếp nhận).
 * Chuẩn hóa theo thiết kế CarZone:
 * - 4 chỉ số KPI tức thời (dạng số nguyên 0, 1, 2...).
 * - Tải công việc kỹ thuật viên (sắp xếp tăng dần từ bé đến lớn).
 * - Danh sách xe sẵn sàng bàn giao (HOAN_TAT).
 * - Danh sách xe đang xử lý tại xưởng (lọc theo trạng thái).
 */
public final class TiepNhanDashboardDao {

    public record KtvWorkloadRow(
            String maNV,
            String hoTen,
            int soXeDangPhuTrach,
            String tinhTrang // "Rảnh rỗi", "Đang làm", "Nhiều việc"
    ) {}

    public record XeChoGiaoRow(
            String maPhieu,
            String bienSo,
            String khachHang,
            String soDienThoai,
            String ktvChinh,
            double tongTien,
            LocalDateTime thoiGianHoanThanh
    ) {}

    public record PhieuDangXuLyRow(
            String maPhieu,
            String bienSo,
            String khachHang,
            String soDienThoai,
            String ktvChinh,
            LocalDateTime thoiGianTiepNhan,
            String trangThai,
            String yeuCauBanDau
    ) {}

    public record DashboardData(
            int tiepNhanHomNay,
            int chuaPhanCongKTV,
            int dangSua,
            int hoanTatChoGiao,
            int choSua,
            int choLinhKien,
            List<KtvWorkloadRow> dsKtvWorkload,
            List<XeChoGiaoRow> dsChoGiao,
            List<PhieuDangXuLyRow> dsDangXuLy
    ) {}

    public DashboardData getDashboardData() throws SQLException {
        int tiepNhanHomNay = 0;
        int chuaPhanCongKTV = 0;
        int dangSua = 0;
        int hoanTatChoGiao = 0;
        int choSua = 0;
        int choLinhKien = 0;
        List<KtvWorkloadRow> dsKtvWorkload = new ArrayList<>();
        List<XeChoGiaoRow> dsChoGiao = new ArrayList<>();
        List<PhieuDangXuLyRow> dsDangXuLy = new ArrayList<>();

        try (Connection c = ConnectDB.getConnection()) {
            // 1. Số xe tiếp nhận hôm nay
            String sqlToday = "SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE CAST(ngayGioTiepNhan AS DATE) = CAST(SYSDATETIME() AS DATE)";
            try (PreparedStatement ps = c.prepareStatement(sqlToday); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tiepNhanHomNay = rs.getInt(1);
            }

            // 2. Số xe cần phân công KTV (chưa có KTV phụ trách)
            String sqlNoKtv = "SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai IN ('CHO_SUA', 'DANG_SUA') AND (maNVKyThuat IS NULL OR LTRIM(RTRIM(maNVKyThuat)) = '')";
            try (PreparedStatement ps = c.prepareStatement(sqlNoKtv); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) chuaPhanCongKTV = rs.getInt(1);
            }

            // 3. Phân bổ theo 4 trạng thái
            String sqlStatus = "SELECT trangThai, COUNT(*) FROM PhieuTiepNhanSuaChua GROUP BY trangThai";
            try (PreparedStatement ps = c.prepareStatement(sqlStatus); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tt = rs.getString(1);
                    int count = rs.getInt(2);
                    if ("CHO_SUA".equalsIgnoreCase(tt)) choSua = count;
                    else if ("DANG_SUA".equalsIgnoreCase(tt)) dangSua = count;
                    else if ("CHO_LINH_KIEN".equalsIgnoreCase(tt)) choLinhKien = count;
                    else if ("HOAN_TAT".equalsIgnoreCase(tt)) hoanTatChoGiao = count;
                }
            }

            // 4. Tải công việc kỹ thuật viên (SẮP XẾP TỪ BÉ ĐẾN LỚN để ưu tiên thợ rảnh)
            String sqlKtvWorkload = """
                SELECT nv.maNV, nv.hoTen, 
                       COUNT(CASE WHEN p.trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN') THEN 1 END) AS soXe
                FROM NhanVien nv
                LEFT JOIN PhieuTiepNhanSuaChua p ON p.maNVKyThuat = nv.maNV
                WHERE nv.vaiTro = N'Kỹ thuật viên' AND nv.dangLam = 1
                GROUP BY nv.maNV, nv.hoTen
                ORDER BY soXe ASC, nv.hoTen ASC
                """;
            try (PreparedStatement ps = c.prepareStatement(sqlKtvWorkload); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int soXe = rs.getInt("soXe");
                    String tinhTrang;
                    if (soXe == 0) tinhTrang = "Rảnh rỗi (0 xe)";
                    else if (soXe <= 2) tinhTrang = "Đang làm (" + soXe + " xe)";
                    else tinhTrang = "Nhiều việc (" + soXe + " xe)";

                    dsKtvWorkload.add(new KtvWorkloadRow(
                            rs.getString("maNV"),
                            rs.getString("hoTen"),
                            soXe,
                            tinhTrang
                    ));
                }
            }

            // 5. Danh sách xe chờ bàn giao / chờ thu tiền (HOAN_TAT)
            String sqlChoGiao = """
                SELECT TOP 10 p.maPhieu, p.bienSo, kh.hoTen, kh.soDienThoai, nv.hoTen AS tenKTV, 
                       ISNULL(v.tongChiPhi, 0) AS tongTien, p.thoiGianHoanThanh
                FROM PhieuTiepNhanSuaChua p
                JOIN Xe x ON x.bienSo = p.bienSo
                JOIN KhachHang kh ON kh.maKH = x.maKH
                LEFT JOIN NhanVien nv ON nv.maNV = p.maNVKyThuat
                LEFT JOIN vw_PhieuTiepNhanSuaChua_TongTien v ON v.maPhieu = p.maPhieu
                WHERE p.trangThai = 'HOAN_TAT'
                ORDER BY p.thoiGianHoanThanh DESC
                """;
            try (PreparedStatement ps = c.prepareStatement(sqlChoGiao); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("thoiGianHoanThanh");
                    LocalDateTime dt = ts != null ? ts.toLocalDateTime() : null;
                    dsChoGiao.add(new XeChoGiaoRow(
                            rs.getString("maPhieu"),
                            rs.getString("bienSo"),
                            rs.getString("hoTen"),
                            rs.getString("soDienThoai"),
                            rs.getString("tenKTV") != null ? rs.getString("tenKTV") : "Chưa phân công",
                            rs.getDouble("tongTien"),
                            dt
                    ));
                }
            }

            // 6. Danh sách xe đang xử lý trong xưởng (CHO_SUA, DANG_SUA, CHO_LINH_KIEN)
            String sqlLive = """
                SELECT p.maPhieu, p.bienSo, kh.hoTen, kh.soDienThoai, nv.hoTen AS tenKTV, 
                       p.ngayGioTiepNhan, p.trangThai, p.yeuCauBanDau
                FROM PhieuTiepNhanSuaChua p
                JOIN Xe x ON x.bienSo = p.bienSo
                JOIN KhachHang kh ON kh.maKH = x.maKH
                LEFT JOIN NhanVien nv ON nv.maNV = p.maNVKyThuat
                WHERE p.trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN')
                ORDER BY p.ngayGioTiepNhan ASC
                """;
            try (PreparedStatement ps = c.prepareStatement(sqlLive); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("ngayGioTiepNhan");
                    LocalDateTime dt = ts != null ? ts.toLocalDateTime() : null;
                    dsDangXuLy.add(new PhieuDangXuLyRow(
                            rs.getString("maPhieu"),
                            rs.getString("bienSo"),
                            rs.getString("hoTen"),
                            rs.getString("soDienThoai"),
                            rs.getString("tenKTV") != null ? rs.getString("tenKTV") : "Chưa phân công",
                            dt,
                            rs.getString("trangThai"),
                            rs.getString("yeuCauBanDau")
                    ));
                }
            }
        }
        return new DashboardData(tiepNhanHomNay, chuaPhanCongKTV, dangSua, hoanTatChoGiao, choSua, choLinhKien, dsKtvWorkload, dsChoGiao, dsDangXuLy);
    }
}

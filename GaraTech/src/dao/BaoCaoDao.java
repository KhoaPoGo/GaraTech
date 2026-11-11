package dao;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Các truy vấn thống kê cho Dashboard và Báo cáo của quản lý (chỉ đọc).
 * Khoảng thời gian luôn là [từ 00:00 ngày from, đến 00:00 ngày to+1) nên bao gồm trọn ngày cuối.
 * "Thực thu" tính từ GiaoDichThanhToan trạng thái "Đã thanh toán"; hóa đơn "Đã hủy" không tính.
 */
public class BaoCaoDao {

    public record DongThongKe(String ten, long soLuong, long doanhThu) {}

    public record TonKhoThap(String ten, int ton, int toiThieu) {}

    public record CanhBaoKho(int chamNguong, int hetHang, List<TonKhoThap> top) {}

    private static Timestamp start(LocalDate d) { return Timestamp.valueOf(d.atStartOfDay()); }

    private static Timestamp endExclusive(LocalDate d) { return Timestamp.valueOf(d.plusDays(1).atStartOfDay()); }

    private static long scalarLong(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Tổng tiền đã thu trong khoảng ngày. */
    public long thucThu(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT ISNULL(SUM(tongTienThanhToan), 0) FROM HoaDon"
                + " WHERE trangThai = N'Đã thanh toán' AND ngayLap >= ? AND ngayLap < ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, endExclusive(to));
            return scalarLong(ps);
        }
    }

    /** Tiền còn phải thu của các hóa đơn lập trong khoảng ngày (bỏ hóa đơn hủy). */
    public long conChoThu(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT ISNULL(SUM(tongTienThanhToan), 0) FROM HoaDon"
                + " WHERE trangThai = N'Chưa thanh toán' AND ngayLap >= ? AND ngayLap < ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, endExclusive(to));
            return scalarLong(ps);
        }
    }

    public long soHoaDon(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM HoaDon WHERE trangThai <> N'Đã hủy' AND ngayLap >= ? AND ngayLap < ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, endExclusive(to));
            return scalarLong(ps);
        }
    }

    /** Số phiếu sửa chữa hoàn thành trong khoảng ngày. */
    public long soPhieuHoanThanh(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai = 'HOAN_TAT'"
                + " AND thoiGianHoanThanh >= ? AND thoiGianHoanThanh < ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, endExclusive(to));
            return scalarLong(ps);
        }
    }

    /** Thực thu theo từng ngày có giao dịch (ngày không có giao dịch không xuất hiện trong kết quả). */
    public Map<LocalDate, Long> thucThuTheoNgay(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT CAST(ngayLap AS DATE) AS ngay, SUM(tongTienThanhToan) AS tong FROM HoaDon"
                + " WHERE trangThai = N'Đã thanh toán' AND ngayLap >= ? AND ngayLap < ?"
                + " GROUP BY CAST(ngayLap AS DATE)";
        Map<LocalDate, Long> out = new LinkedHashMap<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, endExclusive(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getDate("ngay").toLocalDate(), rs.getLong("tong"));
            }
        }
        return out;
    }

    /** Doanh thu theo dịch vụ (từ chi tiết hóa đơn), nhiều nhất trước. */
    public List<DongThongKe> coCauDichVu(LocalDate from, LocalDate to) throws SQLException {
        String sql = """
                SELECT c.tenChiTiet AS ten, SUM(c.soLuong) AS soLuong, SUM(c.thanhTien) AS doanhThu
                FROM ChiTietHoaDon c JOIN HoaDon h ON h.maHoaDon = c.maHoaDon
                WHERE c.loaiChiTiet = 'DichVu' AND h.trangThai <> N'Đã hủy' AND h.ngayLap >= ? AND h.ngayLap < ?
                GROUP BY c.maDichVu, c.tenChiTiet
                ORDER BY SUM(c.thanhTien) DESC
                """;
        return thongKe(sql, from, to);
    }

    /** Phụ tùng tiêu thụ theo nhóm (cột loaiPhuTung), nhiều nhất trước. */
    public List<DongThongKe> nhomPhuTung(LocalDate from, LocalDate to) throws SQLException {
        String sql = """
                SELECT p.loaiPhuTung AS ten, SUM(c.soLuong) AS soLuong, SUM(c.thanhTien) AS doanhThu
                FROM ChiTietHoaDon c
                JOIN HoaDon h ON h.maHoaDon = c.maHoaDon
                JOIN PhuTung p ON p.maPhuTung = c.maPhuTung
                WHERE c.loaiChiTiet = 'PhuTung' AND h.trangThai <> N'Đã hủy' AND h.ngayLap >= ? AND h.ngayLap < ?
                GROUP BY p.loaiPhuTung
                ORDER BY SUM(c.thanhTien) DESC
                """;
        return thongKe(sql, from, to);
    }

    private List<DongThongKe> thongKe(String sql, LocalDate from, LocalDate to) throws SQLException {
        List<DongThongKe> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, endExclusive(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new DongThongKe(rs.getString("ten"), rs.getLong("soLuong"), rs.getLong("doanhThu")));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- Dashboard

    public long demTiepNhanTrongNgay(LocalDate d) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE ngayGioTiepNhan >= ? AND ngayGioTiepNhan < ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(d));
            ps.setTimestamp(2, endExclusive(d));
            return scalarLong(ps);
        }
    }

    public long demPhieuTiepNhanChoXuLy() throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai = 'CHO_SUA'")) {
            return scalarLong(ps);
        }
    }

    /** Số phiếu sửa chữa theo từng trạng thái (chỉ trạng thái có phiếu). */
    public Map<String, Long> demPhieuTheoTrangThai() throws SQLException {
        Map<String, Long> out = new LinkedHashMap<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT trangThai, COUNT(*) AS soPhieu FROM PhieuTiepNhanSuaChua GROUP BY trangThai");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString("trangThai"), rs.getLong("soPhieu"));
        }
        return out;
    }

    /** Số phiếu đang sửa / chờ sửa mà quá hạn dự kiến. */
    public long demPhieuQuaHan(LocalDateTime now) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN') AND thoiGianHenTra IS NOT NULL AND thoiGianHenTra < ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            return scalarLong(ps);
        }
    }

    /** Số phiếu hoàn thành trong ngày. */
    public Map<String, Long> demHoanThanhTrongNgay(LocalDate d) throws SQLException {
        Map<String, Long> out = new LinkedHashMap<>();
        String sql = "SELECT trangThai, COUNT(*) AS soPhieu FROM PhieuTiepNhanSuaChua"
                + " WHERE trangThai = 'HOAN_TAT' AND thoiGianHoanThanh >= ? AND thoiGianHoanThanh < ?"
                + " GROUP BY trangThai";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, start(d));
            ps.setTimestamp(2, endExclusive(d));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("trangThai"), rs.getLong("soPhieu"));
            }
        }
        return out;
    }

    public CanhBaoKho canhBaoTonKho(int top) throws SQLException {
        int cham = 0, het = 0;
        String dem = "SELECT SUM(CASE WHEN soLuongTon = 0 THEN 1 ELSE 0 END) AS het,"
                + " SUM(CASE WHEN soLuongTon > 0 THEN 1 ELSE 0 END) AS cham FROM PhuTung WHERE soLuongTon <= tonKhoToiThieu";
        List<TonKhoThap> ds = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(dem); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { het = rs.getInt("het"); cham = rs.getInt("cham"); }
            }
            String sql = "SELECT TOP " + top + " tenPhuTung, soLuongTon, tonKhoToiThieu FROM PhuTung"
                    + " WHERE soLuongTon <= tonKhoToiThieu ORDER BY soLuongTon - tonKhoToiThieu, tenPhuTung";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ds.add(new TonKhoThap(rs.getString("tenPhuTung"), rs.getInt("soLuongTon"), rs.getInt("tonKhoToiThieu")));
            }
        }
        return new CanhBaoKho(cham, het, ds);
    }
}

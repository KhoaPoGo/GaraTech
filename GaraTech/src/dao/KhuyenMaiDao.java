package dao;

import connectDB.ConnectDB;
import entity.KhuyenMai;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object quản lý dữ liệu Khuyến mãi.
 * Hỗ trợ chế độ Demo Mode khi chưa kết nối CSDL.
 */
public class KhuyenMaiDao {

    // Danh sách khuyến mãi mẫu cho Demo Mode
    private static final List<KhuyenMai> DEMO_LIST = new ArrayList<>();

    static {
        initDemoData();
    }

    private static synchronized void initDemoData() {
        DEMO_LIST.clear();
        LocalDate now = LocalDate.now();

        // 1. KM001: Ưu đãi bảo dưỡng định kỳ - giảm 10% toàn bộ
        KhuyenMai km1 = new KhuyenMai(
                "KM001",
                "Ưu đãi bảo dưỡng định kỳ",
                10.0,
                now.minusMonths(1),
                now.plusMonths(2),
                "Giảm 10% toàn bộ dịch vụ và linh kiện bảo dưỡng"
        );
        DEMO_LIST.add(km1);

        // 2. KM002: Khuyến mãi thay nhớt & lọc dầu - giảm 15%
        KhuyenMai km2 = new KhuyenMai(
                "KM002",
                "Khuyến mãi thay nhớt & lọc dầu",
                15.0,
                now.minusMonths(1),
                now.plusMonths(1),
                "Giảm 15% cho phụ tùng dầu nhớt và lọc dầu"
        );
        Set<String> ptSet = new HashSet<>();
        ptSet.add("PT0001");
        ptSet.add("PT0002");
        km2.setDsPhuTungApDung(ptSet);
        DEMO_LIST.add(km2);

        // 3. KM003: Tri ân khách hàng thân thiết - giảm 5%
        KhuyenMai km3 = new KhuyenMai(
                "KM003",
                "Tri ân khách hàng thân thiết",
                5.0,
                now.minusMonths(2),
                now.plusMonths(3),
                "Áp dụng giảm 5% trên toàn bộ hóa đơn thanh toán"
        );
        DEMO_LIST.add(km3);
    }

    /**
     * Lấy danh sách các chương trình khuyến mãi đang có hiệu lực.
     */
    public List<KhuyenMai> layDanhSachHieuLuc() {
        if (ConnectDB.demoMode()) {
            LocalDate today = LocalDate.now();
            return DEMO_LIST.stream().filter(k -> k.conHieuLuc(today)).toList();
        }

        String sql = """
                SELECT maKM, maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu
                FROM KhuyenMai
                WHERE ngayBatDau <= CAST(SYSDATETIME() AS DATE)
                  AND ngayKetThuc >= CAST(SYSDATETIME() AS DATE)
                ORDER BY ngayBatDau DESC, maKM ASC
                """;

        List<KhuyenMai> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapKhuyenMai(rs, c));
            }
            return list;
        } catch (SQLException e) {
            // Khi bảng chưa tạo hoặc DB offline, tự động fallback về Demo
            LocalDate today = LocalDate.now();
            return DEMO_LIST.stream().filter(k -> k.conHieuLuc(today)).toList();
        }
    }

    /**
     * Lấy toàn bộ chương trình khuyến mãi.
     */
    public List<KhuyenMai> layTatCa() {
        if (ConnectDB.demoMode()) {
            return new ArrayList<>(DEMO_LIST);
        }

        String sql = "SELECT maKM, maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu FROM KhuyenMai ORDER BY ngayBatDau DESC, maKM ASC";
        List<KhuyenMai> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapKhuyenMai(rs, c));
            }
            return list;
        } catch (SQLException e) {
            return new ArrayList<>(DEMO_LIST);
        }
    }

    /**
     * Tìm khuyến mãi theo mã.
     */
    public KhuyenMai timTheoMa(String maKM) {
        if (maKM == null || maKM.isBlank()) return null;
        if (ConnectDB.demoMode()) {
            return DEMO_LIST.stream().filter(k -> k.getMaKM().equalsIgnoreCase(maKM.trim())).findFirst().orElse(null);
        }

        String sql = "SELECT maKM, maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu FROM KhuyenMai WHERE maKM = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKM.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapKhuyenMai(rs, c);
            }
        } catch (SQLException e) {
            return DEMO_LIST.stream().filter(k -> k.getMaKM().equalsIgnoreCase(maKM.trim())).findFirst().orElse(null);
        }
    }

    private KhuyenMai mapKhuyenMai(ResultSet rs, Connection c) throws SQLException {
        KhuyenMai km = new KhuyenMai();
        km.setMaKM(rs.getString("maKM"));
        km.setMaNVTao(rs.getString("maNV"));
        km.setTenChuongTrinh(rs.getString("tenChuongTrinh"));
        km.setMucGiamPhanTram(rs.getDouble("mucGiamPhanTram"));

        Date bd = rs.getDate("ngayBatDau");
        if (bd != null) km.setNgayBatDau(bd.toLocalDate());

        Date kt = rs.getDate("ngayKetThuc");
        if (kt != null) km.setNgayKetThuc(kt.toLocalDate());

        km.setGhiChu(rs.getString("ghiChu"));

        // Nạp danh mục áp dụng nếu có bảng phụ
        km.setDsDichVuApDung(layDichVuApDung(c, km.getMaKM()));
        km.setDsPhuTungApDung(layPhuTungApDung(c, km.getMaKM()));

        return km;
    }

    private Set<String> layDichVuApDung(Connection c, String maKM) {
        Set<String> set = new HashSet<>();
        String sql = "SELECT maDV FROM KhuyenMai_DichVu WHERE maKM = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString("maDV"));
            }
        } catch (SQLException ignored) {
        }
        return set;
    }

    private Set<String> layPhuTungApDung(Connection c, String maKM) {
        Set<String> set = new HashSet<>();
        String sql = "SELECT maLK FROM KhuyenMai_LinhKien WHERE maKM = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString("maLK"));
            }
        } catch (SQLException ignored) {
        }
        return set;
    }
}

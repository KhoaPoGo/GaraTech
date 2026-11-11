package dao;

import connectDB.ConnectDB;
import entity.ChiTietDichVu;
import entity.ChiTietHoaDon;
import entity.ChiTietLinhKien;
import entity.GiaoDichThanhToan;
import entity.HoaDon;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object cho module Hóa đơn & Thanh toán.
 * Quản lý các bảng HoaDon, ChiTietHoaDon, GiaoDichThanhToan.
 * Hỗ trợ chế độ Demo Mode khi chưa khởi động CSDL.
 */
public class HoaDonDao {

    public record HoaDonChoThanhToanRow(
            String maPhieu,
            String bienSo,
            String thongTinXe,
            String tenKH,
            String sdtKH,
            String tenKTV,
            double tongTienDichVu,
            double tongTienLinhKien,
            double chiPhiTamTinh,
            LocalDateTime thoiGianHoanThanh
    ) {}

    public record HoaDonLichSuRow(
            String maHoaDon,
            String maPhieu,
            String bienSo,
            String thongTinXe,
            String tenKH,
            String sdtKH,
            LocalDateTime ngayLap,
            double tongTienDichVu,
            double tongTienPhuTung,
            double chiPhiKhac,
            double giamGia,
            double thueVAT,
            double tongTienThanhToan,
            String phuongThuc,
            String maGiaoDich,
            String maKM,
            String trangThai,
            String tenNVLap
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // 1. LẤY DANH SÁCH XE HOÀN TẤT CHỜ THANH TOÁN (TAB 1)
    // ─────────────────────────────────────────────────────────────────────────
    public List<HoaDonChoThanhToanRow> layDanhSachChoThanhToan(String tuKhoa) {
        if (ConnectDB.demoMode()) {
            return DemoData.layDanhSachChoThanhToan(tuKhoa);
        }

        String sql = """
                SELECT p.maPhieu, p.bienSo, x.hangXe, x.model, x.namSanXuat, x.mauXe,
                       kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                       nvkt.hoTen AS tenKTV,
                       p.thoiGianHoanThanh,
                       ISNULL((SELECT SUM(cdv.thanhTien) FROM ChiTietDichVu cdv WHERE cdv.maPhieu = p.maPhieu), 0) AS tongTienDichVu,
                       ISNULL((SELECT SUM(clk.thanhTien) FROM ChiTietLinhKien clk WHERE clk.maPhieu = p.maPhieu), 0) AS tongTienLinhKien
                FROM PhieuTiepNhanSuaChua p
                JOIN Xe x ON p.bienSo = x.bienSo
                JOIN KhachHang kh ON x.maKH = kh.maKH
                LEFT JOIN NhanVien nvkt ON p.maNVKyThuat = nvkt.maNV
                WHERE p.trangThai = 'HOAN_TAT'
                  AND NOT EXISTS (
                      SELECT 1 FROM HoaDon hd WHERE hd.maPhieu = p.maPhieu AND hd.trangThai = N'Đã thanh toán'
                  )
                ORDER BY p.thoiGianHoanThanh DESC, p.maPhieu DESC
                """;

        List<HoaDonChoThanhToanRow> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String kw = tuKhoa != null ? tuKhoa.trim().toLowerCase() : "";
            while (rs.next()) {
                String maPhieu = rs.getString("maPhieu");
                String bienSo = rs.getString("bienSo");
                String hangXe = rs.getString("hangXe");
                String model = rs.getString("model");
                int nam = rs.getInt("namSanXuat");
                String mau = rs.getString("mauXe");
                String thongTinXe = String.format("%s %s (%d - %s)", hangXe, model, nam, mau);
                String tenKH = rs.getString("tenKH");
                String sdtKH = rs.getString("sdtKH");
                String tenKTV = rs.getString("tenKTV") != null ? rs.getString("tenKTV") : "Chưa chỉ định";

                double tienDV = rs.getDouble("tongTienDichVu");
                double tienLK = rs.getDouble("tongTienLinhKien");
                double tamTinh = tienDV + tienLK;

                Timestamp tsHt = rs.getTimestamp("thoiGianHoanThanh");
                LocalDateTime thoiGianHt = tsHt != null ? tsHt.toLocalDateTime() : null;

                if (!kw.isEmpty()) {
                    boolean match = (maPhieu != null && maPhieu.toLowerCase().contains(kw))
                            || (bienSo != null && bienSo.toLowerCase().contains(kw))
                            || (tenKH != null && tenKH.toLowerCase().contains(kw))
                            || (sdtKH != null && sdtKH.toLowerCase().contains(kw));
                    if (!match) continue;
                }

                list.add(new HoaDonChoThanhToanRow(
                        maPhieu, bienSo, thongTinXe, tenKH, sdtKH, tenKTV,
                        tienDV, tienLK, tamTinh, thoiGianHt
                ));
            }
            return list;
        } catch (SQLException e) {
            if (ConnectDB.demoMode()) {
                return DemoData.layDanhSachChoThanhToan(tuKhoa);
            }
            throw new RuntimeException("Lỗi tải danh sách xe chờ thanh toán: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. LẤY LỊCH SỬ HÓA ĐƠN ĐÃ THANH TOÁN (TAB 2)
    // ─────────────────────────────────────────────────────────────────────────
    public List<HoaDonLichSuRow> layDanhSachLichSu(String tuKhoa, String locPhuongThuc) {
        if (ConnectDB.demoMode()) {
            return DemoData.layDanhSachLichSu(tuKhoa, locPhuongThuc);
        }

        String sql = """
                SELECT hd.maHoaDon, hd.maPhieu, hd.ngayLap, hd.tongTienDichVu, hd.tongTienPhuTung,
                       hd.chiPhiKhac, hd.giamGia, hd.thueVAT, hd.tongTienThanhToan, hd.trangThai,
                       x.bienSo, x.hangXe, x.model, x.namSanXuat, x.mauXe,
                       kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                       nv.hoTen AS tenNVLap,
                       hd.phuongThuc, hd.maGiaoDich
                FROM HoaDon hd
                JOIN PhieuTiepNhanSuaChua p ON hd.maPhieu = p.maPhieu
                JOIN Xe x ON p.bienSo = x.bienSo
                JOIN KhachHang kh ON x.maKH = kh.maKH
                JOIN NhanVien nv ON hd.maNV = nv.maNV
                ORDER BY hd.ngayLap DESC, hd.maHoaDon DESC
                """;

        List<HoaDonLichSuRow> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String kw = tuKhoa != null ? tuKhoa.trim().toLowerCase() : "";
            String ptLoc = locPhuongThuc != null ? locPhuongThuc.trim() : "";

            while (rs.next()) {
                String maHD = rs.getString("maHoaDon");
                String maPhieu = rs.getString("maPhieu");
                String bienSo = rs.getString("bienSo");
                String hangXe = rs.getString("hangXe");
                String model = rs.getString("model");
                int nam = rs.getInt("namSanXuat");
                String mau = rs.getString("mauXe");
                String thongTinXe = String.format("%s %s (%d - %s)", hangXe, model, nam, mau);

                String tenKH = rs.getString("tenKH");
                String sdtKH = rs.getString("sdtKH");
                String tenNV = rs.getString("tenNVLap");

                Timestamp tsLap = rs.getTimestamp("ngayLap");
                LocalDateTime ngayLap = tsLap != null ? tsLap.toLocalDateTime() : null;

                double tienDV = rs.getDouble("tongTienDichVu");
                double tienPT = rs.getDouble("tongTienPhuTung");
                double cpKhac = rs.getDouble("chiPhiKhac");
                double giamGia = rs.getDouble("giamGia");
                double vat = rs.getDouble("thueVAT");
                double tongTT = rs.getDouble("tongTienThanhToan");
                String trangThai = rs.getString("trangThai");
                String phuongThuc = rs.getString("phuongThuc") != null ? rs.getString("phuongThuc") : "Tiền mặt";
                String maGD = rs.getString("maGiaoDich") != null ? rs.getString("maGiaoDich") : "";

                if (!ptLoc.isEmpty() && !ptLoc.equals("-- Tất cả phương thức --") && !ptLoc.equalsIgnoreCase(phuongThuc)) {
                    continue;
                }

                if (!kw.isEmpty()) {
                    boolean match = (maHD != null && maHD.toLowerCase().contains(kw))
                            || (maPhieu != null && maPhieu.toLowerCase().contains(kw))
                            || (bienSo != null && bienSo.toLowerCase().contains(kw))
                            || (tenKH != null && tenKH.toLowerCase().contains(kw))
                            || (sdtKH != null && sdtKH.toLowerCase().contains(kw));
                    if (!match) continue;
                }

                list.add(new HoaDonLichSuRow(
                        maHD, maPhieu, bienSo, thongTinXe, tenKH, sdtKH, ngayLap,
                        tienDV, tienPT, cpKhac, giamGia, vat, tongTT, phuongThuc, maGD, null, trangThai, tenNV
                ));
            }
            return list;
        } catch (SQLException e) {
            if (ConnectDB.demoMode()) {
                return DemoData.layDanhSachLichSu(tuKhoa, locPhuongThuc);
            }
            throw new RuntimeException("Lỗi tải lịch sử hóa đơn: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TÌM HÓA ĐƠN THEO MÃ
    // ─────────────────────────────────────────────────────────────────────────
    public HoaDon timTheoMa(String maHoaDon) {
        if (ConnectDB.demoMode()) {
            return DemoData.timTheoMa(maHoaDon);
        }

        String sql = """
                SELECT hd.maHoaDon, hd.maPhieu, hd.maNV, hd.ngayLap, hd.tongTienDichVu, hd.tongTienPhuTung,
                       hd.chiPhiKhac, hd.giamGia, hd.thueVAT, hd.tongTienThanhToan, hd.trangThai,
                       nv.hoTen AS tenNV
                FROM HoaDon hd
                JOIN NhanVien nv ON hd.maNV = nv.maNV
                WHERE hd.maHoaDon = ?
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                HoaDon hd = new HoaDon();
                hd.setMaHoaDon(rs.getString("maHoaDon"));
                hd.setMaPhieu(rs.getString("maPhieu"));
                hd.setMaNV(rs.getString("maNV"));

                NhanVien nv = new NhanVien();
                nv.setMaNV(rs.getString("maNV"));
                nv.setHoTen(rs.getString("tenNV"));
                hd.setNhanVien(nv);

                Timestamp ts = rs.getTimestamp("ngayLap");
                if (ts != null) hd.setNgayLap(ts.toLocalDateTime());

                hd.setTongTienDichVu(rs.getDouble("tongTienDichVu"));
                hd.setTongTienPhuTung(rs.getDouble("tongTienPhuTung"));
                hd.setChiPhiKhac(rs.getDouble("chiPhiKhac"));
                hd.setGiamGia(rs.getDouble("giamGia"));
                hd.setThueVAT(rs.getDouble("thueVAT"));
                hd.setTongTienThanhToan(rs.getDouble("tongTienThanhToan"));
                hd.setTrangThai(rs.getString("trangThai"));

                // Lấy chi tiết hóa đơn
                hd.setDsChiTiet(layChiTietHoaDon(c, maHoaDon));

                // Lấy lịch sử giao dịch (nếu có để tương thích ngược)
                hd.setDsGiaoDich(layGiaoDichThanhToan(c, maHoaDon));

                // Lấy phương thức thanh toán, mã giao dịch, khuyến mãi
                String phuongThuc = "Tiền mặt";
                String maGD = null;
                String maKM = null;
                try {
                    phuongThuc = rs.getString("phuongThuc");
                    maGD = rs.getString("maGiaoDich");
                    maKM = rs.getString("maKM");
                } catch (Exception ignored) {}

                if ((phuongThuc == null || phuongThuc.isBlank()) && !hd.getDsGiaoDich().isEmpty()) {
                    phuongThuc = hd.getDsGiaoDich().get(0).getPhuongThuc();
                    maGD = hd.getDsGiaoDich().get(0).getMaGiaoDich();
                }

                hd.setPhuongThucTT(phuongThuc != null ? phuongThuc : "Tiền mặt");
                hd.setMaGiaoDich(maGD != null ? maGD : "");
                hd.setThoiGianThanhToan(hd.getNgayLap());
                hd.setMaKM(maKM);
                if (maKM != null && !maKM.isBlank()) {
                    KhuyenMaiDao kmDao = new KhuyenMaiDao();
                    hd.setKhuyenMai(kmDao.timTheoMa(maKM));
                }

                // Lấy thông tin phiếu sửa chữa
                PhieuTiepNhanSuaChuaDao pDao = new PhieuTiepNhanSuaChuaDao();
                hd.setPhieuSuaChua(pDao.findById(hd.getMaPhieu()));

                return hd;
            }
        } catch (SQLException e) {
            if (ConnectDB.demoMode()) {
                return DemoData.timTheoMa(maHoaDon);
            }
            throw new RuntimeException("Lỗi tìm hóa đơn theo mã: " + e.getMessage(), e);
        }
    }

    public HoaDon timTheoPhieu(String maPhieu) {
        if (ConnectDB.demoMode()) {
            return DemoData.timTheoPhieu(maPhieu);
        }

        String sql = "SELECT maHoaDon FROM HoaDon WHERE maPhieu = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return timTheoMa(rs.getString("maHoaDon"));
            }
        } catch (SQLException e) {
            if (ConnectDB.demoMode()) {
                return DemoData.timTheoPhieu(maPhieu);
            }
            throw new RuntimeException("Lỗi tìm hóa đơn theo phiếu: " + e.getMessage(), e);
        }
    }

    private List<ChiTietHoaDon> layChiTietHoaDon(Connection c, String maHoaDon) throws SQLException {
        String sql = """
                SELECT maChiTietHD, maHoaDon, loaiChiTiet, maDichVu, maPhuTung, tenChiTiet,
                       soLuong, donGia, giamGia, thanhTien
                FROM ChiTietHoaDon
                WHERE maHoaDon = ?
                ORDER BY maChiTietHD
                """;
        List<ChiTietHoaDon> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietHoaDon ct = new ChiTietHoaDon();
                    ct.setMaChiTietHD(rs.getString("maChiTietHD"));
                    ct.setMaHoaDon(rs.getString("maHoaDon"));
                    ct.setLoaiChiTiet(rs.getString("loaiChiTiet"));
                    ct.setMaDichVu(rs.getString("maDichVu"));
                    ct.setMaPhuTung(rs.getString("maPhuTung"));
                    ct.setTenChiTiet(rs.getString("tenChiTiet"));
                    ct.setSoLuong(rs.getInt("soLuong"));
                    ct.setDonGia(rs.getDouble("donGia"));
                    ct.setGiamGia(rs.getDouble("giamGia"));
                    ct.setThanhTien(rs.getDouble("thanhTien"));
                    list.add(ct);
                }
            }
        }
        return list;
    }

    private List<GiaoDichThanhToan> layGiaoDichThanhToan(Connection c, String maHoaDon) throws SQLException {
        String sql = """
                SELECT maGiaoDich, maHoaDon, maNV, ngayThanhToan, soTien, phuongThuc, trangThai
                FROM GiaoDichThanhToan
                WHERE maHoaDon = ?
                ORDER BY ngayThanhToan DESC
                """;
        List<GiaoDichThanhToan> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GiaoDichThanhToan gd = new GiaoDichThanhToan();
                    gd.setMaGiaoDich(rs.getString("maGiaoDich"));
                    gd.setMaHoaDon(rs.getString("maHoaDon"));
                    gd.setMaNV(rs.getString("maNV"));
                    Timestamp ts = rs.getTimestamp("ngayThanhToan");
                    if (ts != null) gd.setNgayThanhToan(ts.toLocalDateTime());
                    gd.setSoTien(rs.getDouble("soTien"));
                    gd.setPhuongThuc(rs.getString("phuongThuc"));
                    gd.setTrangThai(rs.getString("trangThai"));
                    list.add(gd);
                }
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. LẬP HÓA ĐƠN & THANH TOÁN (TRANSACTION TOÀN VẸN)
    // ─────────────────────────────────────────────────────────────────────────
    public String lapHoaDonVaThanhToan(
            String maPhieu,
            String maNV,
            double chiPhiKhac,
            double giamGia,
            double thueVAT,
            String phuongThuc,
            String maGiaoDichNganHang
    ) throws SQLException {
        return lapHoaDonVaThanhToan(maPhieu, maNV, null, chiPhiKhac, giamGia, thueVAT, phuongThuc, maGiaoDichNganHang);
    }

    public String lapHoaDonVaThanhToan(
            String maPhieu,
            String maNV,
            String maKM,
            double chiPhiKhac,
            double giamGia,
            double thueVAT,
            String phuongThuc,
            String maGiaoDichNganHang
    ) throws SQLException {
        if (ConnectDB.demoMode()) {
            return DemoData.lapHoaDonVaThanhToan(maPhieu, maNV, maKM, chiPhiKhac, giamGia, thueVAT, phuongThuc, maGiaoDichNganHang);
        }

        return ConnectDB.inTransaction(c -> {
            // Kiểm tra phiếu đã hoàn tất chưa
            String checkSql = "SELECT trangThai FROM PhieuTiepNhanSuaChua WHERE maPhieu = ?";
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setString(1, maPhieu);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy phiếu " + maPhieu);
                    String tt = rs.getString("trangThai");
                    if (!"HOAN_TAT".equalsIgnoreCase(tt)) {
                        throw new IllegalStateException("Phiếu chưa hoàn tất kỹ thuật, không thể lập hóa đơn!");
                    }
                }
            }

            // Kiểm tra phiếu đã có hóa đơn đã thanh toán chưa
            String checkHdSql = "SELECT maHoaDon, trangThai FROM HoaDon WHERE maPhieu = ?";
            try (PreparedStatement ps = c.prepareStatement(checkHdSql)) {
                ps.setString(1, maPhieu);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String ttHd = rs.getString("trangThai");
                        if ("Đã thanh toán".equalsIgnoreCase(ttHd)) {
                            throw new IllegalStateException("Phiếu này đã được lập hóa đơn và thanh toán trước đó!");
                        }
                    }
                }
            }

            // Lấy chi tiết dịch vụ và linh kiện của phiếu
            PhieuTiepNhanSuaChuaDao pDao = new PhieuTiepNhanSuaChuaDao();
            List<ChiTietDichVu> dsDv = pDao.layChiTietDichVu(maPhieu);
            List<ChiTietLinhKien> dsLk = pDao.layChiTietLinhKien(maPhieu);

            double tongTienDv = 0;
            for (ChiTietDichVu dv : dsDv) {
                tongTienDv += dv.getSoLuong() * dv.getDonGiaThucHien();
            }

            double tongTienLk = 0;
            for (ChiTietLinhKien lk : dsLk) {
                tongTienLk += lk.getSoLuong() * lk.getDonGiaXuat();
            }

            double tongTruocThue = tongTienDv + tongTienLk + chiPhiKhac - giamGia;
            if (tongTruocThue < 0) tongTruocThue = 0;
            double tienThue = Math.round(tongTruocThue * (thueVAT / 100.0));
            double tongThanhToan = tongTruocThue + tienThue;

            // 1. Sinh mã hóa đơn mới
            String maHD = IdGenerator.next(c, IdGenerator.Kind.HOA_DON);

            // 2. Insert HoaDon
            String insertHdSql = """
                    INSERT INTO HoaDon (
                        maHoaDon, maPhieu, maNV, maKM, ngayLap,
                        tongTienDichVu, tongTienPhuTung, chiPhiKhac, giamGia, thueVAT,
                        tongTienThanhToan, phuongThuc, maGiaoDich, trangThai
                    ) VALUES (?, ?, ?, ?, SYSDATETIME(), ?, ?, ?, ?, ?, ?, ?, ?, N'Đã thanh toán')
                    """;
            try (PreparedStatement ps = c.prepareStatement(insertHdSql)) {
                ps.setString(1, maHD);
                ps.setString(2, maPhieu);
                ps.setString(3, maNV);
                if (maKM != null && !maKM.isBlank()) {
                    ps.setString(4, maKM);
                } else {
                    ps.setNull(4, Types.VARCHAR);
                }
                ps.setDouble(5, tongTienDv);
                ps.setDouble(6, tongTienLk);
                ps.setDouble(7, chiPhiKhac);
                ps.setDouble(8, giamGia);
                ps.setDouble(9, thueVAT);
                ps.setDouble(10, tongThanhToan);
                ps.setString(11, phuongThuc);
                ps.setString(12, maGiaoDichNganHang);
                ps.executeUpdate();
            } catch (SQLException exCol) {
                // Fallback nếu CSDL cũ chưa có cột maKM, phuongThuc, maGiaoDich
                String insertHdSqlOld = """
                        INSERT INTO HoaDon (
                            maHoaDon, maPhieu, maNV, ngayLap,
                            tongTienDichVu, tongTienPhuTung, chiPhiKhac, giamGia, thueVAT,
                            tongTienThanhToan, trangThai
                        ) VALUES (?, ?, ?, SYSDATETIME(), ?, ?, ?, ?, ?, ?, N'Đã thanh toán')
                        """;
                try (PreparedStatement ps = c.prepareStatement(insertHdSqlOld)) {
                    ps.setString(1, maHD);
                    ps.setString(2, maPhieu);
                    ps.setString(3, maNV);
                    ps.setDouble(4, tongTienDv);
                    ps.setDouble(5, tongTienLk);
                    ps.setDouble(6, chiPhiKhac);
                    ps.setDouble(7, giamGia);
                    ps.setDouble(8, thueVAT);
                    ps.setDouble(9, tongThanhToan);
                    ps.executeUpdate();
                }
            }

            // 3. Insert ChiTietHoaDon
            String insertCtSql = """
                    INSERT INTO ChiTietHoaDon (
                        maChiTietHD, maHoaDon, loaiChiTiet, maDichVu, maPhuTung,
                        tenChiTiet, soLuong, donGia, giamGia
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = c.prepareStatement(insertCtSql)) {
                for (ChiTietDichVu dv : dsDv) {
                    String maCt = IdGenerator.next(c, IdGenerator.Kind.CHI_TIET_HOA_DON);
                    ps.setString(1, maCt);
                    ps.setString(2, maHD);
                    ps.setString(3, "DichVu");
                    ps.setString(4, dv.getDichVu().getMaDichVu());
                    ps.setNull(5, Types.VARCHAR);
                    ps.setString(6, dv.getDichVu().getTenDichVu());
                    ps.setInt(7, dv.getSoLuong());
                    ps.setDouble(8, dv.getDonGiaThucHien());
                    ps.setDouble(9, 0);
                    ps.executeUpdate();
                }

                for (ChiTietLinhKien lk : dsLk) {
                    String maCt = IdGenerator.next(c, IdGenerator.Kind.CHI_TIET_HOA_DON);
                    ps.setString(1, maCt);
                    ps.setString(2, maHD);
                    ps.setString(3, "PhuTung");
                    ps.setNull(4, Types.VARCHAR);
                    ps.setString(5, lk.getPhuTung().getMaPhuTung());
                    ps.setString(6, lk.getPhuTung().getTenPhuTung());
                    ps.setInt(7, lk.getSoLuong());
                    ps.setDouble(8, lk.getDonGiaXuat());
                    ps.setDouble(9, 0);
                    ps.executeUpdate();
                }
            }

            return maHD;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. DEMO DATA FALLBACK KHI CHƯA NỐI CSDL
    // ─────────────────────────────────────────────────────────────────────────
    private static final class DemoData {
        private static final List<HoaDonChoThanhToanRow> DS_CHO_TT = new ArrayList<>();
        private static final List<HoaDonLichSuRow> DS_LICHSU = new ArrayList<>();
        private static int counterHD = 2;
        private static int counterGD = 2;

        static {
            initMockData();
        }

        private static synchronized void initMockData() {
            DS_CHO_TT.clear();
            DS_LICHSU.clear();

            LocalDateTime now = LocalDateTime.now();

            // Xe hoàn tất chờ thanh toán
            DS_CHO_TT.add(new HoaDonChoThanhToanRow(
                    "PSC-20261110-001", "59C-678.90", "Honda City (2021 - Đen)",
                    "Nguyễn Thị Mai", "0908765432", "Lê Minh Cường",
                    1500000, 1200000, 2700000, now.minusHours(1)
            ));
            DS_CHO_TT.add(new HoaDonChoThanhToanRow(
                    "PSC-20261110-002", "51H-999.88", "Mazda CX-5 (2022 - Đỏ)",
                    "Trần Quốc Bảo", "0987112233", "Lê Minh Cường",
                    850000, 2400000, 3250000, now.minusHours(3)
            ));
            DS_CHO_TT.add(new HoaDonChoThanhToanRow(
                    "PSC-20261110-003", "51A-123.45", "Toyota Vios (2020 - Trắng)",
                    "Võ Văn Em", "0912345678", "Lê Minh Cường",
                    500000, 600000, 1100000, now.minusHours(5)
            ));

            // Lịch sử hóa đơn đã thanh toán
            DS_LICHSU.add(new HoaDonLichSuRow(
                    "HD20261108-0001", "PSC-20261108-001", "51A-123.45", "Toyota Vios (2020 - Trắng)",
                    "Võ Văn Em", "0912345678", now.minusDays(2).withHour(11).withMinute(15),
                    400000, 640000, 0, 0, 10.0, 1144000, "Chuyển khoản", "VCB98234182", "KM001", "Đã thanh toán", "Trần Thị Bình"
            ));
        }

        static synchronized List<HoaDonChoThanhToanRow> layDanhSachChoThanhToan(String tuKhoa) {
            if (tuKhoa == null || tuKhoa.isBlank()) return new ArrayList<>(DS_CHO_TT);
            String kw = tuKhoa.trim().toLowerCase();
            return DS_CHO_TT.stream().filter(r ->
                    r.maPhieu().toLowerCase().contains(kw)
                            || r.bienSo().toLowerCase().contains(kw)
                            || r.tenKH().toLowerCase().contains(kw)
                            || r.sdtKH().toLowerCase().contains(kw)
            ).toList();
        }

        static synchronized List<HoaDonLichSuRow> layDanhSachLichSu(String tuKhoa, String locPhuongThuc) {
            String kw = tuKhoa != null ? tuKhoa.trim().toLowerCase() : "";
            String pt = locPhuongThuc != null ? locPhuongThuc.trim() : "";
            return DS_LICHSU.stream().filter(r -> {
                if (!pt.isEmpty() && !pt.equals("-- Tất cả phương thức --") && !pt.equalsIgnoreCase(r.phuongThuc())) {
                    return false;
                }
                if (!kw.isEmpty()) {
                    return r.maHoaDon().toLowerCase().contains(kw)
                            || r.maPhieu().toLowerCase().contains(kw)
                            || r.bienSo().toLowerCase().contains(kw)
                            || r.tenKH().toLowerCase().contains(kw)
                            || r.sdtKH().toLowerCase().contains(kw);
                }
                return true;
            }).toList();
        }

        static synchronized HoaDon timTheoMa(String maHoaDon) {
            HoaDonLichSuRow row = DS_LICHSU.stream().filter(r -> r.maHoaDon().equalsIgnoreCase(maHoaDon)).findFirst().orElse(null);
            if (row == null) return null;

            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(row.maHoaDon());
            hd.setMaPhieu(row.maPhieu());
            hd.setNgayLap(row.ngayLap());
            hd.setTongTienDichVu(row.tongTienDichVu());
            hd.setTongTienPhuTung(row.tongTienPhuTung());
            hd.setChiPhiKhac(row.chiPhiKhac());
            hd.setGiamGia(row.giamGia());
            hd.setThueVAT(row.thueVAT());
            hd.setTongTienThanhToan(row.tongTienThanhToan());
            hd.setTrangThai(row.trangThai());
            hd.setPhuongThucTT(row.phuongThuc());
            hd.setMaGiaoDich(row.maGiaoDich());
            hd.setThoiGianThanhToan(row.ngayLap());
            hd.setMaKM(row.maKM());
            if (row.maKM() != null && !row.maKM().isBlank()) {
                KhuyenMaiDao kmDao = new KhuyenMaiDao();
                hd.setKhuyenMai(kmDao.timTheoMa(row.maKM()));
            }

            NhanVien nv = new NhanVien();
            nv.setMaNV("NV26002");
            nv.setHoTen(row.tenNVLap());
            hd.setNhanVien(nv);

            // Tạo phiếu và xe cho mockup
            PhieuTiepNhanSuaChua p = new PhieuTiepNhanSuaChua();
            p.setMaPhieuRaw(row.maPhieu());
            Xe x = new Xe();
            x.setBienSo(row.bienSo());
            KhachHang kh = new KhachHang();
            kh.setHoTen(row.tenKH());
            kh.setSoDienThoai(row.sdtKH());
            x.setKhachHang(kh);
            p.setXeRaw(x);
            hd.setPhieuSuaChua(p);

            // Chi tiết mẫu
            List<ChiTietHoaDon> dsCt = new ArrayList<>();
            ChiTietHoaDon ct1 = new ChiTietHoaDon("CTHD00001", "DichVu", "Bảo dưỡng & Thay dầu động cơ", 1, row.tongTienDichVu(), 0, row.tongTienDichVu());
            ct1.setDonViTinh("Gói");
            dsCt.add(ct1);

            if (row.tongTienPhuTung() > 0) {
                ChiTietHoaDon ct2 = new ChiTietHoaDon("CTHD00002", "PhuTung", "Dầu nhớt động cơ & Phụ tùng thay thế", 1, row.tongTienPhuTung(), 0, row.tongTienPhuTung());
                ct2.setDonViTinh("Bộ");
                dsCt.add(ct2);
            }
            hd.setDsChiTiet(dsCt);

            // Giao dịch thanh toán mẫu
            List<GiaoDichThanhToan> dsGd = new ArrayList<>();
            GiaoDichThanhToan gd = new GiaoDichThanhToan("GD-20261108-001", row.maHoaDon(), "NV26002", row.ngayLap(), row.tongTienThanhToan(), row.phuongThuc(), "Đã thanh toán");
            dsGd.add(gd);
            hd.setDsGiaoDich(dsGd);

            return hd;
        }

        static synchronized HoaDon timTheoPhieu(String maPhieu) {
            HoaDonLichSuRow row = DS_LICHSU.stream().filter(r -> r.maPhieu().equalsIgnoreCase(maPhieu)).findFirst().orElse(null);
            if (row == null) return null;
            return timTheoMa(row.maHoaDon());
        }

        static synchronized String lapHoaDonVaThanhToan(
                String maPhieu,
                String maNV,
                String maKM,
                double chiPhiKhac,
                double giamGia,
                double thueVAT,
                String phuongThuc,
                String maGiaoDichNganHang
        ) {
            HoaDonChoThanhToanRow cho = DS_CHO_TT.stream().filter(r -> r.maPhieu().equalsIgnoreCase(maPhieu)).findFirst().orElse(null);
            if (cho == null) {
                throw new IllegalArgumentException("Không tìm thấy xe chờ thanh toán mã: " + maPhieu);
            }

            double tongTruocThue = cho.tongTienDichVu() + cho.tongTienLinhKien() + chiPhiKhac - giamGia;
            if (tongTruocThue < 0) tongTruocThue = 0;
            double tienThue = Math.round(tongTruocThue * (thueVAT / 100.0));
            double tongThanhToan = tongTruocThue + tienThue;

            String maHD = String.format("HD20261111-%04d", counterHD++);
            String maGD = String.format("GD-20261111-%03d", counterGD++);

            // Xóa khỏi danh sách chờ
            DS_CHO_TT.remove(cho);

            // Thêm vào danh sách lịch sử
            DS_LICHSU.add(0, new HoaDonLichSuRow(
                    maHD, maPhieu, cho.bienSo(), cho.thongTinXe(), cho.tenKH(), cho.sdtKH(),
                    LocalDateTime.now(), cho.tongTienDichVu(), cho.tongTienLinhKien(),
                    chiPhiKhac, giamGia, thueVAT, tongThanhToan,
                    phuongThuc, (maGiaoDichNganHang != null && !maGiaoDichNganHang.isBlank()) ? maGiaoDichNganHang : maGD,
                    maKM,
                    "Đã thanh toán", "Trần Thị Bình"
            ));

            return maHD;
        }
    }
}

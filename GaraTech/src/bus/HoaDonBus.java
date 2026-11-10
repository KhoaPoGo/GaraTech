package bus;

import dao.HoaDonDao;
import dao.HoaDonDao.HoaDonChoThanhToanRow;
import dao.HoaDonDao.HoaDonLichSuRow;
import dao.KhuyenMaiDao;
import dao.PhieuTiepNhanSuaChuaDao;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.HoaDon;
import entity.KhuyenMai;
import entity.PhieuTiepNhanSuaChua;
import util.Session;

import java.sql.SQLException;
import java.util.List;

/**
 * Nghiệp vụ Lập hóa đơn & Quản lý thanh toán.
 * Tiếp nhận viên hoặc Quản lý thực hiện thu tiền và xuất hóa đơn dịch vụ.
 */
public class HoaDonBus {

    private final HoaDonDao hoaDonDao = new HoaDonDao();
    private final PhieuTiepNhanSuaChuaDao phieuDao = new PhieuTiepNhanSuaChuaDao();
    private final KhuyenMaiDao khuyenMaiDao = new KhuyenMaiDao();

    public List<HoaDonChoThanhToanRow> layDanhSachChoThanhToan(String tuKhoa) {
        return hoaDonDao.layDanhSachChoThanhToan(tuKhoa);
    }

    public List<HoaDonLichSuRow> layDanhSachLichSu(String tuKhoa, String locPhuongThuc) {
        return hoaDonDao.layDanhSachLichSu(tuKhoa, locPhuongThuc);
    }

    public List<KhuyenMai> layDanhSachKhuyenMaiHieuLuc() {
        return khuyenMaiDao.layDanhSachHieuLuc();
    }

    public KhuyenMai timKhuyenMaiTheoMa(String maKM) {
        if (maKM == null || maKM.isBlank()) return null;
        return khuyenMaiDao.timTheoMa(maKM.trim());
    }

    public PhieuTiepNhanSuaChua layPhieuTheoMa(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return null;
        try {
            return phieuDao.findById(maPhieu.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm phiếu sửa chữa: " + e.getMessage(), e);
        }
    }

    public List<ChiTietDichVu> layChiTietDichVu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        try {
            return phieuDao.layChiTietDichVu(maPhieu.trim());
        } catch (SQLException e) {
            return List.of();
        }
    }

    public List<ChiTietLinhKien> layChiTietLinhKien(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        try {
            return phieuDao.layChiTietLinhKien(maPhieu.trim());
        } catch (SQLException e) {
            return List.of();
        }
    }

    public HoaDon timHoaDonTheoMa(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.isBlank()) return null;
        return hoaDonDao.timTheoMa(maHoaDon.trim());
    }

    public HoaDon timHoaDonTheoPhieu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return null;
        return hoaDonDao.timTheoPhieu(maPhieu.trim());
    }

    /**
     * Xác nhận lập hóa đơn và thanh toán cho phiếu sửa chữa (không có khuyến mãi).
     */
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

    /**
     * Xác nhận lập hóa đơn và thanh toán cho phiếu sửa chữa (có kèm khuyến mãi).
     */
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
        if (maPhieu == null || maPhieu.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phiếu sửa chữa cần thanh toán.");
        }
        if (phuongThuc == null || phuongThuc.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        }

        // Chỉ hỗ trợ 2 phương thức: "Tiền mặt" và "Chuyển khoản"
        if (!"Tiền mặt".equalsIgnoreCase(phuongThuc.trim()) && !"Chuyển khoản".equalsIgnoreCase(phuongThuc.trim())) {
            throw new IllegalArgumentException("Chỉ chấp nhận 2 phương thức thanh toán: 'Tiền mặt' hoặc 'Chuyển khoản'.");
        }

        if ("Chuyển khoản".equalsIgnoreCase(phuongThuc.trim())
                && (maGiaoDichNganHang == null || maGiaoDichNganHang.isBlank())) {
            throw new IllegalArgumentException("Phương thức Chuyển khoản bắt buộc phải nhập mã giao dịch ngân hàng.");
        }
        if (giamGia < 0) {
            throw new IllegalArgumentException("Số tiền giảm giá không được âm.");
        }
        if (chiPhiKhac < 0) {
            throw new IllegalArgumentException("Chi phí khác không được âm.");
        }
        if (thueVAT < 0 || thueVAT > 100) {
            throw new IllegalArgumentException("Thuế VAT phải từ 0% đến 100%.");
        }

        String nvLap = maNV;
        if (nvLap == null || nvLap.isBlank()) {
            if (Session.user() != null) {
                nvLap = Session.user().maNV();
            } else {
                nvLap = "NV26002";
            }
        }

        return hoaDonDao.lapHoaDonVaThanhToan(
                maPhieu.trim(),
                nvLap,
                maKM != null ? maKM.trim() : null,
                chiPhiKhac,
                giamGia,
                thueVAT,
                phuongThuc.trim(),
                maGiaoDichNganHang != null ? maGiaoDichNganHang.trim() : null
        );
    }
}

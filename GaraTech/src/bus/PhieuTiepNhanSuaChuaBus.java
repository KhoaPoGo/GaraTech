package bus;

import dao.DichVuDao;
import dao.KhachHangDao;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.PhieuTiepNhanSuaChuaDao.PhieuTiepNhanSuaChuaRow;
import dao.PhuTungDao;
import dao.XeDao;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.DichVu;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuTiepNhanSuaChua;
import entity.PhuTung;
import entity.Xe;
import enums.TrangThaiPhieu;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Tầng Service/BUS điều phối toàn bộ nghiệp vụ Phiếu Tiếp Nhận - Sửa Chữa (PhieuTiepNhanSuaChua).
 * Thực hiện kiểm tra ràng buộc nghiệp vụ, bảo vệ tính toàn vẹn và chống gian lận.
 */
public class PhieuTiepNhanSuaChuaBus {

    private final PhieuTiepNhanSuaChuaDao phieuDao = new PhieuTiepNhanSuaChuaDao();
    private final DichVuDao dichVuDao = new DichVuDao();
    private final PhuTungDao phuTungDao = new PhuTungDao();

    public record TiepNhanInput(
            String maKH,
            String bienSo,
            String maNVTiepNhan,
            String maNVKyThuat,
            float soKmTiepNhan,
            float mucNhienLieu,
            String yeuCauBanDau,
            String tinhTrangXe,
            LocalDateTime thoiGianTiepNhan,
            LocalDateTime thoiGianHenTra
    ) {}

    public record ChiPhiSummary(
            double tienDichVu,
            double tienLinhKien,
            double tongCong
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // 1. TẠO PHIẾU TIẾP NHẬN BAN ĐẦU (Chỉ định KTV, trạng thái CHO_SUA)
    // ─────────────────────────────────────────────────────────────────────────
    public String taoPhieuTiepNhan(TiepNhanInput in, List<ChiTietDichVu> dsDichVuDuKien) throws Exception {
        validateTiepNhanInput(in);

        // 1. Kiểm tra Khách hàng
        KhachHang kh = KhachHangDao.findById(in.maKH());
        if (kh == null) {
            throw new BusinessException("Khách hàng không tồn tại trong hệ thống.");
        }

        // 2. Kiểm tra Xe và quyền sở hữu
        Xe xe = XeDao.findByPlate(in.bienSo());
        if (xe == null) {
            throw new BusinessException("Xe mang biển số " + in.bienSo() + " không tồn tại.");
        }
        if (xe.getKhachHang() == null || !xe.getKhachHang().getMaKH().equals(kh.getMaKH())) {
            throw new BusinessException("Xe " + in.bienSo() + " không thuộc sở hữu của khách hàng " + kh.getHoTen() + ".");
        }

        // 3. Kiểm tra số ODO
        if (in.soKmTiepNhan() < xe.getSoKmHienTai()) {
            throw new BusinessException(String.format(
                    "Số km tiếp nhận (%,.0f km) không được nhỏ hơn số km hiện tại của xe (%,.0f km).",
                    in.soKmTiepNhan(), xe.getSoKmHienTai()));
        }

        // 4. Kiểm tra KTV chính không trùng TNV
        if (in.maNVKyThuat() != null && !in.maNVKyThuat().isBlank() && in.maNVKyThuat().equals(in.maNVTiepNhan())) {
            throw new BusinessException("Kỹ thuật viên chính phụ trách phải khác với nhân viên tiếp nhận.");
        }

        PhieuTiepNhanSuaChua phieu = new PhieuTiepNhanSuaChua();
        phieu.setXeRaw(xe);

        NhanVien tnv = new NhanVien();
        tnv.setMaNV(in.maNVTiepNhan());
        phieu.setNhanVienTiepNhanRaw(tnv);

        if (in.maNVKyThuat() != null && !in.maNVKyThuat().isBlank()) {
            NhanVien ktv = new NhanVien();
            ktv.setMaNV(in.maNVKyThuat().trim());
            phieu.setNhanVienKyThuatRaw(ktv);
        }

        phieu.setNgayGioTiepNhan(in.thoiGianTiepNhan() != null ? in.thoiGianTiepNhan() : LocalDateTime.now());
        phieu.setThoiGianHenTra(in.thoiGianHenTra());
        phieu.setYeuCauBanDau(in.yeuCauBanDau().trim());
        phieu.setTinhTrangXe(in.tinhTrangXe() != null ? in.tinhTrangXe().trim() : "");
        phieu.setSoKmTiepNhan(in.soKmTiepNhan());
        phieu.setMucNhienLieu(in.mucNhienLieu());
        phieu.setTrangThai(TrangThaiPhieu.CHO_SUA);

        if (dsDichVuDuKien != null) {
            phieu.setChiTietDichVu(dsDichVuDuKien);
        }

        return phieuDao.insert(phieu);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CHUYỂN TRẠNG THÁI PHIẾU (Kiểm tra điều kiện & chống gian lận)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kỹ thuật viên bắt đầu sửa chữa xe được phân công: CHO_SUA -> DANG_SUA.
     */
    public void batDauSuaChua(String maPhieu, String maKTV) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() != TrangThaiPhieu.CHO_SUA && phieu.getTrangThai() != TrangThaiPhieu.CHO_LINH_KIEN) {
            throw new BusinessException("Chỉ phiếu ở trạng thái 'Chờ sửa' hoặc 'Chờ linh kiện' mới có thể chuyển sang 'Đang sửa'.");
        }

        // Nếu phiếu chưa có KTV chính thì gán KTV đang thực hiện
        String ktvThucHien = (phieu.getNhanVienKyThuat() != null && phieu.getNhanVienKyThuat().getMaNV() != null)
                ? phieu.getNhanVienKyThuat().getMaNV()
                : maKTV;

        if (ktvThucHien == null || ktvThucHien.isBlank()) {
            throw new BusinessException("Vui lòng chỉ định Kỹ thuật viên phụ trách trước khi bắt đầu sửa.");
        }

        phieuDao.updateTrangThai(maPhieu, TrangThaiPhieu.DANG_SUA, ktvThucHien);
    }

    /**
     * Tạm dừng chờ linh kiện: DANG_SUA -> CHO_LINH_KIEN.
     */
    public void choLinhKien(String maPhieu) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() != TrangThaiPhieu.DANG_SUA) {
            throw new BusinessException("Chỉ phiếu đang ở trạng thái 'Đang sửa' mới có thể tạm dừng chuyển sang 'Chờ linh kiện'.");
        }

        phieuDao.updateTrangThai(maPhieu, TrangThaiPhieu.CHO_LINH_KIEN, null);
    }

    /**
     * Kỹ thuật viên hoàn tất sửa chữa và nghiệm thu xe: DANG_SUA -> HOAN_TAT.
     * Phiếu sẽ bị KHÓA hoàn toàn (Read-only).
     */
    public void hoanTatSuaChua(String maPhieu) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() != TrangThaiPhieu.DANG_SUA) {
            throw new BusinessException("Chỉ phiếu đang ở trạng thái 'Đang sửa' mới có thể báo 'Hoàn tất'.");
        }

        // Chống gian lận: Kiểm tra phiếu phải có ít nhất 1 dịch vụ hoặc linh kiện
        boolean coDichVu = phieu.getChiTietDichVu() != null && !phieu.getChiTietDichVu().isEmpty();
        boolean coLinhKien = phieu.getChiTietLinhKien() != null && !phieu.getChiTietLinhKien().isEmpty();
        if (!coDichVu && !coLinhKien) {
            throw new BusinessException("Phiếu sửa chữa chưa có bất kỳ Dịch vụ hay Linh kiện nào. Vui lòng thêm hạng mục thực hiện trước khi Hoàn tất.");
        }

        phieuDao.updateTrangThai(maPhieu, TrangThaiPhieu.HOAN_TAT, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. THÊM / XÓA CHI TIẾT DỊCH VỤ VÀ LINH KIỆN
    // ─────────────────────────────────────────────────────────────────────────

    public void themDichVu(String maPhieu, String maDichVu, int soLuong, String ghiChu) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() == TrangThaiPhieu.HOAN_TAT) {
            throw new BusinessException("Phiếu đã HOÀN TẤT - Bị khóa toàn diện, không thể thêm dịch vụ.");
        }
        if (soLuong <= 0) {
            throw new BusinessException("Số lượng dịch vụ phải lớn hơn 0.");
        }

        DichVu dv = dichVuDao.findById(maDichVu);
        if (dv == null) {
            throw new BusinessException("Dịch vụ không tồn tại trong hệ thống.");
        }

        phieuDao.themChiTietDichVu(maPhieu, maDichVu, soLuong, dv.getTienCong(), ghiChu);
    }

    public void xoaDichVu(String maPhieu, String maDichVu) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() == TrangThaiPhieu.HOAN_TAT) {
            throw new BusinessException("Phiếu đã HOÀN TẤT - Bị khóa toàn diện, không thể xóa dịch vụ.");
        }

        phieuDao.xoaChiTietDichVu(maPhieu, maDichVu);
    }

    public void themLinhKien(String maPhieu, String maPhuTung, int soLuong, String ghiChu) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() == TrangThaiPhieu.HOAN_TAT) {
            throw new BusinessException("Phiếu đã HOÀN TẤT - Bị khóa toàn diện, không thể thêm linh kiện.");
        }
        if (soLuong <= 0) {
            throw new BusinessException("Số lượng linh kiện phải lớn hơn 0.");
        }

        PhuTung pt = phuTungDao.findById(maPhuTung);
        if (pt == null) {
            throw new BusinessException("Phụ tùng không tồn tại trong hệ thống.");
        }
        if (pt.getSoLuongTon() < soLuong) {
            throw new BusinessException(String.format(
                    "Tồn kho phụ tùng '%s' không đủ (Hiện còn: %d %s, Yêu cầu: %d). Vui lòng chuyển trạng thái sang 'Chờ linh kiện'.",
                    pt.getTenPhuTung(), pt.getSoLuongTon(), pt.getDonViTinh(), soLuong));
        }

        phieuDao.themChiTietLinhKien(maPhieu, maPhuTung, soLuong, pt.getDonGiaBan(), ghiChu);
    }

    public void xoaLinhKien(String maPhieu, String maPhuTung) throws Exception {
        PhieuTiepNhanSuaChua phieu = layThongTinPhieu(maPhieu);
        if (phieu == null) throw new BusinessException("Không tìm thấy phiếu " + maPhieu);

        if (phieu.getTrangThai() == TrangThaiPhieu.HOAN_TAT) {
            throw new BusinessException("Phiếu đã HOÀN TẤT - Bị khóa toàn diện, không thể xóa linh kiện.");
        }

        phieuDao.xoaChiTietLinhKien(maPhieu, maPhuTung);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TRUY VẤN DỮ LIỆU
    // ─────────────────────────────────────────────────────────────────────────

    public PhieuTiepNhanSuaChua layThongTinPhieu(String maPhieu) throws SQLException {
        if (maPhieu == null || maPhieu.isBlank()) return null;
        return phieuDao.findById(maPhieu.trim());
    }

    public List<PhieuTiepNhanSuaChuaRow> layDanhSachChoTiepNhanVien(String tuKhoa, TrangThaiPhieu trangThai,
                                                                   LocalDate tuNgay, LocalDate denNgay) {
        try {
            return phieuDao.layDanhSachChoTiepNhanVien(tuKhoa, trangThai, tuNgay, denNgay);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<PhieuTiepNhanSuaChuaRow> layDanhSachTheoKTV(String maNVKyThuat, String tuKhoa, TrangThaiPhieu trangThai,
                                                           LocalDate tuNgay, LocalDate denNgay) {
        try {
            return phieuDao.layDanhSachTheoKTV(maNVKyThuat, tuKhoa, trangThai, tuNgay, denNgay);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<PhieuTiepNhanSuaChuaRow> layDanhSachDuDieuKienLapHoaDon() {
        try {
            return phieuDao.layDanhSachDuDieuKienLapHoaDon();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static ChiPhiSummary tinhChiPhi(PhieuTiepNhanSuaChua phieu) {
        if (phieu == null) return new ChiPhiSummary(0, 0, 0);
        double tDv = phieu.tinhTongTienDichVu();
        double tLk = phieu.tinhTongTienLinhKien();
        return new ChiPhiSummary(tDv, tLk, tDv + tLk);
    }

    private void validateTiepNhanInput(TiepNhanInput in) {
        if (in == null) throw new IllegalArgumentException("Dữ liệu tiếp nhận không được null.");
        if (in.maKH() == null || in.maKH().isBlank()) throw new IllegalArgumentException("Vui lòng chọn khách hàng.");
        if (in.bienSo() == null || in.bienSo().isBlank()) throw new IllegalArgumentException("Vui lòng chọn phương tiện.");
        if (in.maNVTiepNhan() == null || in.maNVTiepNhan().isBlank()) throw new IllegalArgumentException("Không xác định được nhân viên tiếp nhận.");
        if (in.yeuCauBanDau() == null || in.yeuCauBanDau().isBlank()) throw new IllegalArgumentException("Vui lòng nhập yêu cầu của khách hàng.");
        if (in.soKmTiepNhan() < 0) throw new IllegalArgumentException("Số km tiếp nhận không được âm.");
        if (in.mucNhienLieu() < 0) throw new IllegalArgumentException("Mức nhiên liệu không được âm.");
    }
}

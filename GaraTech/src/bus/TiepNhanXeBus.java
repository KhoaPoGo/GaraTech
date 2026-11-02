package bus;

import dao.KhachHangDao;
import dao.PhieuTiepNhanDao;
import dao.PhieuTiepNhanDao.PhieuTiepNhanRow;
import dao.XeDao;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuTiepNhan;
import entity.Xe;
import enums.TrangThaiPhieuTiepNhan;
import util.IdGenerator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Nghiệp vụ Tiếp nhận xe (Tầng BUS).
 * Quản lý toàn bộ quy tắc nghiệp vụ và ràng buộc khi lập phiếu và tra cứu phiếu tiếp nhận.
 * 3 Trạng thái chuẩn: Chờ khách hàng xác nhận, Đã xác nhận, Đã hủy.
 */
public final class TiepNhanXeBus {

    public record TiepNhanInput(
            String maKH,
            String bienSo,
            String maNV,
            float soKmTiepNhan,
            float mucNhienLieu,
            String tinhTrangBanDau,
            String yeuCauKhachHang,
            LocalDateTime thoiGianTiepNhan,
            LocalDateTime thoiGianHenTra
    ) {}

    private TiepNhanXeBus() {}

    /**
     * Tạo và lưu mới một phiếu tiếp nhận với đầy đủ kiểm tra ràng buộc nghiệp vụ.
     * Mặc định khởi tạo ở trạng thái: Chờ khách hàng xác nhận.
     */
    public static PhieuTiepNhan taoPhieuTiepNhan(TiepNhanInput in) throws Exception {
        validateTiepNhanInput(in);

        // 1. Kiểm tra tồn tại của Khách hàng
        KhachHang kh = KhachHangDao.findById(in.maKH());
        if (kh == null) {
            throw new IllegalArgumentException("Khách hàng không tồn tại trong hệ thống.");
        }

        // 2. Kiểm tra tồn tại của Xe và xe phải thuộc khách hàng này
        Xe xe = XeDao.findByPlate(in.bienSo());
        if (xe == null) {
            throw new IllegalArgumentException("Phương tiện mang biển số " + in.bienSo() + " không tồn tại.");
        }
        if (xe.getKhachHang() == null || !xe.getKhachHang().getMaKH().equals(kh.getMaKH())) {
            throw new IllegalArgumentException("Phương tiện " + in.bienSo() + " không thuộc sở hữu của khách hàng " + kh.getHoTen() + ".");
        }

        // 3. Kiểm tra ràng buộc ODO không nhỏ hơn số km lần trước
        float odoTruocDo = PhieuTiepNhanDao.getLatestOdo(in.bienSo());
        if (in.soKmTiepNhan() < odoTruocDo) {
            throw new IllegalArgumentException(String.format(
                    "Số km tiếp nhận (%,.0f km) không được nhỏ hơn số km đã ghi nhận trước đó (%,.0f km).",
                    in.soKmTiepNhan(), odoTruocDo));
        }

        // 4. Sinh mã phiếu tiếp nhận tự động theo quy chuẩn: PTN-YYYYMMDD-STT
        String maPhieuTN = IdGenerator.next(IdGenerator.Kind.PHIEU_TIEP_NHAN);

        NhanVien nv = new NhanVien();
        nv.setMaNV(in.maNV());

        LocalDateTime tgTiepNhan = in.thoiGianTiepNhan() != null ? in.thoiGianTiepNhan() : LocalDateTime.now();

        PhieuTiepNhan ptn = new PhieuTiepNhan(
                maPhieuTN,
                xe,
                kh,
                nv,
                tgTiepNhan,
                in.thoiGianHenTra(),
                in.tinhTrangBanDau().trim(),
                in.yeuCauKhachHang().trim(),
                in.mucNhienLieu(),
                in.soKmTiepNhan(),
                TrangThaiPhieuTiepNhan.CHO_KHACH_HANG_XAC_NHAN.label()
        );

        boolean ok = PhieuTiepNhanDao.insert(ptn);
        if (!ok) {
            throw new SQLException("Không thể lưu phiếu tiếp nhận vào cơ sở dữ liệu.");
        }

        return ptn;
    }

    /**
     * Tra cứu danh sách phiếu tiếp nhận với bộ lọc ngày, từ khóa và trạng thái.
     */
    public static List<PhieuTiepNhanRow> traCuuDanhSach(LocalDate fromDate, LocalDate toDate, String keyword, String trangThai) {
        try {
            return PhieuTiepNhanDao.search(fromDate, toDate, keyword, trangThai);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Lấy chi tiết một phiếu tiếp nhận theo mã phiếu.
     */
    public static PhieuTiepNhanRow layChiTietPhieu(String maPhieuTN) {
        if (maPhieuTN == null || maPhieuTN.isBlank()) return null;
        try {
            return PhieuTiepNhanDao.findById(maPhieuTN.trim());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lấy số ODO gần nhất của xe để hiển thị gợi ý và kiểm tra ràng buộc.
     */
    public static float layOdoGanNhat(String bienSo) {
        if (bienSo == null || bienSo.isBlank()) return 0;
        try {
            return PhieuTiepNhanDao.getLatestOdo(bienSo.trim().toUpperCase());
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Xác nhận phiếu tiếp nhận (chuyển sang trạng thái 'Đã xác nhận' khi khách đồng ý tiếp nhận / sửa chữa).
     */
    public static void xacNhanPhieuTiepNhan(String maPhieuTN) throws Exception {
        if (maPhieuTN == null || maPhieuTN.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu tiếp nhận không hợp lệ.");
        }
        PhieuTiepNhanRow ptn = PhieuTiepNhanDao.findById(maPhieuTN);
        if (ptn == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu tiếp nhận " + maPhieuTN);
        }
        if (TrangThaiPhieuTiepNhan.DA_HUY.label().equalsIgnoreCase(ptn.trangThai())) {
            throw new IllegalArgumentException("Phiếu đã hủy không thể xác nhận.");
        }
        boolean ok = PhieuTiepNhanDao.updateTrangThai(maPhieuTN, TrangThaiPhieuTiepNhan.DA_XAC_NHAN.label());
        if (!ok) {
            throw new SQLException("Cập nhật trạng thái 'Đã xác nhận' không thành công.");
        }
    }

    /**
     * Hủy phiếu tiếp nhận.
     */
    public static void huyPhieuTiepNhan(String maPhieuTN) throws Exception {
        if (maPhieuTN == null || maPhieuTN.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu tiếp nhận không hợp lệ.");
        }
        PhieuTiepNhanRow ptn = PhieuTiepNhanDao.findById(maPhieuTN);
        if (ptn == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu tiếp nhận " + maPhieuTN);
        }
        if (TrangThaiPhieuTiepNhan.DA_HUY.label().equalsIgnoreCase(ptn.trangThai())) {
            throw new IllegalArgumentException("Phiếu này đã ở trạng thái hủy.");
        }
        boolean ok = PhieuTiepNhanDao.updateTrangThai(maPhieuTN, TrangThaiPhieuTiepNhan.DA_HUY.label());
        if (!ok) {
            throw new SQLException("Cập nhật trạng thái hủy phiếu không thành công.");
        }
    }

    private static void validateTiepNhanInput(TiepNhanInput in) {
        if (in == null) {
            throw new IllegalArgumentException("Dữ liệu tiếp nhận không được để trống.");
        }
        if (in.maKH() == null || in.maKH().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng.");
        }
        if (in.bienSo() == null || in.bienSo().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phương tiện của khách hàng.");
        }
        if (in.maNV() == null || in.maNV().isBlank()) {
            throw new IllegalArgumentException("Không xác định được nhân viên tiếp nhận.");
        }
        if (in.yeuCauKhachHang() == null || in.yeuCauKhachHang().trim().isBlank()) {
            throw new IllegalArgumentException("Yêu cầu của khách hàng là bắt buộc, không được để trống.");
        }
        if (in.tinhTrangBanDau() == null || in.tinhTrangBanDau().trim().isBlank()) {
            throw new IllegalArgumentException("Tình trạng ban đầu / hiện trạng xe là bắt buộc, không được để trống.");
        }
        if (in.soKmTiepNhan() < 0) {
            throw new IllegalArgumentException("Số km tiếp nhận (ODO) không được âm.");
        }
        if (in.mucNhienLieu() < 0 || in.mucNhienLieu() > 1.0f) {
            throw new IllegalArgumentException("Mức nhiên liệu không hợp lệ (từ 0 đến 1.0 bình).");
        }
        if (in.thoiGianHenTra() != null && in.thoiGianTiepNhan() != null) {
            if (!in.thoiGianHenTra().isAfter(in.thoiGianTiepNhan())) {
                throw new IllegalArgumentException("Thời gian hẹn trả xe phải sau thời gian tiếp nhận.");
            }
        }
    }
}

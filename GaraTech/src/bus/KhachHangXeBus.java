package bus;

import dao.KhachHangDao;
import dao.XeDao;
import dao.XeDao.LichSuSuaChuaRow;
import entity.KhachHang;
import entity.Xe;
import util.IdGenerator;
import util.Validator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Nghiệp vụ Quản lý Khách hàng & Phương tiện (Tầng BUS).
 * Toàn bộ dữ liệu được truy vấn và lưu trữ trực tiếp từ cơ sở dữ liệu.
 */
public final class KhachHangXeBus {

    public record KhachHangOverview(
            KhachHang khachHang,
            List<Xe> danhSachXe
    ) {
        public String maKH() { return khachHang.getMaKH(); }
        public String hoTen() { return khachHang.getHoTen(); }
        public String soDienThoai() { return khachHang.getSoDienThoai(); }
        public int diemTichLuy() { return khachHang.getDiemTichLuy(); }
        public int soLuongXe() { return danhSachXe != null ? danhSachXe.size() : 0; }
        public String xeSummary() {
            int count = soLuongXe();
            return count > 0 ? count + " phương tiện" : "Chưa có xe";
        }
    }

    private KhachHangXeBus() {}

    /**
     * Lấy toàn bộ danh sách khách hàng kèm danh sách xe trực tiếp từ Database.
     */
    public static List<KhachHangOverview> getAll() {
        try {
            List<KhachHang> khList = KhachHangDao.getAll();
            List<Xe> xeList = XeDao.getAll();
            return buildOverviewList(khList, xeList);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Tìm kiếm khách hàng theo từ khóa tên/SĐT và biển số xe.
     */
    public static List<KhachHangOverview> search(String keyword, String bienSo) {
        List<KhachHangOverview> all = getAll();
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        String bs = bienSo != null ? bienSo.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "") : "";

        if (kw.isEmpty() && bs.isEmpty()) {
            return all;
        }

        List<KhachHangOverview> result = new ArrayList<>();
        for (KhachHangOverview item : all) {
            boolean matchKw = kw.isEmpty();
            if (!matchKw) {
                String ten = item.hoTen() != null ? item.hoTen().toLowerCase(Locale.ROOT) : "";
                String sdt = item.soDienThoai() != null ? item.soDienThoai() : "";
                if (ten.contains(kw) || sdt.contains(kw)) {
                    matchKw = true;
                }
            }

            boolean matchBs = bs.isEmpty();
            if (!matchBs && item.danhSachXe() != null) {
                for (Xe x : item.danhSachXe()) {
                    String cleanPlate = x.getBienSo().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
                    if (cleanPlate.contains(bs)) {
                        matchBs = true;
                        break;
                    }
                }
            }

            if (matchKw && matchBs) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Lấy danh sách xe của một khách hàng từ CSDL.
     */
    public static List<Xe> getDanhSachXe(String maKH) {
        if (maKH == null) return Collections.emptyList();
        try {
            return XeDao.findByMaKH(maKH);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Thêm mới khách hàng độc lập, tự động phát sinh mã KH theo quy chuẩn KH-YYMM-STT.
     */
    public static KhachHang themKhachHang(KhachHang kh) throws Exception {
        validateKhachHang(kh, true);
        String maKH = IdGenerator.next(IdGenerator.Kind.KHACH_HANG);
        kh.setMaKH(maKH);
        KhachHangDao.insert(kh);
        return kh;
    }

    /**
     * Cập nhật thông tin liên hệ của khách hàng.
     */
    public static void capNhatKhachHang(KhachHang kh) throws Exception {
        validateKhachHang(kh, false);
        KhachHangDao.update(kh);
    }

    /**
     * Xóa khách hàng khỏi hệ thống (nếu chưa có giao dịch ràng buộc).
     */
    public static void xoaKhachHang(String maKH) throws Exception {
        if (maKH == null || maKH.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã khách hàng không hợp lệ.");
        }
        if (KhachHangDao.hasTransactions(maKH)) {
            throw new IllegalArgumentException("Không thể xóa khách hàng này vì đã có phương tiện hoặc chứng từ tiếp nhận / sửa chữa liên kết.");
        }
        if (!KhachHangDao.delete(maKH)) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng hoặc xóa không thành công.");
        }
    }

    /**
     * Thêm xe cho khách hàng đã có.
     */
    public static void themXeChoKhachHang(Xe xe) throws Exception {
        validateXe(xe, true, 0);
        XeDao.insert(xe);
    }

    /**
     * Cập nhật phương tiện với ràng buộc ODO không nhỏ hơn số km lần trước.
     */
    public static void capNhatPhuongTien(Xe xe, float odoTruocDo) throws Exception {
        validateXe(xe, false, odoTruocDo);
        XeDao.update(xe);
    }

    /**
     * Xóa phương tiện khỏi hệ thống (nếu chưa có phiếu tiếp nhận/sửa chữa).
     */
    public static void xoaXe(String bienSo) throws Exception {
        if (bienSo == null || bienSo.trim().isEmpty()) {
            throw new IllegalArgumentException("Biển số xe không hợp lệ.");
        }
        String cleanPlate = bienSo.trim().toUpperCase(Locale.ROOT);
        if (XeDao.hasTickets(cleanPlate)) {
            throw new IllegalArgumentException("Không thể xóa phương tiện '" + cleanPlate + "' vì đã có lịch sử phiếu tiếp nhận / sửa chữa trong hệ thống.");
        }
        if (!XeDao.delete(cleanPlate)) {
            throw new IllegalArgumentException("Không tìm thấy phương tiện hoặc xóa không thành công.");
        }
    }

    /**
     * Lấy lịch sử sửa chữa theo biển số xe hoặc tất cả các xe của khách hàng từ Database.
     */
    public static List<LichSuSuaChuaRow> getLichSuSuaChua(String bienSo, List<Xe> allVehicles) {
        if (bienSo != null && !bienSo.trim().isEmpty() && !bienSo.startsWith("ALL")) {
            try {
                return XeDao.getLichSuSuaChua(bienSo);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        List<LichSuSuaChuaRow> allHistory = new ArrayList<>();
        if (allVehicles != null) {
            for (Xe x : allVehicles) {
                try {
                    allHistory.addAll(XeDao.getLichSuSuaChua(x.getBienSo()));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return allHistory;
    }

    /**
     * Kiểm tra ràng buộc hợp lệ cho khách hàng.
     */
    public static void validateKhachHang(KhachHang kh, boolean isCreate) throws Exception {
        if (kh == null) throw new IllegalArgumentException("Thông tin khách hàng không được rỗng.");

        if (!Validator.notBlank(kh.getHoTen())) {
            throw new IllegalArgumentException("Họ tên khách hàng không được để trống.");
        }

        if (!Validator.phone(kh.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0.");
        }

        if (kh.getEmail() != null && !kh.getEmail().trim().isEmpty() && !Validator.email(kh.getEmail())) {
            throw new IllegalArgumentException("Email không đúng định dạng (ví dụ: ten@mien.com).");
        }

        if (kh.getDiemTichLuy() < 0) {
            throw new IllegalArgumentException("Điểm tích lũy không được nhỏ hơn 0.");
        }

        // Kiểm tra trùng SĐT trong CSDL
        String excludeId = isCreate ? null : kh.getMaKH();
        if (KhachHangDao.existsPhone(kh.getSoDienThoai(), excludeId)) {
            throw new IllegalArgumentException("Số điện thoại '" + kh.getSoDienThoai() + "' đã tồn tại trong hệ thống.");
        }
    }

    /**
     * Kiểm tra ràng buộc hợp lệ cho xe.
     */
    public static void validateXe(Xe xe, boolean isCreate, float odoTruocDo) throws Exception {
        if (xe == null) throw new IllegalArgumentException("Thông tin xe không được rỗng.");
        if (xe.getKhachHang() == null) throw new IllegalArgumentException("Xe phải được liên kết với một khách hàng hợp lệ.");

        if (!Validator.notBlank(xe.getBienSo())) {
            throw new IllegalArgumentException("Biển số xe không được để trống.");
        }

        String plate = xe.getBienSo().trim().toUpperCase(Locale.ROOT);
        if (plate.length() < 6 || plate.length() > 12) {
            throw new IllegalArgumentException("Biển số xe phải có độ dài từ 6 đến 12 ký tự.");
        }

        if (!Validator.notBlank(xe.getHangXe())) {
            throw new IllegalArgumentException("Hãng xe không được để trống.");
        }

        if (!Validator.notBlank(xe.getModel())) {
            throw new IllegalArgumentException("Dòng xe (model) không được để trống.");
        }

        int currentYear = LocalDate.now().getYear();
        if (xe.getNamSanXuat() < 1980 || xe.getNamSanXuat() > currentYear) {
            throw new IllegalArgumentException("Năm sản xuất phải từ năm 1980 đến " + currentYear + ".");
        }

        if (!Validator.notBlank(xe.getMauXe())) {
            throw new IllegalArgumentException("Màu sơn không được để trống.");
        }

        if (xe.getSoKmHienTai() < 0) {
            throw new IllegalArgumentException("Số km hiện tại (ODO) phải lớn hơn hoặc bằng 0.");
        }

        if (!isCreate && odoTruocDo > 0 && xe.getSoKmHienTai() < odoTruocDo) {
            throw new IllegalArgumentException(String.format(
                    "Số km hiện tại (%,.0f km) không được nhỏ hơn số km đã ghi nhận lần trước (%,.0f km).",
                    xe.getSoKmHienTai(), odoTruocDo));
        }

        if (isCreate) {
            if (XeDao.existsPlate(plate)) {
                throw new IllegalArgumentException("Biển số xe '" + plate + "' đã tồn tại trong hệ thống.");
            }
        }
    }

    private static List<KhachHangOverview> buildOverviewList(List<KhachHang> khList, List<Xe> xeList) {
        List<KhachHangOverview> result = new ArrayList<>();
        if (khList == null) return result;
        for (KhachHang kh : khList) {
            List<Xe> vehicles = new ArrayList<>();
            if (xeList != null) {
                for (Xe x : xeList) {
                    if (x.getKhachHang() != null && kh.getMaKH().equals(x.getKhachHang().getMaKH())) {
                        vehicles.add(x);
                    }
                }
            }
            result.add(new KhachHangOverview(kh, vehicles));
        }
        return result;
    }
}

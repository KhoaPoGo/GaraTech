package bus;

import connectDB.ConnectDB;
import dao.KhachHangDao;
import dao.XeDao;
import dao.XeDao.LichSuSuaChuaRow;
import entity.KhachHang;
import entity.Xe;
import util.IdGenerator;
import util.Validator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Nghiệp vụ Quản lý Khách hàng & Phương tiện (Tầng BUS - SV2 phụ trách).
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

    // In-memory mock store dùng khi demo hoặc chưa nối SQL Server
    private static final List<KhachHang> mockKhachHangs = new ArrayList<>();
    private static final List<Xe> mockXes = new ArrayList<>();
    private static final List<LichSuSuaChuaRow> mockLichSus = new ArrayList<>();

    static {
        seedMockData();
    }

    private KhachHangXeBus() {}

    private static void seedMockData() {
        DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        KhachHang kh1 = new KhachHang("KH00012", "Nguyễn Minh Anh", "0901234567", "25 Nguyễn Văn Cừ, TP.HCM", "minhanh.ng@gmail.com", 420);
        Xe xe1_1 = new Xe("51H-12345", kh1, "Toyota", "Vios", 2021, "Trắng", "2NRX123456", 48250.0f, LocalDate.parse("15/03/2021", dmy).atStartOfDay());
        Xe xe1_2 = new Xe("51K-68290", kh1, "Mazda", "CX-5", 2023, "Xám", "PE984123", 21600.0f, LocalDate.parse("10/01/2023", dmy).atStartOfDay());
        Xe xe1_3 = new Xe("51L-90428", kh1, "Honda", "CR-V", 2024, "Đen", "L15BG9988", 12800.0f, LocalDate.parse("05/04/2024", dmy).atStartOfDay());

        KhachHang kh2 = new KhachHang("KH-2610-002", "Trần Quốc Bảo", "0912345678", "124 Cách Mạng Tháng 8, Q.3, TP.HCM", "baotran@gmail.com", 260);
        Xe xe2_1 = new Xe("51G-67890", kh2, "Honda", "City", 2022, "Đen", "L15Z198273", 35000.0f, LocalDate.parse("10/05/2022", dmy).atStartOfDay());

        KhachHang kh3 = new KhachHang("KH-2610-003", "Lê Hoàng Lan", "0987654321", "88 Lê Văn Sỹ, Phú Nhuận, TP.HCM", "lan.le@yahoo.com", 180);
        Xe xe3_1 = new Xe("60A-24680", kh3, "Mazda", "3", 2020, "Đỏ", "PE0183746", 52100.0f, LocalDate.parse("20/11/2020", dmy).atStartOfDay());

        KhachHang kh4 = new KhachHang("KH-2610-004", "Vũ Đức Long", "0934567890", "302 Hoàng Văn Thụ, Tân Bình, TP.HCM", "longvd@gmail.com", 610);
        Xe xe4_1 = new Xe("51F-45678", kh4, "Ford", "Ranger", 2019, "Cam", "P4AT984721", 89400.0f, LocalDate.parse("05/08/2019", dmy).atStartOfDay());

        mockKhachHangs.add(kh1);
        mockKhachHangs.add(kh2);
        mockKhachHangs.add(kh3);
        mockKhachHangs.add(kh4);

        mockXes.add(xe1_1);
        mockXes.add(xe1_2);
        mockXes.add(xe1_3);
        mockXes.add(xe2_1);
        mockXes.add(xe3_1);
        mockXes.add(xe4_1);

        mockLichSus.add(new LichSuSuaChuaRow("06/10/2026", "51H-12345", "PSC-20261006-001", "Bảo dưỡng, phanh trước", "Chưa phát hành"));
        mockLichSus.add(new LichSuSuaChuaRow("15/04/2026", "51H-12345", "PSC-20260415-008", "Bảo dưỡng 40.000 km", "HD20260415-0008"));
        mockLichSus.add(new LichSuSuaChuaRow("10/10/2025", "51H-12345", "PSC-20251010-005", "Thay lốp, cân chỉnh", "HD20251010-0005"));
    }

    /**
     * Lấy toàn bộ danh sách khách hàng kèm danh sách xe.
     */
    public static List<KhachHangOverview> getAll() {
        try {
            List<KhachHang> khList = KhachHangDao.getAll();
            List<Xe> xeList = XeDao.getAll();
            return buildOverviewList(khList, xeList);
        } catch (SQLException e) {
            return buildOverviewList(mockKhachHangs, mockXes);
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
     * Lấy danh sách xe của một khách hàng cụ thể.
     */
    public static List<Xe> getDanhSachXe(String maKH) {
        if (maKH == null) return Collections.emptyList();
        try {
            return XeDao.findByMaKH(maKH);
        } catch (SQLException e) {
            List<Xe> list = new ArrayList<>();
            for (Xe x : mockXes) {
                if (x.getKhachHang() != null && maKH.equals(x.getKhachHang().getMaKH())) {
                    list.add(x);
                }
            }
            return list;
        }
    }

    /**
     * Thêm mới khách hàng độc lập, tự động phát sinh mã KH theo quy chuẩn KH-YYMM-STT.
     * Trả về đối tượng KhachHang đã có mã.
     */
    public static KhachHang themKhachHang(KhachHang kh) throws Exception {
        validateKhachHang(kh, true);
        try {
            String maKH = IdGenerator.next(IdGenerator.Kind.KHACH_HANG);
            kh.setMaKH(maKH);
            KhachHangDao.insert(kh);
            return kh;
        } catch (SQLException e) {
            String maKH = "KH-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-" + String.format("%03d", mockKhachHangs.size() + 1);
            kh.setMaKH(maKH);
            mockKhachHangs.add(0, kh);
            return kh;
        }
    }

    /**
     * Cập nhật thông tin liên hệ của khách hàng.
     */
    public static void capNhatKhachHang(KhachHang kh) throws Exception {
        validateKhachHang(kh, false);
        try {
            KhachHangDao.update(kh);
        } catch (SQLException e) {
            for (int i = 0; i < mockKhachHangs.size(); i++) {
                if (mockKhachHangs.get(i).getMaKH().equals(kh.getMaKH())) {
                    mockKhachHangs.set(i, kh);
                    break;
                }
            }
        }
    }

    /**
     * Thêm xe cho khách hàng đã có.
     */
    public static void themXeChoKhachHang(Xe xe) throws Exception {
        validateXe(xe, true, 0);
        try {
            XeDao.insert(xe);
        } catch (SQLException e) {
            mockXes.add(0, xe);
        }
    }

    /**
     * Cập nhật phương tiện với ràng buộc ODO không nhỏ hơn số km lần trước.
     */
    public static void capNhatPhuongTien(Xe xe, float odoTruocDo) throws Exception {
        validateXe(xe, false, odoTruocDo);
        try {
            XeDao.update(xe);
        } catch (SQLException e) {
            for (int i = 0; i < mockXes.size(); i++) {
                if (mockXes.get(i).getBienSo().equalsIgnoreCase(xe.getBienSo())) {
                    mockXes.set(i, xe);
                    break;
                }
            }
        }
    }

    /**
     * Lấy lịch sử sửa chữa theo biển số xe hoặc tất cả các xe của khách hàng.
     */
    public static List<LichSuSuaChuaRow> getLichSuSuaChua(String bienSo, List<Xe> allVehicles) {
        if (bienSo != null && !bienSo.trim().isEmpty() && !bienSo.startsWith("ALL")) {
            try {
                List<LichSuSuaChuaRow> list = XeDao.getLichSuSuaChua(bienSo);
                if (!list.isEmpty()) return list;
            } catch (SQLException ignored) {}

            List<LichSuSuaChuaRow> filtered = new ArrayList<>();
            for (LichSuSuaChuaRow r : mockLichSus) {
                if (r.bienSo().equalsIgnoreCase(bienSo)) {
                    filtered.add(r);
                }
            }
            return filtered;
        }

        // Lấy tất cả các xe
        List<LichSuSuaChuaRow> allHistory = new ArrayList<>();
        if (allVehicles != null) {
            for (Xe x : allVehicles) {
                try {
                    allHistory.addAll(XeDao.getLichSuSuaChua(x.getBienSo()));
                } catch (SQLException ignored) {}
            }
        }
        if (!allHistory.isEmpty()) {
            return allHistory;
        }
        return mockLichSus;
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

        // Kiểm tra trùng SĐT
        try {
            String excludeId = isCreate ? null : kh.getMaKH();
            if (KhachHangDao.existsPhone(kh.getSoDienThoai(), excludeId)) {
                throw new IllegalArgumentException("Số điện thoại '" + kh.getSoDienThoai() + "' đã tồn tại trong hệ thống.");
            }
        } catch (SQLException ignored) {
            for (KhachHang m : mockKhachHangs) {
                if ((isCreate || !m.getMaKH().equals(kh.getMaKH())) && m.getSoDienThoai().equals(kh.getSoDienThoai())) {
                    throw new IllegalArgumentException("Số điện thoại '" + kh.getSoDienThoai() + "' đã tồn tại trong hồ sơ khác.");
                }
            }
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
            try {
                if (XeDao.existsPlate(plate)) {
                    throw new IllegalArgumentException("Biển số xe '" + plate + "' đã tồn tại trong hệ thống.");
                }
            } catch (SQLException ignored) {
                for (Xe mx : mockXes) {
                    if (mx.getBienSo().equalsIgnoreCase(plate)) {
                        throw new IllegalArgumentException("Biển số xe '" + plate + "' đã có trong hệ thống.");
                    }
                }
            }
        }
    }

    private static List<KhachHangOverview> buildOverviewList(List<KhachHang> khList, List<Xe> xeList) {
        List<KhachHangOverview> result = new ArrayList<>();
        for (KhachHang kh : khList) {
            List<Xe> vehicles = new ArrayList<>();
            for (Xe x : xeList) {
                if (x.getKhachHang() != null && kh.getMaKH().equals(x.getKhachHang().getMaKH())) {
                    vehicles.add(x);
                }
            }
            result.add(new KhachHangOverview(kh, vehicles));
        }
        return result;
    }
}

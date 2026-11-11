package gui;

import enums.Role;
import gui.component.BasePanel;
import gui.panel.BaoCaoPanel;
import gui.panel.CongViecKTVPanel;
import gui.panel.DashBoardPanel;
import gui.panel.DashboardTiepNhanPanel;
import gui.panel.DichVuNhanSuPanel;
import gui.panel.DieuPhoiPanel;
import gui.panel.HoaDonPanel;
import gui.panel.KiemTraChatLuongPanel;
import gui.panel.KhuyenMaiPanel;
import gui.panel.KhachHangXePanel;
import gui.panel.NhapKhoPanel;
import gui.panel.PlaceholderPanel;
import gui.panel.PhuTungTonKhoPanel;
import gui.panel.TiepNhanXePanel;
import gui.panel.XuatKhoPanel;
import gui.panel.YeuCauPhuTungPanel;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static enums.Role.KHO;
import static enums.Role.KY_THUAT_VIEN;
import static enums.Role.QUAN_LY;
import static enums.Role.TIEP_NHAN;

/**
 * DANH SÁCH MÀN HÌNH + AI ĐƯỢC VÀO + AI PHỤ TRÁCH. Thứ tự khai báo = thứ tự hiện trong sidebar.
 */
public enum Screen {
    // ---- Quản lý (SV1) ----
    DASHBOARD("Tổng quan hệ thống", "SV1", EnumSet.of(QUAN_LY),
            "Số xe tiếp nhận / đang sửa / hoàn thành kỹ thuật / doanh thu ngày; biểu đồ 7 ngày; cảnh báo tồn kho; phiếu đang hoạt động.",
            DashBoardPanel::new),
    DIEU_PHOI("Điều phối & tiến độ", "SV1", EnumSet.of(QUAN_LY),
            "Chỉ xem các phiếu tiếp nhận-sửa chữa thống nhất theo trạng thái và thông tin tiến độ.",
            DieuPhoiPanel::new),
    KIEM_TRA_CL("Kiểm tra chất lượng (KCS)", "SV1", EnumSet.of(QUAN_LY),
    		"2 cột đối chiếu (KTV báo / quản lý kiểm); checklist bắt buộc (đã chạy thử, không rò rỉ nhớt, vệ sinh xe) mới cho chuyển Hoàn thành.",
    		KiemTraChatLuongPanel::new),

    // ---- Kỹ thuật viên (SV4) ----
    CONG_VIEC_KTV("Thực hiện sửa chữa", "SV4", EnumSet.of(KY_THUAT_VIEN),
            "Chỉ KTV thấy phiếu được phân công cho mình; tick hạng mục đã làm; cập nhật tiến độ & kết quả; xong thì chuyển sang Chờ kiểm tra.",
            CongViecKTVPanel::new),
    
    YEU_CAU_PHU_TUNG("Yêu cầu cấp phụ tùng", "SV4", EnumSet.of(KY_THUAT_VIEN),
            "KTV gửi yêu cầu xuất phụ tùng cho kho (trạng thái Chờ xác nhận); xem tình trạng yêu cầu đã gửi.",
            YeuCauPhuTungPanel::new),

    // ---- Tiếp nhận (SV2) ----
    DASHBOARD_TIEP_NHAN("Điều hành tiếp nhận", "SV2", EnumSet.of(TIEP_NHAN),
            "Giám sát xe vào xưởng, 4 chỉ số KPI ca trực, thanh tiến độ xưởng và hàng đợi giao xe.",
            DashboardTiepNhanPanel::new),
    KHACH_HANG_XE("Quản lý khách hàng & xe", "SV2", EnumSet.of(QUAN_LY, TIEP_NHAN, KY_THUAT_VIEN),
            "Danh sách + chi tiết + lịch sử sửa chữa; popup thêm khách; SĐT 10 số bắt đầu 0; chặn trùng biển số; không xóa nếu đã có chứng từ. KTV chỉ xem.",
            KhachHangXePanel::new),
    TIEP_NHAN_XE("Quản lý tiếp nhận & sửa chữa", "SV2", EnumSet.of(TIEP_NHAN),
            "Quản lý vòng đời phiếu Tiếp nhận - Sửa chữa thống nhất; chọn khách/xe, phân công KTV, dịch vụ đề nghị, theo dõi 4 trạng thái.",
            TiepNhanXePanel::new),
    HOA_DON("Lập hóa đơn & thanh toán", "SV4", EnumSet.of(TIEP_NHAN),
            "Chỉ lập khi phiếu Hoàn thành; tiền công + phụ tùng − giảm giá + VAT 10%; lịch sử thanh toán; in hóa đơn.",
            HoaDonPanel::new),
    BAN_GIAO("Bàn giao xe cho khách", "SV4", EnumSet.noneOf(Role.class),
            "Chỉ cho 'Xác nhận đã bàn giao' khi hóa đơn thanh toán đủ; tick đã trả chìa khóa và giấy tờ.",
            null),

    // ---- Kho (SV3) ----
    PHU_TUNG_TON_KHO("Quản lý phụ tùng & tồn kho", "SV3", EnumSet.of(QUAN_LY, KHO, KY_THUAT_VIEN),
            "Bảng phụ tùng, cột tình trạng xanh/cam/đỏ; bấm 1 món xem lịch sử nhập xuất + người thực hiện; KTV chỉ đọc.",
            PhuTungTonKhoPanel::new),
    NHAP_KHO("Quản lý nhập kho", "SV3", EnumSet.of(KHO),
            "Chọn nhà cung cấp; số lượng × đơn giá = thành tiền, tổng lô hàng; cột Tồn trước / Tồn sau; ghi trong 1 transaction.",
            NhapKhoPanel::new),
    XUAT_KHO("Quản lý xuất kho", "SV3", EnumSet.of(KHO),
            "Duyệt yêu cầu từ KTV; số lượng xuất > tồn thì báo đỏ và khóa nút xác nhận; gắn với phiếu sửa chữa; transaction.",
            XuatKhoPanel::new),

    // ---- Quản lý (SV1) ----
    DICH_VU_NHAN_SU("Quản lý dịch vụ & nhân sự", "SV1", EnumSet.of(QUAN_LY),
            "Quản lý dịch vụ, nhân viên, tài khoản và bảng quyền theo vai trò.",
            DichVuNhanSuPanel::new),
    KHUYEN_MAI("Quản lý khuyến mãi", "SV1", EnumSet.of(QUAN_LY),
            "Quản lý chương trình, tỷ lệ giảm và thời hạn theo bảng KhuyenMai.", KhuyenMaiPanel::new),
    BAO_CAO("Báo cáo & thống kê doanh thu", "SV1", EnumSet.of(QUAN_LY),
            "Lọc từ ngày – đến ngày; thực thu vs còn chờ thu; cơ cấu dịch vụ; nhóm phụ tùng tiêu thụ nhiều.",
            BaoCaoPanel::new);

    private final String title;
    private final String owner;
    private final Set<Role> roles;
    private final String hint;
    private final Supplier<BasePanel> factory;

    Screen(String title, String owner, Set<Role> roles, String hint, Supplier<BasePanel> factory) {
        this.title = title;
        this.owner = owner;
        this.roles = roles;
        this.hint = hint;
        this.factory = factory;
    }

    public String title() { return title; }
    public String owner() { return owner; }
    public String hint() { return hint; }
    public boolean allows(Role r) { return r != null && roles.contains(r); }

    /** Tạo panel của màn hình (hoặc trang giữ chỗ nếu chưa làm). */
    BasePanel create() {
        return factory != null ? factory.get() : new PlaceholderPanel(this);
    }

    /** Các màn hình hiện trong sidebar của một vai trò, theo thứ tự khai báo. */
    public static List<Screen> forRole(Role r) {
        return Arrays.stream(values()).filter(s -> s.allows(r)).toList();
    }
}

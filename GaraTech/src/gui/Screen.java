package gui;

import enums.Role;
import gui.component.BasePanel;
import gui.panel.DashBoardPanel;
import gui.panel.PlaceholderPanel;

import gui.label.CongViecKTVLabel;
import gui.label.YeuCauPhuTungLabel;

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
 *
 * Khi làm xong một màn hình, chỉ cần thay tham số cuối (null) bằng constructor của panel, ví dụ:
 *   KHACH_HANG_XE(..., KhachHangXePanel::new)
 * null nghĩa là màn hình chưa làm, hệ thống tự hiện trang giữ chỗ kèm yêu cầu cần làm.
 * Mỗi người chỉ sửa đúng dòng của mình để tránh xung đột khi merge.
 */
public enum Screen {
    // ---- Quản lý (SV1) ----
    DASHBOARD("Tổng quan", "SV1", EnumSet.of(QUAN_LY),
            "Số xe tiếp nhận / đang sửa / hoàn thành kỹ thuật / doanh thu ngày; biểu đồ 7 ngày; cảnh báo tồn kho; phiếu đang hoạt động.",
            DashBoardPanel::new),
    DIEU_PHOI("Điều phối & tiến độ", "SV1", EnumSet.of(QUAN_LY),
            "Kanban 5 cột theo trạng thái phiếu; chọn kỹ thuật viên (chuyên môn, rảnh/bận); hạn hoàn thành; khung kiểm soát tiến độ.",
            null),
    KIEM_TRA_CL("Kiểm tra chất lượng", "SV1", EnumSet.of(QUAN_LY),
            "2 cột đối chiếu (KTV báo / quản lý kiểm); checklist bắt buộc (đã chạy thử, không rò rỉ nhớt, vệ sinh xe) mới cho chuyển Hoàn thành.",
            null),

    // ---- Kỹ thuật viên (SV4) ----
    CONG_VIEC_KTV("Công việc của tôi", "SV4", EnumSet.of(KY_THUAT_VIEN),
            "Chỉ Hoàng thấy phiếu được phân công cho mình; tick hạng mục đã làm; cập nhật tiến độ & kết quả; xong thì chuyển sang Chờ kiểm tra.",
            CongViecKTVLabel::new),
    
    YEU_CAU_PHU_TUNG("Yêu cầu phụ tùng", "SV4", EnumSet.of(KY_THUAT_VIEN),
            "Hoang Gửi yêu cầu xuất phụ tùng cho kho (trạng thái Chờ xác nhận); xem tình trạng yêu cầu đã gửi.",
            YeuCauPhuTungLabel::new),

    // ---- Tiếp nhận (SV2) ----
    KHACH_HANG_XE("Khách hàng & xe", "SV2", EnumSet.of(QUAN_LY, TIEP_NHAN, KY_THUAT_VIEN),
            "Danh sách + chi tiết + lịch sử sửa chữa; popup thêm khách; SĐT 10 số bắt đầu 0; chặn trùng biển số; không xóa nếu đã có chứng từ. KTV chỉ xem.",
            null),
    TIEP_NHAN_XE("Tiếp nhận xe", "SV2", EnumSet.of(TIEP_NHAN),
            "Chọn khách/xe, số km, mức nhiên liệu, ghi vết xước (bắt buộc); hẹn trả ≥ thời gian tiếp nhận; mã PTN-YYYYMMDD-STT.",
            null),
    PHIEU_SUA_CHUA("Lập phiếu sửa chữa", "SV2", EnumSet.of(TIEP_NHAN),
            "Bảng hạng mục (dịch vụ HOẶC phụ tùng); báo giá; Chờ duyệt / Đã xác nhận / Từ chối; checklist phê duyệt; hủy phiếu có lý do.",
            null),
    HOA_DON("Hóa đơn", "SV4", EnumSet.of(TIEP_NHAN),
            "Chỉ lập khi phiếu Hoàn thành; tiền công + phụ tùng − giảm giá + VAT 10%; lịch sử thanh toán nhiều lần; in hóa đơn.",
            null),
    BAN_GIAO("Bàn giao xe", "SV4", EnumSet.of(TIEP_NHAN),
            "Chỉ cho 'Xác nhận đã bàn giao' khi hóa đơn thanh toán đủ; tick đã trả chìa khóa và giấy tờ.",
            null),

    // ---- Kho (SV3) ----
    PHU_TUNG_TON_KHO("Phụ tùng & tồn kho", "SV3", EnumSet.of(QUAN_LY, KHO, KY_THUAT_VIEN),
            "Bảng phụ tùng, cột tình trạng xanh/cam/đỏ; bấm 1 món xem lịch sử nhập xuất + người thực hiện; KTV chỉ đọc.",
            null),
    NHAP_KHO("Nhập kho", "SV3", EnumSet.of(KHO),
            "Chọn nhà cung cấp; số lượng × đơn giá = thành tiền, tổng lô hàng; cột Tồn trước / Tồn sau; ghi trong 1 transaction.",
            null),
    XUAT_KHO("Xuất kho", "SV3", EnumSet.of(KHO),
            "Duyệt yêu cầu từ KTV; số lượng xuất > tồn thì báo đỏ và khóa nút xác nhận; gắn với phiếu sửa chữa; transaction.",
            null),

    // ---- Quản lý (SV1) ----
    DICH_VU_NHAN_SU("Dịch vụ & nhân sự", "SV1", EnumSet.of(QUAN_LY),
            "Tab: Dịch vụ & đơn giá / Nhân viên / Tài khoản / Bảng phân quyền; KHÓA tài khoản thay vì xóa; heSoLuong 1.84–6.5.",
            null),
    BAO_CAO("Báo cáo & thống kê", "SV1", EnumSet.of(QUAN_LY),
            "Lọc từ ngày – đến ngày; thực thu vs còn chờ thu; cơ cấu dịch vụ; nhóm phụ tùng tiêu thụ nhiều.",
            null);

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

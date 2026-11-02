package bus;

import dao.DieuPhoiDao;
import dao.DieuPhoiDao.HangMuc;
import dao.DieuPhoiDao.KtvLoad;
import dao.DieuPhoiDao.PhieuCard;
import enums.TrangThaiPhieuSuaChua;
import util.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ điều phối & kiểm tra chất lượng của quản lý. Mọi chuyển trạng thái đều qua
 * {@link TrangThaiPhieuSuaChua#canMoveTo} và cập nhật có điều kiện "trạng thái cũ" để tránh ghi đè khi hai người thao tác cùng lúc.
 */
public final class DieuPhoiService {
    private static final String DA_DOI = "Phiếu vừa được người khác thay đổi. Hãy tải lại danh sách rồi thao tác lại.";

    private final DieuPhoiDao dao = new DieuPhoiDao();

    // ---------------- đọc ----------------
    /** 5 cột Kanban: từ Chờ khách duyệt đến Hoàn thành. */
    public static final TrangThaiPhieuSuaChua[] CAC_COT = {
            TrangThaiPhieuSuaChua.CHO_KHACH_DUYET, TrangThaiPhieuSuaChua.DA_XAC_NHAN, TrangThaiPhieuSuaChua.DANG_SUA_CHUA,
            TrangThaiPhieuSuaChua.CHO_KIEM_TRA, TrangThaiPhieuSuaChua.HOAN_THANH};

    public List<PhieuCard> phieuTrenBang() throws SQLException { return dao.findByTrangThai(0, CAC_COT); }

    public List<KtvLoad> danhSachKtv() throws SQLException { return dao.findKtvVaTaiTrong(); }

    public List<PhieuCard> phieuChoKiemTra() throws SQLException {
        return dao.findByTrangThai(0, TrangThaiPhieuSuaChua.CHO_KIEM_TRA);
    }

    public List<HangMuc> hangMuc(String maPhieu) throws SQLException { return dao.findHangMuc(maPhieu); }

    public String ghiChuKyThuat(String maPhieu) throws SQLException { return dao.findGhiChuKyThuat(maPhieu); }

    // ---------------- điều phối ----------------
    public void phanCong(String maPhieu, String maKTV, LocalDateTime duKien) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        if (p.trangThai() != TrangThaiPhieuSuaChua.DA_XAC_NHAN && p.trangThai() != TrangThaiPhieuSuaChua.DANG_SUA_CHUA)
            throw new BusinessException("Chỉ phân công được phiếu ở trạng thái \"Đã xác nhận\" hoặc \"Đang sửa chữa\" (phiếu này đang \""
                    + p.trangThai() + "\").");
        if (maKTV == null || maKTV.isBlank()) throw new BusinessException("Vui lòng chọn kỹ thuật viên.");
        if (!dao.laKtvDangLam(maKTV)) throw new BusinessException("Người được chọn không phải kỹ thuật viên đang làm việc.");
        if (duKien == null) throw new BusinessException("Vui lòng nhập hạn hoàn thành dự kiến.");
        if (!duKien.isAfter(LocalDateTime.now())) throw new BusinessException("Hạn hoàn thành phải sau thời điểm hiện tại.");
        if (p.batDau() != null && duKien.isBefore(p.batDau()))
            throw new BusinessException("Hạn hoàn thành không thể sớm hơn thời điểm bắt đầu sửa.");
        if (dao.phanCong(maPhieu, maKTV, duKien) == 0) throw new BusinessException(DA_DOI);
    }

    public void batDauSua(String maPhieu) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        if (p.trangThai() != TrangThaiPhieuSuaChua.DA_XAC_NHAN)
            throw new BusinessException("Không thể bắt đầu sửa từ trạng thái \"" + p.trangThai() + "\". Chỉ phiếu \"Đã xác nhận\" (khách đã duyệt báo giá) mới bắt đầu sửa được.");
        if (p.maKTV() == null) throw new BusinessException("Hãy phân công kỹ thuật viên trước khi bắt đầu sửa.");
        if (dao.batDauSua(maPhieu) == 0) throw new BusinessException(DA_DOI);
    }

    public void huyPhieu(String maPhieu, String lyDo) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        lyDo = lyDo == null ? "" : lyDo.trim();
        if (lyDo.isEmpty()) throw new BusinessException("Vui lòng nhập lý do hủy phiếu.");
        if (lyDo.length() > 300) throw new BusinessException("Lý do hủy tối đa 300 ký tự.");
        if (!p.trangThai().canMoveTo(TrangThaiPhieuSuaChua.DA_HUY))
            throw new BusinessException("Phiếu đang \"" + p.trangThai() + "\" nên không thể hủy.");
        if (dao.daXuatKho(maPhieu))
            throw new BusinessException("Phiếu này đã xuất phụ tùng từ kho nên không thể hủy. Hãy xử lý hoàn kho trước.");
        if (dao.huy(maPhieu, p.trangThai(), lyDo) == 0) throw new BusinessException(DA_DOI);
    }

    // ---------------- kiểm tra chất lượng ----------------
    /** Duyệt hoàn thành. Điều kiện: đang "Chờ kiểm tra" và mọi hạng mục (trừ hạng mục bị từ chối) đã hoàn thành. */
    public void duyetHoanThanh(String maPhieu, String ghiChuQuanLy) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        if (!p.trangThai().canMoveTo(TrangThaiPhieuSuaChua.HOAN_THANH))
            throw new BusinessException("Chỉ duyệt được phiếu đang \"Chờ kiểm tra\" (phiếu này đang \"" + p.trangThai() + "\").");
        int chuaXong = dao.demHangMucChuaXong(maPhieu);
        if (chuaXong > 0) throw new BusinessException("Còn " + chuaXong + " hạng mục chưa hoàn thành. Hãy trả phiếu về cho kỹ thuật viên làm tiếp.");
        String note = noiGhiChu(dao.findGhiChuKyThuat(maPhieu), "QL duyệt", ghiChuQuanLy);
        if (dao.hoanThanh(maPhieu, note) == 0) throw new BusinessException(DA_DOI);
    }

    public void traVeLamLai(String maPhieu, String lyDo) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        lyDo = lyDo == null ? "" : lyDo.trim();
        if (lyDo.isEmpty()) throw new BusinessException("Vui lòng nhập lý do trả về để kỹ thuật viên biết cần làm lại gì.");
        if (!p.trangThai().canMoveTo(TrangThaiPhieuSuaChua.DANG_SUA_CHUA) || p.trangThai() != TrangThaiPhieuSuaChua.CHO_KIEM_TRA)
            throw new BusinessException("Chỉ trả về được phiếu đang \"Chờ kiểm tra\".");
        String note = noiGhiChu(dao.findGhiChuKyThuat(maPhieu), "QL trả về làm lại", lyDo);
        if (dao.traVeLamLai(maPhieu, note) == 0) throw new BusinessException(DA_DOI);
    }

    // ---------------- nội bộ ----------------
    private PhieuCard layPhieu(String maPhieu) throws SQLException {
        PhieuCard p = dao.findById(maPhieu);
        if (p == null) throw new BusinessException("Phiếu " + maPhieu + " không còn tồn tại.");
        if (p.trangThai() == null) throw new BusinessException("Phiếu " + maPhieu + " có trạng thái không hợp lệ.");
        return p;
    }

    /** Nối ghi chú mới vào ghi chú cũ, cắt còn tối đa 500 ký tự (độ dài cột ghiChuKyThuat), giữ phần mới nhất. */
    static String noiGhiChu(String cu, String nhan, String moi) {
        String m = moi == null ? "" : moi.trim();
        if (m.isEmpty() && !"QL duyệt".equals(nhan)) return cu;
        String dong = "[" + nhan + " " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                + (Session.user() == null ? "" : " - " + Session.user().hoTen()) + "] " + m;
        String all = (cu == null || cu.isBlank()) ? dong : cu.trim() + "\n" + dong;
        return all.length() <= 500 ? all : all.substring(all.length() - 500);
    }
}

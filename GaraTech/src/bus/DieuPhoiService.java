package bus;

import dao.DieuPhoiDao;
import dao.DieuPhoiDao.HangMuc;
import dao.DieuPhoiDao.KtvLoad;
import dao.DieuPhoiDao.PhieuCard;
import enums.TrangThaiPhieu;
import util.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ điều phối & kiểm tra chất lượng của quản lý.
 */
public final class DieuPhoiService {
    private static final String DA_DOI = "Phiếu vừa được người khác thay đổi. Hãy tải lại danh sách rồi thao tác lại.";

    private final DieuPhoiDao dao = new DieuPhoiDao();

    // ---------------- đọc ----------------
    /** 4 cột Kanban chuẩn: Chờ sửa chữa, Đang sửa chữa, Chờ linh kiện, Hoàn tất. */
    public static final TrangThaiPhieu[] CAC_COT = {
            TrangThaiPhieu.CHO_SUA, TrangThaiPhieu.DANG_SUA,
            TrangThaiPhieu.CHO_LINH_KIEN, TrangThaiPhieu.HOAN_TAT
    };

    public List<PhieuCard> phieuTrenBang() throws SQLException { return dao.findByTrangThai(0, CAC_COT); }

    public List<KtvLoad> danhSachKtv() throws SQLException { return dao.findKtvVaTaiTrong(); }

    public List<PhieuCard> phieuChoKiemTra() throws SQLException {
        return dao.findByTrangThai(0, TrangThaiPhieu.DANG_SUA, TrangThaiPhieu.CHO_SUA);
    }

    public List<HangMuc> hangMuc(String maPhieu) throws SQLException { return dao.findHangMuc(maPhieu); }

    public String ghiChuKyThuat(String maPhieu) throws SQLException { return dao.findGhiChuKyThuat(maPhieu); }

    // ---------------- điều phối ----------------
    public void phanCong(String maPhieu, String maKTV, LocalDateTime duKien) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        if (p.trangThai() == TrangThaiPhieu.HOAN_TAT)
            throw new BusinessException("Phiếu đã hoàn tất, không thể thay đổi phân công.");
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
        if (p.trangThai() != TrangThaiPhieu.CHO_SUA)
            throw new BusinessException("Không thể bắt đầu sửa từ trạng thái \"" + p.trangThai() + "\". Chỉ phiếu \"Chờ sửa chữa\" mới bắt đầu sửa được.");
        if (p.maKTV() == null) throw new BusinessException("Hãy phân công kỹ thuật viên trước khi bắt đầu sửa.");
        if (dao.batDauSua(maPhieu) == 0) throw new BusinessException(DA_DOI);
    }

    public void huyPhieu(String maPhieu, String lyDo) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        lyDo = lyDo == null ? "" : lyDo.trim();
        if (lyDo.isEmpty()) throw new BusinessException("Vui lòng nhập lý do hủy phiếu.");
        if (lyDo.length() > 300) throw new BusinessException("Lý do hủy tối đa 300 ký tự.");
        if (p.trangThai() == TrangThaiPhieu.HOAN_TAT)
            throw new BusinessException("Phiếu đã hoàn tất nên không thể hủy.");
        if (dao.daXuatKho(maPhieu))
            throw new BusinessException("Phiếu này đã xuất phụ tùng từ kho nên không thể hủy. Hãy xử lý hoàn kho trước.");
        if (dao.huy(maPhieu, p.trangThai(), lyDo) == 0) throw new BusinessException(DA_DOI);
    }

    // ---------------- kiểm tra chất lượng ----------------
    /** Duyệt hoàn thành. */
    public void duyetHoanThanh(String maPhieu, String ghiChuQuanLy) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        if (p.trangThai() == TrangThaiPhieu.HOAN_TAT)
            throw new BusinessException("Phiếu này đã ở trạng thái Hoàn tất.");
        String note = noiGhiChu(dao.findGhiChuKyThuat(maPhieu), "QL duyệt", ghiChuQuanLy);
        if (dao.hoanThanh(maPhieu, note) == 0) throw new BusinessException(DA_DOI);
    }

    public void traVeLamLai(String maPhieu, String lyDo) throws SQLException {
        PhieuCard p = layPhieu(maPhieu);
        lyDo = lyDo == null ? "" : lyDo.trim();
        if (lyDo.isEmpty()) throw new BusinessException("Vui lòng nhập lý do trả về để kỹ thuật viên biết cần làm lại gì.");
        if (p.trangThai() == TrangThaiPhieu.HOAN_TAT)
            throw new BusinessException("Phiếu đã hoàn tất không thể trả về.");
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

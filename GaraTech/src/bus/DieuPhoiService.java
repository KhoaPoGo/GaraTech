package bus;

import dao.DieuPhoiDao;
import dao.DieuPhoiDao.HangMuc;
import dao.DieuPhoiDao.PhieuCard;
import enums.TrangThaiPhieu;
import java.sql.SQLException;
import java.util.List;

/** Chỉ đọc tiến độ của phiếu tiếp nhận-sửa chữa thống nhất. */
public final class DieuPhoiService {
    private final DieuPhoiDao dao = new DieuPhoiDao();
    public static final TrangThaiPhieu[] CAC_COT = TrangThaiPhieu.values();
    public List<PhieuCard> phieuTrenBang() throws SQLException { return dao.findByTrangThai(0, CAC_COT); }
    /** Schema hiện không có trạng thái KCS riêng; CHO_LINH_KIEN không được hiểu nhầm là chờ nghiệm thu. */
    public List<PhieuCard> phieuChoKiemTra() { return List.of(); }
    public List<HangMuc> hangMuc(String id) throws SQLException { return dao.findHangMuc(id); }
    public String ghiChuKyThuat(String id) throws SQLException { return dao.findGhiChuKyThuat(id); }
    public int demHangMucChuaXong(String id) { return dao.demHangMucChuaXong(id); }
    public void duyetHoanThanh(String id,String note) throws SQLException {
        if (dao.hoanThanh(id,note)==0) throw new BusinessException("Phiếu không còn ở trạng thái có thể hoàn tất.");
    }
    public void traVeLamLai(String id,String note) throws SQLException {
        if (note==null||note.isBlank()) throw new BusinessException("Vui lòng ghi lý do trả về.");
        if (dao.traVeLamLai(id,note.trim())==0) throw new BusinessException("Phiếu không còn ở trạng thái chờ linh kiện.");
    }
}

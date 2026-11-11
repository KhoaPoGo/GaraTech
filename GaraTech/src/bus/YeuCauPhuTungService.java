package bus;

import dao.PhieuXuatKhoDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Xử lý nghiệp vụ kỹ thuật viên gửi yêu cầu xuất phụ tùng. */
public final class YeuCauPhuTungService {

    private final PhieuXuatKhoDao phieuXuatKhoDao;

    public record ChiTietYeuCauInput(String maPhuTung, int soLuongXuat, String ghiChu) { }

    public YeuCauPhuTungService() {
        this(new PhieuXuatKhoDao());
    }

    public YeuCauPhuTungService(PhieuXuatKhoDao phieuXuatKhoDao) {
        if (phieuXuatKhoDao == null) {
            throw new IllegalArgumentException("DAO phiếu xuất kho không được null.");
        }
        this.phieuXuatKhoDao = phieuXuatKhoDao;
    }

    /**
     * Tạo phiếu xuất kho và toàn bộ dòng chi tiết.
     * PhieuXuatKhoDao.insertRequest tự kiểm tra kỹ thuật viên được phân công,
     * ghi dữ liệu trong transaction và không trừ tồn kho khi gửi yêu cầu.
     */
    public String guiYeuCauXuatPhuTung(
            String maPhieuSC,
            String maKTVYeuCau,
            String lyDoXuat,
            List<ChiTietYeuCauInput> danhSach
    ) throws SQLException {
        String phieuId = requireText(maPhieuSC, "Mã phiếu sửa chữa");
        String ktvId = requireText(maKTVYeuCau, "Mã kỹ thuật viên");
        String reason = requireText(lyDoXuat, "Lý do xuất");

        if (reason.length() > 200) {
            throw new IllegalArgumentException("Lý do xuất không được vượt quá 200 ký tự.");
        }
        if (danhSach == null || danhSach.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một phụ tùng.");
        }

        Set<String> ids = new HashSet<>();
        List<PhieuXuatKhoDao.ChiTietYeuCau> chiTiet = new ArrayList<>();
        for (ChiTietYeuCauInput item : danhSach) {
            if (item == null) throw new IllegalArgumentException("Chi tiết yêu cầu không được null.");
            String partId = requireText(item.maPhuTung(), "Mã phụ tùng");
            if (item.soLuongXuat() <= 0) {
                throw new IllegalArgumentException("Số lượng yêu cầu phải lớn hơn 0.");
            }
            if (!ids.add(partId.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Phụ tùng bị trùng trong cùng yêu cầu: " + partId);
            }
            String note = item.ghiChu() == null || item.ghiChu().isBlank()
                    ? null : item.ghiChu().trim();
            if (note != null && note.length() > 200) {
                throw new IllegalArgumentException("Ghi chú phụ tùng không được vượt quá 200 ký tự.");
            }
            chiTiet.add(new PhieuXuatKhoDao.ChiTietYeuCau(partId, item.soLuongXuat(), note));
        }

        return phieuXuatKhoDao.insertRequest(phieuId, ktvId, reason, chiTiet);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " không được để trống.");
        }
        return value.trim();
    }
}

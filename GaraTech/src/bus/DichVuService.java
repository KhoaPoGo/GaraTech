package bus;

import connectDB.ConnectDB;
import dao.DichVuDao;
import entity.DichVu;
import util.IdGenerator;

import java.sql.SQLException;
import java.util.List;

/** Nghiệp vụ danh mục dịch vụ & đơn giá (quản lý). Dịch vụ không bị xóa, chỉ chuyển sang "Ngừng cung cấp". */
public final class DichVuService {
    public static final List<String> CAP_DO = List.of("Cấp 1", "Cấp 2", "Cấp 3", "Cấp 4");

    private final DichVuDao dao = new DichVuDao();

    public List<DichVu> danhSach() throws SQLException { return dao.findAll(); }

    /** Thêm dịch vụ mới (trạng thái Hoạt động), trả về mã vừa tạo. */
    public String them(String ten, String moTa, double donGia, double gio, String capDo) throws SQLException {
        DichVu d = kiemTra(null, ten, moTa, donGia, gio, capDo, DichVuDao.HOAT_DONG);
        return ConnectDB.inTransaction(c -> {
            d.setMaDichVu(IdGenerator.next(c, IdGenerator.Kind.DICH_VU));
            dao.insert(c, d);
            return d.getMaDichVu();
        });
    }

    public void sua(String ma, String ten, String moTa, double donGia, double gio, String capDo) throws SQLException {
        DichVu d = kiemTra(ma, ten, moTa, donGia, gio, capDo, null);
        if (dao.update(d) == 0) throw new BusinessException("Dịch vụ " + ma + " không còn tồn tại.");
    }

    public void ngungCungCap(String ma, boolean ngung) throws SQLException {
        String tt = ngung ? DichVuDao.NGUNG : DichVuDao.HOAT_DONG;
        if (dao.setTrangThai(ma, tt) == 0) throw new BusinessException("Dịch vụ " + ma + " không còn tồn tại.");
    }

    private DichVu kiemTra(String ma, String ten, String moTa, double donGia, double gio, String capDo, String tt) throws SQLException {
        ten = ten == null ? "" : ten.trim();
        moTa = moTa == null ? "" : moTa.trim();
        if (ten.isEmpty()) throw new BusinessException("Vui lòng nhập tên dịch vụ.");
        if (ten.length() > 150) throw new BusinessException("Tên dịch vụ tối đa 150 ký tự.");
        if (moTa.length() > 500) throw new BusinessException("Mô tả tối đa 500 ký tự.");
        if (Double.isNaN(donGia) || donGia < 0) throw new BusinessException("Đơn giá công không được âm.");
        if (donGia > 1_000_000_000_000L) throw new BusinessException("Đơn giá công quá lớn.");
        if (Double.isNaN(gio) || gio <= 0) throw new BusinessException("Thời gian ước tính phải lớn hơn 0 giờ.");
        if (gio > 1000) throw new BusinessException("Thời gian ước tính quá lớn (tối đa 1000 giờ).");
        if (capDo != null && !capDo.isBlank() && !CAP_DO.contains(capDo))
            throw new BusinessException("Cấp độ bảo dưỡng không hợp lệ.");
        if (dao.existsByName(ten, ma)) throw new BusinessException("Đã có dịch vụ tên \"" + ten + "\".");
        return new DichVu(ma, ten, moTa, donGia, gio, capDo == null || capDo.isBlank() ? null : capDo, tt);
    }
}

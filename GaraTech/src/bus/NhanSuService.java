package bus;

import connectDB.ConnectDB;
import dao.NhanVienDao;
import dao.TaiKhoanQuanLyDao;
import dao.TaiKhoanQuanLyDao.TaiKhoanRow;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.Role;
import util.IdGenerator;
import util.PasswordUtil;
import util.Session;
import util.Validator;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Nghiệp vụ nhân sự & tài khoản (quản lý).
 * Quy tắc: không xóa nhân viên / tài khoản (chỉ cho nghỉ việc / khóa); không tự khóa mình;
 * không đổi vai trò hoặc cho nghỉ việc kỹ thuật viên đang có phiếu sửa chữa dở dang.
 */
public final class NhanSuService {
    private static final Pattern TEN_DANG_NHAP = Pattern.compile("^[A-Za-z0-9._]{5,50}$");

    private final NhanVienDao nvDao = new NhanVienDao();
    private final TaiKhoanQuanLyDao tkDao = new TaiKhoanQuanLyDao();

    // ---------------- Nhân viên ----------------
    public List<NhanVien> danhSachNhanVien() throws SQLException { return nvDao.findAll(); }

    /** Thêm nhân viên mới (đang làm việc). Trả về mã vừa tạo. */
    public String themNhanVien(String hoTen, String sdt, String email, String diaChi, Role vaiTro,
                               double heSo, long luongCoBan) throws SQLException {
        NhanVien n = kiemTra(null, hoTen, sdt, email, diaChi, vaiTro, heSo, luongCoBan, true);
        return ConnectDB.inTransaction(c -> {
            NhanVien moi = new NhanVien(IdGenerator.next(c, IdGenerator.Kind.NHAN_VIEN), n.getHoTen(), n.getSoDienThoai(),
                    n.getEmail(), n.getDiaChi(), n.getVaiTro(), n.getHeSoLuong(), n.getLuongCoBan(), true);
            nvDao.insert(c, moi);
            return moi.getMaNV();
        });
    }

    public void suaNhanVien(String maNV, String hoTen, String sdt, String email, String diaChi, Role vaiTro,
                            double heSo, long luongCoBan) throws SQLException {
        NhanVien cu = nvDao.findById(maNV);
        if (cu == null) throw new BusinessException("Nhân viên " + maNV + " không còn tồn tại.");
        NhanVien n = kiemTra(maNV, hoTen, sdt, email, diaChi, vaiTro, heSo, luongCoBan, cu.isDangLam());
        if (cu.getRole() != vaiTro) {
            if (maNV.equals(Session.maNV()))
                throw new BusinessException("Bạn không thể tự đổi vai trò của chính mình.");
            if (cu.getRole() == Role.KY_THUAT_VIEN) chanNeuConViec(maNV, "đổi vai trò");
        }
        nvDao.update(n);
    }

    /** Cho nghỉ việc / cho làm lại. Không xóa dữ liệu nhân viên. */
    public void datTrangThaiLamViec(String maNV, boolean dangLam) throws SQLException {
        if (!dangLam) {
            if (maNV.equals(Session.maNV())) throw new BusinessException("Bạn không thể tự cho mình nghỉ việc.");
            NhanVien cu = nvDao.findById(maNV);
            if (cu != null && cu.getRole() == Role.KY_THUAT_VIEN) chanNeuConViec(maNV, "cho nghỉ việc");
        }
        if (nvDao.setDangLam(maNV, dangLam) == 0) throw new BusinessException("Nhân viên " + maNV + " không còn tồn tại.");
    }

    private void chanNeuConViec(String maKTV, String hanhDong) throws SQLException {
        int n = nvDao.demPhieuDangGiao(maKTV);
        if (n > 0) throw new BusinessException("Không thể " + hanhDong + " vì kỹ thuật viên này đang phụ trách " + n
                + " phiếu sửa chữa chưa hoàn tất. Hãy phân công lại ở màn Điều phối trước.");
    }

    private NhanVien kiemTra(String maNV, String hoTen, String sdt, String email, String diaChi, Role vaiTro,
                             double heSo, long luong, boolean dangLam) throws SQLException {
        hoTen = hoTen == null ? "" : hoTen.trim();
        sdt = sdt == null ? "" : sdt.trim();
        email = email == null ? "" : email.trim();
        diaChi = diaChi == null ? "" : diaChi.trim();
        if (hoTen.isEmpty()) throw new BusinessException("Vui lòng nhập họ tên.");
        if (hoTen.length() > 100) throw new BusinessException("Họ tên tối đa 100 ký tự.");
        if (!Validator.phone(sdt)) throw new BusinessException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        if (!Validator.email(email)) throw new BusinessException("Email chưa đúng định dạng (ví dụ ten@garatech.vn).");
        if (email.length() > 100) throw new BusinessException("Email tối đa 100 ký tự.");
        if (diaChi.isEmpty()) throw new BusinessException("Vui lòng nhập địa chỉ.");
        if (diaChi.length() > 200) throw new BusinessException("Địa chỉ tối đa 200 ký tự.");
        if (vaiTro == null) throw new BusinessException("Vui lòng chọn vai trò.");
        if (Double.isNaN(heSo) || !Validator.heSoLuong(heSo)) throw new BusinessException("Hệ số lương phải từ 1,0 đến 5,0.");
        if (luong <= 0) throw new BusinessException("Lương cơ bản phải lớn hơn 0.");
        if (luong > Integer.MAX_VALUE) throw new BusinessException("Lương cơ bản quá lớn.");
        String trung = nvDao.findDuplicate(sdt, email, maNV);
        if (trung != null) throw new BusinessException(trung + " đã được nhân viên khác sử dụng.");
        return new NhanVien(maNV, hoTen, sdt, email, diaChi, vaiTro.dbValue(), Math.round(heSo * 100) / 100.0, (int) luong, dangLam);
    }

    // ---------------- Tài khoản ----------------
    public List<TaiKhoanRow> danhSachTaiKhoan() throws SQLException { return tkDao.findAll(); }

    public List<NhanVien> nhanVienChuaCoTaiKhoan() throws SQLException { return tkDao.findNhanVienChuaCoTaiKhoan(); }

    public void capTaiKhoan(String maNV, String tenDangNhap, String matKhau, String nhapLai) throws SQLException {
        tenDangNhap = tenDangNhap == null ? "" : tenDangNhap.trim();
        if (maNV == null) throw new BusinessException("Vui lòng chọn nhân viên.");
        if (!TEN_DANG_NHAP.matcher(tenDangNhap).matches())
            throw new BusinessException("Tên đăng nhập gồm 5–50 ký tự, chỉ dùng chữ không dấu, số, dấu chấm hoặc gạch dưới.");
        kiemTraMatKhau(matKhau, nhapLai);
        if (tkDao.existsTenDangNhap(tenDangNhap)) throw new BusinessException("Tên đăng nhập \"" + tenDangNhap + "\" đã có người dùng.");
        tkDao.insert(tenDangNhap, PasswordUtil.hash(matKhau), maNV);
    }

    public void datLaiMatKhau(String tenDangNhap, String matKhau, String nhapLai) throws SQLException {
        kiemTraMatKhau(matKhau, nhapLai);
        if (tkDao.updateMatKhau(tenDangNhap, PasswordUtil.hash(matKhau)) == 0)
            throw new BusinessException("Tài khoản không còn tồn tại.");
    }

    public void khoaTaiKhoan(String tenDangNhap, boolean khoa) throws SQLException {
        if (khoa && Session.user() != null && tenDangNhap.equalsIgnoreCase(Session.user().tenDangNhap()))
            throw new BusinessException("Bạn không thể tự khóa tài khoản đang đăng nhập.");
        int n = tkDao.updateTrangThai(tenDangNhap, khoa ? TaiKhoan.DANG_KHOA : TaiKhoan.HOAT_DONG);
        if (n == 0) throw new BusinessException("Tài khoản không còn tồn tại.");
    }

    private static void kiemTraMatKhau(String mk, String nhapLai) {
        if (mk == null || mk.length() < 8) throw new BusinessException("Mật khẩu phải có ít nhất 8 ký tự.");
        if (mk.length() > 64) throw new BusinessException("Mật khẩu tối đa 64 ký tự.");
        if (!mk.equals(nhapLai)) throw new BusinessException("Mật khẩu nhập lại không khớp.");
    }
}

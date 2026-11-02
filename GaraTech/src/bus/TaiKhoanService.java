package bus;

import connectDB.ConnectDB;
import dao.TaiKhoanDao;
import dao.TaiKhoanDao.LoginRow;
import enums.Role;
import enums.User;
import util.PasswordUtil;
import util.Session;

import java.sql.SQLException;

/** Nghiệp vụ đăng nhập. Gọi từ LoginFrame; thành công thì ghi vào Session. */
public final class TaiKhoanService {
    /** Dùng chung cho "không có tài khoản" và "sai mật khẩu" để không lộ tài khoản nào tồn tại. */
    private static final String SAI_THONG_TIN = "Tài khoản hoặc mật khẩu không đúng.";

    private TaiKhoanService() {}

    public static User login(String username, String password) throws AuthException {
        if (username == null || username.trim().isEmpty())
            throw new AuthException("Vui lòng nhập tài khoản nhân viên.");
        if (password == null || password.length() < 8)
            throw new AuthException("Mật khẩu chưa hợp lệ. Vui lòng nhập ít nhất 8 ký tự để tiếp tục.");

        String user = username.trim();
        try {
            LoginRow row = TaiKhoanDao.findByUsername(user);
            if (row == null || !PasswordUtil.verify(password, row.matKhau()))
                throw new AuthException(SAI_THONG_TIN);

            // Chỉ báo các lý do dưới đây SAU KHI mật khẩu đúng, tránh lộ trạng thái tài khoản cho người khác.
            if (!row.hoatDong())
                throw new AuthException("Tài khoản đã bị khóa. Liên hệ quản lý gara để được mở lại.");
            if (!row.dangLam())
                throw new AuthException("Nhân viên này đã nghỉ việc nên không thể đăng nhập.");
            if (row.user().role() == null)
                throw new AuthException("Tài khoản chưa được gán vai trò hợp lệ. Liên hệ quản lý gara.");

            Session.login(row.user(), false);
            return row.user();
        } catch (SQLException e) {
            if (ConnectDB.demoMode()) {
                User demo = demoLogin(user, password);
                if (demo == null) throw new AuthException(SAI_THONG_TIN);
                Session.login(demo, true);
                return demo;
            }
            throw new AuthException("Không kết nối được cơ sở dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Tài khoản demo, chỉ dùng khi KHÔNG kết nối được CSDL và app.demoMode=true.
     * Trùng với dữ liệu mẫu trong sql/02_ThemDuLieu.sql (cùng mã NV, tên, mật khẩu Garatech@123)
     * để dữ liệu ghi bởi tài khoản demo không vi phạm khóa ngoại khi sau này nối CSDL thật.
     */
    private static User demoLogin(String user, String pass) {
        if (!"Garatech@123".equals(pass)) return null;
        return switch (user.toLowerCase()) {
            case "quanly" -> new User("NV26001", "Nguyễn Văn An", Role.QUAN_LY, "quanly");
            case "tiepnhan" -> new User("NV26002", "Trần Thị Bình", Role.TIEP_NHAN, "tiepnhan");
            case "kythuat" -> new User("NV26003", "Lê Minh Cường", Role.KY_THUAT_VIEN, "kythuat");
            case "nvkho" -> new User("NV26004", "Phạm Thu Dung", Role.KHO, "nvkho");
            default -> null;
        };
    }
}

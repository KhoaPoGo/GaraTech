package enums;

/** Người dùng đang đăng nhập (thông tin lấy từ TaiKhoan + NhanVien). */
public record User(String maNV, String hoTen, Role role, String tenDangNhap) {}

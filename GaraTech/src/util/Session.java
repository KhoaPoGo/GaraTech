package util;

import enums.Role;
import enums.User;


/** Lưu người dùng đang đăng nhập. Mọi màn hình lấy vai trò / mã nhân viên từ đây. */
public final class Session {
    private static User user;
    private static boolean demo;

    private Session() {}

    public static void login(User u, boolean isDemo) { user = u; demo = isDemo; }
    public static void logout() { user = null; demo = false; }

    public static User user() { return user; }
    public static Role role() { return user == null ? null : user.role(); }
    public static String maNV() { return user == null ? null : user.maNV(); }
    public static boolean isDemo() { return demo; }
    public static boolean is(Role r) { return role() == r; }
}

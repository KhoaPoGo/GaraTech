package util;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Đổi lỗi SQL Server thành câu tiếng Việt dễ hiểu để hiện cho người dùng. */
public final class DbError {
    private static final Pattern DUP_VALUE = Pattern.compile("duplicate key value is \\((.*?)\\)");
    private static final Pattern CONSTRAINT = Pattern.compile("constraint \"?'?([A-Za-z0-9_]+)");

    private DbError() {}

    public static String friendly(SQLException e) {
        int code = e.getErrorCode();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (code == 2627 || code == 2601) {
            Matcher m = DUP_VALUE.matcher(msg);
            return "Dữ liệu bị trùng" + (m.find() ? " (" + m.group(1) + ")" : "")
                    + ". Vui lòng kiểm tra lại số điện thoại, email hoặc mã đã nhập.";
        }
        if (code == 547) {
            Matcher m = CONSTRAINT.matcher(msg);
            String name = m.find() ? m.group(1) : "";
            return switch (name) {
                case "CK_NV_heso" -> "Hệ số lương phải từ 1,0 đến 5,0.";
                case "CK_NV_sdt" -> "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.";
                case "CK_NV_luong" -> "Lương cơ bản phải lớn hơn 0.";
                case "CK_TK_ten" -> "Tên đăng nhập phải từ 5 ký tự và không có khoảng trắng.";
                case "CK_DV_gia" -> "Đơn giá công không được âm.";
                case "CK_DV_tg" -> "Thời gian ước tính phải lớn hơn 0.";
                case "CK_PSC_gio" -> "Thời gian hoàn thành không thể sớm hơn thời gian bắt đầu.";
                default -> msg.contains("REFERENCE")
                        ? "Dữ liệu này đang được sử dụng ở nơi khác nên không thể thay đổi."
                        : "Dữ liệu không hợp lệ so với ràng buộc của cơ sở dữ liệu" + (name.isEmpty() ? "." : " (" + name + ").");
            };
        }
        if (code == 1205) return "Hệ thống đang bận xử lý, vui lòng thử lại sau ít giây.";
        String state = e.getSQLState();
        if (state != null && state.startsWith("08") || msg.contains("TCP/IP") || msg.contains("Login failed"))
            return "Không kết nối được cơ sở dữ liệu: " + msg;
        return "Lỗi cơ sở dữ liệu: " + msg;
    }
}

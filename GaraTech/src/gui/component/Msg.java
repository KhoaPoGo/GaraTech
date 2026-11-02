package gui.component;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Hộp thoại thông báo thống nhất ("hiển thị thông báo rõ ràng khi thành công / lỗi" và
 * "bước xác nhận cho thao tác quan trọng" trong yêu cầu phi chức năng).
 */
public final class Msg {
    private Msg() {}

    public static void info(Component parent, String text) {
        JOptionPane.showMessageDialog(parent, text, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String text) {
        JOptionPane.showMessageDialog(parent, text, "Có lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Hỏi xác nhận trước khi lập hóa đơn, xuất kho, hủy phiếu, đổi dữ liệu quan trọng... */
    public static boolean confirm(Component parent, String text) {
        return JOptionPane.showConfirmDialog(parent, text, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}

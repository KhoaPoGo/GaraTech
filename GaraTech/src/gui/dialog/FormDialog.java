package gui.dialog;

import gui.component.Msg;
import gui.component.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Supplier;

/**
 * Hộp thoại nhập liệu dùng chung: dựng form nhãn + ô nhập, bấm Lưu thì chạy kiểm tra,
 * nếu có lỗi thì báo rồi mở lại để người dùng sửa (không mất dữ liệu đã gõ).
 */
public final class FormDialog {
    private FormDialog() {}

    /** Form hai cột (nhãn | ô nhập). */
    public static final class Form extends JPanel {
        private int row;

        public Form() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        public Form add(String label, JComponent field) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = row++;
            g.insets = new Insets(5, 0, 5, 12);
            g.anchor = GridBagConstraints.NORTHWEST;
            JLabel l = new JLabel(label);
            l.setFont(Theme.font(Font.PLAIN, 13));
            l.setForeground(Theme.MUTED);
            g.gridx = 0;
            add(l, g);
            g.gridx = 1;
            g.weightx = 1;
            g.insets = new Insets(5, 0, 5, 0);
            g.fill = GridBagConstraints.HORIZONTAL;
            add(field, g);
            return this;
        }

        /** Dòng chú thích nhỏ trải hết bề ngang. */
        public Form note(String text) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = row++;
            g.gridx = 0;
            g.gridwidth = 2;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(2, 0, 6, 0);
            JLabel l = new JLabel("<html><body style='width:340px'>" + text + "</body></html>");
            l.setFont(Theme.font(Font.PLAIN, 12));
            l.setForeground(Theme.MUTED);
            add(l, g);
            return this;
        }
    }

    /**
     * @param validate trả về null nếu hợp lệ, ngược lại là câu báo lỗi
     * @return true nếu người dùng bấm Lưu và dữ liệu hợp lệ
     */
    public static boolean show(Component parent, String title, JComponent form, Supplier<String> validate) {
        return show(parent, title, form, "Lưu", validate);
    }

    public static boolean show(Component parent, String title, JComponent form, String okText, Supplier<String> validate) {
        Object[] options = {okText, "Hủy"};
        while (true) {
            int r = JOptionPane.showOptionDialog(parent, form, title, JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (r != 0) return false;
            String err = validate == null ? null : validate.get();
            if (err == null) return true;
            Msg.error(parent, err);
        }
    }
}

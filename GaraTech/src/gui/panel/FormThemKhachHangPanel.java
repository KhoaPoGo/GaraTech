package gui.panel;

import bus.KhachHangXeBus;
import com.formdev.flatlaf.FlatClientProperties;
import entity.KhachHang;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Form thêm mới khách hàng.
 * Sau khi lưu thành công sẽ chuyển tiếp sang form thêm xe cho khách hàng vừa tạo.
 */
public final class FormThemKhachHangPanel extends JPanel {

    private final Consumer<KhachHang> onSaveAndProceed;
    private final Runnable onCancel;

    private final JTextField txtAddCustCode = new JTextField();
    private final JTextField txtAddCustName = new JTextField();
    private final JTextField txtAddCustPhone = new JTextField();
    private final JTextField txtAddCustEmail = new JTextField();
    private final JTextField txtAddCustAddress = new JTextField();
    private final JTextField txtAddCustPoints = new JTextField("0");

    public FormThemKhachHangPanel(Consumer<KhachHang> onSaveAndProceed, Runnable onCancel) {
        this.onSaveAndProceed = Objects.requireNonNull(onSaveAndProceed, "onSaveAndProceed must not be null");
        this.onCancel = Objects.requireNonNull(onCancel, "onCancel must not be null");

        setOpaque(false);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        card.add(UI.h2("Thông tin liên hệ"), BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(3, 2, 24, 12));
        formGrid.setOpaque(false);

        // Row 1
        formGrid.add(formFieldReadonly("Mã khách hàng", txtAddCustCode, "Hệ thống cấp mã khi lưu"));
        formGrid.add(formFieldInput("Họ tên *", txtAddCustName, null));

        // Row 2
        formGrid.add(formFieldInput("Số điện thoại *", txtAddCustPhone, "10 chữ số, bắt đầu bằng 0; không trùng"));
        formGrid.add(formFieldInput("Email", txtAddCustEmail, "Không bắt buộc"));

        // Row 3
        formGrid.add(formFieldInput("Địa chỉ", txtAddCustAddress, null));
        formGrid.add(formFieldInput("Điểm tích lũy", txtAddCustPoints, "Hệ thống quản lý · khách mới mặc định 0"));

        // Bottom Actions: Lưu và thêm xe | Hủy
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bot.setOpaque(false);

        JButton btnSaveAndAddVeh = UI.primaryButton("Lưu và thêm xe");
        JButton btnCancel = UI.secondaryButton("Hủy");

        btnSaveAndAddVeh.addActionListener(e -> handleSaveCustomerAndProceed());
        btnCancel.addActionListener(e -> onCancel.run());

        bot.add(btnSaveAndAddVeh);
        bot.add(btnCancel);
        bot.setAlignmentX(LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(formGrid);
        content.add(Box.createVerticalStrut(22));
        content.add(bot);

        card.add(content, BorderLayout.NORTH);
        add(card, BorderLayout.NORTH);
    }

    private void handleSaveCustomerAndProceed() {
        try {
            String hoTen = txtAddCustName.getText().trim();
            String phone = txtAddCustPhone.getText().trim();
            String email = txtAddCustEmail.getText().trim();
            String address = txtAddCustAddress.getText().trim();
            int points = 0;
            try {
                points = Integer.parseInt(txtAddCustPoints.getText().trim());
            } catch (NumberFormatException ignored) {}

            KhachHang newKh = new KhachHang(null, hoTen, phone, address, email, points);
            KhachHang savedKh = KhachHangXeBus.themKhachHang(newKh);

            Msg.info(this, "Đã tạo hồ sơ khách hàng " + savedKh.getHoTen() + " (" + savedKh.getMaKH() + "). Tiến hành thêm xe.");
            onSaveAndProceed.accept(savedKh);
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi lưu khách hàng: " + ex.getMessage());
        }
    }

    public void resetForm() {
        txtAddCustCode.setText("");
        txtAddCustName.setText("");
        txtAddCustPhone.setText("");
        txtAddCustEmail.setText("");
        txtAddCustAddress.setText("");
        txtAddCustPoints.setText("0");
        txtAddCustName.requestFocusInWindow();
    }

    public void setInitialQuery(String q) {
        if (q == null || q.isBlank()) return;
        String trimmed = q.trim();
        if (trimmed.matches("^[0-9+]+$") || trimmed.startsWith("0")) {
            txtAddCustPhone.setText(trimmed);
            txtAddCustName.requestFocusInWindow();
        } else {
            txtAddCustName.setText(trimmed);
            txtAddCustPhone.requestFocusInWindow();
        }
    }

    private JPanel formFieldInput(String label, JTextField tf, String hint) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setPreferredSize(new Dimension(200, 20));
        l.setMinimumSize(new Dimension(50, 20));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        styleInput(tf, null);
        tf.setAlignmentX(LEFT_ALIGNMENT);
        p.add(tf);

        p.add(Box.createVerticalStrut(4));
        JLabel h = new JLabel(hint != null && !hint.isEmpty() ? hint : " ");
        h.setFont(Theme.font(Font.PLAIN, 11));
        h.setForeground(hint != null && !hint.isEmpty() ? Theme.MUTED : new Color(0, 0, 0, 0));
        h.setPreferredSize(new Dimension(200, 16));
        h.setMinimumSize(new Dimension(50, 16));
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        h.setAlignmentX(LEFT_ALIGNMENT);
        p.add(h);

        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JPanel formFieldReadonly(String label, JTextField tf, String hint) {
        JPanel p = formFieldInput(label, tf, hint);
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setForeground(Theme.TEXT);
        return p;
    }

    private void styleInput(JTextField tf, String placeholder) {
        tf.setFont(Theme.font(Font.PLAIN, 13));
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        tf.setPreferredSize(new Dimension(200, 38));
        tf.setMinimumSize(new Dimension(50, 38));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        if (placeholder != null) {
            tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
    }
}

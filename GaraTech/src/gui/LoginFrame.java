package gui;

import bus.AuthException;
import bus.TaiKhoanService;
import enums.User;
import gui.component.Theme;
import gui.component.UI;
import util.Images;
import util.RoundedPanel;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

/** Màn hình đăng nhập (mockup "Hình 1"): trái giới thiệu, phải form; lỗi hiện ngay dưới ô nhập. */
public final class LoginFrame extends JFrame {
    private static final String PREF_USER = "rememberedUser";

    private final JTextField userField = new JTextField();
    private final JPasswordField passField = new JPasswordField();
    private final JCheckBox remember = new JCheckBox("Ghi nhớ tài khoản trên máy này");
    private final javax.swing.JButton loginBtn = UI.primaryButton("Đăng nhập");
    private final RoundedPanel errorBox = new RoundedPanel(10, Theme.DANGER_BG).borderColor(new Color(0xF2C4BF));
    private final javax.swing.JTextArea errorText = new javax.swing.JTextArea();
    private final Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);

    public LoginFrame() {
        setTitle("GaraTech – Đăng nhập");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 660));
        setLocationRelativeTo(null);

        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridx = 0; g.weightx = 0.42;
        add(buildLeft(), g);
        g.gridx = 1; g.weightx = 0.58;
        add(buildRight(), g);

        getRootPane().setDefaultButton(loginBtn);
        String saved = prefs.get(PREF_USER, "");
        if (!saved.isEmpty()) {
            userField.setText(saved);
            remember.setSelected(true);
        }
    }

    // ---------------- Bên trái: giới thiệu ----------------
    private JComponent buildLeft() {
        JPanel p = new JPanel();
        p.setBackground(Theme.NAVY);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(56, 56, 40, 48));

        RoundedPanel logoCard = new RoundedPanel(14, Color.WHITE).borderColor(null);
        logoCard.setLayout(new BorderLayout());
        logoCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ImageIcon logo = Images.icon("logo.png", 170, 130);
        logoCard.add(logo != null ? new JLabel(logo) : UI.label("GARATECH", Font.BOLD, 22, Theme.NAVY), BorderLayout.CENTER);
        logoCard.setAlignmentX(LEFT_ALIGNMENT);
        logoCard.setMaximumSize(new Dimension(200, 150));
        p.add(logoCard);
        p.add(Box.createVerticalStrut(44));

        p.add(left(UI.label("HỆ THỐNG QUẢN LÝ GARA Ô TÔ", Font.PLAIN, 11, Theme.YELLOW)));
        p.add(Box.createVerticalStrut(14));
        JLabel title = new JLabel("<html>Một luồng vận hành.<br>Mọi công việc rõ ràng.</html>");
        title.setFont(Theme.font(Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        p.add(left(title));
        p.add(Box.createVerticalStrut(18));
        JLabel desc = new JLabel("<html><body style='width:380px'>Không gian làm việc dành cho đội ngũ GaraTech. "
                + "Theo dõi xe, sửa chữa và phụ tùng trong cùng một hệ thống.</body></html>");
        desc.setFont(Theme.font(Font.PLAIN, 14));
        desc.setForeground(new Color(0xB8C4D6));
        p.add(left(desc));
        p.add(Box.createVerticalStrut(30));

        p.add(feature("1", "Tiếp nhận & kiểm tra", "Ghi nhận đúng khách hàng, đúng phương tiện."));
        p.add(Box.createVerticalStrut(16));
        p.add(feature("2", "Sửa chữa & kiểm soát", "Phân công rõ ràng, theo dõi tiến độ từng xe."));
        p.add(Box.createVerticalStrut(16));
        p.add(feature("3", "Thanh toán & bàn giao", "Hoàn tất kỹ thuật trước khi xuất hóa đơn."));

        p.add(Box.createVerticalGlue());
        p.add(left(UI.label("GaraTech · Chỉ dành cho nhân viên được cấp tài khoản", Font.PLAIN, 11, new Color(0x8FA0BA))));
        return p;
    }

    private JComponent feature(String num, String title, String desc) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        JLabel badge = new JLabel(num, JLabel.CENTER) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.NAVY_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(Theme.font(Font.BOLD, 14));
        badge.setForeground(Theme.YELLOW);
        badge.setPreferredSize(new Dimension(36, 36));
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.add(left(UI.label(title, Font.BOLD, 14, Color.WHITE)));
        txt.add(Box.createVerticalStrut(2));
        txt.add(left(UI.label(desc, Font.PLAIN, 12, new Color(0xB8C4D6))));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(badge, BorderLayout.NORTH);
        row.add(wrap, BorderLayout.WEST);
        row.add(txt, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return row;
    }

    // ---------------- Bên phải: form đăng nhập ----------------
    private JComponent buildRight() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG);

        RoundedPanel card = new RoundedPanel(14, Color.WHITE) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(430, super.getPreferredSize().height);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        card.add(left(UI.label("GaraTech xin chào!", Font.BOLD, 24, Theme.TEXT)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(UI.muted("Đăng nhập để bắt đầu ca làm việc của bạn.")));
        card.add(Box.createVerticalStrut(22));

        card.add(left(UI.label("Tài khoản nhân viên *", Font.BOLD, 12, Theme.TEXT)));
        card.add(Box.createVerticalStrut(6));
        userField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tài khoản được cấp");
        userField.putClientProperty(FlatClientProperties.STYLE, "margin:7,10,7,10");
        card.add(left(userField));
        card.add(Box.createVerticalStrut(16));

        card.add(left(UI.label("Mật khẩu *", Font.BOLD, 12, Theme.TEXT)));
        card.add(Box.createVerticalStrut(6));
        passField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; margin:7,10,7,10");
        card.add(left(passField));
        card.add(Box.createVerticalStrut(4));
        card.add(left(UI.label("Hiện / ẩn mật khẩu · Tối thiểu 8 ký tự", Font.PLAIN, 11, Theme.MUTED)));
        card.add(Box.createVerticalStrut(14));

        errorBox.setLayout(new BorderLayout());
        errorBox.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        errorText.setFont(Theme.font(Font.PLAIN, 12));
        errorText.setForeground(Theme.DANGER);
        errorText.setOpaque(false);
        errorText.setEditable(false);
        errorText.setFocusable(false);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorBox.add(errorText, BorderLayout.CENTER);
        errorBox.setVisible(false);
        card.add(left(errorBox));
        card.add(Box.createVerticalStrut(12));

        remember.setFont(Theme.font(Font.PLAIN, 12));
        remember.setOpaque(false);
        remember.setForeground(Theme.MUTED);
        card.add(left(remember));
        card.add(Box.createVerticalStrut(16));

        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.addActionListener(e -> doLogin());
        card.add(left(loginBtn));
        card.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        card.add(left(sep));
        card.add(Box.createVerticalStrut(14));
        JLabel note = new JLabel("<html>Vai trò được quản lý gán theo tài khoản.<br>"
                + "Bạn không thể tự chọn hoặc thay đổi quyền truy cập.</html>");
        note.setFont(Theme.font(Font.PLAIN, 11));
        note.setForeground(Theme.MUTED);
        card.add(left(note));
        card.add(Box.createVerticalStrut(14));
        card.add(left(UI.label("Không đăng nhập được? Liên hệ quản lý gara", Font.BOLD, 12, Theme.INFO)));

        p.add(card);
        return p;
    }

    private void doLogin() {
        hideError();
        final String u = userField.getText();
        final String pw = new String(passField.getPassword());
        loginBtn.setEnabled(false);
        loginBtn.setText("Đang đăng nhập…");

        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() throws Exception {
                return TaiKhoanService.login(u, pw);
            }

            @Override protected void done() {
                try {
                    User user = get();
                    if (remember.isSelected()) prefs.put(PREF_USER, user.tenDangNhap());
                    else prefs.remove(PREF_USER);
                    dispose();
                    new MainFrame().setVisible(true);
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    showError(c instanceof AuthException ? c.getMessage() : "Có lỗi xảy ra: " + c);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Đăng nhập");
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorBox.setVisible(true);
        if (msg.startsWith("Vui lòng nhập tài khoản")) {
            userField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        } else {
            passField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        }
        errorBox.getParent().revalidate();
        errorBox.getParent().repaint();
    }

    private void hideError() {
        errorBox.setVisible(false);
        userField.putClientProperty(FlatClientProperties.OUTLINE, null);
        passField.putClientProperty(FlatClientProperties.OUTLINE, null);
    }

    private static <T extends JComponent> T left(T c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }
}

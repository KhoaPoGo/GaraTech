package gui.component;

import enums.User;
import gui.Screen;
import util.Images;
import util.RoundedPanel;
import util.Session;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Thanh menu bên trái (nền navy). Menu hiện ra tùy vai trò đăng nhập. */
public final class SidebarPanel extends JPanel {
    private final Map<Screen, NavButton> buttons = new EnumMap<>(Screen.class);

    public SidebarPanel(List<Screen> screens, Consumer<Screen> onSelect, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(Theme.NAVY);
        setPreferredSize(new Dimension(245, 0));
        setBorder(BorderFactory.createEmptyBorder(24, 16, 18, 16));
        User u = Session.user();

        // ---- Trên: logo + vai trò ----
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        RoundedPanel logoCard = new RoundedPanel(14, Color.WHITE).borderColor(null);
        logoCard.setLayout(new BorderLayout());
        logoCard.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ImageIcon logo = Images.icon("logo.png", 160, 100);
        if (logo != null) logoCard.add(new JLabel(logo), BorderLayout.CENTER);
        else logoCard.add(UI.label("GARATECH", Font.BOLD, 18, Theme.NAVY), BorderLayout.CENTER);
        logoCard.setAlignmentX(LEFT_ALIGNMENT);
        logoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        top.add(logoCard);
        top.add(Box.createVerticalStrut(20));
        top.add(left(UI.label("KHÔNG GIAN LÀM VIỆC", Font.PLAIN, 10, new Color(0x8FA0BA))));
        top.add(Box.createVerticalStrut(4));
        top.add(left(UI.label(u.role().displayName(), Font.BOLD, 14, Color.WHITE)));
        top.add(Box.createVerticalStrut(18));
        add(top, BorderLayout.NORTH);

        // ---- Giữa: menu ----
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        for (Screen s : screens) {
            NavButton b = new NavButton(s.title());
            b.addActionListener(e -> onSelect.accept(s));
            b.setAlignmentX(LEFT_ALIGNMENT);
            buttons.put(s, b);
            menu.add(b);
            menu.add(Box.createVerticalStrut(4));
        }
        add(menu, BorderLayout.CENTER);

        // ---- Dưới: ca làm việc + người dùng ----
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        RoundedPanel shift = new RoundedPanel(12, Theme.NAVY_LIGHT).borderColor(null);
        shift.setLayout(new BoxLayout(shift, BoxLayout.Y_AXIS));
        shift.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        shift.add(UI.label("GARA TRUNG TÂM", Font.PLAIN, 10, new Color(0x8FA0BA)));
        shift.add(Box.createVerticalStrut(4));
        shift.add(UI.label("Ca ngày · 07:30 – 17:30", Font.BOLD, 12, Color.WHITE));
        shift.add(Box.createVerticalStrut(2));
        shift.add(UI.label("Hệ thống đang hoạt động", Font.PLAIN, 11, new Color(0xB8C4D6)));
        shift.setAlignmentX(LEFT_ALIGNMENT);
        shift.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        bottom.add(shift);
        bottom.add(Box.createVerticalStrut(18));
        bottom.add(left(UI.label(u.hoTen(), Font.BOLD, 13, Color.WHITE)));
        bottom.add(Box.createVerticalStrut(6));
        JButton logout = new JButton("Đăng xuất");
        logout.setContentAreaFilled(false);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setForeground(new Color(0xB8C4D6));
        logout.setFont(Theme.font(Font.PLAIN, 12));
        logout.setMargin(new java.awt.Insets(0, 0, 0, 0));
        logout.setHorizontalAlignment(JButton.LEFT);
        logout.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        logout.addActionListener(e -> onLogout.run());
        logout.setAlignmentX(LEFT_ALIGNMENT);
        bottom.add(logout);
        add(bottom, BorderLayout.SOUTH);
    }

    private static JComponent left(JComponent c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    public void select(Screen s) {
        buttons.forEach((k, b) -> b.setSelectedItem(k == s));
    }
}

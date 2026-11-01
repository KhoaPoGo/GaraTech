package gui;

import enums.User;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.SidebarPanel;
import gui.component.StatusBadge;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.Session;


import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Cửa sổ chính: sidebar (theo vai trò) + thanh trên + vùng nội dung CardLayout.
 * Mỗi màn hình được tạo LẦN ĐẦU khi người dùng mở nó, sau đó giữ lại; mỗi lần mở gọi panel.onShow().
 */
public final class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Screen, BasePanel> panels = new EnumMap<>(Screen.class);
    private final SidebarPanel sidebar;
    private final JLabel crumb = UI.label("", Font.PLAIN, 13, Theme.MUTED);

    public MainFrame() {
        User u = Session.user();
        setTitle("GaraTech – Quản lý gara ô tô");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1360, 840);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        Navigator.setHandler(this::show);
        List<Screen> menu = Screen.forRole(u.role());
        sidebar = new SidebarPanel(menu, this::show, this::logout);

        content.setBackground(Theme.BG);
        JPanel right = new JPanel(new BorderLayout());
        right.add(buildTopBar(u), BorderLayout.NORTH);
        right.add(content, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        show(menu.get(0));
    }

    private JComponent buildTopBar(User u) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 28, 14, 24)));
        bar.add(crumb, BorderLayout.WEST);

        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        r.setOpaque(false);
        if (Session.isDemo()) {
            StatusBadge demo = new StatusBadge("Chế độ demo – chưa nối CSDL");
            r.add(demo);
        }
        r.add(UI.label(Fmt.weekdayDate(LocalDate.now()), Font.PLAIN, 13, Theme.MUTED));
        r.add(new Avatar(u.hoTen().substring(0, 1).toUpperCase()));
        bar.add(r, BorderLayout.EAST);
        return bar;
    }

    /** Chuyển sang một màn hình (có kiểm tra quyền). */
    private void show(Screen s) {
        if (!s.allows(Session.role())) {
            Msg.error(this, "Bạn không có quyền truy cập chức năng này.");
            return;
        }
        BasePanel p = panels.get(s);
        if (p == null) {
            p = s.create();
            JScrollPane sp = new JScrollPane(p);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(24);
            sp.getViewport().setBackground(Theme.BG);
            content.add(sp, s.name());
            panels.put(s, p);
        }
        cards.show(content, s.name());
        p.onShow();
        sidebar.select(s);
        crumb.setText("GaraTech  /  " + s.title());
    }

    private void logout() {
        if (!Msg.confirm(this, "Bạn có chắc muốn đăng xuất?")) return;
        Session.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

    /** Hình tròn chứa chữ cái đầu của tên người dùng. */
    private static final class Avatar extends JComponent {
        private final String letter;

        Avatar(String letter) {
            this.letter = letter;
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.INFO_BG);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.INFO);
            g2.setFont(Theme.font(Font.BOLD, 12));
            int w = g2.getFontMetrics().stringWidth(letter);
            g2.drawString(letter, (getWidth() - w) / 2, getHeight() / 2 + 4);
            g2.dispose();
        }
    }
}

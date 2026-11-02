package gui.panel;

import gui.Navigator;
import gui.Screen;
import gui.component.BarChartPanel;
import gui.component.BasePanel;
import gui.component.ProgressCellRenderer;
import gui.component.StatCard;
import gui.component.StatusBadge;
import gui.component.StatusCellRenderer;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.RoundedPanel;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

/**
 * Màn "Tổng quan quản lý" (mockup "Hình 2") — làm MẪU để cả nhóm xem cách dùng BasePanel và các component chung.
 * Số liệu bên dưới là DỮ LIỆU GIẢ; SV1 sẽ thay bằng truy vấn thật (DAO) ở ngày 4.
 */
public final class DashBoardPanel extends BasePanel {
    private final DefaultTableModel tableModel =
            UI.readOnlyModel("Phiếu / phương tiện", "Khách hàng", "Kỹ thuật viên", "Trạng thái", "Tiến độ", "Hẹn hoàn thành");

    public DashBoardPanel() {
        super("Điều hành gara", "Tổng quan quản lý", "Nắm bắt hoạt động gara và các công việc cần ưu tiên trong ngày.");

        actions.add(UI.secondaryButton(Fmt.date(LocalDate.now())));
        JButton go = UI.primaryButton("Điều phối công việc");
        go.addActionListener(e -> Navigator.go(Screen.DIEU_PHOI));
        actions.add(go);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new java.awt.Insets(0, 0, 18, 0);

        g.gridy = 0; content.add(statRow(), g);
        g.gridy = 1; content.add(middleRow(), g);
        g.gridy = 2; content.add(activeTicketsCard(), g);
        g.gridy = 3; g.insets = new java.awt.Insets(0, 0, 0, 0);
        content.add(shortcutRow(), g);

        body.add(content, BorderLayout.NORTH);
    }

    @Override
    public void onShow() {
        // TODO(SV1): nạp số liệu thật từ CSDL ở đây (nên chạy bằng SwingWorker).
    }

    private JComponent statRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(new StatCard("Xe tiếp nhận hôm nay", "12", "8 khách cũ · 4 khách mới"));
        row.add(new StatCard("Đang sửa chữa", "8", "2 xe đến hạn trong hôm nay"));
        row.add(new StatCard("Hoàn thành kỹ thuật", "5", "3 xe đã bàn giao · 2 chờ thanh toán"));
        row.add(new StatCard("Doanh thu đã thu", "18,6 tr", "Không gồm 6,76 tr chưa thanh toán"));
        return row;
    }

    private JComponent middleRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridy = 0;
        g.gridx = 0; g.weightx = 0.66; g.insets = new java.awt.Insets(0, 0, 0, 16);
        row.add(revenueCard(), g);
        g.gridx = 1; g.weightx = 0.34; g.insets = new java.awt.Insets(0, 0, 0, 0);
        row.add(alertCard(), g);
        return row;
    }

    private JComponent revenueCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UI.h2("Doanh thu đã thu · 7 ngày"), BorderLayout.WEST);
        head.add(link("Xem báo cáo →", () -> Navigator.go(Screen.BAO_CAO)), BorderLayout.EAST);

        JPanel sum = new JPanel();
        sum.setOpaque(false);
        sum.setLayout(new BoxLayout(sum, BoxLayout.Y_AXIS));
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.add(UI.label(Fmt.money(112_400_000L), Font.BOLD, 24, Theme.TEXT), BorderLayout.WEST);
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(new StatusBadge("Đã thanh toán") {{ setText("+12,4% so với kỳ trước"); }});
        line.add(badgeWrap, BorderLayout.EAST);
        line.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = UI.muted("30/09 – 06/10/2026 · Đơn vị: triệu đồng");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        sum.add(line);
        sum.add(Box.createVerticalStrut(2));
        sum.add(sub);

        BarChartPanel chart = new BarChartPanel();
        chart.setData(
                new String[]{"30/09", "01/10", "02/10", "03/10", "04/10", "05/10", "06/10"},
                new double[]{12.4, 16.8, 14.2, 20.1, 13.7, 16.6, 18.6},
                new String[]{"12.4tr", "16.8tr", "14.2tr", "20.1tr", "13.7tr", "16.6tr", "18.6tr"});

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(sum, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JComponent alertCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UI.h2("Cần xử lý hôm nay"), BorderLayout.WEST);
        head.add(link("Xem tất cả", () -> Navigator.go(Screen.PHU_TUNG_TON_KHO)), BorderLayout.EAST);
        head.setAlignmentX(LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        card.add(head);
        card.add(Box.createVerticalStrut(12));

        RoundedPanel warn = new RoundedPanel(10, Theme.WARN_BG).borderColor(new Color(0xF5DFA0));
        warn.setLayout(new BorderLayout());
        warn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel wt = new JLabel("<html><body style='width:220px'>3 phụ tùng chạm mức tối thiểu; 1 phụ tùng đã hết hàng.</body></html>");
        wt.setFont(Theme.font(Font.PLAIN, 12));
        wt.setForeground(Theme.WARN);
        warn.add(wt, BorderLayout.CENTER);
        warn.setAlignmentX(LEFT_ALIGNMENT);
        warn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        card.add(warn);
        card.add(Box.createVerticalStrut(12));

        card.add(stockRow("Lọc dầu OF-01", "Tồn 2 / Tối thiểu 5"));
        card.add(stockRow("Má phanh BP-02", "Tồn 1 / Tối thiểu 3"));
        card.add(stockRow("Bugi SP-04", "Tồn 0 / Tối thiểu 4"));
        card.add(Box.createVerticalStrut(10));

        JButton open = UI.secondaryButton("Mở cảnh báo tồn kho");
        open.addActionListener(e -> Navigator.go(Screen.PHU_TUNG_TON_KHO));
        open.setAlignmentX(LEFT_ALIGNMENT);
        card.add(open);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(10));
        JLabel foot = UI.label("Kho cập nhật lần cuối lúc 10:15", Font.PLAIN, 11, Theme.MUTED);
        foot.setAlignmentX(LEFT_ALIGNMENT);
        card.add(foot);
        return card;
    }

    private JComponent stockRow(String name, String qty) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        r.add(UI.label(name, Font.PLAIN, 12, Theme.MUTED), BorderLayout.WEST);
        r.add(UI.label(qty, Font.BOLD, 12, Theme.TEXT), BorderLayout.EAST);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return r;
    }

    private JComponent activeTicketsCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UI.h2("Phiếu sửa chữa đang hoạt động"), BorderLayout.WEST);
        head.add(link("Điều phối & tiến độ →", () -> Navigator.go(Screen.DIEU_PHOI)), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Dữ liệu giả — thay bằng PhieuSuaChuaDAO.findActive() khi làm thật
        addRow("PSC-20261006-001", "51H-12345 · Toyota Vios", "Nguyễn Minh Anh", "Nguyễn Văn Nam", "Đang sửa chữa", 65, "06/10 · 16:30");
        addRow("PSC-20261006-002", "51G-67890 · Honda City", "Trần Quốc Bảo", "Lê Đức Hùng", "Chờ kiểm tra", 100, "06/10 · 15:00");
        addRow("PSC-20261006-003", "60A-24680 · Mazda 3", "Lê Hoàng Lan", "Phạm Tuấn Đạt", "Đã xác nhận", 0, "07/10 · 10:00");
        addRow("PSC-20261005-012", "51F-45678 · Ford Ranger", "Vũ Đức Long", "Nguyễn Văn Nam", "Đang sửa chữa", 80, "06/10 · 17:00");

        JTable t = UI.table(tableModel);
        t.setRowHeight(58);
        t.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        t.getColumnModel().getColumn(4).setCellRenderer(new ProgressCellRenderer());
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(4).setPreferredWidth(170);
        card.add(UI.tableBlock(t), BorderLayout.CENTER);
        return card;
    }

    private void addRow(String phieu, String xe, String kh, String ktv, String tt, int pct, String han) {
        String two = "<html><b>" + phieu + "</b><br><span style='color:#6B7A90'>" + xe + "</span></html>";
        tableModel.addRow(new Object[]{two, kh, ktv, tt, pct, han});
    }

    private JComponent shortcutRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        JButton a = UI.secondaryButton("Kiểm tra chất lượng · 2 xe");
        a.addActionListener(e -> Navigator.go(Screen.KIEM_TRA_CL));
        JButton b = UI.secondaryButton("Báo cáo trong ngày");
        b.addActionListener(e -> Navigator.go(Screen.BAO_CAO));
        JButton c = UI.secondaryButton("Quản lý nhân sự");
        c.addActionListener(e -> Navigator.go(Screen.DICH_VU_NHAN_SU));
        row.add(a); row.add(b); row.add(c);
        return row;
    }

    private static JButton link(String text, Runnable r) {
        JButton b = new JButton(text);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(Theme.INFO);
        b.setFont(Theme.font(Font.BOLD, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> r.run());
        return b;
    }
}

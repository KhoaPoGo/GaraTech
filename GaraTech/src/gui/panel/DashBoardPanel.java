package gui.panel;

import bus.BaoCaoService;
import bus.BaoCaoService.Cot;
import bus.BaoCaoService.DashboardData;
import dao.BaoCaoDao.TonKhoThap;
import dao.DieuPhoiDao.PhieuCard;
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
import util.Async;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Màn "Tổng quan quản lý" (mockup "Hình 2"): số liệu thật từ CSDL, nạp lại mỗi lần mở màn hình.
 * Mọi truy vấn nằm trong {@link BaoCaoService#dashboard()}.
 */
public final class DashBoardPanel extends BasePanel {
    private final BaoCaoService service = new BaoCaoService();

    private final DefaultTableModel tableModel =
            UI.readOnlyModel("Phiếu / phương tiện", "Khách hàng", "Kỹ thuật viên", "Trạng thái", "Tiến độ", "Hẹn hoàn thành");

    private final StatCard cTiepNhan = new StatCard("Xe tiếp nhận hôm nay", "—", " ");
    private final StatCard cDangSua = new StatCard("Đang sửa chữa", "—", " ");
    private final StatCard cHoanThanh = new StatCard("Hoàn thành kỹ thuật", "—", " ");
    private final StatCard cDoanhThu = new StatCard("Doanh thu đã thu", "—", " ");

    private final JLabel lbTong7 = UI.label("—", Font.BOLD, 24, Theme.TEXT);
    private final StatusBadge badgeSoSanh = new StatusBadge("");
    private final JLabel lbKy = UI.muted(" ");
    private final BarChartPanel chart = new BarChartPanel();

    private final JPanel alertBody = new JPanel();
    private final JLabel lbCapNhat = UI.label(" ", Font.PLAIN, 11, Theme.MUTED);

    public DashBoardPanel() {
        super("Điều hành gara", "Tổng quan quản lý", "Nắm bắt hoạt động gara và các công việc cần ưu tiên trong ngày.");

        actions.add(UI.secondaryButton(Fmt.date(LocalDate.now())));
        JButton refresh = UI.secondaryButton("Làm mới");
        refresh.addActionListener(e -> reload());
        actions.add(refresh);
        JButton go = UI.primaryButton("Theo dõi tiến độ");
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
    public void onShow() { reload(); }

    private void reload() {
        Async.run(this, service::dashboard, this::show);
    }

    // ====================================================================== đổ dữ liệu
    private void show(DashboardData d) {
        cTiepNhan.setValue(String.valueOf(d.tiepNhanHomNay()), d.phieuChoXuLy() + " phiếu tiếp nhận đang chờ xử lý");
        cDangSua.setValue(String.valueOf(d.dangSua()), d.quaHan() > 0 ? d.quaHan() + " phiếu đã quá hạn dự kiến" : "Không có phiếu quá hạn");
        cHoanThanh.setValue(String.valueOf(d.hoanThanhHomNay() + d.daBanGiaoHomNay()),
                d.daBanGiaoHomNay() + " đã bàn giao · " + d.hoanThanhHomNay() + " chờ bàn giao");
        cDoanhThu.setValue(Fmt.trieu(d.thuHomNay()), "Chưa thu: " + Fmt.trieu(d.conChoThu()));

        lbTong7.setText(Fmt.money(d.tong7Ngay()));
        lbKy.setText(DateTimeFormatter.ofPattern("dd/MM").format(d.tuNgay()) + " – " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(d.denNgay())
                + " · Đơn vị: triệu đồng");
        if (d.tong7NgayTruoc() > 0) {
            double pct = (d.tong7Ngay() - d.tong7NgayTruoc()) * 100.0 / d.tong7NgayTruoc();
            badgeSoSanh.setStatus(String.format(Locale.ROOT, "%+.1f%% so với kỳ trước", pct).replace('.', ','));
        } else {
            badgeSoSanh.setStatus("Chưa có số liệu kỳ trước");
        }
        List<Cot> cols = d.bayNgay();
        String[] nhan = new String[cols.size()];
        double[] gt = new double[cols.size()];
        String[] ghi = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            nhan[i] = cols.get(i).nhan();
            gt[i] = cols.get(i).giaTri() / 1_000_000.0;
            ghi[i] = cols.get(i).giaTri() == 0 ? "0" : String.format(Locale.ROOT, "%.1ftr", gt[i]).replace('.', ',');
        }
        chart.setData(nhan, gt, ghi);

        fillAlerts(d);
        fillActive(d.phieuDangHoatDong());
        lbCapNhat.setText("Cập nhật lúc " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        revalidate();
        repaint();
    }

    private void fillAlerts(DashboardData d) {
        alertBody.removeAll();
        int cham = d.khoCanhBao().chamNguong(), het = d.khoCanhBao().hetHang();
        boolean ok = cham + het == 0;
        RoundedPanel warn = ok
                ? new RoundedPanel(10, Theme.SUCCESS_BG).borderColor(new Color(0xBFE6D1))
                : new RoundedPanel(10, Theme.WARN_BG).borderColor(new Color(0xF5DFA0));
        warn.setLayout(new BorderLayout());
        warn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        String text = ok ? "Tồn kho ổn định, chưa có phụ tùng nào chạm mức tối thiểu."
                : cham + " phụ tùng chạm mức tối thiểu; " + het + " phụ tùng đã hết hàng.";
        JLabel wt = new JLabel("<html><body style='width:220px'>" + text + "</body></html>");
        wt.setFont(Theme.font(Font.PLAIN, 12));
        wt.setForeground(ok ? Theme.SUCCESS : Theme.WARN);
        warn.add(wt, BorderLayout.CENTER);
        warn.setAlignmentX(LEFT_ALIGNMENT);
        warn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        alertBody.add(warn);
        alertBody.add(Box.createVerticalStrut(12));
        for (TonKhoThap t : d.khoCanhBao().top())
            alertBody.add(stockRow(t.ten(), "Tồn " + t.ton() + " / Tối thiểu " + t.toiThieu()));
        alertBody.revalidate();
        alertBody.repaint();
    }

    private void fillActive(List<PhieuCard> ds) {
        tableModel.setRowCount(0);
        LocalDateTime now = LocalDateTime.now();
        for (PhieuCard p : ds) {
            String two = "<html><b>" + p.maPhieuSC() + "</b><br><span style='color:#6B7A90'>" + p.bienSo() + " · " + p.tenXe() + "</span></html>";
            String han = p.duKien() == null ? "Chưa đặt hạn"
                    : DateTimeFormatter.ofPattern("dd/MM · HH:mm").format(p.duKien()) + (p.quaHan(now) ? " (quá hạn)" : "");
            tableModel.addRow(new Object[]{two, p.tenKhach(), p.tenKTV() == null ? "Chưa phân công" : p.tenKTV(),
                    p.trangThai().label(), p.tienDo(), han});
        }
    }

    // ====================================================================== khối giao diện
    private JComponent statRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(cTiepNhan);
        row.add(cDangSua);
        row.add(cHoanThanh);
        row.add(cDoanhThu);
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
        line.add(lbTong7, BorderLayout.WEST);
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badgeSoSanh);
        line.add(badgeWrap, BorderLayout.EAST);
        line.setAlignmentX(LEFT_ALIGNMENT);
        lbKy.setAlignmentX(LEFT_ALIGNMENT);
        sum.add(line);
        sum.add(Box.createVerticalStrut(2));
        sum.add(lbKy);

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

        alertBody.setOpaque(false);
        alertBody.setLayout(new BoxLayout(alertBody, BoxLayout.Y_AXIS));
        alertBody.setAlignmentX(LEFT_ALIGNMENT);
        card.add(alertBody);
        card.add(Box.createVerticalStrut(10));

        JButton open = UI.secondaryButton("Mở cảnh báo tồn kho");
        open.addActionListener(e -> Navigator.go(Screen.PHU_TUNG_TON_KHO));
        open.setAlignmentX(LEFT_ALIGNMENT);
        card.add(open);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(10));
        lbCapNhat.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lbCapNhat);
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

        JTable t = UI.table(tableModel);
        t.setRowHeight(58);
        t.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        t.getColumnModel().getColumn(4).setCellRenderer(new ProgressCellRenderer());
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(4).setPreferredWidth(170);
        card.add(UI.tableBlock(t), BorderLayout.CENTER);
        return card;
    }

    private JComponent shortcutRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        JButton b = UI.secondaryButton("Báo cáo trong ngày");
        b.addActionListener(e -> Navigator.go(Screen.BAO_CAO));
        JButton c = UI.secondaryButton("Quản lý nhân sự");
        c.addActionListener(e -> Navigator.go(Screen.DICH_VU_NHAN_SU));
        JButton d = UI.secondaryButton("Quản lý dịch vụ");
        d.addActionListener(e -> Navigator.go(Screen.QUAN_LY_DICH_VU));
        row.add(b); row.add(c); row.add(d);
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

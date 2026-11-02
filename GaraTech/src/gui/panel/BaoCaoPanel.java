package gui.panel;

import bus.BaoCaoService;
import bus.BaoCaoService.BaoCao;
import bus.BaoCaoService.Cot;
import dao.BaoCaoDao.DongThongKe;
import gui.component.BarChartPanel;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.ProgressCellRenderer;
import gui.component.StatCard;
import gui.component.Theme;
import gui.component.UI;
import util.Async;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Màn "Báo cáo & thống kê" (quản lý): chọn khoảng ngày, xem thực thu so với còn chờ thu,
 * biểu đồ thực thu, cơ cấu doanh thu theo dịch vụ và nhóm phụ tùng tiêu thụ.
 */
public final class BaoCaoPanel extends BasePanel {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final BaoCaoService service = new BaoCaoService();

    private final JTextField tfTu = new JTextField(9);
    private final JTextField tfDen = new JTextField(9);

    private final StatCard cThu = new StatCard("Thực thu", "—", " ");
    private final StatCard cCho = new StatCard("Còn chờ thu", "—", " ");
    private final StatCard cHd = new StatCard("Hóa đơn đã lập", "—", " ");
    private final StatCard cXong = new StatCard("Phiếu hoàn thành", "—", " ");

    private final JLabel chartTitle = UI.h2("Thực thu theo ngày");
    private final JLabel chartSub = UI.muted(" ");
    private final BarChartPanel chart = new BarChartPanel();

    private final DefaultTableModel dvModel = UI.readOnlyModel("Dịch vụ", "Số lần", "Doanh thu", "Tỷ trọng");
    private final DefaultTableModel ptModel = UI.readOnlyModel("Nhóm phụ tùng", "Số lượng", "Doanh thu", "Tỷ trọng");
    private final JLabel dvEmpty = UI.muted(" ");
    private final JLabel ptEmpty = UI.muted(" ");

    public BaoCaoPanel() {
        super("Điều hành gara", "Báo cáo & thống kê",
                "Doanh thu thực thu, khoản còn chờ thu, cơ cấu dịch vụ và phụ tùng tiêu thụ theo khoảng thời gian.");

        LocalDate hom = LocalDate.now();
        tfTu.setText(hom.withDayOfMonth(1).format(D));
        tfDen.setText(hom.format(D));
        tfTu.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");
        tfDen.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UI.muted("Từ ngày"));
        bar.add(tfTu);
        bar.add(UI.muted("đến ngày"));
        bar.add(tfDen);
        JButton xem = UI.primaryButton("Xem báo cáo");
        xem.addActionListener(e -> load());
        bar.add(xem);
        bar.add(quick("7 ngày qua", () -> set(hom.minusDays(6), hom)));
        bar.add(quick("Tháng này", () -> set(hom.withDayOfMonth(1), hom)));
        bar.add(quick("Tháng trước", () -> {
            LocalDate dau = hom.minusMonths(1).withDayOfMonth(1);
            set(dau, dau.withDayOfMonth(dau.lengthOfMonth()));
        }));

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(cThu);
        stats.add(cCho);
        stats.add(cHd);
        stats.add(cXong);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new java.awt.Insets(0, 0, 18, 0);
        g.gridy = 0; content.add(bar, g);
        g.gridy = 1; content.add(stats, g);
        g.gridy = 2; content.add(chartCard(), g);
        g.gridy = 3; g.insets = new java.awt.Insets(0, 0, 0, 0);
        JPanel two = new JPanel(new GridLayout(1, 2, 16, 0));
        two.setOpaque(false);
        two.add(listCard("Cơ cấu doanh thu theo dịch vụ", dvModel, dvEmpty));
        two.add(listCard("Nhóm phụ tùng tiêu thụ", ptModel, ptEmpty));
        content.add(two, g);
        body.add(content, BorderLayout.NORTH);
    }

    @Override
    public void onShow() { load(); }

    private JButton quick(String text, Runnable r) {
        JButton b = UI.secondaryButton(text);
        b.addActionListener(e -> r.run());
        return b;
    }

    private void set(LocalDate from, LocalDate to) {
        tfTu.setText(from.format(D));
        tfDen.setText(to.format(D));
        load();
    }

    private void load() {
        LocalDate from, to;
        try {
            from = LocalDate.parse(tfTu.getText().trim(), D);
            to = LocalDate.parse(tfDen.getText().trim(), D);
        } catch (DateTimeParseException ex) {
            Msg.error(this, "Ngày phải theo dạng dd/MM/yyyy (ví dụ 01/11/2026) và là ngày có thật.");
            return;
        }
        LocalDate f = from, t = to;
        Async.run(this, () -> service.baoCao(f, t), r -> show(r, f, t));
    }

    private void show(BaoCao r, LocalDate from, LocalDate to) {
        String ky = Fmt.date(from) + " – " + Fmt.date(to);
        cThu.setValue(Fmt.money(r.thucThu()), "Giao dịch đã thanh toán · " + ky);
        cCho.setValue(Fmt.money(r.conChoThu()), "Hóa đơn lập trong kỳ, chưa thu đủ");
        cHd.setValue(String.valueOf(r.soHoaDon()), "Không tính hóa đơn đã hủy");
        cXong.setValue(String.valueOf(r.soPhieuHoanThanh()), "Phiếu sửa chữa hoàn thành trong kỳ");

        chartTitle.setText("Thực thu theo " + r.donViCot());
        chartSub.setText(ky + " · Đơn vị: triệu đồng");
        List<Cot> cols = r.bieuDo();
        String[] nhan = new String[cols.size()];
        double[] gt = new double[cols.size()];
        String[] ghi = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            nhan[i] = cols.get(i).nhan();
            gt[i] = cols.get(i).giaTri() / 1_000_000.0;
            ghi[i] = cols.get(i).giaTri() == 0 ? "0" : String.format(Locale.ROOT, "%.1f", gt[i]).replace('.', ',');
        }
        chart.setData(nhan, gt, ghi);

        fill(dvModel, r.dichVu(), dvEmpty, "Chưa có hóa đơn nào có dịch vụ trong kỳ này.");
        fill(ptModel, r.phuTung(), ptEmpty, "Chưa có phụ tùng nào được bán trong kỳ này.");
        revalidate();
        repaint();
    }

    private static void fill(DefaultTableModel m, List<DongThongKe> ds, JLabel empty, String emptyText) {
        m.setRowCount(0);
        long tong = ds.stream().mapToLong(DongThongKe::doanhThu).sum();
        for (DongThongKe d : ds) {
            int pct = tong == 0 ? 0 : (int) Math.round(d.doanhThu() * 100.0 / tong);
            m.addRow(new Object[]{d.ten(), d.soLuong(), Fmt.money(d.doanhThu()), pct});
        }
        empty.setText(ds.isEmpty() ? emptyText : " ");
    }

    // ====================================================================== khối giao diện
    private JComponent chartCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(chartTitle, BorderLayout.WEST);
        head.add(chartSub, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JComponent listCard(String title, DefaultTableModel model, JLabel empty) {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
        card.add(UI.h2(title), BorderLayout.NORTH);
        JTable t = UI.table(model);
        t.setRowHeight(40);
        t.getColumnModel().getColumn(0).setPreferredWidth(200);
        t.getColumnModel().getColumn(3).setCellRenderer(new ProgressCellRenderer());
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        t.getColumnModel().getColumn(1).setCellRenderer(right);
        t.getColumnModel().getColumn(2).setCellRenderer(right);
        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        mid.add(UI.tableBlock(t), BorderLayout.CENTER);
        empty.setFont(Theme.font(Font.PLAIN, 12));
        mid.add(empty, BorderLayout.SOUTH);
        card.add(mid, BorderLayout.CENTER);
        return card;
    }
}

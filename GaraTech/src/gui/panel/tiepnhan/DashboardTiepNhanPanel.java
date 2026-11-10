package gui.panel.tiepnhan;

import dao.TiepNhanDashboardDao;
import dao.TiepNhanDashboardDao.DashboardData;
import dao.TiepNhanDashboardDao.PhieuDangXuLyRow;
import dao.TiepNhanDashboardDao.XeChoGiaoRow;
import enums.TrangThaiPhieu;
import gui.Navigator;
import gui.Screen;
import gui.component.BasePanel;
import gui.component.StatCard;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bảng điều hành Tiếp nhận xe (Dashboard Tiếp nhận viên).
 * Lấy cảm hứng và chuẩn hóa theo thiết kế Bảng điều hành tiếp nhận của CarZone:
 * - 4 Thẻ KPI ca trực thời gian thực.
 * - Thanh trực quan phân bổ tiến độ xe tại xưởng.
 * - Danh sách xe hoàn tất sẵn sàng bàn giao & thu tiền.
 * - Danh sách xe đang xử lý trong xưởng.
 */
public final class DashboardTiepNhanPanel extends BasePanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM · HH:mm");

    private final TiepNhanDashboardDao dashboardDao = new TiepNhanDashboardDao();

    // 1. Actions Bar
    private final JButton btnLamMoi = UI.secondaryButton("Làm mới (F5)");
    private final JButton btnTiepNhanMoi = UI.primaryButton("+ Tiếp nhận xe (F2)");
    private final JButton btnTraCuuKhach = UI.secondaryButton("Khách hàng & xe");

    // 2. 4 Thẻ KPI
    private final StatCard cardHomNay = new StatCard("Tiếp nhận hôm nay", "0", "Lượt xe vào xưởng hôm nay");
    private final StatCard cardChuaPhanCong = new StatCard("Cần phân công KTV", "0", "Đã phân công đầy đủ");
    private final StatCard cardDangSua = new StatCard("Xe đang sửa chữa", "0", "Đang thi công trên khoang");
    private final StatCard cardChoGiao = new StatCard("Sẵn sàng giao xe", "0", "Đã xong · Chờ bàn giao");

    // 3. Phân bổ tiến độ xe trong xưởng
    private final JLabel lblCountChoSua = UI.label("🟡 Chờ sửa: 0", Font.BOLD, 12, new Color(0xB45309));
    private final JLabel lblCountDangSua = UI.label("🔵 Đang sửa: 0", Font.BOLD, 12, new Color(0x175CD3));
    private final JLabel lblCountChoLK = UI.label("🟣 Chờ linh kiện: 0", Font.BOLD, 12, new Color(0x7A5AF8));
    private final JLabel lblCountHoanTat = UI.label("🟢 Hoàn tất: 0", Font.BOLD, 12, new Color(0x027A48));
    private final WorkshopProgressBar progressBar = new WorkshopProgressBar();

    // 4. Bảng xe sẵn sàng bàn giao & thu tiền (HOAN_TAT)
    private final DefaultTableModel modelChoGiao = UI.readOnlyModel(
            "Mã phiếu", "Biển số xe", "Khách hàng", "Số điện thoại", "KTV phụ trách", "Chi phí tạm tính", "Hoàn tất lúc"
    );
    private final JTable tblChoGiao = UI.table(modelChoGiao);

    // 5. Bảng xe đang xử lý tại xưởng
    private final DefaultTableModel modelDangXuLy = UI.readOnlyModel(
            "Mã phiếu", "Biển số xe", "Khách hàng", "KTV phụ trách", "Tiếp nhận lúc", "Yêu cầu của khách", "Trạng thái"
    );
    private final JTable tblDangXuLy = UI.table(modelDangXuLy);

    public DashboardTiepNhanPanel() {
        super("BẢNG ĐIỀU HÀNH", "Bảng điều hành Tiếp nhận xe",
                "Ca trực hôm nay: " + LocalDate.now().format(DATE_FMT) + " · Giám sát xưởng, theo dõi tiến độ và hàng đợi giao xe");

        buildUI();
        initShortcuts();
        initEvents();

        loadData();
    }

    @Override
    public void onShow() {
        setHeader("BẢNG ĐIỀU HÀNH", "Bảng điều hành Tiếp nhận xe",
                "Ca trực hôm nay: " + LocalDate.now().format(DATE_FMT) + " · Giám sát xưởng, theo dõi tiến độ và hàng đợi giao xe");
        loadData();
    }

    private void buildUI() {
        // Quick Actions
        actions.removeAll();
        actions.add(btnLamMoi);
        actions.add(btnTraCuuKhach);
        actions.add(btnTiepNhanMoi);
        actions.revalidate();
        actions.repaint();

        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        // 1. Hàng 4 Stat Cards
        JPanel pStats = new JPanel(new GridLayout(1, 4, 14, 0));
        pStats.setOpaque(false);
        pStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        pStats.setPreferredSize(new Dimension(800, 115));
        pStats.add(cardHomNay);
        pStats.add(cardChuaPhanCong);
        pStats.add(cardDangSua);
        pStats.add(cardChoGiao);
        pStats.setAlignmentX(LEFT_ALIGNMENT);
        root.add(pStats);

        root.add(Box.createVerticalStrut(16));

        // 2. Card Phân bổ tiến độ xe tại xưởng
        RoundedPanel pProgressCard = new RoundedPanel(14, Theme.CARD);
        pProgressCard.setLayout(new BorderLayout(0, 10));
        pProgressCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel pProgHeader = new JPanel(new BorderLayout());
        pProgHeader.setOpaque(false);
        pProgHeader.add(UI.h2("Tiến độ xe đang có mặt tại xưởng"), BorderLayout.WEST);

        JPanel pChips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        pChips.setOpaque(false);
        pChips.add(lblCountChoSua);
        pChips.add(lblCountDangSua);
        pChips.add(lblCountChoLK);
        pChips.add(lblCountHoanTat);
        pProgHeader.add(pChips, BorderLayout.EAST);

        pProgressCard.add(pProgHeader, BorderLayout.NORTH);
        pProgressCard.add(progressBar, BorderLayout.CENTER);
        pProgressCard.setAlignmentX(LEFT_ALIGNMENT);
        root.add(pProgressCard);

        root.add(Box.createVerticalStrut(16));

        // 3. Card Xe hoàn tất - Sẵn sàng bàn giao & thu tiền
        RoundedPanel pReadyCard = new RoundedPanel(14, Theme.CARD);
        pReadyCard.setLayout(new BorderLayout(0, 12));
        pReadyCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel pReadyHead = new JPanel(new BorderLayout());
        pReadyHead.setOpaque(false);
        pReadyHead.add(UI.h2("Xe sẵn sàng bàn giao & Chờ thanh toán"), BorderLayout.WEST);
        pReadyHead.add(UI.muted("Các xe đã nghiệm thu hoàn tất thi công"), BorderLayout.EAST);
        pReadyCard.add(pReadyHead, BorderLayout.NORTH);

        setupTable(tblChoGiao, 7);
        tblChoGiao.getColumnModel().getColumn(5).setCellRenderer(new MoneyCellRenderer());

        JScrollPane spReady = new JScrollPane(tblChoGiao);
        spReady.setPreferredSize(new Dimension(800, 140));
        spReady.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pReadyCard.add(spReady, BorderLayout.CENTER);
        pReadyCard.setAlignmentX(LEFT_ALIGNMENT);
        root.add(pReadyCard);

        root.add(Box.createVerticalStrut(16));

        // 4. Card Hàng đợi xe đang xử lý
        RoundedPanel pQueueCard = new RoundedPanel(14, Theme.CARD);
        pQueueCard.setLayout(new BorderLayout(0, 12));
        pQueueCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel pQueueHead = new JPanel(new BorderLayout());
        pQueueHead.setOpaque(false);
        pQueueHead.add(UI.h2("Hàng đợi xe đang sửa chữa tại xưởng"), BorderLayout.WEST);
        pQueueHead.add(UI.muted("Theo dõi trạng thái từng xe theo thời gian thực"), BorderLayout.EAST);
        pQueueCard.add(pQueueHead, BorderLayout.NORTH);

        setupTable(tblDangXuLy, 7);
        tblDangXuLy.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane spQueue = new JScrollPane(tblDangXuLy);
        spQueue.setPreferredSize(new Dimension(800, 180));
        spQueue.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pQueueCard.add(spQueue, BorderLayout.CENTER);
        pQueueCard.setAlignmentX(LEFT_ALIGNMENT);
        root.add(pQueueCard);

        root.add(Box.createVerticalStrut(20));

        body.removeAll();
        body.add(root, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    private void setupTable(JTable tbl, int cols) {
        tbl.setRowHeight(38);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        for (int i = 0; i < cols; i++) {
            tbl.getColumnModel().getColumn(i).setCellRenderer(r);
        }
        TableCellRenderer head = tbl.getTableHeader().getDefaultRenderer();
        if (head instanceof DefaultTableCellRenderer hr) {
            hr.setHorizontalAlignment(SwingConstants.LEFT);
            hr.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }
    }

    public void loadData() {
        new Thread(() -> {
            try {
                DashboardData data = dashboardDao.getDashboardData();
                SwingUtilities.invokeLater(() -> renderData(data));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void renderData(DashboardData data) {
        // 1. Cập nhật thẻ
        cardHomNay.setValue(String.format("%02d", data.tiepNhanHomNay()), "Lượt xe vào xưởng hôm nay");

        if (data.chuaPhanCongKTV() > 0) {
            cardChuaPhanCong.setValue(String.format("%02d", data.chuaPhanCongKTV()), "⚠️ Cần chỉ định thợ ngay!");
        } else {
            cardChuaPhanCong.setValue("00", "Đã phân công đầy đủ");
        }

        cardDangSua.setValue(String.format("%02d", data.dangSua()), "Đang thi công trên khoang");
        cardChoGiao.setValue(String.format("%02d", data.hoanTatChoGiao()), "Đã xong · Chờ bàn giao");

        // 2. Cập nhật thanh tiến độ
        lblCountChoSua.setText("🟡 Chờ sửa: " + data.choSua());
        lblCountDangSua.setText("🔵 Đang sửa: " + data.dangSua());
        lblCountChoLK.setText("🟣 Chờ linh kiện: " + data.choLinhKien());
        lblCountHoanTat.setText("🟢 Hoàn tất: " + data.hoanTatChoGiao());
        progressBar.setValues(data.choSua(), data.dangSua(), data.choLinhKien(), data.hoanTatChoGiao());

        // 3. Nạp bảng xe chờ giao
        modelChoGiao.setRowCount(0);
        for (XeChoGiaoRow r : data.dsChoGiao()) {
            modelChoGiao.addRow(new Object[]{
                    r.maPhieu(),
                    r.bienSo(),
                    r.khachHang(),
                    r.soDienThoai(),
                    r.ktvChinh(),
                    Fmt.money(r.tongTien()),
                    r.thoiGianHoanThanh() != null ? r.thoiGianHoanThanh().format(TIME_FMT) : "---"
            });
        }

        // 4. Nạp bảng xe đang xử lý
        modelDangXuLy.setRowCount(0);
        for (PhieuDangXuLyRow r : data.dsDangXuLy()) {
            modelDangXuLy.addRow(new Object[]{
                    r.maPhieu(),
                    r.bienSo(),
                    r.khachHang(),
                    r.ktvChinh(),
                    r.thoiGianTiepNhan() != null ? r.thoiGianTiepNhan().format(TIME_FMT) : "---",
                    r.yeuCauBanDau(),
                    r.trangThai()
            });
        }
    }

    private void initShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "actTiepNhan");
        getActionMap().put("actTiepNhan", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                Navigator.go(Screen.TIEP_NHAN_XE);
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "actRefresh");
        getActionMap().put("actRefresh", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                loadData();
            }
        });
    }

    // =========================================================================
    // RENDERERS & PROGRESS BAR
    // =========================================================================

    private static final class MoneyCellRenderer extends DefaultTableCellRenderer {
        public MoneyCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(Theme.font(Font.BOLD, 13));
            setForeground(new Color(0x027A48));
        }
    }

    private static final class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String raw = value != null ? value.toString() : "";
            TrangThaiPhieu tt = TrangThaiPhieu.fromDb(raw);
            lbl.setText(tt.label());
            lbl.setFont(Theme.font(Font.BOLD, 12));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);

            Color bg;
            Color fg;
            switch (tt) {
                case CHO_SUA -> { bg = new Color(0xFE, 0xF3, 0xC7); fg = new Color(0xB4, 0x53, 0x09); }
                case DANG_SUA -> { bg = new Color(0xEB, 0xF8, 0xFF); fg = new Color(0x17, 0x5C, 0xD3); }
                case CHO_LINH_KIEN -> { bg = new Color(0xF4, 0xF3, 0xFF); fg = new Color(0x7A, 0x5A, 0xF8); }
                case HOAN_TAT -> { bg = new Color(0xEC, 0xFD, 0xF3); fg = new Color(0x02, 0x7A, 0x48); }
                default -> { bg = new Color(0xF2, 0xF4, 0xF7); fg = Theme.MUTED; }
            }
            lbl.setOpaque(true);
            lbl.setBackground(bg);
            lbl.setForeground(fg);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(4, 8, 4, 8),
                    BorderFactory.createLineBorder(fg, 1)
            ));
            return lbl;
        }
    }

    private static final class WorkshopProgressBar extends JPanel {
        private int choSua = 0, dangSua = 0, choLk = 0, hoanTat = 0;

        public WorkshopProgressBar() {
            setPreferredSize(new Dimension(200, 14));
            setOpaque(false);
        }

        public void setValues(int choSua, int dangSua, int choLk, int hoanTat) {
            this.choSua = choSua;
            this.dangSua = dangSua;
            this.choLk = choLk;
            this.hoanTat = hoanTat;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int total = choSua + dangSua + choLk + hoanTat;
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (total == 0) {
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.dispose();
                return;
            }

            int wChoSua = (int) Math.round(((double) choSua / total) * w);
            int wDangSua = (int) Math.round(((double) dangSua / total) * w);
            int wChoLk = (int) Math.round(((double) choLk / total) * w);
            int wHoanTat = w - wChoSua - wDangSua - wChoLk;

            int x = 0;
            // Draw background clip round
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, h, h));

            if (wChoSua > 0) {
                g2.setColor(new Color(0xFB, 0xC5, 0x2D)); // Vàng
                g2.fillRect(x, 0, wChoSua, h);
                x += wChoSua;
            }
            if (wDangSua > 0) {
                g2.setColor(new Color(0x17, 0x5C, 0xD3)); // Xanh dương
                g2.fillRect(x, 0, wDangSua, h);
                x += wDangSua;
            }
            if (wChoLk > 0) {
                g2.setColor(new Color(0x7A, 0x5A, 0xF8)); // Tím
                g2.fillRect(x, 0, wChoLk, h);
                x += wChoLk;
            }
            if (wHoanTat > 0) {
                g2.setColor(new Color(0x02, 0x7A, 0x48)); // Xanh lá
                g2.fillRect(x, 0, wHoanTat, h);
            }
            g2.dispose();
        }
    }

    // =========================================================================
    // TẤT CẢ XỬ LÝ SỰ KIỆN Ở CUỐI CÙNG THEO QUY TẮC BẮT BUỘC
    // =========================================================================
    private void initEvents() {
        btnLamMoi.addActionListener(e -> loadData());

        btnTiepNhanMoi.addActionListener(e -> Navigator.go(Screen.TIEP_NHAN_XE));

        btnTraCuuKhach.addActionListener(e -> Navigator.go(Screen.KHACH_HANG_XE));

        tblDangXuLy.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblDangXuLy.getSelectedRow() >= 0) {
                    Navigator.go(Screen.TIEP_NHAN_XE);
                }
            }
        });

        tblChoGiao.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblChoGiao.getSelectedRow() >= 0) {
                    Navigator.go(Screen.TIEP_NHAN_XE);
                }
            }
        });
    }
}

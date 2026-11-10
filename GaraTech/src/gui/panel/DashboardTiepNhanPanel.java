package gui.panel;

import com.formdev.flatlaf.FlatClientProperties;
import dao.TiepNhanDashboardDao;
import dao.TiepNhanDashboardDao.DashboardData;
import dao.TiepNhanDashboardDao.KtvWorkloadRow;
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
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng điều hành Tiếp nhận xe (Dashboard Tiếp nhận viên).
 * Tối ưu hóa giao diện:
 * - Số hiển thị dạng số nguyên gọn gàng (0, 1, 2...).
 * - Bỏ icon/emoji theo yêu cầu.
 * - Khối "Tải công việc Kỹ thuật viên" sắp xếp tăng dần từ ít việc nhất đến nhiều việc nhất.
 * - Bộ lọc trạng thái phiếu cho bảng hàng đợi xe.
 * - Ô trạng thái dạng Pill bo tròn mềm mại, chuẩn UI hiện đại.
 */
public final class DashboardTiepNhanPanel extends BasePanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM · HH:mm");

    private final TiepNhanDashboardDao dashboardDao = new TiepNhanDashboardDao();

    // 1. Actions Bar
    private final JButton btnLamMoi = UI.secondaryButton("Làm mới (F5)");
    private final JButton btnTiepNhanMoi = UI.primaryButton("+ Tiếp nhận xe (F2)");
    private final JButton btnTraCuuKhach = UI.secondaryButton("Khách hàng & xe");

    // 2. 4 Thẻ KPI (Hiển thị 0, 1, 2 không có số 0 ở trước)
    private final StatCard cardHomNay = new StatCard("Tiếp nhận hôm nay", "0", "Lượt xe vào xưởng hôm nay");
    private final StatCard cardChuaPhanCong = new StatCard("Cần phân công KTV", "0", "Đã phân công đầy đủ");
    private final StatCard cardDangSua = new StatCard("Xe đang sửa chữa", "0", "Đang thi công trên khoang");
    private final StatCard cardChoGiao = new StatCard("Sẵn sàng giao xe", "0", "Đã xong · Chờ bàn giao");

    // 3. Tình trạng xe tại xưởng
    private final JLabel lblXeCoMatBadge = new JLabel("0 xe có mặt") {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xEB, 0xF8, 0xFF));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(new Color(0xB2, 0xDD, 0xFF));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    };
    private final WorkshopProgressBar progressBar = new WorkshopProgressBar();
    private final JLabel lblMiniValChoSua = UI.label("0 (0%)", Font.BOLD, 12, new Color(0xC2, 0x41, 0x0C));
    private final JLabel lblMiniValDangSua = UI.label("0 (0%)", Font.BOLD, 12, new Color(0x1D, 0x4E, 0xD8));
    private final JLabel lblMiniValChoLK = UI.label("0 (0%)", Font.BOLD, 12, new Color(0xB4, 0x53, 0x09));
    private final JLabel lblMiniValChoThu = UI.label("0 (0%)", Font.BOLD, 12, new Color(0x04, 0x78, 0x57));
    private JPanel boxChoSua;
    private JPanel boxDangSua;
    private JPanel boxChoLK;
    private JPanel boxChoThu;

    // 4. Tải công việc kỹ thuật viên (3 cột: Kỹ thuật viên, mã nhân viên, số xe đang sửa chữa)
    private final DefaultTableModel modelKtv = UI.readOnlyModel("Kỹ thuật viên", "Mã nhân viên", "Số xe đang sửa chữa");
    private final JTable tblKtv = UI.table(modelKtv);

    // 5. Bảng xe sẵn sàng bàn giao & thu tiền (HOAN_TAT)
    private final DefaultTableModel modelChoGiao = UI.readOnlyModel(
            "Mã phiếu", "Biển số xe", "Khách hàng", "Số điện thoại", "KTV phụ trách", "Chi phí tạm tính", "Hoàn tất lúc"
    );
    private final JTable tblChoGiao = UI.table(modelChoGiao);

    // 6. Bảng xe đang xử lý tại xưởng kèm Bộ lọc trạng thái
    private final JComboBox<String> cboFilterStatus = new JComboBox<>(new String[]{
            "Tất cả trạng thái",
            "Chờ sửa chữa",
            "Đang sửa chữa",
            "Chờ linh kiện"
    });
    private final DefaultTableModel modelDangXuLy = UI.readOnlyModel(
            "Mã phiếu", "Biển số xe", "Khách hàng", "KTV phụ trách", "Tiếp nhận lúc", "Yêu cầu của khách", "Trạng thái"
    );
    private final JTable tblDangXuLy = UI.table(modelDangXuLy);
    private final TableRowSorter<DefaultTableModel> sorterDangXuLy = new TableRowSorter<>(modelDangXuLy);

    private List<PhieuDangXuLyRow> currentDangXuLyRows = new ArrayList<>();

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

        // ── 1. Hàng 4 Stat Cards ─────────────────────────────────────────────
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

        // ── 2. Hàng đôi: Tình trạng xe tại xưởng (Trái) & Tải công việc KTV (Phải) ──────
        JPanel pMiddleRow = new JPanel(new GridLayout(1, 2, 16, 0));
        pMiddleRow.setOpaque(false);
        pMiddleRow.setPreferredSize(new Dimension(800, 205));
        pMiddleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));

        // 2A. Card Tình trạng xe tại xưởng
        RoundedPanel pProgressCard = new RoundedPanel(14, Theme.CARD);
        pProgressCard.setLayout(new BorderLayout(0, 10));
        pProgressCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel pProgHeader = new JPanel(new BorderLayout());
        pProgHeader.setOpaque(false);
        pProgHeader.add(UI.h2("Tình trạng xe tại xưởng"), BorderLayout.WEST);

        lblXeCoMatBadge.setFont(Theme.font(Font.BOLD, 11));
        lblXeCoMatBadge.setForeground(new Color(0x17, 0x5C, 0xD3));
        lblXeCoMatBadge.setHorizontalAlignment(SwingConstants.CENTER);
        lblXeCoMatBadge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        lblXeCoMatBadge.setOpaque(false);
        pProgHeader.add(lblXeCoMatBadge, BorderLayout.EAST);

        pProgressCard.add(pProgHeader, BorderLayout.NORTH);

        JPanel pProgCenter = new JPanel();
        pProgCenter.setOpaque(false);
        pProgCenter.setLayout(new BoxLayout(pProgCenter, BoxLayout.Y_AXIS));
        pProgCenter.add(Box.createVerticalStrut(4));
        pProgCenter.add(progressBar);
        pProgCenter.add(Box.createVerticalStrut(10));

        JPanel pnlBoxes = new JPanel(new GridLayout(2, 2, 8, 8));
        pnlBoxes.setOpaque(false);

        boxChoSua = taoMiniBox("Chờ sửa chữa", lblMiniValChoSua, new Color(0xEA, 0x58, 0x0C), new Color(0xFF, 0xED, 0xD5));
        boxDangSua = taoMiniBox("Đang sửa chữa", lblMiniValDangSua, new Color(0x25, 0x63, 0xEB), new Color(0xDB, 0xEA, 0xFE));
        boxChoLK = taoMiniBox("Chờ linh kiện", lblMiniValChoLK, new Color(0xD9, 0x77, 0x06), new Color(0xFE, 0xF3, 0xC7));
        boxChoThu = taoMiniBox("Chờ thu tiền", lblMiniValChoThu, new Color(0x05, 0x96, 0x69), new Color(0xD1, 0xFA, 0xE5));

        pnlBoxes.add(boxChoSua);
        pnlBoxes.add(boxDangSua);
        pnlBoxes.add(boxChoLK);
        pnlBoxes.add(boxChoThu);

        pProgCenter.add(pnlBoxes);
        pProgressCard.add(pProgCenter, BorderLayout.CENTER);

        pMiddleRow.add(pProgressCard);

        // 2B. Card Tải công việc KTV (3 cột: Kỹ thuật viên, mã nhân viên, số xe đang sửa chữa)
        RoundedPanel pKtvCard = new RoundedPanel(14, Theme.CARD);
        pKtvCard.setLayout(new BorderLayout(0, 10));
        pKtvCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel pKtvHead = new JPanel(new BorderLayout());
        pKtvHead.setOpaque(false);
        pKtvHead.add(UI.h2("Tải công việc Kỹ thuật viên"), BorderLayout.WEST);
        pKtvHead.add(UI.muted("Ưu tiên thợ ít việc nhất"), BorderLayout.EAST);
        pKtvCard.add(pKtvHead, BorderLayout.NORTH);

        setupTable(tblKtv, 3);
        tblKtv.setRowHeight(32);
        tblKtv.getColumnModel().getColumn(0).setPreferredWidth(160);
        tblKtv.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblKtv.getColumnModel().getColumn(2).setPreferredWidth(140);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblKtv.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer countRenderer = new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(Theme.font(Font.BOLD, 12));
            }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    int cnt = value instanceof Number n ? n.intValue() : 0;
                    if (cnt == 0) {
                        setForeground(new Color(0x02, 0x7A, 0x48));
                    } else if (cnt <= 2) {
                        setForeground(new Color(0x17, 0x5C, 0xD3));
                    } else {
                        setForeground(new Color(0xEA, 0x58, 0x0C));
                    }
                }
                return c;
            }
        };
        tblKtv.getColumnModel().getColumn(2).setCellRenderer(countRenderer);

        JScrollPane spKtv = new JScrollPane(tblKtv);
        spKtv.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pKtvCard.add(spKtv, BorderLayout.CENTER);

        pMiddleRow.add(pKtvCard);

        pMiddleRow.setAlignmentX(LEFT_ALIGNMENT);
        root.add(pMiddleRow);

        root.add(Box.createVerticalStrut(16));

        // ── 3. Card Xe hoàn tất - Sẵn sàng bàn giao & thu tiền ───────────────
        RoundedPanel pReadyCard = new RoundedPanel(14, Theme.CARD);
        pReadyCard.setLayout(new BorderLayout(0, 12));
        pReadyCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel pReadyHead = new JPanel(new BorderLayout());
        pReadyHead.setOpaque(false);
        pReadyHead.add(UI.h2("Xe sẵn sàng bàn giao & Chờ thanh toán"), BorderLayout.WEST);
        pReadyHead.add(UI.muted("Các xe đã hoàn tất sửa chữa, sẵn sàng giao chìa khóa & thu tiền"), BorderLayout.EAST);
        pReadyCard.add(pReadyHead, BorderLayout.NORTH);

        setupTable(tblChoGiao, 7);
        tblChoGiao.getColumnModel().getColumn(5).setCellRenderer(new MoneyCellRenderer());

        JScrollPane spReady = new JScrollPane(tblChoGiao);
        spReady.setPreferredSize(new Dimension(800, 130));
        spReady.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pReadyCard.add(spReady, BorderLayout.CENTER);
        pReadyCard.setAlignmentX(LEFT_ALIGNMENT);
        root.add(pReadyCard);

        root.add(Box.createVerticalStrut(16));

        // ── 4. Card Hàng đợi xe đang xử lý (Có bộ lọc trạng thái) ─────────────
        RoundedPanel pQueueCard = new RoundedPanel(14, Theme.CARD);
        pQueueCard.setLayout(new BorderLayout(0, 12));
        pQueueCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel pQueueHead = new JPanel(new BorderLayout(14, 0));
        pQueueHead.setOpaque(false);
        pQueueHead.add(UI.h2("Hàng đợi xe đang sửa chữa tại xưởng"), BorderLayout.WEST);

        // Cụm Bộ lọc trạng thái ở bên phải tiêu đề
        JPanel pFilterWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pFilterWrap.setOpaque(false);
        JLabel lblFilter = UI.label("Lọc trạng thái:", Font.BOLD, 12, Theme.TEXT);
        cboFilterStatus.setPreferredSize(new Dimension(175, 32));
        cboFilterStatus.setFont(Theme.font(Font.PLAIN, 12));
        cboFilterStatus.putClientProperty(FlatClientProperties.STYLE, "arc:8; borderColor:#D5DBE5");
        pFilterWrap.add(lblFilter);
        pFilterWrap.add(cboFilterStatus);
        pQueueHead.add(pFilterWrap, BorderLayout.EAST);

        pQueueCard.add(pQueueHead, BorderLayout.NORTH);

        setupTable(tblDangXuLy, 7);
        tblDangXuLy.setRowHeight(42);
        tblDangXuLy.setRowSorter(sorterDangXuLy);
        tblDangXuLy.getColumnModel().getColumn(0).setPreferredWidth(150);
        tblDangXuLy.getColumnModel().getColumn(1).setPreferredWidth(110);
        tblDangXuLy.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblDangXuLy.getColumnModel().getColumn(3).setPreferredWidth(140);
        tblDangXuLy.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblDangXuLy.getColumnModel().getColumn(5).setPreferredWidth(260);
        tblDangXuLy.getColumnModel().getColumn(6).setPreferredWidth(150);
        tblDangXuLy.getColumnModel().getColumn(6).setCellRenderer(new ModernStatusPillRenderer());

        JScrollPane spQueue = new JScrollPane(tblDangXuLy);
        spQueue.setPreferredSize(new Dimension(800, 190));
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
        // 1. Cập nhật thẻ số nguyên (0, 1, 4... không có số 0 ở trước)
        cardHomNay.setValue(String.valueOf(data.tiepNhanHomNay()), "Lượt xe vào xưởng hôm nay");

        if (data.chuaPhanCongKTV() > 0) {
            cardChuaPhanCong.setValue(String.valueOf(data.chuaPhanCongKTV()), "Cần chỉ định thợ ngay!");
        } else {
            cardChuaPhanCong.setValue("0", "Đã phân công đầy đủ");
        }

        cardDangSua.setValue(String.valueOf(data.dangSua()), "Đang thi công trên khoang");
        cardChoGiao.setValue(String.valueOf(data.hoanTatChoGiao()), "Đã xong · Chờ bàn giao");

        // 2. Cập nhật Tình trạng xe tại xưởng
        int choSua = data.choSua();
        int dangSua = data.dangSua();
        int choLK = data.choLinhKien();
        int choThu = data.hoanTatChoGiao();
        int tongXe = choSua + dangSua + choLK + choThu;

        lblXeCoMatBadge.setText(tongXe + " xe có mặt");
        progressBar.setData(choSua, dangSua, choLK, choThu);

        int pctChoSua = tongXe > 0 ? (int) Math.round((double) choSua * 100.0 / tongXe) : 0;
        int pctDangSua = tongXe > 0 ? (int) Math.round((double) dangSua * 100.0 / tongXe) : 0;
        int pctChoLK = tongXe > 0 ? (int) Math.round((double) choLK * 100.0 / tongXe) : 0;
        int pctChoThu = tongXe > 0 ? (int) Math.round((double) choThu * 100.0 / tongXe) : 0;

        lblMiniValChoSua.setText(choSua + " (" + pctChoSua + "%)");
        lblMiniValDangSua.setText(dangSua + " (" + pctDangSua + "%)");
        lblMiniValChoLK.setText(choLK + " (" + pctChoLK + "%)");
        lblMiniValChoThu.setText(choThu + " (" + pctChoThu + "%)");

        // 3. Nạp bảng tải KTV (3 cột: Kỹ thuật viên, mã nhân viên, số xe đang sửa chữa)
        modelKtv.setRowCount(0);
        for (KtvWorkloadRow k : data.dsKtvWorkload()) {
            modelKtv.addRow(new Object[]{
                    k.hoTen(),
                    k.maNV(),
                    k.soXeDangPhuTrach()
            });
        }

        // 4. Nạp bảng xe chờ giao
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

        // 5. Nạp bảng xe đang xử lý
        this.currentDangXuLyRows = data.dsDangXuLy();
        applyFilterStatus();
    }

    private void applyFilterStatus() {
        int idx = cboFilterStatus.getSelectedIndex();
        modelDangXuLy.setRowCount(0);

        for (PhieuDangXuLyRow r : currentDangXuLyRows) {
            boolean match = switch (idx) {
                case 1 -> "CHO_SUA".equalsIgnoreCase(r.trangThai());
                case 2 -> "DANG_SUA".equalsIgnoreCase(r.trangThai());
                case 3 -> "CHO_LINH_KIEN".equalsIgnoreCase(r.trangThai());
                default -> true;
            };

            if (match) {
                modelDangXuLy.addRow(new Object[]{
                        r.maPhieu(),
                        r.bienSo(),
                        r.khachHang(),
                        r.ktvChinh() != null && !r.ktvChinh().isBlank() ? r.ktvChinh() : "Chưa chỉ định",
                        r.thoiGianTiepNhan() != null ? r.thoiGianTiepNhan().format(TIME_FMT) : "---",
                        r.yeuCauBanDau(),
                        r.trangThai()
                });
            }
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

    private JPanel taoMiniBox(String label, JLabel lblVal, Color dotColor, Color bgColor) {
        JPanel box = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        box.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        box.setToolTipText("Bấm để lọc theo trạng thái: " + label);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel(new DotIcon(dotColor, 8)));
        JLabel lbl = UI.label(label, Font.PLAIN, 12, Theme.TEXT);
        left.add(lbl);
        box.add(left, BorderLayout.WEST);

        box.add(lblVal, BorderLayout.EAST);
        return box;
    }

    public static final class DotIcon implements Icon {
        private final Color color;
        private final int size;

        public DotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // =========================================================================
    // RENDERERS HIỆN ĐẠI, BO TRÒN ĐẸP MẮT
    // =========================================================================

    private static final class MoneyCellRenderer extends DefaultTableCellRenderer {
        public MoneyCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(Theme.font(Font.BOLD, 13));
            setForeground(new Color(0x027A48));
        }
    }

    /**
     * Renderer ô trạng thái dạng Pill bo tròn mềm mại, không tràn viền, chuẩn hiện đại.
     */
    private static final class ModernStatusPillRenderer extends JPanel implements TableCellRenderer {
        private final StatusPillLabel pillLabel = new StatusPillLabel();

        public ModernStatusPillRenderer() {
            super(new GridBagLayout());
            setOpaque(true);
            add(pillLabel, new GridBagConstraints());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            String raw = value != null ? value.toString() : "";
            TrangThaiPhieu tt = TrangThaiPhieu.fromDb(raw);

            pillLabel.setText(tt.label());

            Color bg;
            Color fg;
            Color border;
            switch (tt) {
                case CHO_SUA -> {
                    bg = new Color(0xFE, 0xF3, 0xC7); // Cam nhạt
                    fg = new Color(0xB4, 0x53, 0x09);
                    border = new Color(0xFD, 0xE6, 0x8A);
                }
                case DANG_SUA -> {
                    bg = new Color(0xEB, 0xF8, 0xFF); // Xanh dương nhạt
                    fg = new Color(0x17, 0x5C, 0xD3);
                    border = new Color(0xB2, 0xDD, 0xFF);
                }
                case CHO_LINH_KIEN -> {
                    bg = new Color(0xF4, 0xF3, 0xFF); // Tím nhạt
                    fg = new Color(0x7A, 0x5A, 0xF8);
                    border = new Color(0xD9, 0xD6, 0xFE);
                }
                case HOAN_TAT -> {
                    bg = new Color(0xEC, 0xFD, 0xF3); // Xanh lá nhạt
                    fg = new Color(0x02, 0x7A, 0x48);
                    border = new Color(0xA6, 0xF4, 0xC5);
                }
                default -> {
                    bg = new Color(0xF2, 0xF4, 0xF7);
                    fg = Theme.MUTED;
                    border = Theme.BORDER;
                }
            }

            pillLabel.setColors(bg, fg, border);
            return this;
        }
    }

    private static final class StatusPillLabel extends JLabel {
        private Color bgColor = Color.LIGHT_GRAY;
        private Color borderColor = Color.GRAY;

        public StatusPillLabel() {
            setFont(Theme.font(Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            setPreferredSize(new Dimension(130, 26));
            setOpaque(false);
        }

        public void setColors(Color bg, Color fg, Color border) {
            this.bgColor = bg;
            this.borderColor = border;
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Vẽ nền bo tròn mềm mại dạng pill (viên thuốc)
            g2.setColor(bgColor);
            g2.fillRoundRect(1, 1, w - 2, h - 2, h, h);

            // Vẽ viền mỏng thanh thoát
            g2.setColor(borderColor);
            g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class WorkshopProgressBar extends JPanel {
        private int choSua = 0, dangSua = 0, choLk = 0, choThu = 0;

        public WorkshopProgressBar() {
            setPreferredSize(new Dimension(200, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            setOpaque(false);
        }

        public void setData(int choSua, int dangSua, int choLk, int choThu) {
            this.choSua = choSua;
            this.dangSua = dangSua;
            this.choLk = choLk;
            this.choThu = choThu;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int total = choSua + dangSua + choLk + choThu;
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (total == 0) {
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.dispose();
                return;
            }

            int wChoSua = (int) Math.round(((double) choSua / total) * w);
            int wDangSua = (int) Math.round(((double) dangSua / total) * w);
            int wChoLk = (int) Math.round(((double) choLk / total) * w);
            int wChoThu = w - wChoSua - wDangSua - wChoLk;

            int x = 0;
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 8, 8));

            if (wChoSua > 0) {
                g2.setColor(new Color(0xEA, 0x58, 0x0C)); // Cam
                g2.fillRect(x, 0, wChoSua, h);
                x += wChoSua;
            }
            if (wDangSua > 0) {
                g2.setColor(new Color(0x25, 0x63, 0xEB)); // Xanh dương
                g2.fillRect(x, 0, wDangSua, h);
                x += wDangSua;
            }
            if (wChoLk > 0) {
                g2.setColor(new Color(0xD9, 0x77, 0x06)); // Hổ phách
                g2.fillRect(x, 0, wChoLk, h);
                x += wChoLk;
            }
            if (wChoThu > 0) {
                g2.setColor(new Color(0x05, 0x96, 0x69)); // Xanh lá
                g2.fillRect(x, 0, wChoThu, h);
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

        cboFilterStatus.addActionListener(e -> applyFilterStatus());

        boxChoSua.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cboFilterStatus.setSelectedIndex(1);
                applyFilterStatus();
            }
        });

        boxDangSua.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cboFilterStatus.setSelectedIndex(2);
                applyFilterStatus();
            }
        });

        boxChoLK.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cboFilterStatus.setSelectedIndex(3);
                applyFilterStatus();
            }
        });

        boxChoThu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cboFilterStatus.setSelectedIndex(0);
                applyFilterStatus();
                tblChoGiao.requestFocusInWindow();
            }
        });

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
                    Navigator.go(Screen.HOA_DON);
                }
            }
        });
    }
}

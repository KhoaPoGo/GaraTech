package gui.panel.khachhangxe;

import bus.KhachHangXeBus;
import dao.XeDao.LichSuSuaChuaRow;
import com.formdev.flatlaf.FlatClientProperties;
import entity.KhachHang;
import entity.Xe;
import gui.Navigator;
import gui.Screen;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Màn hình xem hồ sơ xe và lịch sử sửa chữa của phương tiện.
 */
public final class LichSuXePanel extends JPanel {

    private final Runnable onBack;

    // Top Stat cards
    private final JLabel lblHistoryOdoVal = UI.h2("0 km");
    private final JLabel lblHistoryRepairCount = UI.h2("0");
    private final JLabel lblHistoryLastDate = UI.h2("---");

    // Vehicle switcher cards container
    private final JPanel historyVehicleCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    // History Table
    private final DefaultTableModel vehHistoryModel = UI.readOnlyModel("Ngày", "Mã phiếu", "Nội dung kỹ thuật", "Trạng thái");
    private final JTable vehHistoryTable = UI.table(vehHistoryModel);

    // Detail view inputs (readonly)
    private final JTextField txtHistoryDetailKtv = new JTextField();
    private final JTextField txtHistoryDetailDeadline = new JTextField();
    private final JTextField txtHistoryDetailNotes = new JTextField();
    private final JTextField txtHistoryDetailDocs = new JTextField();

    // Action buttons
    private final JButton btnViewPsc = UI.primaryButton("Xem phiếu tiếp nhận - sửa chữa");
    private final JButton btnBackCust = UI.secondaryButton("Quay về hồ sơ khách");

    // State
    private Xe vehicleInHistory;
    private KhachHang customerInHistory;
    private List<Xe> customerVehicles = new ArrayList<>();
    private List<LichSuSuaChuaRow> currentVehicleHistoryRows = new ArrayList<>();

    public LichSuXePanel(Runnable onBack) {
        this.onBack = Objects.requireNonNull(onBack, "onBack must not be null");

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        initEvents();
    }

    private void buildUI() {
        // 1. Top 3 Stat Cards in a row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        statsRow.add(buildStatBox("ODO gần nhất", lblHistoryOdoVal));
        statsRow.add(buildStatBox("Lượt sửa chữa", lblHistoryRepairCount));
        statsRow.add(buildStatBox("Lần gần nhất", lblHistoryLastDate));

        statsRow.setAlignmentX(LEFT_ALIGNMENT);
        add(statsRow);
        add(Box.createVerticalStrut(16));

        // 2. Card: Lịch sử theo phương tiện
        RoundedPanel pHistory = new RoundedPanel(16, Theme.CARD);
        pHistory.setLayout(new BorderLayout(0, 14));
        pHistory.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel pHistHead = new JPanel();
        pHistHead.setOpaque(false);
        pHistHead.setLayout(new BoxLayout(pHistHead, BoxLayout.Y_AXIS));
        pHistHead.add(UI.h2("Lịch sử theo phương tiện"));
        pHistHead.add(Box.createVerticalStrut(6));
        pHistHead.add(UI.muted("Chuyển xe"));
        pHistHead.add(Box.createVerticalStrut(10));

        historyVehicleCardsPanel.setOpaque(false);
        historyVehicleCardsPanel.setAlignmentX(LEFT_ALIGNMENT);
        pHistHead.add(historyVehicleCardsPanel);
        pHistory.add(pHistHead, BorderLayout.NORTH);

        // Table setup
        vehHistoryTable.setRowHeight(42);
        vehHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehHistoryTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        vehHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        vehHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        vehHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        DefaultTableCellRenderer vCellR = new DefaultTableCellRenderer();
        vCellR.setHorizontalAlignment(SwingConstants.LEFT);
        vCellR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < 4; i++) {
            vehHistoryTable.getColumnModel().getColumn(i).setCellRenderer(vCellR);
        }
        TableCellRenderer vHeadRenderer = vehHistoryTable.getTableHeader().getDefaultRenderer();
        if (vHeadRenderer instanceof DefaultTableCellRenderer vHeadR) {
            vHeadR.setHorizontalAlignment(SwingConstants.LEFT);
            vHeadR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        JPanel tableBlock = UI.tableBlock(vehHistoryTable);
        pHistory.add(tableBlock, BorderLayout.CENTER);

        pHistory.setAlignmentX(LEFT_ALIGNMENT);
        add(pHistory);
        add(Box.createVerticalStrut(16));

        // 3. Card: Thông tin lần sửa chữa đang chọn
        RoundedPanel pDetail = new RoundedPanel(16, Theme.CARD);
        pDetail.setLayout(new BorderLayout(0, 12));
        pDetail.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        pDetail.add(UI.h2("Thông tin lần sửa chữa đang chọn"), BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(2, 2, 24, 8));
        formGrid.setOpaque(false);

        formGrid.add(formFieldDetail("Kỹ thuật viên", txtHistoryDetailKtv));
        formGrid.add(formFieldDetail("Hạn hoàn thành", txtHistoryDetailDeadline));
        formGrid.add(formFieldDetail("Ghi chú kỹ thuật", txtHistoryDetailNotes));
        formGrid.add(formFieldDetail("Chứng từ liên quan", txtHistoryDetailDocs));

        JPanel pDetailContent = new JPanel(new BorderLayout());
        pDetailContent.setOpaque(false);
        pDetailContent.add(formGrid, BorderLayout.NORTH);

        pDetail.add(pDetailContent, BorderLayout.CENTER);
        pDetail.setAlignmentX(LEFT_ALIGNMENT);
        add(pDetail);
        add(Box.createVerticalStrut(18));

        // 4. Bottom Action buttons
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        actionsRow.setOpaque(false);

        actionsRow.add(btnViewPsc);
        actionsRow.add(btnBackCust);
        actionsRow.setAlignmentX(LEFT_ALIGNMENT);

        add(actionsRow);

        attachMouseWheelRecursively(this);
    }

    public void setVehicle(Xe xe, KhachHang kh, List<Xe> allVehicles) {
        if (xe == null) return;
        this.vehicleInHistory = xe;
        this.customerInHistory = kh != null ? kh : xe.getKhachHang();
        this.customerVehicles = allVehicles != null ? allVehicles : Collections.emptyList();

        renderHistoryVehicleCards();
        loadVehicleHistory(xe);

        revalidate();
        repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollPane parentScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            if (parentScroll != null) {
                parentScroll.getVerticalScrollBar().setValue(0);
            }
        });
    }

    private void renderHistoryVehicleCards() {
        historyVehicleCardsPanel.removeAll();
        List<Xe> vehicles = (customerVehicles != null && !customerVehicles.isEmpty())
                ? customerVehicles
                : (customerInHistory != null
                    ? KhachHangXeBus.getDanhSachXe(customerInHistory.getMaKH())
                    : Collections.singletonList(vehicleInHistory));

        for (Xe x : vehicles) {
            boolean isSelected = vehicleInHistory != null && x.getBienSo().equalsIgnoreCase(vehicleInHistory.getBienSo());
            JPanel card = createVehicleSelectorCard(x, isSelected);
            attachMouseWheelRecursively(card);
            historyVehicleCardsPanel.add(card);
        }
        historyVehicleCardsPanel.revalidate();
        historyVehicleCardsPanel.repaint();
    }

    private JPanel createVehicleSelectorCard(Xe x, boolean isSelected) {
        Color bg = isSelected ? new Color(0xFE, 0xF7, 0xD6) : Color.WHITE;
        Color border = isSelected ? new Color(0xFB, 0xC5, 0x2D) : Theme.BORDER;
        RoundedPanel card = new RoundedPanel(12, bg).borderColor(border);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.setPreferredSize(new Dimension(240, 95));
        card.setMinimumSize(new Dimension(220, 95));
        card.setMaximumSize(new Dimension(260, 95));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Row 1: Plate + (if selected) badge "Đang xem"
        JPanel r1 = new JPanel(new BorderLayout());
        r1.setOpaque(false);
        JLabel lPlate = new JLabel(x.getBienSo());
        lPlate.setFont(Theme.font(Font.BOLD, 14));
        lPlate.setForeground(Theme.TEXT);
        r1.add(lPlate, BorderLayout.WEST);

        if (isSelected) {
            JLabel badge = new JLabel("Đang xem");
            badge.setFont(Theme.font(Font.BOLD, 10));
            badge.setForeground(Color.WHITE);
            badge.setOpaque(true);
            badge.setBackground(new Color(0x1E, 0x29, 0x3B));
            badge.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
            badge.putClientProperty(FlatClientProperties.STYLE, "arc:8");
            r1.add(badge, BorderLayout.EAST);
        }
        r1.setAlignmentX(LEFT_ALIGNMENT);
        card.add(r1);
        card.add(Box.createVerticalStrut(4));

        // Row 2: Make + Model + Year
        JLabel lModel = new JLabel(x.getHangXe() + " " + x.getModel() + " " + x.getNamSanXuat());
        lModel.setFont(Theme.font(isSelected ? Font.BOLD : Font.PLAIN, 13));
        lModel.setForeground(Theme.TEXT);
        lModel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lModel);
        card.add(Box.createVerticalStrut(4));

        // Row 3: Color · ODO [· Chưa có lịch sử]
        String odoStr = String.format("%,.0f km", x.getSoKmHienTai());
        List<LichSuSuaChuaRow> hist = KhachHangXeBus.getLichSuSuaChua(x.getBienSo(), Collections.emptyList());
        String note = (hist == null || hist.isEmpty()) ? " · Chưa có lịch sử" : "";
        JLabel lSub = UI.muted(x.getMauXe() + " · " + odoStr + note);
        lSub.setFont(Theme.font(Font.PLAIN, 12));
        lSub.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lSub);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVehicle(x, customerInHistory, customerVehicles);
            }
        });

        return card;
    }

    private void loadVehicleHistory(Xe xe) {
        vehHistoryModel.setRowCount(0);
        currentVehicleHistoryRows.clear();

        List<Xe> dummyList = Collections.singletonList(xe);
        List<LichSuSuaChuaRow> list = KhachHangXeBus.getLichSuSuaChua(xe.getBienSo(), dummyList);
        currentVehicleHistoryRows = list != null ? list : new ArrayList<>();

        // 1. Update Stat cards
        lblHistoryOdoVal.setText(String.format("%,.0f km", xe.getSoKmHienTai()));
        lblHistoryRepairCount.setText(String.valueOf(currentVehicleHistoryRows.size()));

        if (!currentVehicleHistoryRows.isEmpty()) {
            lblHistoryLastDate.setText(currentVehicleHistoryRows.get(0).ngay());
        } else {
            lblHistoryLastDate.setText("---");
        }

        // 2. Populate table
        for (LichSuSuaChuaRow r : currentVehicleHistoryRows) {
            vehHistoryModel.addRow(new Object[]{
                    r.ngay(),
                    r.maPhieu(),
                    r.noiDung(),
                    r.trangThai()
            });
        }

        // 3. Select first row if available, otherwise clear details
        if (!currentVehicleHistoryRows.isEmpty()) {
            vehHistoryTable.setRowSelectionInterval(0, 0);
            populateHistoryDetail(currentVehicleHistoryRows.get(0));
        } else {
            clearHistoryDetail();
        }
    }

    private void populateHistoryDetail(LichSuSuaChuaRow row) {
        if (row == null) {
            clearHistoryDetail();
            return;
        }
        txtHistoryDetailKtv.setText(row.kyThuatVien() != null ? row.kyThuatVien() : "Chưa phân công");
        txtHistoryDetailDeadline.setText(row.hanHoanThanh() != null ? row.hanHoanThanh() : "---");
        txtHistoryDetailNotes.setText(row.ghiChuKyThuat() != null ? row.ghiChuKyThuat() : "---");
        txtHistoryDetailDocs.setText(row.chungTuLienQuan() != null ? row.chungTuLienQuan() : "---");
    }

    private void clearHistoryDetail() {
        txtHistoryDetailKtv.setText("---");
        txtHistoryDetailDeadline.setText("---");
        txtHistoryDetailNotes.setText("Chưa có lịch sử sửa chữa cho phương tiện này.");
        txtHistoryDetailDocs.setText("---");
    }

    private JPanel buildStatBox(String label, JLabel valLabel) {
        RoundedPanel box = new RoundedPanel(14, Theme.CARD);
        box.setLayout(new BorderLayout(0, 6));
        box.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel lbl = UI.muted(label);
        lbl.setFont(Theme.font(Font.PLAIN, 12));
        box.add(lbl, BorderLayout.NORTH);

        valLabel.setFont(Theme.font(Font.BOLD, 24));
        valLabel.setForeground(Theme.TEXT);
        box.add(valLabel, BorderLayout.CENTER);

        return box;
    }

    private JPanel formFieldDetail(String label, JTextField tf) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setPreferredSize(new Dimension(200, 18));
        l.setMinimumSize(new Dimension(50, 18));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));

        styleInput(tf);
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setForeground(Theme.TEXT);
        tf.setAlignmentX(LEFT_ALIGNMENT);
        p.add(tf);

        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private void styleInput(JTextField tf) {
        tf.setFont(Theme.font(Font.PLAIN, 13));
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        tf.setPreferredSize(new Dimension(200, 38));
        tf.setMinimumSize(new Dimension(50, 38));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private void attachMouseWheelRecursively(Component comp) {
        if (comp == null) return;
        comp.addMouseWheelListener(e -> {
            JScrollPane parentScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, comp);
            if (parentScroll != null) {
                JScrollBar bar = parentScroll.getVerticalScrollBar();
                if (bar != null) {
                    int amount = e.getWheelRotation() * bar.getUnitIncrement() * 2;
                    bar.setValue(bar.getValue() + amount);
                }
            }
        });
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachMouseWheelRecursively(child);
            }
        }
    }

    public Xe getVehicleInHistory() {
        return vehicleInHistory;
    }

    public KhachHang getCustomerInHistory() {
        return customerInHistory;
    }

    // =========================================================================
    // QUY TẮC BẮT BUỘC: TOÀN BỘ SỰ KIỆN NẰM TẠI initEvents() Ở CUỐI FILE
    // =========================================================================
    private void initEvents() {
        vehHistoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = vehHistoryTable.getSelectedRow();
                if (row >= 0 && row < currentVehicleHistoryRows.size()) {
                    populateHistoryDetail(currentVehicleHistoryRows.get(row));
                }
            }
        });

        vehHistoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    moChiTietPhieuDangChon();
                }
            }
        });

        btnViewPsc.addActionListener(e -> moChiTietPhieuDangChon());
        btnBackCust.addActionListener(e -> onBack.run());
    }

    private void moChiTietPhieuDangChon() {
        int row = vehHistoryTable.getSelectedRow();
        if (row >= 0 && row < currentVehicleHistoryRows.size()) {
            String maPhieu = currentVehicleHistoryRows.get(row).maPhieu();
            if (maPhieu != null && !maPhieu.isBlank() && !maPhieu.equals("---")) {
                Navigator.setPayload(maPhieu);
            }
        }
        Navigator.go(Screen.TIEP_NHAN_XE);
    }
}

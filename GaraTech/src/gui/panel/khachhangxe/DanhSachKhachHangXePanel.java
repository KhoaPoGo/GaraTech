package gui.panel.khachhangxe;

import bus.KhachHangXeBus;
import bus.KhachHangXeBus.KhachHangOverview;
import dao.XeDao.LichSuSuaChuaRow;
import com.formdev.flatlaf.FlatClientProperties;
import entity.KhachHang;
import entity.Xe;
import enums.Role;
import gui.Navigator;
import gui.Screen;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main View: Quản lý danh sách khách hàng, hồ sơ chi tiết và danh sách phương tiện liên kết.
 */
public final class DanhSachKhachHangXePanel extends JPanel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Callbacks
    private final Runnable onAddCustomer;
    private final Consumer<KhachHang> onAddVehicle;
    private final Consumer<Xe> onEditVehicle;
    private final Consumer<Xe> onViewVehicleHistory;

    // Filter & Search
    private final JTextField searchKeywordField = new JTextField();
    private final JTextField searchPlateField = new JTextField();
    private final JLabel lblCustomerCount = UI.muted("0 khách hàng");

    // Customer table
    private final DefaultTableModel tableModel = UI.readOnlyModel("Khách hàng", "Số điện thoại", "Xe liên kết", "Điểm");
    private final JTable customerTable = UI.table(tableModel);
    private final JLabel lblPagingStatus = UI.muted("Đang chọn --- · Trang 1 / 1");

    // Profile card
    private final JLabel lblProfileHeader = UI.h2("Hồ sơ khách hàng");
    private final JButton btnEditProfile = UI.secondaryButton("Chỉnh sửa hồ sơ");
    private final JButton btnAddVehicleTop = UI.primaryButton("+ Thêm xe");
    private final JButton btnDeleteCustomer = UI.dangerButton("Xóa khách hàng");

    private final JTextField txtDetailPhone = new JTextField();
    private final JTextField txtDetailEmail = new JTextField();
    private final JTextField txtDetailAddress = new JTextField();
    private final JTextField txtDetailPoints = new JTextField();

    // Vehicles sub-card
    private final JLabel lblVehicleCountHeader = UI.h2("Phương tiện (0)");
    private final JPanel vehiclesListContainer = new JPanel();

    // Repair history card
    private final JPanel filterChipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final DefaultTableModel historyModel = UI.readOnlyModel("Ngày", "Biển số", "Mã phiếu", "Nội dung", "Hóa đơn");
    private final JTable historyTable = UI.table(historyModel);

    // State
    private List<KhachHangOverview> currentList = new ArrayList<>();
    private KhachHangOverview selectedOverview = null;
    private String selectedHistoryFilter = "ALL";

    public DanhSachKhachHangXePanel(
            Runnable onAddCustomer,
            Consumer<KhachHang> onAddVehicle,
            Consumer<Xe> onEditVehicle,
            Consumer<Xe> onViewVehicleHistory
    ) {
        this.onAddCustomer = onAddCustomer;
        this.onAddVehicle = onAddVehicle;
        this.onEditVehicle = onEditVehicle;
        this.onViewVehicleHistory = onViewVehicleHistory;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        initEvents();
    }

    private void buildUI() {
        add(buildCustomerListCard());
        add(Box.createVerticalStrut(18));
        add(buildCustomerProfileCard());
        add(Box.createVerticalStrut(18));
        add(buildRepairHistoryCard());
    }

    private JComponent buildCustomerListCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UI.h2("Danh sách khách hàng"), BorderLayout.WEST);
        head.add(lblCustomerCount, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Filter row
        JPanel filterRow = new JPanel(new GridLayout(1, 2, 16, 0));
        filterRow.setOpaque(false);

        JPanel pKw = new JPanel(new BorderLayout(0, 4));
        pKw.setOpaque(false);
        pKw.add(UI.muted("Tên / số điện thoại"), BorderLayout.NORTH);
        styleInput(searchKeywordField, "Nguyễn Minh Anh - 0901234567");
        pKw.add(searchKeywordField, BorderLayout.CENTER);

        JPanel pPlate = new JPanel(new BorderLayout(0, 4));
        pPlate.setOpaque(false);
        pPlate.add(UI.muted("Xe liên kết"), BorderLayout.NORTH);
        styleInput(searchPlateField, "Tìm theo biển số xe (vd: 51H-12345)");
        pPlate.add(searchPlateField, BorderLayout.CENTER);

        filterRow.add(pKw);
        filterRow.add(pPlate);
        filterRow.setAlignmentX(LEFT_ALIGNMENT);
        center.add(filterRow);
        center.add(Box.createVerticalStrut(12));

        // Table
        customerTable.setRowHeight(44);
        customerTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        customerTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        customerTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        customerTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        DefaultTableCellRenderer cellR = new DefaultTableCellRenderer();
        cellR.setHorizontalAlignment(SwingConstants.LEFT);
        cellR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < 4; i++) {
            customerTable.getColumnModel().getColumn(i).setCellRenderer(cellR);
        }

        TableCellRenderer headRenderer = customerTable.getTableHeader().getDefaultRenderer();
        if (headRenderer instanceof DefaultTableCellRenderer headR) {
            headR.setHorizontalAlignment(SwingConstants.LEFT);
            headR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        JScrollPane sp = new JScrollPane(customerTable);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        sp.setPreferredSize(new Dimension(800, 220));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        center.add(sp);
        center.add(Box.createVerticalStrut(8));

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(lblPagingStatus, BorderLayout.WEST);

        JPanel pageBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pageBtns.setOpaque(false);
        JButton btnPrev = new JButton("< Trước");
        btnPrev.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        btnPrev.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton btnNext = new JButton("Sau >");
        btnNext.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        btnNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pageBtns.add(btnPrev);
        pageBtns.add(btnNext);
        footer.add(pageBtns, BorderLayout.EAST);
        footer.setAlignmentX(LEFT_ALIGNMENT);
        center.add(footer);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildCustomerProfileCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(lblProfileHeader, BorderLayout.WEST);

        JPanel headActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headActions.setOpaque(false);

        boolean canEdit = Session.role() == null || Session.role() == Role.TIEP_NHAN || Session.role() == Role.QUAN_LY;
        btnEditProfile.setVisible(canEdit);
        btnAddVehicleTop.setVisible(canEdit);
        btnDeleteCustomer.setVisible(canEdit);

        btnEditProfile.addActionListener(e -> openEditCustomerDialog());
        btnAddVehicleTop.addActionListener(e -> {
            if (selectedOverview != null && onAddVehicle != null) {
                onAddVehicle.accept(selectedOverview.khachHang());
            }
        });
        btnDeleteCustomer.addActionListener(e -> handleDeleteCustomer());

        headActions.add(btnDeleteCustomer);
        headActions.add(btnEditProfile);
        headActions.add(btnAddVehicleTop);
        head.add(headActions, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // 1. Contact info sub-card
        RoundedPanel contactCard = new RoundedPanel(12, Theme.BG);
        contactCard.setLayout(new BorderLayout(0, 12));
        contactCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        contactCard.add(UI.h2("Thông tin liên hệ"), BorderLayout.NORTH);

        JPanel contactGrid = new JPanel(new GridLayout(2, 2, 16, 10));
        contactGrid.setOpaque(false);

        contactGrid.add(formFieldReadonly("Số điện thoại", txtDetailPhone, null));
        contactGrid.add(formFieldReadonly("Email", txtDetailEmail, null));
        contactGrid.add(formFieldReadonly("Địa chỉ", txtDetailAddress, null));
        contactGrid.add(formFieldReadonly("Điểm tích lũy", txtDetailPoints, "Hệ thống quản lý · khách mới mặc định 0"));

        contactCard.add(contactGrid, BorderLayout.CENTER);
        contactCard.setAlignmentX(LEFT_ALIGNMENT);
        body.add(contactCard);

        body.add(Box.createVerticalStrut(16));

        // 2. Vehicles sub-card
        RoundedPanel vehiclesCard = new RoundedPanel(12, Theme.BG);
        vehiclesCard.setLayout(new BorderLayout(0, 12));
        vehiclesCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel vHead = new JPanel(new BorderLayout());
        vHead.setOpaque(false);
        vHead.add(lblVehicleCountHeader, BorderLayout.WEST);
        vehiclesCard.add(vHead, BorderLayout.NORTH);

        vehiclesListContainer.setOpaque(false);
        vehiclesListContainer.setLayout(new BoxLayout(vehiclesListContainer, BoxLayout.Y_AXIS));
        vehiclesCard.add(vehiclesListContainer, BorderLayout.CENTER);
        vehiclesCard.setAlignmentX(LEFT_ALIGNMENT);
        body.add(vehiclesCard);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void renderVehiclesList(List<Xe> vehicles) {
        vehiclesListContainer.removeAll();
        int count = vehicles != null ? vehicles.size() : 0;
        lblVehicleCountHeader.setText("Phương tiện (" + count + ")");

        if (vehicles == null || vehicles.isEmpty()) {
            JLabel empty = UI.muted("Khách hàng này chưa đăng ký phương tiện nào.");
            empty.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
            vehiclesListContainer.add(empty);
        } else {
            for (int i = 0; i < vehicles.size(); i++) {
                Xe x = vehicles.get(i);
                boolean isPrimary = (i == 0);
                vehiclesListContainer.add(buildSingleVehicleCard(x, isPrimary));
                if (i < vehicles.size() - 1) {
                    vehiclesListContainer.add(Box.createVerticalStrut(10));
                }
            }
        }
        vehiclesListContainer.revalidate();
        vehiclesListContainer.repaint();
    }

    private JComponent buildSingleVehicleCard(Xe xe, boolean isPrimary) {
        Color bgColor = isPrimary ? new Color(0xFFF9E6) : Color.WHITE;
        Color borderColor = isPrimary ? Theme.YELLOW : Theme.BORDER;

        RoundedPanel card = new RoundedPanel(12, bgColor).borderColor(borderColor);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel lPlate = new JLabel(xe.getBienSo());
        lPlate.setFont(Theme.font(Font.BOLD, 15));
        lPlate.setForeground(Theme.TEXT);
        info.add(lPlate);
        info.add(Box.createVerticalStrut(3));

        JLabel lModel = new JLabel(xe.getHangXe() + " " + xe.getModel() + " " + xe.getNamSanXuat());
        lModel.setFont(Theme.font(Font.BOLD, 13));
        lModel.setForeground(Theme.TEXT);
        info.add(lModel);
        info.add(Box.createVerticalStrut(3));

        String odoStr = String.format("%,.0f km", xe.getSoKmHienTai());
        JLabel lColorOdo = UI.muted(xe.getMauXe() + " · " + odoStr);
        info.add(lColorOdo);
        info.add(Box.createVerticalStrut(3));

        String regDateStr = xe.getNgayDangKy() != null ? xe.getNgayDangKy().format(DMY) : "---";
        String engineStr = xe.getSoMay() != null && !xe.getSoMay().isEmpty() ? xe.getSoMay() : "---";
        JLabel lEngineReg = UI.muted("Số máy: " + engineStr + " · Đăng ký: " + regDateStr);
        info.add(lEngineReg);

        card.add(info, BorderLayout.CENTER);

        // Right actions
        JPanel btnCol = new JPanel();
        btnCol.setOpaque(false);
        btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));

        JButton btnView = UI.secondaryButton("Xem hồ sơ");
        btnView.addActionListener(e -> {
            if (onViewVehicleHistory != null) onViewVehicleHistory.accept(xe);
        });

        JButton btnCheckIn = UI.primaryButton("Tiếp nhận xe");
        btnCheckIn.addActionListener(e -> {
            Navigator.setPayload(xe);
            Navigator.go(Screen.TIEP_NHAN_XE);
        });

        JButton btnDeleteVeh = UI.dangerButton("Xóa xe");
        btnDeleteVeh.addActionListener(e -> handleDeleteVehicle(xe));

        btnView.setAlignmentX(RIGHT_ALIGNMENT);
        btnCheckIn.setAlignmentX(RIGHT_ALIGNMENT);
        btnDeleteVeh.setAlignmentX(RIGHT_ALIGNMENT);
        btnCol.add(btnView);
        btnCol.add(Box.createVerticalStrut(6));
        btnCol.add(btnCheckIn);
        btnCol.add(Box.createVerticalStrut(6));
        btnCol.add(btnDeleteVeh);

        card.add(btnCol, BorderLayout.EAST);
        return card;
    }

    private JComponent buildRepairHistoryCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel head = new JPanel(new BorderLayout(0, 10));
        head.setOpaque(false);
        head.add(UI.h2("Lịch sử sửa chữa"), BorderLayout.NORTH);

        filterChipsPanel.setOpaque(false);
        head.add(filterChipsPanel, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        historyTable.setRowHeight(42);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(220);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        JScrollPane sp = new JScrollPane(historyTable);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        sp.setPreferredSize(new Dimension(800, 180));
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private void renderHistoryFilterChips(List<Xe> vehicles) {
        filterChipsPanel.removeAll();

        int count = vehicles != null ? vehicles.size() : 0;
        JButton btnAll = createChipButton("Tất cả xe (" + count + ")", "ALL".equals(selectedHistoryFilter));
        btnAll.addActionListener(e -> {
            selectedHistoryFilter = "ALL";
            renderHistoryFilterChips(vehicles);
            loadHistory(selectedHistoryFilter);
        });
        filterChipsPanel.add(btnAll);

        if (vehicles != null) {
            for (Xe x : vehicles) {
                boolean active = x.getBienSo().equals(selectedHistoryFilter);
                JButton btnChip = createChipButton(x.getBienSo(), active);
                btnChip.addActionListener(e -> {
                    selectedHistoryFilter = x.getBienSo();
                    renderHistoryFilterChips(vehicles);
                    loadHistory(selectedHistoryFilter);
                });
                filterChipsPanel.add(btnChip);
            }
        }
        filterChipsPanel.revalidate();
        filterChipsPanel.repaint();
    }

    private JButton createChipButton(String text, boolean active) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.PLAIN, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) {
            b.setBackground(new Color(0x10, 0x25, 0x44));
            b.setForeground(Color.WHITE);
            b.putClientProperty(FlatClientProperties.STYLE, "arc:14; borderWidth:0; margin:4,12,4,12");
        } else {
            b.setBackground(Theme.BG);
            b.setForeground(Theme.TEXT);
            b.putClientProperty(FlatClientProperties.STYLE, "arc:14; borderWidth:1; borderColor:#D5DBE5; margin:4,12,4,12");
        }
        return b;
    }

    private void loadHistory(String filter) {
        historyModel.setRowCount(0);
        if (selectedOverview == null) return;

        List<Xe> vehicles = selectedOverview.danhSachXe();
        List<LichSuSuaChuaRow> rows = KhachHangXeBus.getLichSuSuaChua(filter, vehicles);

        for (LichSuSuaChuaRow r : rows) {
            historyModel.addRow(new Object[]{
                    r.ngay(),
                    r.bienSo(),
                    r.maPhieu(),
                    r.noiDung(),
                    r.hoaDon()
            });
        }
    }

    private void initEvents() {
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = customerTable.getSelectedRow();
                if (row >= 0 && row < currentList.size()) {
                    selectCustomer(currentList.get(row));
                    lblPagingStatus.setText(String.format("Đang chọn: %s · %d / %d khách hàng",
                            currentList.get(row).hoTen(), row + 1, currentList.size()));
                }
            }
        });

        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = historyTable.getSelectedRow();
                    if (row >= 0) {
                        Object val = historyModel.getValueAt(row, 2);
                        if (val != null) {
                            String maPhieu = val.toString().trim();
                            if (!maPhieu.isEmpty() && !maPhieu.equals("---")) {
                                Navigator.setPayload(maPhieu);
                                Navigator.go(Screen.TIEP_NHAN_XE);
                            }
                        }
                    }
                }
            }
        });

        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { doSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { doSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { doSearch(); }
        };
        searchKeywordField.getDocument().addDocumentListener(dl);
        searchPlateField.getDocument().addDocumentListener(dl);
    }

    public void loadData() {
        currentList = KhachHangXeBus.getAll();
        updateCustomerTable(currentList);
    }

    private void doSearch() {
        String kw = searchKeywordField.getText();
        String plate = searchPlateField.getText();
        currentList = KhachHangXeBus.search(kw, plate);
        updateCustomerTable(currentList);
    }

    private void updateCustomerTable(List<KhachHangOverview> list) {
        tableModel.setRowCount(0);
        for (KhachHangOverview k : list) {
            tableModel.addRow(new Object[]{
                    k.hoTen() + " (" + k.maKH() + ")",
                    k.soDienThoai(),
                    k.xeSummary(),
                    k.diemTichLuy()
            });
        }
        lblCustomerCount.setText(list.size() + " khách hàng");

        if (!list.isEmpty()) {
            customerTable.setRowSelectionInterval(0, 0);
            selectCustomer(list.get(0));
            lblPagingStatus.setText(String.format("Đang chọn: %s · 1 / %d khách hàng", list.get(0).hoTen(), list.size()));
        } else {
            clearDetail();
            lblPagingStatus.setText("Không tìm thấy khách hàng nào");
        }
    }

    public void selectCustomer(KhachHangOverview item) {
        this.selectedOverview = item;
        if (item == null) {
            clearDetail();
            return;
        }

        KhachHang kh = item.khachHang();
        lblProfileHeader.setText("Hồ sơ " + kh.getHoTen() + " · " + kh.getMaKH());
        txtDetailPhone.setText(kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "");
        txtDetailEmail.setText(kh.getEmail() != null && !kh.getEmail().isEmpty() ? kh.getEmail() : "Chưa có email");
        txtDetailAddress.setText(kh.getDiaChi() != null && !kh.getDiaChi().isEmpty() ? kh.getDiaChi() : "Chưa có địa chỉ");
        txtDetailPoints.setText(String.valueOf(kh.getDiemTichLuy()));

        renderVehiclesList(item.danhSachXe());

        selectedHistoryFilter = "ALL";
        renderHistoryFilterChips(item.danhSachXe());
        loadHistory(selectedHistoryFilter);
    }

    public void selectCustomerById(String maKH) {
        if (maKH == null) return;
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).maKH().equals(maKH)) {
                customerTable.setRowSelectionInterval(i, i);
                selectCustomer(currentList.get(i));
                break;
            }
        }
    }

    private void clearDetail() {
        lblProfileHeader.setText("Hồ sơ khách hàng");
        txtDetailPhone.setText("");
        txtDetailEmail.setText("");
        txtDetailAddress.setText("");
        txtDetailPoints.setText("");
        vehiclesListContainer.removeAll();
        vehiclesListContainer.revalidate();
        vehiclesListContainer.repaint();
        filterChipsPanel.removeAll();
        filterChipsPanel.revalidate();
        filterChipsPanel.repaint();
        historyModel.setRowCount(0);
    }

    private void openEditCustomerDialog() {
        if (selectedOverview == null) return;
        KhachHang kh = selectedOverview.khachHang();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Chỉnh sửa hồ sơ khách hàng", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(null);

        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        p.add(UI.h2("Hồ sơ " + kh.getHoTen() + " (" + kh.getMaKH() + ")"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(5, 1, 0, 10));
        fields.setOpaque(false);

        JTextField tfName = new JTextField(kh.getHoTen());
        JTextField tfPhone = new JTextField(kh.getSoDienThoai());
        JTextField tfEmail = new JTextField(kh.getEmail() != null ? kh.getEmail() : "");
        JTextField tfAddress = new JTextField(kh.getDiaChi() != null ? kh.getDiaChi() : "");
        JTextField tfPoints = new JTextField(String.valueOf(kh.getDiemTichLuy()));

        fields.add(formFieldInput("Họ tên *", tfName, null));
        fields.add(formFieldInput("Số điện thoại *", tfPhone, null));
        fields.add(formFieldInput("Email", tfEmail, null));
        fields.add(formFieldInput("Địa chỉ", tfAddress, null));
        fields.add(formFieldInput("Điểm tích lũy", tfPoints, null));
        p.add(fields, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bot.setOpaque(false);
        JButton btnCancel = UI.secondaryButton("Hủy");
        JButton btnSave = UI.primaryButton("Lưu thay đổi");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                kh.setHoTen(tfName.getText().trim());
                kh.setSoDienThoai(tfPhone.getText().trim());
                kh.setEmail(tfEmail.getText().trim());
                kh.setDiaChi(tfAddress.getText().trim());
                kh.setDiemTichLuy(Integer.parseInt(tfPoints.getText().trim()));

                KhachHangXeBus.capNhatKhachHang(kh);
                Msg.info(dialog, "Cập nhật thông tin khách hàng thành công!");
                dialog.dispose();

                loadData();
                selectCustomerById(kh.getMaKH());
            } catch (IllegalArgumentException ex) {
                Msg.error(dialog, ex.getMessage());
            } catch (Exception ex) {
                Msg.error(dialog, "Lỗi: " + ex.getMessage());
            }
        });

        bot.add(btnCancel);
        bot.add(btnSave);
        p.add(bot, BorderLayout.SOUTH);

        dialog.setContentPane(p);
        dialog.setVisible(true);
    }

    private void handleDeleteCustomer() {
        if (selectedOverview == null) {
            Msg.error(this, "Vui lòng chọn khách hàng cần xóa.");
            return;
        }
        KhachHang kh = selectedOverview.khachHang();
        if (!Msg.confirm(this, "Bạn có chắc chắn muốn xóa khách hàng " + kh.getHoTen() + " (" + kh.getMaKH() + ") không?")) {
            return;
        }
        try {
            KhachHangXeBus.xoaKhachHang(kh.getMaKH());
            Msg.info(this, "Đã xóa khách hàng " + kh.getHoTen() + " thành công.");
            selectedOverview = null;
            loadData();
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi xóa khách hàng: " + ex.getMessage());
        }
    }

    private void handleDeleteVehicle(Xe xe) {
        if (xe == null) {
            Msg.error(this, "Vui lòng chọn phương tiện cần xóa.");
            return;
        }
        if (!Msg.confirm(this, "Bạn có chắc chắn muốn xóa phương tiện có biển số " + xe.getBienSo() + " không?")) {
            return;
        }
        try {
            KhachHangXeBus.xoaXe(xe.getBienSo());
            Msg.info(this, "Đã xóa phương tiện " + xe.getBienSo() + " thành công.");
            loadData();
            if (selectedOverview != null) {
                selectCustomerById(selectedOverview.maKH());
            }
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi xóa phương tiện: " + ex.getMessage());
        }
    }

    private JPanel formFieldInput(String label, JTextField tf, String hint) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
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
        if (placeholder != null) {
            tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
    }

    public KhachHangOverview getSelectedOverview() {
        return selectedOverview;
    }
}

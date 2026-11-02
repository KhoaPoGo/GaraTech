package gui.panel;

import bus.KhachHangXeBus;
import bus.KhachHangXeBus.KhachHangOverview;
import com.formdev.flatlaf.FlatClientProperties;
import dao.XeDao.LichSuSuaChuaRow;
import entity.KhachHang;
import entity.Xe;
import enums.Role;
import gui.Navigator;
import gui.Screen;
import gui.component.BasePanel;
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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class KhachHangXePanel extends BasePanel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String CARD_MAIN = "MAIN";
    private static final String CARD_ADD_CUSTOMER = "ADD_CUSTOMER";
    private static final String CARD_ADD_VEHICLE = "ADD_VEHICLE";
    private static final String CARD_EDIT_VEHICLE = "EDIT_VEHICLE";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // =========================================================================
    // 1. MAIN VIEW COMPONENTS
    // =========================================================================
    private final JTextField searchKeywordField = new JTextField();
    private final JTextField searchPlateField = new JTextField();
    private final JLabel lblCustomerCount = UI.muted("0 khách hàng");

    private final DefaultTableModel tableModel = UI.readOnlyModel("Khách hàng", "Số điện thoại", "Xe liên kết", "Điểm");
    private final JTable customerTable = UI.table(tableModel);
    private final JLabel lblPagingStatus = UI.muted("Đang chọn --- · Trang 1 / 1");

    // Profile section
    private final JLabel lblProfileHeader = UI.h2("Hồ sơ khách hàng");
    private final JButton btnEditProfile = UI.secondaryButton("Chỉnh sửa hồ sơ");
    private final JButton btnAddVehicleTop = UI.primaryButton("+ Thêm xe");

    // Contact info fields
    private final JTextField txtDetailPhone = new JTextField();
    private final JTextField txtDetailEmail = new JTextField();
    private final JTextField txtDetailAddress = new JTextField();
    private final JTextField txtDetailPoints = new JTextField();

    // Vehicle cards container
    private final JLabel lblVehicleCountHeader = UI.h2("Phương tiện (0)");
    private final JPanel vehiclesListContainer = new JPanel();

    // Repair history section
    private final JPanel filterChipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final DefaultTableModel historyModel = UI.readOnlyModel("Ngày", "Biển số", "Phiếu sửa chữa", "Nội dung", "Hóa đơn");
    private final JTable historyTable = UI.table(historyModel);

    // Action button in top header of BasePanel
    private final JButton btnAddCustomerHeader = UI.primaryButton("+ Thêm khách hàng");

    // =========================================================================
    // 2. ADD CUSTOMER VIEW COMPONENTS
    // =========================================================================
    private final JTextField txtAddCustCode = new JTextField("Tự động khi lưu");
    private final JTextField txtAddCustName = new JTextField();
    private final JTextField txtAddCustPhone = new JTextField();
    private final JTextField txtAddCustEmail = new JTextField();
    private final JTextField txtAddCustAddress = new JTextField();
    private final JTextField txtAddCustPoints = new JTextField("0");

    // =========================================================================
    // 3. ADD VEHICLE VIEW COMPONENTS
    // =========================================================================
    private final JLabel lblAddVehCustName = UI.h1("Tên khách hàng");
    private final JLabel lblAddVehCustSub = UI.muted("KH00000 · SĐT: 0000000000 · Thông tin khách hàng chỉ đọc");
    private final JLabel lblAddVehPlatesCompare = UI.muted("Biển số đã có để đối chiếu: ---");
    private final JLabel lblAddVehPointsStat = UI.h1("0");
    private final JLabel lblAddVehCountStat = UI.h1("0 xe");

    private final JTextField txtAddVehPlate = new JTextField();
    private final JTextField txtAddVehMakeModel = new JTextField();
    private final JTextField txtAddVehYear = new JTextField(String.valueOf(LocalDate.now().getYear()));
    private final JTextField txtAddVehColor = new JTextField();
    private final JTextField txtAddVehEngine = new JTextField();
    private final JTextField txtAddVehOdo = new JTextField("0");
    private final JTextField txtAddVehRegDate = new JTextField(LocalDate.now().format(DMY));
    private final JLabel lblAddVehReturnNote = UI.muted("");

    // =========================================================================
    // 4. EDIT VEHICLE VIEW COMPONENTS
    // =========================================================================
    private final JTextField txtEditVehOwner = new JTextField();
    private final JTextField txtEditVehPlate = new JTextField();
    private final JTextField txtEditVehMakeModel = new JTextField();
    private final JTextField txtEditVehYear = new JTextField();
    private final JTextField txtEditVehColor = new JTextField();
    private final JTextField txtEditVehEngine = new JTextField();
    private final JTextField txtEditVehOdo = new JTextField();
    private final JLabel lblEditVehOdoHint = UI.muted("Không nhỏ hơn số km lần trước");
    private final JTextField txtEditVehRegDate = new JTextField();
    private float editVehPreviousOdo = 0;

    // State
    private List<KhachHangOverview> currentList = new ArrayList<>();
    private KhachHangOverview selectedOverview = null;
    private KhachHang customerForAddingVehicle = null;
    private Xe vehicleToEdit = null;
    private String selectedHistoryFilter = "ALL";

    public KhachHangXePanel() {
        super("GARATECH / KHÁCH HÀNG & XE", "Khách hàng & phương tiện",
                "Tra cứu hồ sơ khách hàng, phương tiện và lịch sử sửa chữa được liên kết.");

        boolean canEdit = Session.role() == null || Session.role() == Role.TIEP_NHAN || Session.role() == Role.QUAN_LY;

        btnAddCustomerHeader.setVisible(canEdit);
        btnAddCustomerHeader.addActionListener(e -> switchToAddCustomerView());
        actions.add(btnAddCustomerHeader);

        // Setup CardLayout
        cardsPanel.setOpaque(false);
        cardsPanel.add(buildMainView(), CARD_MAIN);
        cardsPanel.add(buildAddCustomerView(), CARD_ADD_CUSTOMER);
        cardsPanel.add(buildAddVehicleView(), CARD_ADD_VEHICLE);
        cardsPanel.add(buildEditVehicleView(), CARD_EDIT_VEHICLE);

        body.add(cardsPanel, BorderLayout.CENTER);

        // Bind table events
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = customerTable.getSelectedRow();
                if (row >= 0 && row < currentList.size()) {
                    selectCustomer(currentList.get(row));
                }
            }
        });

        // Search listeners
        DocumentListener searchListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { doSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { doSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { doSearch(); }
        };
        searchKeywordField.getDocument().addDocumentListener(searchListener);
        searchPlateField.getDocument().addDocumentListener(searchListener);

        onShow();
    }

    @Override
    public void onShow() {
        loadData();
    }

    private void loadData() {
        currentList = KhachHangXeBus.getAll();
        updateCustomerTable(currentList);
        if (!currentList.isEmpty()) {
            customerTable.setRowSelectionInterval(0, 0);
            selectCustomer(currentList.get(0));
        } else {
            clearDetail();
        }
    }

    private void doSearch() {
        String kw = searchKeywordField.getText();
        String plate = searchPlateField.getText();
        currentList = KhachHangXeBus.search(kw, plate);
        updateCustomerTable(currentList);
        if (!currentList.isEmpty()) {
            customerTable.setRowSelectionInterval(0, 0);
            selectCustomer(currentList.get(0));
        } else {
            clearDetail();
        }
    }

    private void updateCustomerTable(List<KhachHangOverview> list) {
        tableModel.setRowCount(0);
        for (KhachHangOverview item : list) {
            tableModel.addRow(new Object[]{
                    "<html><b>" + item.hoTen() + "</b> <span style='color:#24508F'>↗</span></html>",
                    item.soDienThoai(),
                    item.xeSummary(),
                    String.valueOf(item.diemTichLuy())
            });
        }
        lblCustomerCount.setText(list.size() + " khách hàng");
    }

    private void selectCustomer(KhachHangOverview item) {
        this.selectedOverview = item;
        if (item == null) {
            clearDetail();
            return;
        }

        KhachHang kh = item.khachHang();
        lblProfileHeader.setText("Hồ sơ " + kh.getHoTen() + " " + kh.getMaKH());
        lblPagingStatus.setText("Đang chọn " + kh.getHoTen() + " · Tổng " + currentList.size() + " hồ sơ");

        // Contact info
        txtDetailPhone.setText(kh.getSoDienThoai());
        txtDetailEmail.setText(kh.getEmail() != null ? kh.getEmail() : "");
        txtDetailAddress.setText(kh.getDiaChi() != null ? kh.getDiaChi() : "");
        txtDetailPoints.setText(String.valueOf(kh.getDiemTichLuy()));

        // Vehicles
        renderVehiclesList(item.danhSachXe());

        // History
        renderHistoryFilterChips(item.danhSachXe());
        loadHistory(selectedHistoryFilter);
    }

    private void clearDetail() {
        lblProfileHeader.setText("Hồ sơ khách hàng");
        lblPagingStatus.setText("Không có hồ sơ");
        txtDetailPhone.setText("");
        txtDetailEmail.setText("");
        txtDetailAddress.setText("");
        txtDetailPoints.setText("");
        vehiclesListContainer.removeAll();
        vehiclesListContainer.revalidate();
        vehiclesListContainer.repaint();
        historyModel.setRowCount(0);
    }

    // =========================================================================
    // 1. BUILD MAIN VIEW
    // =========================================================================
    private JComponent buildMainView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(buildCustomerListCard());
        panel.add(Box.createVerticalStrut(18));
        panel.add(buildCustomerProfileCard());
        panel.add(Box.createVerticalStrut(18));
        panel.add(buildRepairHistoryCard());

        return panel;
    }

    private JComponent buildCustomerListCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Header: Title + Count
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UI.h2("Danh sách khách hàng"), BorderLayout.WEST);
        head.add(lblCustomerCount, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Center: Filters + Table
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

        DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
        rightR.setHorizontalAlignment(SwingConstants.RIGHT);
        customerTable.getColumnModel().getColumn(3).setCellRenderer(rightR);

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

        // Header: Profile Name + Edit Profile + Add Vehicle Button
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(lblProfileHeader, BorderLayout.WEST);

        JPanel headActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headActions.setOpaque(false);

        btnEditProfile.addActionListener(e -> openEditCustomerDialog());
        btnAddVehicleTop.addActionListener(e -> {
            if (selectedOverview != null) {
                switchToAddVehicleView(selectedOverview.khachHang());
            }
        });

        headActions.add(btnEditProfile);
        headActions.add(btnAddVehicleTop);
        head.add(headActions, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Body: Contact info sub-card + Vehicles sub-card
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

        JButton btnAddVehInner = UI.primaryButton("+ Thêm xe");
        btnAddVehInner.addActionListener(e -> {
            if (selectedOverview != null) {
                switchToAddVehicleView(selectedOverview.khachHang());
            }
        });
        vHead.add(btnAddVehInner, BorderLayout.EAST);
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
                boolean isPrimary = (i == 0); // First vehicle highlighted as in mockup
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

        // Left info
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
        btnView.addActionListener(e -> switchToEditVehicleView(xe));

        JButton btnCheckIn = UI.primaryButton("Tiếp nhận xe");
        btnCheckIn.addActionListener(e -> {
            // Chuyển thẳng sang bước lập Phiếu tiếp nhận với xe này
            Navigator.go(Screen.TIEP_NHAN_XE);
        });

        btnView.setAlignmentX(RIGHT_ALIGNMENT);
        btnCheckIn.setAlignmentX(RIGHT_ALIGNMENT);
        btnCol.add(btnView);
        btnCol.add(Box.createVerticalStrut(6));
        btnCol.add(btnCheckIn);

        card.add(btnCol, BorderLayout.EAST);
        return card;
    }

    private JComponent buildRepairHistoryCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        // Header: Title + Filter Chips
        JPanel head = new JPanel(new BorderLayout(0, 10));
        head.setOpaque(false);
        head.add(UI.h2("Lịch sử sửa chữa"), BorderLayout.NORTH);

        filterChipsPanel.setOpaque(false);
        head.add(filterChipsPanel, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        // Table
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
        b.setFont(Theme.font(active ? Font.BOLD : Font.PLAIN, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) {
            b.putClientProperty(FlatClientProperties.STYLE,
                    "background:#FBC52D; foreground:#0F2342; borderWidth:0; arc:12; margin:4,12,4,12");
        } else {
            b.putClientProperty(FlatClientProperties.STYLE,
                    "background:#FFFFFF; foreground:#6B7A90; borderColor:#D5DBE5; arc:12; margin:4,12,4,12");
        }
        return b;
    }

    private void loadHistory(String filter) {
        historyModel.setRowCount(0);
        List<Xe> allVehicles = selectedOverview != null ? selectedOverview.danhSachXe() : Collections.emptyList();
        List<LichSuSuaChuaRow> history = KhachHangXeBus.getLichSuSuaChua(filter, allVehicles);

        for (LichSuSuaChuaRow row : history) {
            String pscHtml = "<html><span style='color:#24508F; font-weight:bold'>" + row.maPhieuSC() + " ↗</span></html>";
            String hdHtml = row.hoaDon().startsWith("HD")
                    ? "<html><span style='color:#24508F; font-weight:bold'>" + row.hoaDon() + " ↗</span></html>"
                    : "<html><span style='color:#6B7A90'>" + row.hoaDon() + "</span></html>";

            historyModel.addRow(new Object[]{
                    row.ngay(),
                    row.bienSo(),
                    pscHtml,
                    row.noiDung(),
                    hdHtml
            });
        }
    }

    // =========================================================================
    // 2. BUILD ADD CUSTOMER VIEW
    // =========================================================================
    private JComponent buildAddCustomerView() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        card.add(UI.h2("Thông tin liên hệ"), BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(3, 2, 20, 16));
        formGrid.setOpaque(false);

        // Row 1
        formGrid.add(formFieldReadonly("Mã khách hàng", txtAddCustCode, "Hệ thống cấp mã khi lưu"));
        formGrid.add(formFieldInput("Họ tên *", txtAddCustName, null));

        // Row 2
        formGrid.add(formFieldInput("Số điện thoại *", txtAddCustPhone, "10 chữ số, bắt đầu bằng 0; không trùng"));
        formGrid.add(formFieldInput("Email", txtAddCustEmail, "Không bắt buộc"));

        // Row 3
        formGrid.add(formFieldInput("Địa chỉ", txtAddCustAddress, null));
        formGrid.add(formFieldInput("Điểm tích lũy", txtAddCustPoints, "Hệ thống quản lý · khách mới mặc định 0"));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(formGrid);
        center.add(Box.createVerticalStrut(18));

        // Blue notice
        RoundedPanel notice = new RoundedPanel(10, Theme.INFO_BG).borderColor(new Color(0xB8D5F8));
        notice.setLayout(new BorderLayout());
        notice.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel lblNotice = new JLabel("Khi thêm mới, mã được tự sinh theo quy tắc KH-YYMM-STT trong báo cáo. Không xóa hồ sơ đã có chứng từ.");
        lblNotice.setFont(Theme.font(Font.PLAIN, 12));
        lblNotice.setForeground(Theme.INFO);
        notice.add(lblNotice, BorderLayout.CENTER);
        notice.setAlignmentX(LEFT_ALIGNMENT);
        center.add(notice);

        card.add(center, BorderLayout.CENTER);

        // Bottom Actions: Lưu và thêm xe | Hủy
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bot.setOpaque(false);

        JButton btnSaveAndAddVeh = UI.primaryButton("Lưu và thêm xe");
        JButton btnCancel = UI.secondaryButton("Hủy");

        btnSaveAndAddVeh.addActionListener(e -> handleSaveCustomerAndProceed());
        btnCancel.addActionListener(e -> switchToMainView());

        bot.add(btnSaveAndAddVeh);
        bot.add(btnCancel);
        card.add(bot, BorderLayout.SOUTH);

        return card;
    }

    private void handleSaveCustomerAndProceed() {
        try {
            String hoTen = txtAddCustName.getText().trim();
            String phone = txtAddCustPhone.getText().trim();
            String email = txtAddCustEmail.getText().trim();
            String address = txtAddCustAddress.getText().trim();
            int points = 0;
            try {
                points = Integer.parseInt(txtAddCustPoints.getText().trim());
            } catch (NumberFormatException ignored) {}

            KhachHang newKh = new KhachHang(null, hoTen, phone, address, email, points);
            KhachHang savedKh = KhachHangXeBus.themKhachHang(newKh);

            Msg.info(this, "Đã tạo hồ sơ khách hàng " + savedKh.getHoTen() + " (" + savedKh.getMaKH() + "). Tiến hành thêm xe.");
            // Seamless transition: Chuyển tiếp ngay sang bước thêm xe cho khách hàng vừa tạo
            switchToAddVehicleView(savedKh);
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi lưu khách hàng: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 3. BUILD ADD VEHICLE VIEW
    // =========================================================================
    private JComponent buildAddVehicleView() {
        JPanel view = new JPanel();
        view.setOpaque(false);
        view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));

        // 1. Customer summary header banner card
        RoundedPanel banner = new RoundedPanel(14, Theme.CARD);
        banner.setLayout(new BorderLayout(20, 0));
        banner.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel bLeft = new JPanel();
        bLeft.setOpaque(false);
        bLeft.setLayout(new BoxLayout(bLeft, BoxLayout.Y_AXIS));
        bLeft.add(lblAddVehCustName);
        bLeft.add(Box.createVerticalStrut(4));
        bLeft.add(lblAddVehCustSub);
        bLeft.add(Box.createVerticalStrut(4));
        bLeft.add(lblAddVehPlatesCompare);
        banner.add(bLeft, BorderLayout.CENTER);

        JPanel bRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 0));
        bRight.setOpaque(false);

        JPanel statPoints = new JPanel(new BorderLayout(0, 2));
        statPoints.setOpaque(false);
        statPoints.add(lblAddVehPointsStat, BorderLayout.NORTH);
        statPoints.add(UI.muted("Điểm tích lũy"), BorderLayout.SOUTH);

        JPanel statCount = new JPanel(new BorderLayout(0, 2));
        statCount.setOpaque(false);
        statCount.add(lblAddVehCountStat, BorderLayout.NORTH);
        statCount.add(UI.muted("Đang thuộc khách hàng"), BorderLayout.SOUTH);

        bRight.add(statPoints);
        bRight.add(statCount);
        banner.add(bRight, BorderLayout.EAST);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        view.add(banner);

        view.add(Box.createVerticalStrut(18));

        // 2. Vehicle form card
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel formHead = new JPanel(new BorderLayout());
        formHead.setOpaque(false);
        formHead.add(UI.h2("Thông tin phương tiện"), BorderLayout.WEST);
        formHead.add(UI.muted("* Thông tin bắt buộc"), BorderLayout.EAST);
        card.add(formHead, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 16));
        grid.setOpaque(false);

        grid.add(formFieldInput("Biển số *", txtAddVehPlate, "Chữ in hoa; 6~12 ký tự; không trùng bất kỳ xe đã có."));
        grid.add(formFieldInput("Hãng / dòng xe *", txtAddVehMakeModel, "Nhập hãng / dòng xe (ví dụ: Kia / Seltos)"));

        grid.add(formFieldInput("Năm sản xuất *", txtAddVehYear, "Từ 1980 đến năm hiện tại"));
        grid.add(formFieldInput("Màu sơn *", txtAddVehColor, "Nhập màu sơn"));

        grid.add(formFieldInput("Số máy", txtAddVehEngine, "Không bắt buộc · 5~20 chữ in hoa và chữ số"));
        grid.add(formFieldInput("ODO hiện tại *", txtAddVehOdo, "Số km không âm"));

        grid.add(formFieldInput("Ngày đăng ký *", txtAddVehRegDate, "Không sau ngày hiện tại"));

        // Right blue box
        RoundedPanel pBox = new RoundedPanel(10, Theme.INFO_BG).borderColor(new Color(0xB8D5F8));
        pBox.setLayout(new BorderLayout());
        pBox.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel lblNotice = new JLabel("<html><b>Biển số phải là duy nhất</b><br>"
                + "<span style='font-size:11px'>Không thể lưu nếu biển số trùng với xe đã có trong hệ thống, kể cả xe của khách hàng này.</span></html>");
        lblNotice.setForeground(Theme.INFO);
        pBox.add(lblNotice, BorderLayout.CENTER);
        grid.add(pBox);

        card.add(grid, BorderLayout.CENTER);

        // Bottom Actions: Lưu xe | Hủy
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        bot.setOpaque(false);

        JButton btnSaveVeh = UI.primaryButton("Lưu xe");
        JButton btnCancelVeh = UI.secondaryButton("Hủy");

        btnSaveVeh.addActionListener(e -> handleSaveVehicle());
        btnCancelVeh.addActionListener(e -> switchToMainView());

        bot.add(btnSaveVeh);
        bot.add(btnCancelVeh);
        bot.add(lblAddVehReturnNote);

        card.add(bot, BorderLayout.SOUTH);
        card.setAlignmentX(LEFT_ALIGNMENT);
        view.add(card);

        return view;
    }

    private void handleSaveVehicle() {
        if (customerForAddingVehicle == null) return;
        try {
            String plate = txtAddVehPlate.getText().trim().toUpperCase();
            String makeModel = txtAddVehMakeModel.getText().trim();
            String hangXe = "";
            String model = "";
            if (makeModel.contains("/")) {
                String[] p = makeModel.split("/", 2);
                hangXe = p[0].trim();
                model = p[1].trim();
            } else if (makeModel.contains(" ")) {
                int idx = makeModel.indexOf(" ");
                hangXe = makeModel.substring(0, idx).trim();
                model = makeModel.substring(idx + 1).trim();
            } else {
                hangXe = makeModel;
                model = makeModel;
            }

            int year = Integer.parseInt(txtAddVehYear.getText().trim());
            String color = txtAddVehColor.getText().trim();
            float odo = Float.parseFloat(txtAddVehOdo.getText().trim().replaceAll("[^0-9.]", ""));
            String engine = txtAddVehEngine.getText().trim();
            LocalDateTime regDate = LocalDate.parse(txtAddVehRegDate.getText().trim(), DMY).atStartOfDay();

            Xe newXe = new Xe(plate, customerForAddingVehicle, hangXe, model, year, color, engine, odo, regDate);
            KhachHangXeBus.themXeChoKhachHang(newXe);

            Msg.info(this, "Đã thêm phương tiện " + plate + " cho khách hàng " + customerForAddingVehicle.getHoTen() + "!");

            // Switch to main view and select this customer
            switchToMainView();
            loadData();
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).maKH().equals(customerForAddingVehicle.getMaKH())) {
                    customerTable.setRowSelectionInterval(i, i);
                    selectCustomer(currentList.get(i));
                    break;
                }
            }
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi lưu phương tiện: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 4. BUILD EDIT VEHICLE VIEW
    // =========================================================================
    private JComponent buildEditVehicleView() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        card.add(UI.h2("Thông tin phương tiện"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 16));
        grid.setOpaque(false);

        grid.add(formFieldReadonly("Chủ xe *", txtEditVehOwner, null));
        grid.add(formFieldReadonly("Biển số *", txtEditVehPlate, "Chữ in hoa; 6~12 ký tự; duy nhất"));

        grid.add(formFieldInput("Hãng / dòng xe *", txtEditVehMakeModel, null));
        grid.add(formFieldInput("Năm sản xuất *", txtEditVehYear, "Từ 1980 đến năm hiện tại"));

        grid.add(formFieldInput("Màu sơn *", txtEditVehColor, null));
        grid.add(formFieldInput("Số máy", txtEditVehEngine, "5~20 chữ in hoa và chữ số"));

        grid.add(formFieldInput("ODO hiện tại *", txtEditVehOdo, "Không nhỏ hơn số km lần trước"));
        grid.add(formFieldInput("Ngày đăng ký *", txtEditVehRegDate, "Không sau ngày hiện tại"));

        card.add(grid, BorderLayout.CENTER);

        // Bottom Actions: Lưu phương tiện | Hủy
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        bot.setOpaque(false);

        JButton btnSaveEditVeh = UI.primaryButton("Lưu phương tiện");
        JButton btnCancelEditVeh = UI.secondaryButton("Hủy");

        btnSaveEditVeh.addActionListener(e -> handleUpdateVehicle());
        btnCancelEditVeh.addActionListener(e -> switchToMainView());

        bot.add(btnSaveEditVeh);
        bot.add(btnCancelEditVeh);

        card.add(bot, BorderLayout.SOUTH);
        return card;
    }

    private void handleUpdateVehicle() {
        if (vehicleToEdit == null) return;
        try {
            String makeModel = txtEditVehMakeModel.getText().trim();
            String hangXe = "";
            String model = "";
            if (makeModel.contains("/")) {
                String[] p = makeModel.split("/", 2);
                hangXe = p[0].trim();
                model = p[1].trim();
            } else if (makeModel.contains(" ")) {
                int idx = makeModel.indexOf(" ");
                hangXe = makeModel.substring(0, idx).trim();
                model = makeModel.substring(idx + 1).trim();
            } else {
                hangXe = makeModel;
                model = makeModel;
            }

            int year = Integer.parseInt(txtEditVehYear.getText().trim());
            String color = txtEditVehColor.getText().trim();
            float newOdo = Float.parseFloat(txtEditVehOdo.getText().trim().replaceAll("[^0-9.]", ""));
            String engine = txtEditVehEngine.getText().trim();
            LocalDateTime regDate = LocalDate.parse(txtEditVehRegDate.getText().trim(), DMY).atStartOfDay();

            vehicleToEdit.setHangXe(hangXe);
            vehicleToEdit.setModel(model);
            vehicleToEdit.setNamSanXuat(year);
            vehicleToEdit.setMauXe(color);
            vehicleToEdit.setSoKmHienTai(newOdo);
            vehicleToEdit.setSoMay(engine);
            vehicleToEdit.setNgayDangKy(regDate);

            // Cập nhật với kiểm tra ràng buộc ODO không nhỏ hơn số km lần trước
            KhachHangXeBus.capNhatPhuongTien(vehicleToEdit, editVehPreviousOdo);

            Msg.info(this, "Cập nhật phương tiện " + vehicleToEdit.getBienSo() + " thành công!");

            switchToMainView();
            loadData();
            if (selectedOverview != null) {
                for (int i = 0; i < currentList.size(); i++) {
                    if (currentList.get(i).maKH().equals(selectedOverview.maKH())) {
                        customerTable.setRowSelectionInterval(i, i);
                        selectCustomer(currentList.get(i));
                        break;
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi cập nhật phương tiện: " + ex.getMessage());
        }
    }

    // =========================================================================
    // EDIT CUSTOMER MODAL DIALOG
    // =========================================================================
    private void openEditCustomerDialog() {
        if (selectedOverview == null) return;
        KhachHang kh = selectedOverview.khachHang();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Chỉnh sửa hồ sơ khách hàng", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);

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
                for (int i = 0; i < currentList.size(); i++) {
                    if (currentList.get(i).maKH().equals(kh.getMaKH())) {
                        customerTable.setRowSelectionInterval(i, i);
                        selectCustomer(currentList.get(i));
                        break;
                    }
                }
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

    // =========================================================================
    // NAVIGATION METHODS
    // =========================================================================
    private void switchToMainView() {
        setHeader("GARATECH / KHÁCH HÀNG & XE", "Khách hàng & phương tiện",
                "Tra cứu hồ sơ khách hàng, phương tiện và lịch sử sửa chữa được liên kết.");
        actions.removeAll();
        actions.add(btnAddCustomerHeader);
        actions.revalidate();
        actions.repaint();
        cardLayout.show(cardsPanel, CARD_MAIN);
    }

    private void switchToAddCustomerView() {
        setHeader("GARATECH / KHÁCH HÀNG & XE", "Thêm khách hàng",
                "Nhập thông tin bắt buộc và kiểm tra trước khi lưu hồ sơ mới.");
        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Mục lục →");
        btnBack.addActionListener(e -> switchToMainView());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        txtAddCustName.setText("");
        txtAddCustPhone.setText("");
        txtAddCustEmail.setText("");
        txtAddCustAddress.setText("");
        txtAddCustPoints.setText("0");

        cardLayout.show(cardsPanel, CARD_ADD_CUSTOMER);
        txtAddCustName.requestFocus();
    }

    private void switchToAddVehicleView(KhachHang kh) {
        this.customerForAddingVehicle = kh;
        setHeader("GARATECH / KHÁCH HÀNG & XE", "Thêm xe cho khách hàng hiện có",
                "Bổ sung phương tiện vào hồ sơ " + kh.getHoTen() + ". Không cần nhập lại thông tin khách hàng.");

        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Mục lục →");
        btnBack.addActionListener(e -> switchToMainView());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        // Populate banner
        lblAddVehCustName.setText(kh.getHoTen());
        lblAddVehCustSub.setText(kh.getMaKH() + " · SĐT: " + kh.getSoDienThoai() + " · Thông tin khách hàng chỉ đọc");

        List<Xe> existingVehicles = KhachHangXeBus.getDanhSachXe(kh.getMaKH());
        StringBuilder sbPlates = new StringBuilder("Biển số đã có để đối chiếu: ");
        if (existingVehicles.isEmpty()) {
            sbPlates.append("Chưa có xe nào");
        } else {
            for (int i = 0; i < existingVehicles.size(); i++) {
                sbPlates.append(existingVehicles.get(i).getBienSo());
                if (i < existingVehicles.size() - 1) sbPlates.append(" · ");
            }
        }
        lblAddVehPlatesCompare.setText(sbPlates.toString());

        lblAddVehPointsStat.setText(String.valueOf(kh.getDiemTichLuy()));
        lblAddVehCountStat.setText(existingVehicles.size() + " xe");
        lblAddVehReturnNote.setText("Lưu xe hoặc Hủy để trở về danh sách xe của " + kh.getHoTen() + ".");

        // Clear inputs
        txtAddVehPlate.setText("");
        txtAddVehMakeModel.setText("");
        txtAddVehYear.setText(String.valueOf(LocalDate.now().getYear()));
        txtAddVehColor.setText("");
        txtAddVehEngine.setText("");
        txtAddVehOdo.setText("0");
        txtAddVehRegDate.setText(LocalDate.now().format(DMY));

        cardLayout.show(cardsPanel, CARD_ADD_VEHICLE);
        txtAddVehPlate.requestFocus();
    }

    private void switchToEditVehicleView(Xe xe) {
        this.vehicleToEdit = xe;
        this.editVehPreviousOdo = xe.getSoKmHienTai();

        setHeader("GARATECH / KHÁCH HÀNG & XE", "Chỉnh sửa phương tiện",
                "Liên kết mỗi xe với một chủ xe hợp lệ trước khi tiếp nhận.");

        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Mục lục →");
        btnBack.addActionListener(e -> switchToMainView());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        KhachHang kh = xe.getKhachHang();
        txtEditVehOwner.setText(kh != null ? kh.getHoTen() + " · " + kh.getSoDienThoai() : "---");
        txtEditVehPlate.setText(xe.getBienSo());
        txtEditVehMakeModel.setText(xe.getHangXe() + " / " + xe.getModel());
        txtEditVehYear.setText(String.valueOf(xe.getNamSanXuat()));
        txtEditVehColor.setText(xe.getMauXe());
        txtEditVehEngine.setText(xe.getSoMay() != null ? xe.getSoMay() : "");
        txtEditVehOdo.setText(String.format("%,.0f km", xe.getSoKmHienTai()));
        lblEditVehOdoHint.setText(String.format("Không nhỏ hơn %,.0f km lần trước", editVehPreviousOdo));
        txtEditVehRegDate.setText(xe.getNgayDangKy() != null ? xe.getNgayDangKy().format(DMY) : "");

        cardLayout.show(cardsPanel, CARD_EDIT_VEHICLE);
        txtEditVehMakeModel.requestFocus();
    }

    // =========================================================================
    // FIELD BUILDER HELPERS
    // =========================================================================
    private JPanel formFieldInput(String label, JTextField tf, String hint) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));

        styleInput(tf, null);
        tf.setAlignmentX(LEFT_ALIGNMENT);
        p.add(tf);

        if (hint != null && !hint.isEmpty()) {
            p.add(Box.createVerticalStrut(3));
            JLabel h = UI.muted(hint);
            h.setFont(Theme.font(Font.PLAIN, 11));
            h.setAlignmentX(LEFT_ALIGNMENT);
            p.add(h);
        }
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
        if (placeholder != null) {
            tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
    }
}

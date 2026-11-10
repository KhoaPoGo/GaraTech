package gui.panel.tiepnhan;

import bus.KhachHangXeBus;
import bus.KhachHangXeBus.KhachHangOverview;
import bus.PhieuTiepNhanSuaChuaBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.DichVuDao;
import dao.NhanVienDao;
import entity.ChiTietDichVu;
import entity.DichVu;
import entity.KhachHang;
import entity.NhanVien;
import entity.Xe;
import enums.User;
import gui.Navigator;
import gui.Screen;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.IdGenerator;
import util.RoundedPanel;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Tạo phiếu tiếp nhận - sửa chữa mới.
 * Tích hợp Kỹ thuật viên chính phụ trách và Dịch vụ đề nghị ban đầu.
 */
public final class LapPhieuTiepNhanPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Runnable onViewListCallback;
    private final Consumer<String> onSavedCallback;
    private final PhieuTiepNhanSuaChuaBus phieuBus = new PhieuTiepNhanSuaChuaBus();

    // Dữ liệu tạm của màn hình
    private KhachHang selectedCustomer = null;
    private Xe selectedVehicle = null;
    private boolean isUpdatingVehicleCombo = false;
    private final List<NhanVien> listTechnicians = new ArrayList<>();
    private final List<DichVu> listAvailableServices = new ArrayList<>();
    private final List<ChiTietDichVu> listSelectedServices = new ArrayList<>();

    // Banner trạng thái xanh lá
    private final RoundedPanel pBanner = new RoundedPanel(12, new Color(0xEB, 0xF9, 0xF1));
    private final JLabel lblBannerMsg = new JLabel();

    // Card 1: Khách hàng & Xe
    private final JLabel lblSelectedCustDisplay = new JLabel("Chưa chọn khách hàng");
    private final JButton btnBrowseCust = UI.primaryButton("Tra cứu");
    private final JComboBox<String> cboVehicle = new JComboBox<>();
    private final JLabel lblSummaryContact = new JLabel("---");
    private final JLabel lblSummaryVehicle = new JLabel("Chưa chọn phương tiện");

    // Card 2: Thông tin tiếp nhận - sửa chữa (Đã bỏ trường Trạng thái và Thời gian hẹn trả dự kiến)
    private final JTextField txtMaPhieu = new JTextField();
    private final JTextField txtThoiGianTiepNhan = new JTextField();
    private final JTextArea taYeuCauKhach = new JTextArea(3, 20);
    private final JTextArea taTinhTrangBanDau = new JTextArea(3, 20);
    private final JTextField txtOdo = new JTextField();
    private final JComboBox<String> cboFuel = new JComboBox<>(new String[]{
            "0% (Hết)", "25% (1/4 bình)", "50% (1/2 bình)", "75% (3/4 bình)", "100% (Đầy bình)"
    });
    private final JTextField txtNhanVien = new JTextField();
    private final JComboBox<String> cboTechnician = new JComboBox<>();

    // Card 3: Dịch vụ đề nghị ban đầu
    private final JComboBox<String> cboDichVu = new JComboBox<>();
    private final JSpinner spnSoLuongDV = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JTextField txtGhiChuDV = new JTextField();
    private final JButton btnThemDV = UI.primaryButton("Thêm dịch vụ");
    private final JButton btnBoDV = UI.dangerButton("Bỏ dịch vụ");
    private final DefaultTableModel modelDichVu = new DefaultTableModel(
            new String[]{"STT", "Mã DV", "Tên dịch vụ", "Đơn vị tính", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDichVu = UI.table(modelDichVu);
    private final JLabel lblTongTienDichVu = UI.label("Tổng tiền dịch vụ tạm tính: 0 đ", Font.BOLD, 14, Theme.NAVY);

    // Card 4: Nút hành động
    private final JButton btnConfirm = UI.primaryButton("Tạo phiếu tiếp nhận - sửa chữa");
    private final JButton btnReset = UI.secondaryButton("Làm mới form");

    public LapPhieuTiepNhanPanel(Runnable onViewListCallback, Consumer<String> onSavedCallback) {
        this.onViewListCallback = onViewListCallback;
        this.onSavedCallback = onSavedCallback;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        loadTechnicians();
        loadServices();
        resetForm();

        // ĐEM TOÀN BỘ CÁC LUỒNG GỌI XỬ LÝ SỰ KIỆN XUỐNG CUỐI CÙNG
        initEvents();
    }

    private void buildUI() {
        // 1. Banner thông báo xanh lá (Ẩn/Hiện tùy theo trạng thái chọn)
        pBanner.setLayout(new BorderLayout(10, 0));
        pBanner.borderColor(new Color(0x9E, 0xD8, 0xBC));
        pBanner.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        lblBannerMsg.setFont(Theme.font(Font.PLAIN, 13));
        lblBannerMsg.setForeground(new Color(0x23, 0x75, 0x55));
        pBanner.add(lblBannerMsg, BorderLayout.CENTER);
        pBanner.setAlignmentX(LEFT_ALIGNMENT);
        pBanner.setVisible(false);

        add(pBanner);
        add(Box.createVerticalStrut(14));

        // 2. Card 1: Khách hàng & Phương tiện
        add(buildCardCustomerVehicle());
        add(Box.createVerticalStrut(16));

        // 3. Card 2: Thông tin tiếp nhận - sửa chữa (kèm Kỹ thuật viên phụ trách)
        add(buildCardReceptionInfo());
        add(Box.createVerticalStrut(16));

        // 4. Card 3: Dịch vụ đề nghị ban đầu
        add(buildCardServices());
        add(Box.createVerticalStrut(16));

        // 5. Card 4: Nút tạo phiếu & Làm mới
        add(buildCardActions());
        add(Box.createVerticalStrut(24));
    }

    private JPanel buildCardCustomerVehicle() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Khách hàng & phương tiện"), BorderLayout.NORTH);

        JPanel cvBody = new JPanel();
        cvBody.setOpaque(false);
        cvBody.setLayout(new BoxLayout(cvBody, BoxLayout.Y_AXIS));

        // Row: Khách hàng tiếp nhận & Dropdown chọn xe
        JPanel rowCustVeh = new JPanel(new GridLayout(1, 2, 20, 0));
        rowCustVeh.setOpaque(false);

        // Cột 1: Khách hàng tiếp nhận (Khung hiển thị + Nút Tra cứu)
        JPanel pCustCol = new JPanel(new BorderLayout(8, 0));
        pCustCol.setOpaque(false);

        RoundedPanel pCustBox = new RoundedPanel(10, new Color(0xF8, 0xFA, 0xFC));
        pCustBox.borderColor(new Color(0xE2, 0xE8, 0xF0));
        pCustBox.setLayout(new BorderLayout(8, 0));
        pCustBox.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
        pCustBox.setPreferredSize(new Dimension(160, 38));

        lblSelectedCustDisplay.setFont(Theme.font(Font.BOLD, 13));
        lblSelectedCustDisplay.setForeground(Theme.MUTED);
        pCustBox.add(lblSelectedCustDisplay, BorderLayout.CENTER);

        btnBrowseCust.setPreferredSize(new Dimension(100, 38));
        pCustCol.add(pCustBox, BorderLayout.CENTER);
        pCustCol.add(btnBrowseCust, BorderLayout.EAST);

        rowCustVeh.add(formFieldWrapComponent("Khách hàng tiếp nhận *", pCustCol));
        rowCustVeh.add(formFieldWrap("Phương tiện của khách *", cboVehicle, null));

        rowCustVeh.setAlignmentX(LEFT_ALIGNMENT);
        cvBody.add(rowCustVeh);
        cvBody.add(Box.createVerticalStrut(14));

        // Dòng tóm tắt nhanh thông tin liên hệ và phương tiện
        JPanel pSummaryRow = new JPanel(new GridLayout(1, 2, 20, 0));
        pSummaryRow.setOpaque(false);

        JPanel pLeftSum = new JPanel(new BorderLayout(8, 0));
        pLeftSum.setOpaque(false);
        pLeftSum.add(UI.muted("Số điện thoại:"), BorderLayout.WEST);
        lblSummaryContact.setFont(Theme.font(Font.BOLD, 13));
        lblSummaryContact.setForeground(Theme.TEXT);
        pLeftSum.add(lblSummaryContact, BorderLayout.CENTER);

        JPanel pRightSum = new JPanel(new BorderLayout(8, 0));
        pRightSum.setOpaque(false);
        pRightSum.add(UI.muted("Phương tiện:"), BorderLayout.WEST);
        lblSummaryVehicle.setFont(Theme.font(Font.BOLD, 13));
        lblSummaryVehicle.setForeground(Theme.TEXT);
        pRightSum.add(lblSummaryVehicle, BorderLayout.CENTER);

        pSummaryRow.add(pLeftSum);
        pSummaryRow.add(pRightSum);
        pSummaryRow.setAlignmentX(LEFT_ALIGNMENT);
        cvBody.add(pSummaryRow);

        card.add(cvBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardReceptionInfo() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Thông tin tiếp nhận - sửa chữa & phân công"), BorderLayout.NORTH);

        JPanel infoBody = new JPanel();
        infoBody.setOpaque(false);
        infoBody.setLayout(new BoxLayout(infoBody, BoxLayout.Y_AXIS));

        // Row 1: Mã phiếu | Thời gian tiếp nhận (Đã bỏ trường Trạng thái)
        JPanel r1 = new JPanel(new GridLayout(1, 2, 20, 0));
        r1.setOpaque(false);
        txtMaPhieu.setEditable(false);
        txtMaPhieu.setBackground(new Color(0xF8FAFC));
        txtThoiGianTiepNhan.setEditable(false);
        txtThoiGianTiepNhan.setBackground(new Color(0xF8FAFC));
        r1.add(formFieldWrap("Mã phiếu tiếp nhận - sửa chữa (Tự động phát sinh)", txtMaPhieu, null));
        r1.add(formFieldWrap("Thời gian tiếp nhận *", txtThoiGianTiepNhan, null));
        r1.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r1);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 2: Yêu cầu của khách hàng
        JPanel r2 = formFieldWrap("Yêu cầu của khách hàng *", taYeuCauKhach, "Ghi lại nguyên văn yêu cầu hoặc mô tả sự cố từ khách hàng...");
        r2.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r2);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 3: Hiện trạng xe lúc nhận
        JPanel r3 = formFieldWrap("Hiện trạng xe lúc tiếp nhận *", taTinhTrangBanDau, "Ghi nhận vết trầy xước, đèn báo lỗi, đồ đạc có giá trị trên xe...");
        r3.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r3);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 4: ODO | Mức nhiên liệu
        JPanel r4 = new JPanel(new GridLayout(1, 2, 20, 0));
        r4.setOpaque(false);
        r4.add(formFieldWrap("Số km hiện tại (ODO) *", txtOdo, null));
        r4.add(formFieldWrap("Mức nhiên liệu lúc nhận *", cboFuel, null));
        r4.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r4);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 5: Nhân viên tiếp nhận | KỸ THUẬT VIÊN CHÍNH PHỤ TRÁCH (Đã bỏ trường thời gian hẹn trả dự kiến)
        JPanel r5 = new JPanel(new GridLayout(1, 2, 20, 0));
        r5.setOpaque(false);
        txtNhanVien.setEditable(false);
        txtNhanVien.setBackground(new Color(0xF8FAFC));
        r5.add(formFieldWrap("Nhân viên tiếp nhận *", txtNhanVien, null));
        r5.add(formFieldWrap("Kỹ thuật viên chính phụ trách *", cboTechnician, null));
        r5.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r5);

        card.add(infoBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardServices() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Dịch vụ đề nghị ban đầu"), BorderLayout.NORTH);

        JPanel sBody = new JPanel();
        sBody.setOpaque(false);
        sBody.setLayout(new BoxLayout(sBody, BoxLayout.Y_AXIS));

        // Hàng 1: Chọn dịch vụ + Số lượng + Ghi chú
        JPanel rowInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        rowInputs.setOpaque(false);

        rowInputs.add(UI.label("Chọn dịch vụ:", Font.BOLD, 12, Theme.TEXT));
        cboDichVu.setFont(Theme.font(Font.PLAIN, 13));
        cboDichVu.setPreferredSize(new Dimension(340, 38));
        rowInputs.add(cboDichVu);

        rowInputs.add(UI.label("Số lượng:", Font.BOLD, 12, Theme.TEXT));
        spnSoLuongDV.setFont(Theme.font(Font.PLAIN, 13));
        spnSoLuongDV.setPreferredSize(new Dimension(70, 38));
        rowInputs.add(spnSoLuongDV);

        rowInputs.add(UI.label("Ghi chú:", Font.BOLD, 12, Theme.TEXT));
        txtGhiChuDV.setFont(Theme.font(Font.PLAIN, 13));
        txtGhiChuDV.setPreferredSize(new Dimension(220, 38));
        txtGhiChuDV.putClientProperty("JTextField.placeholderText", "Ghi chú thực hiện...");
        rowInputs.add(txtGhiChuDV);

        rowInputs.setAlignmentX(LEFT_ALIGNMENT);
        sBody.add(rowInputs);
        sBody.add(Box.createVerticalStrut(10));

        // Hàng 2: Duy nhất 2 nút thao tác rõ ràng: "Thêm dịch vụ" và "Bỏ dịch vụ"
        JPanel rowBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        rowBtns.setOpaque(false);

        btnThemDV.setFont(Theme.font(Font.BOLD, 13));
        btnThemDV.setPreferredSize(new Dimension(145, 38));
        rowBtns.add(btnThemDV);

        btnBoDV.setFont(Theme.font(Font.BOLD, 13));
        btnBoDV.setPreferredSize(new Dimension(135, 38));
        rowBtns.add(btnBoDV);

        JLabel lblGuide = UI.muted("(Chọn dịch vụ rồi bấm 'Thêm dịch vụ'. Để xóa, bấm chọn dòng trong bảng rồi bấm 'Bỏ dịch vụ')");
        rowBtns.add(lblGuide);

        rowBtns.setAlignmentX(LEFT_ALIGNMENT);
        sBody.add(rowBtns);
        sBody.add(Box.createVerticalStrut(14));

        // Bảng dịch vụ đề nghị
        tblDichVu.setFont(Theme.font(Font.PLAIN, 12));
        tblDichVu.setRowHeight(34);
        tblDichVu.setShowGrid(false);
        tblDichVu.getTableHeader().setFont(Theme.font(Font.BOLD, 11));
        tblDichVu.getTableHeader().setPreferredSize(new Dimension(0, 34));

        tblDichVu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblDichVu.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblDichVu.getColumnModel().getColumn(2).setPreferredWidth(260);
        tblDichVu.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblDichVu.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblDichVu.getColumnModel().getColumn(5).setPreferredWidth(110);
        tblDichVu.getColumnModel().getColumn(6).setPreferredWidth(120);
        tblDichVu.getColumnModel().getColumn(7).setPreferredWidth(160);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblDichVu.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblDichVu.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scroll = new JScrollPane(tblDichVu);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        scroll.setPreferredSize(new Dimension(0, 160));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        sBody.add(scroll);
        sBody.add(Box.createVerticalStrut(12));

        // Dòng tổng tiền dịch vụ tạm tính (Không có nút bỏ dịch vụ thừa ở đây)
        JPanel pTong = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pTong.setOpaque(false);
        pTong.add(lblTongTienDichVu);
        pTong.setAlignmentX(LEFT_ALIGNMENT);
        sBody.add(pTong);

        card.add(sBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardActions() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Bên phải: Các nút hành động
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pBtns.setOpaque(false);

        btnReset.setPreferredSize(new Dimension(140, 42));
        btnConfirm.setPreferredSize(new Dimension(240, 42));

        pBtns.add(btnReset);
        pBtns.add(btnConfirm);
        card.add(pBtns, BorderLayout.EAST);

        return card;
    }

    // Helper UI Builders
    private JPanel formFieldWrap(String label, JComponent comp, String hint) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        comp.setFont(Theme.font(Font.PLAIN, 13));
        if (comp instanceof JTextArea ta) {
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            if (hint != null) ta.putClientProperty("JTextField.placeholderText", hint);
            JScrollPane scroll = new JScrollPane(ta);
            scroll.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
            scroll.setPreferredSize(new Dimension(100, 75));
            scroll.setAlignmentX(LEFT_ALIGNMENT);
            p.add(scroll);
        } else {
            if (comp instanceof JTextField tf) {
                tf.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
                if (hint != null) tf.putClientProperty("JTextField.placeholderText", hint);
            } else if (comp instanceof JComboBox) {
                comp.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
            }
            comp.setPreferredSize(new Dimension(100, 38));
            comp.setAlignmentX(LEFT_ALIGNMENT);
            p.add(comp);
        }

        return p;
    }

    private JPanel formFieldWrapComponent(String label, JComponent comp) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        comp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(comp);

        return p;
    }

    // Logic Functions
    private void loadTechnicians() {
        cboTechnician.removeAllItems();
        cboTechnician.addItem("-- Chọn Kỹ thuật viên chính phụ trách --");
        try {
            listTechnicians.clear();
            listTechnicians.addAll(new NhanVienDao().findKyThuatVien());
            for (NhanVien nv : listTechnicians) {
                cboTechnician.addItem(nv.getMaNV() + " - " + nv.getHoTen());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadServices() {
        cboDichVu.removeAllItems();
        cboDichVu.addItem("-- Chọn dịch vụ đề nghị ban đầu --");
        try {
            listAvailableServices.clear();
            listAvailableServices.addAll(new DichVuDao().findActive());
            for (DichVu dv : listAvailableServices) {
                cboDichVu.addItem(dv.getMaDichVu() + " - " + dv.getTenDichVu() + " (" + Fmt.money(dv.getTienCong()) + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectCustomer(KhachHang kh) {
        this.selectedCustomer = kh;
        if (kh == null) {
            lblSelectedCustDisplay.setText("Chưa chọn khách hàng");
            lblSelectedCustDisplay.setForeground(Theme.MUTED);
            lblSummaryContact.setText("---");
            btnBrowseCust.setText("Tra cứu");
            populateVehicles(null);
            pBanner.setVisible(false);
            return;
        }

        lblSelectedCustDisplay.setText(kh.getHoTen() + " (" + kh.getMaKH() + ")");
        lblSelectedCustDisplay.setForeground(Theme.TEXT);
        lblSummaryContact.setText(kh.getSoDienThoai() + " · " + kh.getDiemTichLuy() + " điểm");
        btnBrowseCust.setText("Đổi khách");

        List<Xe> vehicles = KhachHangXeBus.getDanhSachXe(kh.getMaKH());
        populateVehicles(vehicles);

        if (vehicles.isEmpty()) {
            Msg.info(this, "Khách hàng " + kh.getHoTen() + " chưa có phương tiện liên kết.\nBạn có thể thêm xe cho khách hàng tại màn hình Quản lý Xe.");
        }
    }

    private void populateVehicles(List<Xe> vehicles) {
        isUpdatingVehicleCombo = true;
        cboVehicle.removeAllItems();

        if (vehicles == null || vehicles.isEmpty()) {
            cboVehicle.addItem("-- Khách hàng chưa có xe --");
            cboVehicle.setEnabled(false);
            selectVehicle(null);
        } else {
            cboVehicle.addItem("-- Chọn phương tiện của khách --");
            for (Xe x : vehicles) {
                cboVehicle.addItem(x.getBienSo() + " - " + x.getHangXe() + " " + x.getModel() + " (" + x.getMauXe() + ")");
            }
            cboVehicle.setEnabled(true);
            cboVehicle.setSelectedIndex(1);
            selectVehicle(vehicles.get(0));
        }
        isUpdatingVehicleCombo = false;
    }

    public void selectVehicle(Xe xe) {
        this.selectedVehicle = xe;
        if (xe == null) {
            lblSummaryVehicle.setText("Chưa chọn phương tiện");
            lblSummaryVehicle.setForeground(Theme.MUTED);
            pBanner.setVisible(false);
            return;
        }

        lblSummaryVehicle.setText(xe.getBienSo() + " · " + xe.getHangXe() + " " + xe.getModel() + " (" + xe.getNamSanXuat() + ")");
        lblSummaryVehicle.setForeground(Theme.TEXT);

        lblBannerMsg.setText(String.format("Đã chọn hồ sơ %s và xe %s. Tiếp nhận đúng phương tiện, tránh tạo trùng lặp.",
                selectedCustomer != null ? selectedCustomer.getHoTen() : "", xe.getBienSo()));
        pBanner.setVisible(true);

        float lastOdo = xe.getSoKmHienTai();
        if (lastOdo > 0) {
            txtOdo.setText(String.format("%.0f", lastOdo));
        }

        if (cboVehicle.isEnabled() && cboVehicle.getItemCount() > 1) {
            for (int i = 1; i < cboVehicle.getItemCount(); i++) {
                String item = cboVehicle.getItemAt(i);
                if (item != null && item.startsWith(xe.getBienSo())) {
                    if (cboVehicle.getSelectedIndex() != i) {
                        isUpdatingVehicleCombo = true;
                        cboVehicle.setSelectedIndex(i);
                        isUpdatingVehicleCombo = false;
                    }
                    break;
                }
            }
        }
    }

    public void resetForm() {
        // Tự động phát sinh mã phiếu tiếp nhận từ cơ sở dữ liệu
        try {
            txtMaPhieu.setText(IdGenerator.next(IdGenerator.Kind.PHIEU_TIEP_NHAN_SUA_CHUA));
        } catch (Exception ex) {
            txtMaPhieu.setText("PSC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001");
        }

        LocalDateTime now = LocalDateTime.now();
        txtThoiGianTiepNhan.setText(now.format(DT_FMT));

        User u = Session.user();
        txtNhanVien.setText(u != null ? u.hoTen() : "Nguyễn Văn Tiếp Nhận (NV26002)");

        selectCustomer(null);
        selectVehicle(null);

        taYeuCauKhach.setText("");
        taTinhTrangBanDau.setText("");
        txtOdo.setText("");
        cboFuel.setSelectedIndex(2); // 50%
        if (cboTechnician.getItemCount() > 0) {
            cboTechnician.setSelectedIndex(0);
        }

        listSelectedServices.clear();
        capNhatBangDichVu();
        cboDichVu.setSelectedIndex(0);
        spnSoLuongDV.setValue(1);
        txtGhiChuDV.setText("");
    }

    private void themDichVu() {
        int idx = cboDichVu.getSelectedIndex();
        if (idx <= 0 || idx - 1 >= listAvailableServices.size()) {
            Msg.error(this, "Vui lòng chọn một dịch vụ từ danh mục.");
            return;
        }

        DichVu dv = listAvailableServices.get(idx - 1);
        for (ChiTietDichVu ct : listSelectedServices) {
            if (ct.getDichVu() != null && ct.getDichVu().getMaDichVu().equals(dv.getMaDichVu())) {
                Msg.info(this, "Dịch vụ '" + dv.getTenDichVu() + "' đã có trong danh mục đề nghị.");
                return;
            }
        }

        int sl = (int) spnSoLuongDV.getValue();
        String ghiChu = txtGhiChuDV.getText().trim();

        ChiTietDichVu ct = new ChiTietDichVu();
        ct.setDichVu(dv);
        ct.setSoLuong(sl);
        ct.setDonGiaThucHien(dv.getTienCong());
        ct.setGhiChu(ghiChu.isEmpty() ? null : ghiChu);
        listSelectedServices.add(ct);

        capNhatBangDichVu();
        txtGhiChuDV.setText("");
        spnSoLuongDV.setValue(1);
    }

    private void boDichVu() {
        int row = tblDichVu.getSelectedRow();
        if (row < 0 || row >= listSelectedServices.size()) {
            Msg.error(this, "Vui lòng chọn dòng dịch vụ cần bỏ.");
            return;
        }
        listSelectedServices.remove(row);
        capNhatBangDichVu();
    }

    private void capNhatBangDichVu() {
        modelDichVu.setRowCount(0);
        double tong = 0;
        int stt = 1;
        for (ChiTietDichVu ct : listSelectedServices) {
            DichVu dv = ct.getDichVu();
            double dg = ct.getDonGiaThucHien();
            int sl = ct.getSoLuong();
            double tt = dg * sl;
            tong += tt;
            modelDichVu.addRow(new Object[]{
                    stt++,
                    dv != null ? dv.getMaDichVu() : "",
                    dv != null ? dv.getTenDichVu() : "",
                    "Lần",
                    sl,
                    Fmt.money(dg),
                    Fmt.money(tt),
                    ct.getGhiChu() != null ? ct.getGhiChu() : ""
            });
        }
        lblTongTienDichVu.setText("Tổng tiền dịch vụ tạm tính: " + Fmt.money(tong));
    }

    private void handleOpenBrowseDialog() {
        TraCuuKhachHangDialog dlg = new TraCuuKhachHangDialog(
                SwingUtilities.getWindowAncestor(this),
                "",
                this::handleNavigateToAddCustomer
        );
        dlg.setVisible(true);
        if (dlg.getSelectedCustomer() != null) {
            selectCustomer(dlg.getSelectedCustomer());
        }
    }

    private void handleNavigateToAddCustomer(String initialQuery) {
        Navigator.setPayload(new Navigator.TiepNhanAddCustomerRequest(initialQuery));
        Navigator.go(Screen.KHACH_HANG_XE);
    }

    private void handleConfirmReception() {
        try {
            if (selectedCustomer == null) {
                Msg.error(this, "Vui lòng bấm 'Tra cứu' để chọn khách hàng tiếp nhận.");
                btnBrowseCust.requestFocus();
                return;
            }
            if (selectedVehicle == null) {
                Msg.error(this, "Vui lòng chọn xe tiếp nhận.");
                cboVehicle.requestFocus();
                return;
            }

            // Kiểm tra Kỹ thuật viên chính phụ trách
            int idxKtv = cboTechnician.getSelectedIndex();
            if (idxKtv <= 0 || idxKtv - 1 >= listTechnicians.size()) {
                Msg.error(this, "Vui lòng chỉ định Kỹ thuật viên chính phụ trách xe.");
                cboTechnician.requestFocus();
                return;
            }
            NhanVien ktvChon = listTechnicians.get(idxKtv - 1);

            String yeuCau = taYeuCauKhach.getText().trim();
            if (yeuCau.isEmpty()) {
                Msg.error(this, "Yêu cầu của khách hàng là bắt buộc.");
                taYeuCauKhach.requestFocus();
                return;
            }

            String tinhTrang = taTinhTrangBanDau.getText().trim();
            if (tinhTrang.isEmpty()) {
                Msg.error(this, "Tình trạng ban đầu / hiện trạng xe là bắt buộc.");
                taTinhTrangBanDau.requestFocus();
                return;
            }

            float odo = Float.parseFloat(txtOdo.getText().trim().replaceAll("[^0-9.]", ""));

            float fuel = switch (cboFuel.getSelectedIndex()) {
                case 1 -> 0.25f;
                case 2 -> 0.50f;
                case 3 -> 0.75f;
                case 4 -> 1.00f;
                default -> 0.0f;
            };

            LocalDateTime tgTiepNhan = LocalDateTime.now();
            LocalDateTime tgHenTra = null; // Đã bỏ trường nhập hẹn trả dự kiến

            User u = Session.user();
            String maNV = u != null ? u.maNV() : "NV26002";

            // 1. Tạo phiếu tiếp nhận - sửa chữa thống nhất (CHO_SUA)
            PhieuTiepNhanSuaChuaBus.TiepNhanInput inputUnified = new PhieuTiepNhanSuaChuaBus.TiepNhanInput(
                    selectedCustomer.getMaKH(),
                    selectedVehicle.getBienSo(),
                    maNV,
                    ktvChon.getMaNV(),
                    odo,
                    fuel,
                    yeuCau,
                    tinhTrang,
                    tgTiepNhan,
                    tgHenTra
            );

            String maPhieu = phieuBus.taoPhieuTiepNhan(inputUnified, listSelectedServices);

            Msg.info(this, String.format(
                    "Tạo thành công phiếu tiếp nhận - sửa chữa cho xe %s!\nMã phiếu: %s\nKỹ thuật viên phụ trách: %s\nTrạng thái: Chờ sửa chữa (CHO_SUA).",
                    selectedVehicle.getBienSo(), maPhieu, ktvChon.getHoTen()));

            if (onSavedCallback != null) {
                onSavedCallback.accept(maPhieu);
            }

            resetForm();

        } catch (NumberFormatException ex) {
            Msg.error(this, "Số km hiện tại (ODO) không hợp lệ.");
            txtOdo.requestFocus();
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi lưu phiếu tiếp nhận: " + ex.getMessage());
        }
    }

    public void refreshCustomerList() {
        // Không cần load toàn bộ vào combobox nữa
    }

    public void setInitialCustomerAndVehicle(KhachHang kh, Xe xe) {
        if (kh != null) {
            selectCustomer(kh);
        }
        if (xe != null) {
            selectVehicle(xe);
        }
    }

    // =========================================================================
    // HỘP THOẠI TRA CỨU KHÁCH HÀNG (Khôi phục chuẩn 100% giao diện gốc)
    // =========================================================================
    public static final class TraCuuKhachHangDialog extends JDialog {

        private final JTextField txtFilter = new JTextField();
        private final DefaultTableModel tableModel = UI.readOnlyModel("Mã KH", "Họ và tên", "Số điện thoại", "Xe liên kết", "Điểm");
        private final JTable table = UI.table(tableModel);
        private final JLabel lblCount = UI.muted("0 khách hàng");

        private final JButton btnSelect = UI.primaryButton("Chọn khách hàng");
        private final JButton btnAddNew = UI.secondaryButton("Thêm khách mới");
        private final JButton btnCancel = UI.secondaryButton("Đóng");

        private final Consumer<String> onAddNewCallback;
        private List<KhachHangOverview> currentList = new ArrayList<>();
        private KhachHang selectedCustomer = null;

        public TraCuuKhachHangDialog(Window owner, String initialKeyword, Consumer<String> onAddNewCallback) {
            super(owner, "Tra cứu khách hàng", ModalityType.APPLICATION_MODAL);
            this.onAddNewCallback = onAddNewCallback;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setPreferredSize(new Dimension(720, 500));
            setLayout(new BorderLayout());

            buildUI();
            initEvents();

            if (initialKeyword != null && !initialKeyword.isBlank()) {
                txtFilter.setText(initialKeyword.trim());
            }

            loadData();

            pack();
            setLocationRelativeTo(owner);
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout(0, 14));
            root.setBackground(Theme.BG);
            root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            // 1. Top Search Bar
            RoundedPanel pTop = new RoundedPanel(12, Theme.CARD);
            pTop.setLayout(new BorderLayout(12, 0));
            pTop.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JLabel lblSearch = UI.label("Tìm kiếm:", Font.BOLD, 12, Theme.TEXT);
            txtFilter.setFont(Theme.font(Font.PLAIN, 13));
            txtFilter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Gõ số điện thoại, tên khách hàng hoặc biển số xe để lọc tức thì...");
            txtFilter.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
            txtFilter.setPreferredSize(new Dimension(0, 38));

            pTop.add(lblSearch, BorderLayout.WEST);
            pTop.add(txtFilter, BorderLayout.CENTER);
            root.add(pTop, BorderLayout.NORTH);

            // 2. Table Center
            RoundedPanel pTable = new RoundedPanel(12, Theme.CARD);
            pTable.setLayout(new BorderLayout(0, 10));
            pTable.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

            table.setRowHeight(40);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getColumnModel().getColumn(0).setPreferredWidth(90);
            table.getColumnModel().getColumn(1).setPreferredWidth(180);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setPreferredWidth(220);
            table.getColumnModel().getColumn(4).setPreferredWidth(60);

            DefaultTableCellRenderer r = new DefaultTableCellRenderer();
            r.setHorizontalAlignment(SwingConstants.LEFT);
            r.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            for (int i = 0; i < 5; i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(r);
            }

            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            if (headerRenderer instanceof DefaultTableCellRenderer headR) {
                headR.setHorizontalAlignment(SwingConstants.LEFT);
                headR.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            }

            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
            pTable.add(sp, BorderLayout.CENTER);
            pTable.add(lblCount, BorderLayout.SOUTH);
            root.add(pTable, BorderLayout.CENTER);

            // 3. Bottom Actions
            JPanel pBot = new JPanel(new BorderLayout());
            pBot.setOpaque(false);
            pBot.add(btnAddNew, BorderLayout.WEST);

            JPanel pRightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            pRightBtns.setOpaque(false);
            pRightBtns.add(btnCancel);
            pRightBtns.add(btnSelect);
            pBot.add(pRightBtns, BorderLayout.EAST);

            root.add(pBot, BorderLayout.SOUTH);

            // Đưa toàn bộ panel root vào dialog
            add(root, BorderLayout.CENTER);
        }

        private void doSearch() {
            String q = txtFilter.getText().trim();
            loadDataFiltered(q);
        }

        private void loadData() {
            loadDataFiltered(txtFilter.getText().trim());
        }

        private void loadDataFiltered(String kw) {
            currentList = KhachHangXeBus.search(kw, kw);
            tableModel.setRowCount(0);

            for (KhachHangOverview item : currentList) {
                KhachHang kh = item.khachHang();
                tableModel.addRow(new Object[]{
                        kh.getMaKH(),
                        kh.getHoTen(),
                        kh.getSoDienThoai(),
                        item.xeSummary(),
                        kh.getDiemTichLuy()
                });
            }

            lblCount.setText(String.format("Tìm thấy %d khách hàng phù hợp", currentList.size()));

            if (!currentList.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
                btnSelect.setEnabled(true);
            } else {
                btnSelect.setEnabled(false);
            }
        }

        private void handleSelectRow() {
            int row = table.getSelectedRow();
            if (row >= 0 && row < currentList.size()) {
                this.selectedCustomer = currentList.get(row).khachHang();
                dispose();
            }
        }

        public KhachHang getSelectedCustomer() {
            return selectedCustomer;
        }

        // =========================================================================
        // ĐEM TOÀN BỘ CÁC LUỒNG GỌI XỬ LÝ SỰ KIỆN XUỐNG CUỐI CÙNG
        // =========================================================================
        private void initEvents() {
            btnCancel.addActionListener(e -> dispose());

            btnSelect.addActionListener(e -> handleSelectRow());

            btnAddNew.addActionListener(e -> {
                String q = txtFilter.getText().trim();
                dispose();
                if (onAddNewCallback != null) {
                    onAddNewCallback.accept(q);
                }
            });

            table.getSelectionModel().addListSelectionListener(e -> {
                btnSelect.setEnabled(table.getSelectedRow() >= 0);
            });

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                        handleSelectRow();
                    }
                }
            });

            txtFilter.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { doSearch(); }
                @Override public void removeUpdate(DocumentEvent e) { doSearch(); }
                @Override public void changedUpdate(DocumentEvent e) { doSearch(); }
            });
        }
    }

    // =========================================================================
    // ĐEM TOÀN BỘ CÁC LUỒNG GỌI XỬ LÝ SỰ KIỆN XUỐNG CUỐI CÙNG
    // =========================================================================
    private void initEvents() {
        // 1. Nút Tra cứu khách hàng
        btnBrowseCust.addActionListener(e -> handleOpenBrowseDialog());

        // 2. Chọn xe trên ComboBox
        cboVehicle.addActionListener(e -> {
            if (isUpdatingVehicleCombo) return;
            int idx = cboVehicle.getSelectedIndex();
            if (selectedCustomer != null && idx > 0) {
                List<Xe> vehicles = KhachHangXeBus.getDanhSachXe(selectedCustomer.getMaKH());
                if (idx - 1 < vehicles.size()) {
                    selectVehicle(vehicles.get(idx - 1));
                }
            } else if (idx == 0) {
                selectVehicle(null);
            }
        });

        // 3. Nút Thêm dịch vụ
        btnThemDV.addActionListener(e -> themDichVu());

        // 4. Nút Bỏ dịch vụ
        btnBoDV.addActionListener(e -> boDichVu());

        // 5. Nút Tạo phiếu tiếp nhận - sửa chữa
        btnConfirm.addActionListener(e -> handleConfirmReception());

        // 6. Nút Làm mới form
        btnReset.addActionListener(e -> resetForm());
    }
}

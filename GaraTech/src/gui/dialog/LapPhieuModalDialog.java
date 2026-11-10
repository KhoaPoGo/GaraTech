package gui.dialog;

import bus.PhieuTiepNhanSuaChuaBus;
import bus.PhieuTiepNhanSuaChuaBus.TiepNhanInput;
import com.formdev.flatlaf.FlatClientProperties;
import dao.DichVuDao;
import dao.NhanVienDao;
import dao.XeDao;
import entity.ChiTietDichVu;
import entity.DichVu;
import entity.KhachHang;
import entity.NhanVien;
import entity.Xe;
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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Hộp thoại tạo mới phiếu tiếp nhận – sửa chữa (Modal Dialog).
 */
public final class LapPhieuModalDialog extends JDialog {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PhieuTiepNhanSuaChuaBus phieuBus = new PhieuTiepNhanSuaChuaBus();
    private boolean thanhCong = false;

    // Dữ liệu
    private KhachHang selectedCustomer;
    private Xe selectedVehicle;
    private final List<Xe> listVehicles = new ArrayList<>();
    private final List<NhanVien> listTechnicians = new ArrayList<>();
    private final List<DichVu> listAvailableServices = new ArrayList<>();
    private final List<ChiTietDichVu> listSelectedServices = new ArrayList<>();

    // Khách hàng & Xe
    private final JLabel lblCustomerDisplay = new JLabel("Chưa chọn khách hàng");
    private final JLabel lblCustomerContact = UI.muted("Chưa chọn khách hàng");
    private final JButton btnBrowseCustomer = UI.primaryButton("Tra cứu");
    private final JComboBox<String> cboVehicle = new JComboBox<>();
    private final JLabel lblVehicleSummary = UI.muted("Chưa chọn phương tiện");

    // Thông tin tiếp nhận
    private final JTextField txtMaPhieu = new JTextField();
    private final JTextField txtThoiGianTiepNhan = new JTextField();
    private final JTextField txtNhanVienTiepNhan = new JTextField();
    private final JComboBox<String> cboTechnician = new JComboBox<>();
    private final JTextField txtOdo = new JTextField();
    private final JComboBox<String> cboFuel = new JComboBox<>(new String[]{
            "0% (Hết)", "25% (1/4 bình)", "50% (1/2 bình)", "75% (3/4 bình)", "100% (Đầy bình)"
    });
    private final JTextField txtHenTra = new JTextField();
    private final JTextArea taYeuCau = new JTextArea(3, 20);
    private final JTextArea taTinhTrang = new JTextArea(3, 20);
    private final JTextField txtGhiChu = new JTextField();

    // Dịch vụ đề nghị
    private final JComboBox<String> cboDichVu = new JComboBox<>();
    private final JSpinner spnSoLuongDV = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JTextField txtGhiChuDV = new JTextField();
    private final JButton btnThemDV = UI.primaryButton("+ Thêm dịch vụ");
    private final JButton btnBoDV = UI.dangerButton("Bỏ chọn");
    private final DefaultTableModel modelDichVu = new DefaultTableModel(
            new String[]{"STT", "Mã DV", "Tên dịch vụ", "Đơn vị tính", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDichVu = UI.table(modelDichVu);
    private final JLabel lblTongTienDichVu = UI.label("Tổng tiền dịch vụ tạm tính: 0 đ", Font.BOLD, 13, Theme.NAVY);

    // Footer
    private final JLabel lblStatusMsg = UI.muted("Vui lòng nhập đầy đủ thông tin bắt buộc (*)");
    private final JButton btnHuy = UI.secondaryButton("Hủy bỏ");
    private final JButton btnLuu = UI.primaryButton("Lưu phiếu tiếp nhận");

    public LapPhieuModalDialog(Window parent, KhachHang defaultKh, Xe defaultXe) {
        super(parent, "Lập phiếu tiếp nhận – sửa chữa", ModalityType.APPLICATION_MODAL);
        this.selectedCustomer = defaultKh;
        this.selectedVehicle = defaultXe;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = ge.getMaximumWindowBounds();
        int dialogW = Math.min(1100, maxBounds.width - 40);
        int dialogH = Math.min(620, maxBounds.height - 40);
        setPreferredSize(new Dimension(dialogW, dialogH));
        setLayout(new BorderLayout());

        buildUI();
        loadTechnicians();
        loadAvailableServices();
        initDefaultValues();
        initEvents();

        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isThanhCong() {
        return thanhCong;
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        // Header
        JPanel pHead = new JPanel(new BorderLayout());
        pHead.setOpaque(false);
        pHead.add(UI.h1("Lập phiếu tiếp nhận – sửa chữa"), BorderLayout.NORTH);
        pHead.add(UI.muted("Nhập thông tin tiếp nhận xe, phân công kỹ thuật viên và đề xuất dịch vụ ban đầu."), BorderLayout.SOUTH);
        root.add(pHead, BorderLayout.NORTH);

        // Body with ScrollPane
        JPanel pBody = new JPanel();
        pBody.setOpaque(false);
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));

        pBody.add(buildCardCustomerVehicle());
        pBody.add(Box.createVerticalStrut(10));
        pBody.add(buildCardReceptionInfo());
        pBody.add(Box.createVerticalStrut(10));
        pBody.add(buildCardServices());

        JScrollPane scroll = new JScrollPane(pBody);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel pFoot = new JPanel(new BorderLayout(12, 0));
        pFoot.setOpaque(false);
        pFoot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));

        pFoot.add(lblStatusMsg, BorderLayout.WEST);

        JPanel pFootBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pFootBtns.setOpaque(false);
        btnHuy.setPreferredSize(new Dimension(100, 36));
        btnLuu.setPreferredSize(new Dimension(180, 36));
        pFootBtns.add(btnHuy);
        pFootBtns.add(btnLuu);
        pFoot.add(pFootBtns, BorderLayout.EAST);

        root.add(pFoot, BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);
    }

    private JPanel buildCardCustomerVehicle() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Khách hàng & Phương tiện"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);

        // Cột Khách hàng (Hàng 1: Label, Hàng 2: Box + Button tra cứu, Hàng 3: Contact note)
        JPanel pCustCol = new JPanel(new BorderLayout(0, 4));
        pCustCol.setOpaque(false);

        JPanel pCustInputRow = new JPanel(new BorderLayout(8, 0));
        pCustInputRow.setOpaque(false);

        RoundedPanel pCustBox = new RoundedPanel(10, new Color(0xF8, 0xFA, 0xFC));
        pCustBox.borderColor(Theme.BORDER);
        pCustBox.setLayout(new BorderLayout(8, 0));
        pCustBox.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
        pCustBox.setPreferredSize(new Dimension(0, 38));
        lblCustomerDisplay.setFont(Theme.font(Font.BOLD, 13));
        lblCustomerDisplay.setForeground(Theme.TEXT);
        pCustBox.add(lblCustomerDisplay, BorderLayout.CENTER);

        btnBrowseCustomer.setPreferredSize(new Dimension(95, 38));
        pCustInputRow.add(pCustBox, BorderLayout.CENTER);
        pCustInputRow.add(btnBrowseCustomer, BorderLayout.EAST);

        pCustCol.add(pCustInputRow, BorderLayout.NORTH);
        pCustCol.add(lblCustomerContact, BorderLayout.SOUTH);
        grid.add(wrapField("Khách hàng tiếp nhận *", pCustCol));

        // Cột Xe (Hàng 1: Label, Hàng 2: ComboBox, Hàng 3: Vehicle summary note)
        JPanel pVehCol = new JPanel(new BorderLayout(0, 4));
        pVehCol.setOpaque(false);

        cboVehicle.setFont(Theme.font(Font.PLAIN, 13));
        cboVehicle.setPreferredSize(new Dimension(0, 38));
        cboVehicle.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");

        pVehCol.add(cboVehicle, BorderLayout.NORTH);
        pVehCol.add(lblVehicleSummary, BorderLayout.SOUTH);
        grid.add(wrapField("Xe của khách hàng *", pVehCol));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardReceptionInfo() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Thông tin tiếp nhận – sửa chữa"), BorderLayout.NORTH);

        JPanel pContent = new JPanel();
        pContent.setOpaque(false);
        pContent.setLayout(new BoxLayout(pContent, BoxLayout.Y_AXIS));

        // Hàng 1: Mã phiếu | Thời gian tiếp nhận | Người tiếp nhận
        JPanel row1 = new JPanel(new GridLayout(1, 3, 14, 0));
        row1.setOpaque(false);
        txtMaPhieu.setEditable(false);
        txtMaPhieu.setBackground(new Color(0xF1, 0xF5, 0xF9));
        txtMaPhieu.setFont(Theme.font(Font.PLAIN, 13));
        txtMaPhieu.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#E2E8F0");
        txtMaPhieu.setPreferredSize(new Dimension(0, 36));
        row1.add(wrapField("Mã phiếu (Hệ thống tự sinh)", txtMaPhieu));

        txtThoiGianTiepNhan.setEditable(false);
        txtThoiGianTiepNhan.setBackground(new Color(0xF1, 0xF5, 0xF9));
        txtThoiGianTiepNhan.setFont(Theme.font(Font.PLAIN, 13));
        txtThoiGianTiepNhan.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#E2E8F0");
        txtThoiGianTiepNhan.setPreferredSize(new Dimension(0, 36));
        row1.add(wrapField("Thời điểm tiếp nhận *", txtThoiGianTiepNhan));

        txtNhanVienTiepNhan.setEditable(false);
        txtNhanVienTiepNhan.setBackground(new Color(0xF1, 0xF5, 0xF9));
        txtNhanVienTiepNhan.setFont(Theme.font(Font.PLAIN, 13));
        txtNhanVienTiepNhan.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#E2E8F0");
        txtNhanVienTiepNhan.setPreferredSize(new Dimension(0, 36));
        row1.add(wrapField("Người tiếp nhận", txtNhanVienTiepNhan));
        pContent.add(row1);
        pContent.add(Box.createVerticalStrut(10));

        // Hàng 2: KTV chính | ODO | Mức nhiên liệu | Hẹn trả xe
        JPanel row2 = new JPanel(new GridLayout(1, 4, 14, 0));
        row2.setOpaque(false);

        cboTechnician.setFont(Theme.font(Font.PLAIN, 13));
        cboTechnician.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        cboTechnician.setPreferredSize(new Dimension(0, 36));
        row2.add(wrapField("Kỹ thuật viên chính phụ trách", cboTechnician));

        txtOdo.setFont(Theme.font(Font.PLAIN, 13));
        txtOdo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập số km");
        txtOdo.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        txtOdo.setPreferredSize(new Dimension(0, 36));
        row2.add(wrapField("Số km tiếp nhận (ODO) *", txtOdo));

        cboFuel.setFont(Theme.font(Font.PLAIN, 13));
        cboFuel.setSelectedIndex(2); // 50%
        cboFuel.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        cboFuel.setPreferredSize(new Dimension(0, 36));
        row2.add(wrapField("Mức nhiên liệu *", cboFuel));

        txtHenTra.setFont(Theme.font(Font.PLAIN, 13));
        txtHenTra.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/MM/yyyy HH:mm");
        txtHenTra.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        txtHenTra.setPreferredSize(new Dimension(0, 36));
        row2.add(wrapField("Dự kiến hẹn trả xe", txtHenTra));
        pContent.add(row2);
        pContent.add(Box.createVerticalStrut(10));

        // Hàng 3: Yêu cầu của khách hàng (toàn chiều rộng)
        taYeuCau.setFont(Theme.font(Font.PLAIN, 13));
        taYeuCau.setLineWrap(true);
        taYeuCau.setWrapStyleWord(true);
        taYeuCau.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,8,6,8; borderColor:#D5DBE5");
        JScrollPane spYc = new JScrollPane(taYeuCau);
        spYc.setPreferredSize(new Dimension(0, 48));
        spYc.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pContent.add(wrapField("Yêu cầu của khách hàng *", spYc));
        pContent.add(Box.createVerticalStrut(8));

        // Hàng 4: Tình trạng xe khi tiếp nhận (đặt xuống dưới dòng yêu cầu của khách)
        taTinhTrang.setFont(Theme.font(Font.PLAIN, 13));
        taTinhTrang.setLineWrap(true);
        taTinhTrang.setWrapStyleWord(true);
        taTinhTrang.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        JScrollPane spTt = new JScrollPane(taTinhTrang);
        spTt.setPreferredSize(new Dimension(0, 48));
        spTt.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pContent.add(wrapField("Tình trạng xe khi tiếp nhận (vết xước, đèn báo lỗi, móp méo...)", spTt));
        pContent.add(Box.createVerticalStrut(8));

        // Hàng 5: Ghi chú tiếp nhận thêm
        txtGhiChu.setFont(Theme.font(Font.PLAIN, 13));
        txtGhiChu.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ghi chú thêm từ tiếp nhận viên...");
        txtGhiChu.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        txtGhiChu.setPreferredSize(new Dimension(0, 36));
        pContent.add(wrapField("Ghi chú tiếp nhận thêm", txtGhiChu));

        card.add(pContent, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardServices() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Dịch vụ đề nghị ban đầu"), BorderLayout.NORTH);

        JPanel pTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pTop.setOpaque(false);

        cboDichVu.setFont(Theme.font(Font.PLAIN, 13));
        cboDichVu.setPreferredSize(new Dimension(320, 36));
        cboDichVu.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");

        spnSoLuongDV.setFont(Theme.font(Font.PLAIN, 13));
        spnSoLuongDV.setPreferredSize(new Dimension(65, 36));
        spnSoLuongDV.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");

        txtGhiChuDV.setFont(Theme.font(Font.PLAIN, 13));
        txtGhiChuDV.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ghi chú dịch vụ...");
        txtGhiChuDV.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        txtGhiChuDV.setPreferredSize(new Dimension(200, 36));

        btnThemDV.setPreferredSize(new Dimension(135, 36));
        btnBoDV.setPreferredSize(new Dimension(100, 36));

        pTop.add(UI.label("Dịch vụ:", Font.BOLD, 12, Theme.TEXT));
        pTop.add(cboDichVu);
        pTop.add(UI.label("SL:", Font.BOLD, 12, Theme.TEXT));
        pTop.add(spnSoLuongDV);
        pTop.add(txtGhiChuDV);
        pTop.add(btnThemDV);
        pTop.add(btnBoDV);

        card.add(pTop, BorderLayout.NORTH);

        // Table
        tblDichVu.setRowHeight(30);
        tblDichVu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblDichVu.getColumnModel().getColumn(1).setPreferredWidth(85);
        tblDichVu.getColumnModel().getColumn(2).setPreferredWidth(260);
        tblDichVu.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblDichVu.getColumnModel().getColumn(4).setPreferredWidth(65);
        tblDichVu.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblDichVu.getColumnModel().getColumn(6).setPreferredWidth(110);
        tblDichVu.getColumnModel().getColumn(7).setPreferredWidth(150);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblDichVu.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        tblDichVu.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDichVu.getColumnModel().getColumn(5).setCellRenderer(rightRender);
        tblDichVu.getColumnModel().getColumn(6).setCellRenderer(rightRender);

        JScrollPane sp = new JScrollPane(tblDichVu);
        sp.setPreferredSize(new Dimension(0, 130));
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(sp, BorderLayout.CENTER);

        JPanel pBot = new JPanel(new BorderLayout());
        pBot.setOpaque(false);
        pBot.add(lblTongTienDichVu, BorderLayout.EAST);
        card.add(pBot, BorderLayout.SOUTH);

        return card;
    }

    private JPanel wrapField(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = UI.label(label, Font.BOLD, 12, Theme.MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void loadTechnicians() {
        cboTechnician.removeAllItems();
        cboTechnician.addItem("-- Chọn kỹ thuật viên phụ trách --");
        try {
            listTechnicians.clear();
            listTechnicians.addAll(new NhanVienDao().findKyThuatVien());
            for (NhanVien ktv : listTechnicians) {
                cboTechnician.addItem(ktv.getMaNV() + " - " + ktv.getHoTen());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAvailableServices() {
        cboDichVu.removeAllItems();
        cboDichVu.addItem("-- Chọn dịch vụ đề nghị --");
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

    private void initDefaultValues() {
        try {
            txtMaPhieu.setText(IdGenerator.next(IdGenerator.Kind.PHIEU_TIEP_NHAN_SUA_CHUA));
        } catch (Exception ex) {
            txtMaPhieu.setText("Tự động cấp mã");
        }
        LocalDateTime now = LocalDateTime.now();
        txtThoiGianTiepNhan.setText(now.format(DT_FMT));
        txtHenTra.setText(now.plusDays(1).format(DT_FMT));

        if (Session.user() != null) {
            txtNhanVienTiepNhan.setText(Session.user().hoTen() + " (" + Session.user().maNV() + ")");
        } else {
            txtNhanVienTiepNhan.setText("Tiếp nhận viên");
        }

        if (selectedCustomer != null) {
            setCustomer(selectedCustomer);
            if (selectedVehicle != null) {
                selectVehicle(selectedVehicle);
            }
        }
    }

    private void setCustomer(KhachHang kh) {
        this.selectedCustomer = kh;
        if (kh == null) {
            lblCustomerDisplay.setText("Chưa chọn khách hàng");
            lblCustomerContact.setText("Chưa chọn khách hàng");
            cboVehicle.removeAllItems();
            cboVehicle.addItem("-- Chọn phương tiện --");
            lblVehicleSummary.setText("Chưa chọn phương tiện");
            return;
        }

        lblCustomerDisplay.setText(kh.getHoTen());
        lblCustomerContact.setText("Mã KH: " + kh.getMaKH() + " · SĐT: " + kh.getSoDienThoai());
        cboVehicle.removeAllItems();

        try {
            listVehicles.clear();
            listVehicles.addAll(XeDao.findByMaKH(kh.getMaKH()));
            if (listVehicles.isEmpty()) {
                cboVehicle.addItem("-- Khách hàng chưa có xe đăng ký --");
                selectedVehicle = null;
                lblVehicleSummary.setText("Chưa có xe nào");
            } else {
                for (Xe x : listVehicles) {
                    cboVehicle.addItem(String.format("%s - %s %s",
                            x.getBienSo(),
                            x.getHangXe() != null ? x.getHangXe() : "",
                            x.getModel() != null ? x.getModel() : ""));
                }
                cboVehicle.setSelectedIndex(0);
                selectedVehicle = listVehicles.get(0);
                updateSelectedVehicleInfo();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void selectVehicle(Xe xe) {
        if (xe == null || listVehicles.isEmpty()) return;
        for (int i = 0; i < listVehicles.size(); i++) {
            if (listVehicles.get(i).getBienSo().equalsIgnoreCase(xe.getBienSo())) {
                cboVehicle.setSelectedIndex(i);
                selectedVehicle = listVehicles.get(i);
                updateSelectedVehicleInfo();
                break;
            }
        }
    }

    private void updateSelectedVehicleInfo() {
        int idx = cboVehicle.getSelectedIndex();
        if (idx >= 0 && idx < listVehicles.size()) {
            selectedVehicle = listVehicles.get(idx);
            lblVehicleSummary.setText(String.format("Biển số: %s · Hãng: %s · Model: %s · ODO hiện tại: %,.0f km",
                    selectedVehicle.getBienSo(),
                    selectedVehicle.getHangXe() != null ? selectedVehicle.getHangXe() : "---",
                    selectedVehicle.getModel() != null ? selectedVehicle.getModel() : "---",
                    selectedVehicle.getSoKmHienTai()));
            txtOdo.setText(String.format("%.0f", selectedVehicle.getSoKmHienTai()));
        } else {
            selectedVehicle = null;
            lblVehicleSummary.setText("Chưa chọn phương tiện");
        }
    }

    private void handleAddService() {
        int idx = cboDichVu.getSelectedIndex();
        if (idx <= 0 || idx - 1 >= listAvailableServices.size()) {
            Msg.error(this, "Vui lòng chọn dịch vụ đề nghị.");
            return;
        }

        DichVu dv = listAvailableServices.get(idx - 1);
        int sl = (int) spnSoLuongDV.getValue();
        String note = txtGhiChuDV.getText().trim();

        // Kiểm tra trùng
        for (ChiTietDichVu ct : listSelectedServices) {
            if (ct.getDichVu().getMaDichVu().equals(dv.getMaDichVu())) {
                ct.setSoLuong(ct.getSoLuong() + sl);
                if (!note.isEmpty()) {
                    ct.setGhiChu(note);
                }
                refreshServicesTable();
                cboDichVu.setSelectedIndex(0);
                txtGhiChuDV.setText("");
                spnSoLuongDV.setValue(1);
                return;
            }
        }

        ChiTietDichVu ct = new ChiTietDichVu();
        ct.setDichVu(dv);
        ct.setSoLuong(sl);
        ct.setDonGiaThucHien(dv.getTienCong());
        ct.setGhiChu(note.isEmpty() ? null : note);
        listSelectedServices.add(ct);

        refreshServicesTable();
        cboDichVu.setSelectedIndex(0);
        txtGhiChuDV.setText("");
        spnSoLuongDV.setValue(1);
    }

    private void handleRemoveService() {
        int row = tblDichVu.getSelectedRow();
        if (row < 0 || row >= listSelectedServices.size()) {
            Msg.error(this, "Vui lòng chọn dòng dịch vụ cần bỏ.");
            return;
        }
        listSelectedServices.remove(row);
        refreshServicesTable();
    }

    private void refreshServicesTable() {
        modelDichVu.setRowCount(0);
        double tong = 0;
        int stt = 1;
        for (ChiTietDichVu ct : listSelectedServices) {
            DichVu dv = ct.getDichVu();
            double thanhTien = ct.getDonGiaThucHien() * ct.getSoLuong();
            tong += thanhTien;
            modelDichVu.addRow(new Object[]{
                    stt++,
                    dv.getMaDichVu(),
                    dv.getTenDichVu(),
                    "Lần",
                    ct.getSoLuong(),
                    Fmt.money(ct.getDonGiaThucHien()),
                    Fmt.money(thanhTien),
                    ct.getGhiChu() != null ? ct.getGhiChu() : ""
            });
        }
        lblTongTienDichVu.setText("Tổng tiền dịch vụ tạm tính: " + Fmt.money(tong));
    }

    private void handleSave() {
        if (selectedCustomer == null) {
            Msg.error(this, "Vui lòng chọn khách hàng tiếp nhận.");
            return;
        }
        if (selectedVehicle == null) {
            Msg.error(this, "Vui lòng chọn xe của khách hàng.");
            return;
        }

        String odoStr = txtOdo.getText().trim();
        if (odoStr.isEmpty()) {
            Msg.error(this, "Vui lòng nhập số km tiếp nhận (ODO).");
            return;
        }
        float odo;
        try {
            odo = Float.parseFloat(odoStr.replace(",", "").replace(".", ""));
            if (odo < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Msg.error(this, "Số km tiếp nhận phải là số dương hợp lệ.");
            return;
        }

        String yc = taYeuCau.getText().trim();
        if (yc.isEmpty()) {
            Msg.error(this, "Vui lòng nhập yêu cầu của khách hàng.");
            return;
        }

        String tt = taTinhTrang.getText().trim();
        String gc = txtGhiChu.getText().trim();
        if (!gc.isEmpty()) {
            tt = (tt.isEmpty() ? "" : tt + "\n") + "[Ghi chú]: " + gc;
        }

        float fuel = switch (cboFuel.getSelectedIndex()) {
            case 0 -> 0.0f;
            case 1 -> 0.25f;
            case 2 -> 0.5f;
            case 3 -> 0.75f;
            case 4 -> 1.0f;
            default -> 0.5f;
        };

        String maKtv = null;
        int ktvIdx = cboTechnician.getSelectedIndex();
        if (ktvIdx > 0 && ktvIdx - 1 < listTechnicians.size()) {
            maKtv = listTechnicians.get(ktvIdx - 1).getMaNV();
        }

        String maTnv = Session.user() != null ? Session.user().maNV() : "NV001";

        LocalDateTime henTra = null;
        String henTraStr = txtHenTra.getText().trim();
        if (!henTraStr.isEmpty()) {
            try {
                henTra = LocalDateTime.parse(henTraStr, DT_FMT);
            } catch (Exception ex) {
                Msg.error(this, "Định dạng thời gian hẹn trả không hợp lệ (dd/MM/yyyy HH:mm).");
                return;
            }
        }

        TiepNhanInput input = new TiepNhanInput(
                selectedCustomer.getMaKH(),
                selectedVehicle.getBienSo(),
                maTnv,
                maKtv,
                odo,
                fuel,
                yc,
                tt,
                LocalDateTime.now(),
                henTra
        );

        try {
            String maPhieu = phieuBus.taoPhieuTiepNhan(input, listSelectedServices);
            Msg.info(this, "Tạo phiếu tiếp nhận thành công!\nMã phiếu: " + maPhieu);
            thanhCong = true;
            dispose();
        } catch (Exception ex) {
            Msg.error(this, "Lỗi tạo phiếu: " + ex.getMessage());
        }
    }

    // =========================================================================
    // QUY TẮC BẮT BUỘC: TOÀN BỘ SỰ KIỆN NẰM TẠI initEvents() Ở CUỐI FILE
    // =========================================================================
    private void initEvents() {
        btnBrowseCustomer.addActionListener(e -> {
            TraCuuKhachHangDialog dlg = new TraCuuKhachHangDialog(this, null, null);
            dlg.setVisible(true);
            KhachHang chosen = dlg.getSelectedCustomer();
            if (chosen != null) {
                setCustomer(chosen);
            }
        });

        cboVehicle.addActionListener(e -> updateSelectedVehicleInfo());
        btnThemDV.addActionListener(e -> handleAddService());
        btnBoDV.addActionListener(e -> handleRemoveService());
        btnHuy.addActionListener(e -> dispose());
        btnLuu.addActionListener(e -> handleSave());
    }
}

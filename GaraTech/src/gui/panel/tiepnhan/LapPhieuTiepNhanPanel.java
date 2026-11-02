package gui.panel.tiepnhan;

import bus.KhachHangXeBus;
import bus.KhachHangXeBus.KhachHangOverview;
import bus.TiepNhanXeBus;
import bus.TiepNhanXeBus.TiepNhanInput;
import com.formdev.flatlaf.FlatClientProperties;
import entity.KhachHang;
import entity.PhieuTiepNhan;
import entity.Xe;
import enums.User;
import gui.Navigator;
import gui.Screen;
import gui.component.Msg;
import gui.component.StatusBadge;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Lập phiếu tiếp nhận mới.
 */
public final class LapPhieuTiepNhanPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
    private static final DateTimeFormatter INPUT_DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Callbacks điều hướng
    private final Runnable onViewListCallback;
    private final Consumer<PhieuTiepNhan> onSavedCallback;

    // Data State
    private KhachHang selectedCustomer = null;
    private Xe selectedVehicle = null;
    private float vehiclePreviousOdo = 0;
    private boolean isUpdatingVehicleCombo = false;

    // Component: Banner thông báo nhận diện hồ sơ
    private final RoundedPanel pBanner = new RoundedPanel(12, new Color(0xE9, 0xF5, 0xEF));
    private final JLabel lblBannerMsg = new JLabel("");

    // Form Left: Tra cứu & Chọn Khách hàng / Xe
    private final JButton btnBrowseCust = UI.primaryButton("🔍 Tra cứu");
    private final JLabel lblSelectedCustDisplay = new JLabel("Chưa chọn khách hàng");
    private final JComboBox<String> cboVehicle = new JComboBox<>();
    private final JLabel lblSummaryContact = new JLabel("---");
    private final JLabel lblSummaryVehicle = new JLabel("---");

    // Form Left: Thông tin tiếp nhận
    private final JTextField txtMaPhieu = new JTextField("Tự động khi lưu");
    private final JTextField txtTrangThai = new JTextField("Chờ khách hàng xác nhận");
    private final JTextArea taYeuCauKhach = new JTextArea();
    private final JTextArea taTinhTrangBanDau = new JTextArea();
    private final JTextField txtOdo = new JTextField("0");
    private final JLabel lblOdoHint = UI.muted("Số km không âm");
    private final JComboBox<String> cboFuel = new JComboBox<>(new String[]{
            "Hết xăng (0%)",
            "1/4 bình (25%)",
            "1/2 bình (50%)",
            "3/4 bình (75%)",
            "Đầy bình (100%)"
    });
    private final JTextField txtThoiGianTiepNhan = new JTextField();
    private final JTextField txtThoiGianHenTra = new JTextField();
    private final JTextField txtNhanVien = new JTextField();

    // Right Column: Summary Card
    private final JLabel lblSumMaPhieu = new JLabel("Tự động khi lưu");
    private final JLabel lblSumKhachHang = new JLabel("---");
    private final JLabel lblSumBienSo = new JLabel("---");
    private final JLabel lblSumThoiGian = new JLabel("---");

    // Checklist
    private final JCheckBox chkConditionRecorded = new JCheckBox("Đã ghi nhận hiện trạng xe");
    private final JCheckBox chkRequestConfirmed = new JCheckBox("Khách xác nhận yêu cầu ban đầu");

    private final JButton btnConfirm = UI.primaryButton("Xác nhận tiếp nhận");
    private final JButton btnReset = UI.secondaryButton("Làm mới form");

    public LapPhieuTiepNhanPanel(Runnable onViewListCallback, Consumer<PhieuTiepNhan> onSavedCallback) {
        this.onViewListCallback = onViewListCallback;
        this.onSavedCallback = onSavedCallback;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        initEvents();
        resetForm();
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
        add(Box.createVerticalStrut(16));

        // 2. Nội dung 2 cột
        JPanel twoColPanel = new JPanel(new BorderLayout(20, 0));
        twoColPanel.setOpaque(false);

        JPanel leftCol = buildLeftColumn();
        JPanel rightCol = buildRightColumn();

        twoColPanel.add(leftCol, BorderLayout.CENTER);
        twoColPanel.add(rightCol, BorderLayout.EAST);
        twoColPanel.setAlignmentX(LEFT_ALIGNMENT);

        add(twoColPanel);
    }

    private JPanel buildLeftColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        // Card 1: Khách hàng & Phương tiện
        RoundedPanel cardCustVeh = new RoundedPanel(16, Theme.CARD);
        cardCustVeh.setLayout(new BorderLayout(0, 16));
        cardCustVeh.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        cardCustVeh.add(UI.h2("Khách hàng & phương tiện"), BorderLayout.NORTH);

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

        btnBrowseCust.setPreferredSize(new Dimension(110, 38));
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

        cardCustVeh.add(cvBody, BorderLayout.CENTER);
        cardCustVeh.setAlignmentX(LEFT_ALIGNMENT);
        col.add(cardCustVeh);

        col.add(Box.createVerticalStrut(18));

        // Card 2: Thông tin tiếp nhận
        RoundedPanel cardInfo = new RoundedPanel(16, Theme.CARD);
        cardInfo.setLayout(new BorderLayout(0, 16));
        cardInfo.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        cardInfo.add(UI.h2("Thông tin tiếp nhận"), BorderLayout.NORTH);

        JPanel infoBody = new JPanel();
        infoBody.setOpaque(false);
        infoBody.setLayout(new BoxLayout(infoBody, BoxLayout.Y_AXIS));

        // Row 1: Mã phiếu | Trạng thái ban đầu
        JPanel r1 = new JPanel(new GridLayout(1, 2, 20, 0));
        r1.setOpaque(false);
        txtMaPhieu.setEditable(false);
        txtMaPhieu.setBackground(new Color(0xF8FAFC));
        txtTrangThai.setEditable(false);
        txtTrangThai.setBackground(new Color(0xF8FAFC));
        r1.add(formFieldWrap("Mã phiếu tiếp nhận", txtMaPhieu, null));
        r1.add(formFieldWrap("Trạng thái ban đầu", txtTrangThai, null));
        r1.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r1);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 2: Yêu cầu của khách hàng (Có viền rõ ràng, cuộn mượt, không giới hạn ký tự)
        infoBody.add(formScrollableTextArea("Yêu cầu của khách hàng *", taYeuCauKhach));
        infoBody.add(Box.createVerticalStrut(14));

        // Row 3: Tình trạng ban đầu / Hiện trạng xe (Có viền rõ ràng, cuộn mượt, không giới hạn ký tự)
        infoBody.add(formScrollableTextArea("Tình trạng ban đầu / Hiện trạng xe *", taTinhTrangBanDau));
        infoBody.add(Box.createVerticalStrut(14));

        // Row 4: ODO hiện tại | Mức nhiên liệu
        JPanel r4 = new JPanel(new GridLayout(1, 2, 20, 0));
        r4.setOpaque(false);
        r4.add(formFieldWrapWithCustomHint("Số km hiện tại (ODO) *", txtOdo, lblOdoHint));
        r4.add(formFieldWrap("Mức nhiên liệu *", cboFuel, null));
        r4.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r4);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 5: Thời gian tiếp nhận | Hẹn trả
        JPanel r5 = new JPanel(new GridLayout(1, 2, 20, 0));
        r5.setOpaque(false);
        r5.add(formFieldWrap("Thời gian tiếp nhận *", txtThoiGianTiepNhan, null));
        r5.add(formFieldWrap("Hẹn trả xe (tùy chọn)", txtThoiGianHenTra, null));
        r5.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r5);
        infoBody.add(Box.createVerticalStrut(14));

        // Row 6: Nhân viên tiếp nhận
        txtNhanVien.setEditable(false);
        txtNhanVien.setBackground(new Color(0xF8FAFC));
        JPanel r6 = formFieldWrap("Nhân viên tiếp nhận *", txtNhanVien, null);
        r6.setAlignmentX(LEFT_ALIGNMENT);
        infoBody.add(r6);

        cardInfo.add(infoBody, BorderLayout.CENTER);
        cardInfo.setAlignmentX(LEFT_ALIGNMENT);
        col.add(cardInfo);

        return col;
    }

    private JPanel buildRightColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(340, 10));

        // Card Tóm tắt phiếu tiếp nhận
        RoundedPanel cardSum = new RoundedPanel(16, Theme.CARD);
        cardSum.setLayout(new BoxLayout(cardSum, BoxLayout.Y_AXIS));
        cardSum.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JLabel lblSumTitle = UI.h2("Tóm tắt phiếu tiếp nhận");
        lblSumTitle.setAlignmentX(LEFT_ALIGNMENT);
        cardSum.add(lblSumTitle);
        cardSum.add(Box.createVerticalStrut(14));

        // Badge trạng thái
        JPanel pBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pBadge.setOpaque(false);
        StatusBadge statusBadge = new StatusBadge("Chờ khách hàng xác nhận");
        pBadge.add(statusBadge);
        pBadge.setAlignmentX(LEFT_ALIGNMENT);
        cardSum.add(pBadge);
        cardSum.add(Box.createVerticalStrut(16));

        // Key-Value summaries
        cardSum.add(buildSummaryItem("Mã phiếu", lblSumMaPhieu));
        cardSum.add(Box.createVerticalStrut(10));
        cardSum.add(buildSummaryItem("Khách hàng", lblSumKhachHang));
        cardSum.add(Box.createVerticalStrut(10));
        cardSum.add(buildSummaryItem("Biển số", lblSumBienSo));
        cardSum.add(Box.createVerticalStrut(10));
        cardSum.add(buildSummaryItem("Tiếp nhận", lblSumThoiGian));
        cardSum.add(Box.createVerticalStrut(18));

        // Separator
        JPanel sep = new JPanel();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setBackground(Theme.BORDER);
        sep.setAlignmentX(LEFT_ALIGNMENT);
        cardSum.add(sep);
        cardSum.add(Box.createVerticalStrut(18));

        // Checklist 2 mục
        styleCheckbox(chkConditionRecorded);
        styleCheckbox(chkRequestConfirmed);

        cardSum.add(chkConditionRecorded);
        cardSum.add(Box.createVerticalStrut(10));
        cardSum.add(chkRequestConfirmed);
        cardSum.add(Box.createVerticalStrut(24));

        // Nút hành động
        btnConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnConfirm.setAlignmentX(LEFT_ALIGNMENT);
        cardSum.add(btnConfirm);
        cardSum.add(Box.createVerticalStrut(10));

        btnReset.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnReset.setAlignmentX(LEFT_ALIGNMENT);
        cardSum.add(btnReset);

        cardSum.setAlignmentX(LEFT_ALIGNMENT);
        col.add(cardSum);

        return col;
    }

    private void initEvents() {
        // Mở dialog tra cứu danh sách khách hàng
        btnBrowseCust.addActionListener(e -> handleOpenBrowseDialog());

        // Chọn xe trên ComboBox
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

        // Nút Xác nhận tiếp nhận
        btnConfirm.addActionListener(e -> handleConfirmReception());

        // Nút Reset
        btnReset.addActionListener(e -> resetForm());
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

    public void refreshCustomerList() {
        // Không cần load toàn bộ vào combobox nữa
    }

    /**
     * Nạp dữ liệu khởi đầu khi chuyển từ màn hình khác sang (ví dụ bấm "Tiếp nhận xe" ở Hồ sơ khách).
     */
    public void setInitialCustomerAndVehicle(KhachHang kh, Xe xe) {
        if (kh != null) {
            selectCustomer(kh);
        }
        if (xe != null) {
            selectVehicle(xe);
        }
    }

    private void selectCustomer(KhachHang kh) {
        this.selectedCustomer = kh;
        isUpdatingVehicleCombo = true;
        try {
            cboVehicle.removeAllItems();
            cboVehicle.addItem("-- Chọn xe theo biển số --");

            if (kh == null) {
                lblSelectedCustDisplay.setText("Chưa chọn khách hàng");
                lblSelectedCustDisplay.setForeground(Theme.MUTED);
                btnBrowseCust.setText("🔍 Tra cứu");

                lblSummaryContact.setText("---");
                lblSummaryVehicle.setText("---");
                lblSumKhachHang.setText("---");
                lblSumBienSo.setText("---");
                pBanner.setVisible(false);
                selectVehicle(null);
                return;
            }

            // Cập nhật thông tin khách hàng đang chọn
            lblSelectedCustDisplay.setText(String.format("✓ %s · %s (%s)", kh.getHoTen(), kh.getSoDienThoai(), kh.getMaKH()));
            lblSelectedCustDisplay.setForeground(new Color(0x10, 0x25, 0x44));
            btnBrowseCust.setText("🔍 Đổi khách");

            lblSummaryContact.setText(kh.getSoDienThoai() != null && !kh.getSoDienThoai().isBlank() ? kh.getSoDienThoai() : "---");
            lblSumKhachHang.setText(String.format("%s (%s)", kh.getHoTen(), kh.getMaKH()));

            List<Xe> vehicles = KhachHangXeBus.getDanhSachXe(kh.getMaKH());
            for (Xe x : vehicles) {
                cboVehicle.addItem(String.format("%s · %s %s (%s)", x.getBienSo(), x.getHangXe(), x.getModel(), x.getMauXe()));
            }

            // Tự động chọn xe đầu tiên nếu khách có xe
            if (!vehicles.isEmpty()) {
                cboVehicle.setSelectedIndex(1);
                selectVehicle(vehicles.get(0));
            } else {
                selectVehicle(null);
                Msg.info(this, "Khách hàng " + kh.getHoTen() + " chưa có phương tiện liên kết.\nBạn có thể thêm xe cho khách trước khi tiếp nhận.");
            }
        } finally {
            isUpdatingVehicleCombo = false;
        }
    }

    private void selectVehicle(Xe xe) {
        this.selectedVehicle = xe;
        if (xe == null) {
            lblSummaryVehicle.setText("Chưa chọn xe");
            lblSumBienSo.setText("---");
            vehiclePreviousOdo = 0;
            lblOdoHint.setText("Số km không âm");
            pBanner.setVisible(false);
            return;
        }

        lblSummaryVehicle.setText(String.format("%s · %s %s %d · %s", xe.getBienSo(), xe.getHangXe(), xe.getModel(), xe.getNamSanXuat(), xe.getMauXe()));
        lblSumBienSo.setText(xe.getBienSo());

        // Lấy ODO gần nhất
        vehiclePreviousOdo = TiepNhanXeBus.layOdoGanNhat(xe.getBienSo());
        txtOdo.setText(String.format("%.0f", Math.max(xe.getSoKmHienTai(), vehiclePreviousOdo)));
        lblOdoHint.setText(String.format("Không nhỏ hơn lần ghi nhận trước: %,.0f km", vehiclePreviousOdo));

        // Hiển thị banner xanh
        lblBannerMsg.setText(String.format("Đã chọn hồ sơ %s và xe %s. Tiếp nhận đúng phương tiện, tránh tạo bản ghi trùng.",
                selectedCustomer != null ? selectedCustomer.getHoTen() : "", xe.getBienSo()));
        pBanner.setVisible(true);

        // Đồng bộ combobox
        if (!isUpdatingVehicleCombo && cboVehicle.getItemCount() > 1) {
            isUpdatingVehicleCombo = true;
            try {
                for (int i = 1; i < cboVehicle.getItemCount(); i++) {
                    if (cboVehicle.getItemAt(i).startsWith(xe.getBienSo())) {
                        if (cboVehicle.getSelectedIndex() != i) {
                            cboVehicle.setSelectedIndex(i);
                        }
                        break;
                    }
                }
            } finally {
                isUpdatingVehicleCombo = false;
            }
        }
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

            // Kiểm tra Checklist
            if (!chkConditionRecorded.isSelected()) {
                Msg.error(this, "Vui lòng xác nhận mục: 'Đã ghi nhận hiện trạng xe'.");
                chkConditionRecorded.requestFocus();
                return;
            }
            if (!chkRequestConfirmed.isSelected()) {
                Msg.error(this, "Vui lòng xác nhận mục: 'Khách xác nhận yêu cầu ban đầu'.");
                chkRequestConfirmed.requestFocus();
                return;
            }

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
            LocalDateTime tgHenTra = null;
            String henTraStr = txtThoiGianHenTra.getText().trim();
            if (!henTraStr.isEmpty()) {
                try {
                    tgHenTra = LocalDateTime.parse(henTraStr, INPUT_DT_FMT);
                } catch (Exception ex) {
                    Msg.error(this, "Định dạng thời gian hẹn trả không hợp lệ (mẫu: dd/MM/yyyy HH:mm).");
                    txtThoiGianHenTra.requestFocus();
                    return;
                }
            }

            User u = Session.user();
            String maNV = u != null ? u.maNV() : "NV26002";

            TiepNhanInput input = new TiepNhanInput(
                    selectedCustomer.getMaKH(),
                    selectedVehicle.getBienSo(),
                    maNV,
                    odo,
                    fuel,
                    tinhTrang,
                    yeuCau,
                    tgTiepNhan,
                    tgHenTra
            );

            PhieuTiepNhan ptn = TiepNhanXeBus.taoPhieuTiepNhan(input);

            Msg.info(this, String.format("Tiếp nhận thành công xe %s!\nMã phiếu tiếp nhận: %s\nTrạng thái: Chờ khách hàng xác nhận.",
                    selectedVehicle.getBienSo(), ptn.getMaPhieuTN()));

            if (onSavedCallback != null) {
                onSavedCallback.accept(ptn);
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

    public void resetForm() {
        selectCustomer(null);

        taYeuCauKhach.setText("");
        taTinhTrangBanDau.setText("");
        txtOdo.setText("0");
        cboFuel.setSelectedIndex(2); // Mặc định 1/2 bình
        txtThoiGianTiepNhan.setText(LocalDateTime.now().format(INPUT_DT_FMT));
        txtThoiGianHenTra.setText(LocalDateTime.now().plusHours(8).format(INPUT_DT_FMT));

        User u = Session.user();
        if (u != null) {
            txtNhanVien.setText(String.format("%s · %s", u.maNV(), u.hoTen()));
        } else {
            txtNhanVien.setText("NV26002 · Trần Thị Bình");
        }

        chkConditionRecorded.setSelected(true);
        chkRequestConfirmed.setSelected(true);

        lblSumThoiGian.setText(LocalDateTime.now().format(DT_FMT));
    }

    // Helper UI Builders
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

        styleInput(comp);
        comp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(comp);

        if (hint != null && !hint.isEmpty()) {
            p.add(Box.createVerticalStrut(4));
            JLabel h = UI.muted(hint);
            h.setFont(Theme.font(Font.PLAIN, 11));
            h.setAlignmentX(LEFT_ALIGNMENT);
            p.add(h);
        }
        return p;
    }

    private JPanel formFieldWrapWithCustomHint(String label, JComponent comp, JLabel hintLabel) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        styleInput(comp);
        comp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(comp);

        p.add(Box.createVerticalStrut(4));
        hintLabel.setFont(Theme.font(Font.PLAIN, 11));
        hintLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hintLabel);

        return p;
    }

    /**
     * Khung nhập Text Area có viền chuẩn FlatLaf, hỗ trợ cuộn không giới hạn ký tự.
     */
    private JPanel formScrollableTextArea(String label, JTextArea ta) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        ta.setFont(Theme.font(Font.PLAIN, 13));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane sp = new JScrollPane(ta);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xD5, 0xDB, 0xE5), 1));
        sp.setPreferredSize(new Dimension(200, 80));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sp);

        return p;
    }

    private JPanel buildSummaryItem(String label, JLabel valLabel) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JLabel l = UI.muted(label);
        l.setPreferredSize(new Dimension(85, 18));
        p.add(l, BorderLayout.WEST);

        valLabel.setFont(Theme.font(Font.BOLD, 12));
        valLabel.setForeground(Theme.TEXT);
        p.add(valLabel, BorderLayout.CENTER);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private void styleInput(JComponent c) {
        c.setFont(Theme.font(Font.PLAIN, 13));
        if (c instanceof JTextField) {
            c.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        } else {
            c.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        }
        c.setPreferredSize(new Dimension(200, 38));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private void styleCheckbox(JCheckBox chk) {
        chk.setOpaque(false);
        chk.setFont(Theme.font(Font.PLAIN, 13));
        chk.setForeground(Theme.TEXT);
        chk.setSelected(true);
        chk.setAlignmentX(LEFT_ALIGNMENT);
    }

    /**
     * Hộp thoại Tra cứu khách hàng & danh sách xe trực quan.
     * Cho phép nhân viên lọc nhanh theo tên, SĐT, biển số xe và bấm chọn nạp vào phiếu.
     */
    public static final class TraCuuKhachHangDialog extends JDialog {

        private final JTextField txtFilter = new JTextField();
        private final DefaultTableModel tableModel = UI.readOnlyModel("Mã KH", "Họ và tên", "Số điện thoại", "Xe liên kết", "Điểm");
        private final JTable table = UI.table(tableModel);
        private final JLabel lblCount = UI.muted("0 khách hàng");

        private final JButton btnSelect = UI.primaryButton("Chọn khách hàng");
        private final JButton btnAddNew = UI.secondaryButton("+ Thêm khách mới");
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

            JLabel lblSearchIcon = new JLabel("🔍 Tìm kiếm:");
            lblSearchIcon.setFont(Theme.font(Font.BOLD, 12));
            lblSearchIcon.setForeground(Theme.TEXT);
            pTop.add(lblSearchIcon, BorderLayout.WEST);

            txtFilter.setFont(Theme.font(Font.PLAIN, 13));
            txtFilter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Gõ số điện thoại, tên khách hàng hoặc biển số xe để lọc tức thì...");
            txtFilter.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
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

            add(root, BorderLayout.CENTER);
        }

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

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
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

            lblCount.setText(String.format("Hiển thị %d khách hàng phù hợp", currentList.size()));

            if (!currentList.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
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
    }
}

package gui.panel.phieusuachua;

import bus.BusinessException;
import bus.PhieuSuaChuaBus;
import dao.PhieuSuaChuaDao.PhieuSuaChuaRow;
import entity.HangMucSuaChua;
import enums.TrangThaiPhieuSuaChua;
import gui.Navigator;
import gui.Screen;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Chi tiết phiếu sửa chữa (SV2 - Tiếp nhận).
 * Hiển thị đầy đủ thông tin phiếu, bảng hạng mục ban đầu & phát sinh,
 * và thanh hành động xử lý theo đúng vòng đời trạng thái.
 */
public final class ChiTietPhieuSuaChuaPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private final PhieuSuaChuaBus bus = new PhieuSuaChuaBus();
    private final Runnable onBackCallback;
    private PhieuSuaChuaRow currentTicket = null;

    private final JLabel lblHeaderTitle = UI.h2("Chi tiết phiếu sửa chữa");
    private final JLabel lblStatusBadge = new JLabel();

    // Thông tin cơ bản
    private final JTextField txtKhachHang = new JTextField();
    private final JTextField txtPhuongTien = new JTextField();
    private final JTextField txtMaPhieuTN = new JTextField();
    private final JTextField txtNhanVienLap = new JTextField();
    private final JTextField txtKtv = new JTextField();
    private final JTextField txtThoiGianDuKien = new JTextField();
    private final JTextField txtThoiGianBatDau = new JTextField();
    private final JTextField txtThoiGianHoanThanh = new JTextField();
    private final JTextField txtLyDoHuy = new JTextField();
    private final JPanel pLyDoHuy = new JPanel(new BorderLayout());

    // Bảng hạng mục
    private final String[] COLS = {
            "STT", "Loại", "Mã", "Tên hạng mục", "SL", "Đơn giá", "Giảm giá", "Thành tiền", "Phát sinh", "Trạng thái", "Ghi chú"
    };
    private final DefaultTableModel itemModel = UI.readOnlyModel(COLS);
    private final JTable itemTable = UI.table(itemModel);
    private List<HangMucSuaChua> currentHangMucs = new ArrayList<>();

    // Tóm tắt chi phí
    private final JLabel lblTongTien = new JLabel("0 đ");

    // Các nút hành động theo ngữ cảnh trạng thái
    private final JButton btnKhachDuyet = UI.primaryButton("✓ Khách đồng ý sửa chữa");
    private final JButton btnHuyPhieu = UI.dangerButton("❌ Hủy phiếu");
    private final JButton btnBatDauSua = UI.primaryButton("🔧 Bắt đầu sửa chữa");
    private final JButton btnBaoHoanThanh = UI.primaryButton("🏁 Báo hoàn thành (Chờ KCS)");
    private final JButton btnThemPhatSinh = UI.secondaryButton("+ Thêm việc phát sinh");
    private final JButton btnLapHoaDon = UI.primaryButton("🧾 Lập hóa đơn thanh toán ↗");
    private final JButton btnBack = UI.secondaryButton("← Quay lại danh sách");

    public ChiTietPhieuSuaChuaPanel(Runnable onBackCallback) {
        this.onBackCallback = onBackCallback;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        buildUI();
        initEvents();
    }

    private void buildUI() {
        // Cột trên: Thông tin phiếu & phương tiện
        RoundedPanel cardInfo = new RoundedPanel(16, Theme.CARD);
        cardInfo.setLayout(new BorderLayout(0, 14));
        cardInfo.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);
        headRow.add(lblHeaderTitle, BorderLayout.WEST);
        headRow.add(lblStatusBadge, BorderLayout.EAST);
        cardInfo.add(headRow, BorderLayout.NORTH);

        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 20, 10));
        infoGrid.setOpaque(false);

        setupReadonlyField(txtKhachHang);
        setupReadonlyField(txtPhuongTien);
        setupReadonlyField(txtMaPhieuTN);
        setupReadonlyField(txtNhanVienLap);
        setupReadonlyField(txtKtv);
        setupReadonlyField(txtThoiGianDuKien);
        setupReadonlyField(txtThoiGianBatDau);
        setupReadonlyField(txtThoiGianHoanThanh);

        infoGrid.add(formFieldWrap("Khách hàng & SĐT", txtKhachHang));
        infoGrid.add(formFieldWrap("Phương tiện liên kết", txtPhuongTien));
        infoGrid.add(formFieldWrap("Phiếu tiếp nhận ban đầu", txtMaPhieuTN));
        infoGrid.add(formFieldWrap("Nhân viên lập báo giá", txtNhanVienLap));
        infoGrid.add(formFieldWrap("Kỹ thuật viên phụ trách", txtKtv));
        infoGrid.add(formFieldWrap("Hẹn hoàn thành dự kiến", txtThoiGianDuKien));
        infoGrid.add(formFieldWrap("Thời gian bắt đầu làm", txtThoiGianBatDau));
        infoGrid.add(formFieldWrap("Thời gian hoàn tất", txtThoiGianHoanThanh));

        // Khối lý do hủy (ẩn mặc định, chỉ hiện khi đã hủy)
        setupReadonlyField(txtLyDoHuy);
        txtLyDoHuy.setForeground(new Color(0xB91C1C));
        pLyDoHuy.setOpaque(false);
        pLyDoHuy.add(fieldLabel("Lý do hủy phiếu sửa chữa:"), BorderLayout.NORTH);
        pLyDoHuy.add(txtLyDoHuy, BorderLayout.CENTER);
        pLyDoHuy.setVisible(false);

        JPanel pInfoContainer = new JPanel(new BorderLayout(0, 10));
        pInfoContainer.setOpaque(false);
        pInfoContainer.add(infoGrid, BorderLayout.CENTER);
        pInfoContainer.add(pLyDoHuy, BorderLayout.SOUTH);

        cardInfo.add(pInfoContainer, BorderLayout.CENTER);

        // Khối giữa: Bảng hạng mục
        RoundedPanel cardItems = new RoundedPanel(16, Theme.CARD);
        cardItems.setLayout(new BorderLayout(0, 12));
        cardItems.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel headItems = new JPanel(new BorderLayout());
        headItems.setOpaque(false);
        headItems.add(UI.h2("Chi tiết hạng mục sửa chữa & phụ tùng"), BorderLayout.WEST);

        JPanel pRightItemBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pRightItemBtns.setOpaque(false);
        pRightItemBtns.add(btnThemPhatSinh);
        headItems.add(pRightItemBtns, BorderLayout.EAST);
        cardItems.add(headItems, BorderLayout.NORTH);

        setupItemTable();
        JScrollPane scrTable = new JScrollPane(itemTable);
        scrTable.setPreferredSize(new Dimension(100, 240));
        cardItems.add(scrTable, BorderLayout.CENTER);

        // Thanh tóm tắt chi phí dưới bảng
        JPanel pBotTotal = new JPanel(new BorderLayout());
        pBotTotal.setOpaque(false);
        JLabel lblTotalTitle = new JLabel("Tổng chi phí sửa chữa:");
        lblTotalTitle.setFont(Theme.font(14, Font.BOLD));
        lblTongTien.setFont(Theme.font(18, Font.BOLD));
        lblTongTien.setForeground(new Color(0x1E3A8A));
        pBotTotal.add(lblTotalTitle, BorderLayout.WEST);
        pBotTotal.add(lblTongTien, BorderLayout.EAST);
        cardItems.add(pBotTotal, BorderLayout.SOUTH);

        // Khối dưới cùng: Nút thao tác
        JPanel botActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        botActions.setOpaque(false);
        botActions.add(btnKhachDuyet);
        botActions.add(btnBatDauSua);
        botActions.add(btnBaoHoanThanh);
        botActions.add(btnLapHoaDon);
        botActions.add(btnHuyPhieu);
        botActions.add(btnBack);

        JPanel centerContent = new JPanel();
        centerContent.setOpaque(false);
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.add(cardInfo);
        centerContent.add(Box.createVerticalStrut(16));
        centerContent.add(cardItems);

        add(centerContent, BorderLayout.CENTER);
        add(botActions, BorderLayout.SOUTH);
    }

    private void setupItemTable() {
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setRowHeight(34);

        itemTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // STT
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(85);  // Loại
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Mã
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Tên
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // SL
        itemTable.getColumnModel().getColumn(5).setPreferredWidth(105); // Đơn giá
        itemTable.getColumnModel().getColumn(6).setPreferredWidth(95);  // Giảm giá
        itemTable.getColumnModel().getColumn(7).setPreferredWidth(115); // Thành tiền
        itemTable.getColumnModel().getColumn(8).setPreferredWidth(95);  // Phát sinh
        itemTable.getColumnModel().getColumn(9).setPreferredWidth(110); // Trạng thái
        itemTable.getColumnModel().getColumn(10).setPreferredWidth(140);// Ghi chú

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                if (value instanceof Number n) {
                    value = Fmt.money(n.longValue()) + " đ";
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            }
        };
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        itemTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        itemTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        itemTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        itemTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);

        // Renderer trạng thái hạng mục
        itemTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String st = value != null ? value.toString() : "";
                String color = switch (st) {
                    case "Chờ duyệt" -> "#B45309";
                    case "Đã xác nhận" -> "#1D4ED8";
                    case "Hoàn thành" -> "#059669";
                    case "Từ chối" -> "#DC2626";
                    default -> "#475569";
                };
                lbl.setText("<html><span style='color:" + color + "; font-weight:bold'>● " + st + "</span></html>");
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return lbl;
            }
        });
    }

    private void initEvents() {
        btnBack.addActionListener(e -> {
            if (onBackCallback != null) onBackCallback.run();
        });

        // 1. Khách đồng ý sửa chữa (Chờ khách duyệt -> Đã xác nhận)
        btnKhachDuyet.addActionListener(e -> {
            if (currentTicket == null) return;
            if (!Msg.confirm(this, "Khách hàng đã đồng ý duyệt phương án sửa chữa cho xe " + currentTicket.bienSo() + "?")) {
                return;
            }
            try {
                bus.chuyenTrangThai(currentTicket.maPhieuSC(), TrangThaiPhieuSuaChua.DA_XAC_NHAN, null);
                Msg.info(this, "Đã cập nhật trạng thái phiếu sang 'Đã xác nhận'!");
                reloadTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 2. Bắt đầu sửa chữa (Đã xác nhận -> Đang sửa chữa)
        btnBatDauSua.addActionListener(e -> {
            if (currentTicket == null) return;
            if (!Msg.confirm(this, "Bắt đầu tiến hành sửa chữa cho xe " + currentTicket.bienSo() + "?")) {
                return;
            }
            try {
                bus.chuyenTrangThai(currentTicket.maPhieuSC(), TrangThaiPhieuSuaChua.DANG_SUA_CHUA, null);
                Msg.info(this, "Phiếu đã chuyển sang trạng thái 'Đang sửa chữa'.");
                reloadTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 3. Báo hoàn thành (Đang sửa chữa -> Chờ kiểm tra)
        btnBaoHoanThanh.addActionListener(e -> {
            if (currentTicket == null) return;
            if (!Msg.confirm(this, "Xác nhận các hạng mục đã hoàn tất và chuyển xe sang khu vực 'Chờ kiểm tra' KCS?")) {
                return;
            }
            try {
                bus.chuyenTrangThai(currentTicket.maPhieuSC(), TrangThaiPhieuSuaChua.CHO_KIEM_TRA, null);
                Msg.info(this, "Đã chuyển phiếu sang trạng thái 'Chờ kiểm tra' chất lượng.");
                reloadTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 4. Lập hóa đơn (Hoàn thành -> chuyển sang màn hình Hóa đơn)
        btnLapHoaDon.addActionListener(e -> {
            if (currentTicket == null) return;
            Navigator.setPayload(currentTicket);
            try {
                Navigator.go(Screen.HOA_DON);
            } catch (Exception ex) {
                Msg.info(this, "Đã lưu thông tin phiếu sửa chữa " + currentTicket.maPhieuSC() + " để lập hóa đơn.");
            }
        });

        // 5. Hủy phiếu
        btnHuyPhieu.addActionListener(e -> {
            if (currentTicket == null) return;
            String reason = JOptionPane.showInputDialog(
                    this,
                    "Nhập lý do hủy phiếu sửa chữa " + currentTicket.maPhieuSC() + ":",
                    "Hủy phiếu sửa chữa",
                    JOptionPane.WARNING_MESSAGE
            );
            if (reason == null || reason.trim().isBlank()) return;

            try {
                bus.chuyenTrangThai(currentTicket.maPhieuSC(), TrangThaiPhieuSuaChua.DA_HUY, reason.trim());
                Msg.info(this, "Đã hủy phiếu sửa chữa thành công.");
                reloadTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 6. Thêm hạng mục phát sinh
        btnThemPhatSinh.addActionListener(e -> openThemPhatSinhDialog());
    }

    public void displayTicket(PhieuSuaChuaRow row) {
        this.currentTicket = row;
        if (row == null) return;

        lblHeaderTitle.setText("Chi tiết phiếu sửa chữa · " + row.maPhieuSC());
        lblStatusBadge.setText(formatStatusBadge(row.trangThai()));

        txtKhachHang.setText(String.format("%s · SĐT: %s", row.tenKH(), row.sdtKH()));
        txtPhuongTien.setText(String.format("%s · %s", row.bienSo(), row.thongTinXe()));
        txtMaPhieuTN.setText(row.maPhieuTN());
        txtNhanVienLap.setText(row.tenNVLap() != null ? row.tenNVLap() : row.maNVLap());
        txtKtv.setText(row.tenKTV() != null ? row.tenKTV() : (row.maKTV() != null ? row.maKTV() : "Chưa phân công"));

        txtThoiGianDuKien.setText(row.thoiGianDuKien() != null ? row.thoiGianDuKien().format(DT_FMT) : "---");
        txtThoiGianBatDau.setText(row.thoiGianBatDau() != null ? row.thoiGianBatDau().format(DT_FMT) : "Chưa bắt đầu");
        txtThoiGianHoanThanh.setText(row.thoiGianHoanThanh() != null ? row.thoiGianHoanThanh().format(DT_FMT) : "Chưa hoàn tất");

        if ("Đã hủy".equalsIgnoreCase(row.trangThai())) {
            pLyDoHuy.setVisible(true);
            txtLyDoHuy.setText(row.lyDoHuy() != null ? row.lyDoHuy() : "Khách hàng không duyệt");
        } else {
            pLyDoHuy.setVisible(false);
        }

        lblTongTien.setText(Fmt.money((long) row.tongTien()) + " đ");

        // Tải danh sách hạng mục
        loadHangMucs();

        // Cập nhật trạng thái các nút bấm theo ngữ cảnh
        updateButtonStates(row.trangThai());
    }

    private void reloadTicket() {
        if (currentTicket == null) return;
        try {
            PhieuSuaChuaRow refreshed = bus.timTheoMa(currentTicket.maPhieuSC());
            if (refreshed != null) {
                displayTicket(refreshed);
            }
        } catch (SQLException ex) {
            Msg.error(this, "Lỗi tải lại phiếu: " + ex.getMessage());
        }
    }

    private void loadHangMucs() {
        if (currentTicket == null) return;
        try {
            currentHangMucs = bus.layDanhSachHangMuc(currentTicket.maPhieuSC());
            itemModel.setRowCount(0);

            for (int i = 0; i < currentHangMucs.size(); i++) {
                HangMucSuaChua hm = currentHangMucs.get(i);
                String loaiStr = "DichVu".equalsIgnoreCase(hm.getLoaiHangMuc()) ? "Dịch vụ" : "Phụ tùng";
                String ma = hm.getMaDichVu() != null ? hm.getMaDichVu() : hm.getMaPhuTung();
                String phatSinhStr = hm.isLaPhatSinh() ? "Phát sinh" : "Ban đầu";

                itemModel.addRow(new Object[]{
                        i + 1,
                        loaiStr,
                        ma,
                        hm.getTenHangMuc(),
                        hm.getSoLuong(),
                        hm.getDonGia(),
                        hm.getMucGiamGia(),
                        hm.getThanhTien(),
                        phatSinhStr,
                        hm.getTrangThai(),
                        hm.getGhiChu() != null ? hm.getGhiChu() : ""
                });
            }
        } catch (SQLException ex) {
            Msg.error(this, "Lỗi tải hạng mục sửa chữa: " + ex.getMessage());
        }
    }

    private void updateButtonStates(String status) {
        btnKhachDuyet.setVisible(false);
        btnBatDauSua.setVisible(false);
        btnBaoHoanThanh.setVisible(false);
        btnThemPhatSinh.setVisible(false);
        btnLapHoaDon.setVisible(false);
        btnHuyPhieu.setVisible(false);

        if (status == null) return;

        switch (status) {
            case "Chờ khách duyệt" -> {
                btnKhachDuyet.setVisible(true);
                btnHuyPhieu.setVisible(true);
            }
            case "Đã xác nhận" -> {
                btnBatDauSua.setVisible(true);
                btnHuyPhieu.setVisible(true);
            }
            case "Đang sửa chữa" -> {
                btnThemPhatSinh.setVisible(true);
                btnBaoHoanThanh.setVisible(true);
            }
            case "Chờ kiểm tra" -> {
                // Đang chờ KCS kiểm tra bên màn hình Kiểm tra chất lượng của Quản lý
            }
            case "Hoàn thành" -> {
                btnLapHoaDon.setVisible(true);
            }
            case "Đã bàn giao", "Đã hủy" -> {
                // Không có nút hành động nào
            }
        }
    }

    private void openThemPhatSinhDialog() {
        if (currentTicket == null) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        ThemPhatSinhDialog dlg = new ThemPhatSinhDialog(owner, bus, currentTicket.maPhieuSC(), this::reloadTicket);
        dlg.setVisible(true);
    }

    private String formatStatusBadge(String status) {
        if (status == null) status = "Chờ khách duyệt";
        String color = switch (status) {
            case "Chờ khách duyệt" -> "#B45309";
            case "Đã xác nhận" -> "#1D4ED8";
            case "Đang sửa chữa" -> "#0284C7";
            case "Chờ kiểm tra" -> "#7C3AED";
            case "Hoàn thành" -> "#059669";
            case "Đã bàn giao" -> "#15803D";
            case "Đã hủy" -> "#DC2626";
            default -> "#475569";
        };
        return "<html><span style='color:" + color + "; font-size:14px; font-weight:bold'>● " + status + "</span></html>";
    }

    private void setupReadonlyField(JTextField tf) {
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private JPanel formFieldWrap(String label, JTextField tf) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(fieldLabel(label), BorderLayout.NORTH);
        p.add(tf, BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // DIALOG THÊM HẠNG MỤC PHÁT SINH (EMBEDDED STATIC INNER CLASS)
    // =========================================================================
    public static final class ThemPhatSinhDialog extends JDialog {
        private final PhieuSuaChuaBus bus;
        private final String maPhieuSC;
        private final Runnable onSuccessCallback;

        private final JComboBox<String> cboLoai = new JComboBox<>(new String[]{"Dịch vụ", "Phụ tùng"});
        private final JTextField txtTen = new JTextField();
        private final JTextField txtSoLuong = new JTextField("1");
        private final JTextField txtDonGia = new JTextField();
        private final JTextField txtGhiChu = new JTextField();

        public ThemPhatSinhDialog(Window owner, PhieuSuaChuaBus bus, String maPhieuSC, Runnable onSuccessCallback) {
            super(owner, "Thêm hạng mục công việc / vật tư phát sinh", ModalityType.APPLICATION_MODAL);
            this.bus = bus;
            this.maPhieuSC = maPhieuSC;
            this.onSuccessCallback = onSuccessCallback;

            setSize(480, 360);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 16));
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            buildUI();
        }

        private void buildUI() {
            JPanel form = new JPanel(new GridLayout(5, 2, 12, 12));
            form.setOpaque(false);

            form.add(fieldLabel("Loại hạng mục:"));
            form.add(cboLoai);
            form.add(fieldLabel("Tên hạng mục phát sinh *:"));
            form.add(txtTen);
            form.add(fieldLabel("Số lượng *:"));
            form.add(txtSoLuong);
            form.add(fieldLabel("Đơn giá dự kiến (đ) *:"));
            form.add(txtDonGia);
            form.add(fieldLabel("Ghi chú lý do phát sinh:"));
            form.add(txtGhiChu);

            add(form, BorderLayout.CENTER);

            JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bot.setOpaque(false);
            JButton btnSave = UI.primaryButton("Thêm phát sinh");
            JButton btnCancel = UI.secondaryButton("Hủy");
            bot.add(btnSave);
            bot.add(btnCancel);
            add(bot, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> dispose());
            btnSave.addActionListener(e -> {
                String ten = txtTen.getText().trim();
                if (ten.isBlank()) {
                    Msg.error(this, "Vui lòng nhập tên hạng mục phát sinh.");
                    return;
                }
                int sl;
                double gia;
                try {
                    sl = Integer.parseInt(txtSoLuong.getText().trim());
                    if (sl <= 0) throw new Exception();
                } catch (Exception ex) {
                    Msg.error(this, "Số lượng phải là số nguyên > 0.");
                    return;
                }
                try {
                    gia = Double.parseDouble(txtDonGia.getText().trim().replace(".", "").replace(",", ""));
                    if (gia < 0) throw new Exception();
                } catch (Exception ex) {
                    Msg.error(this, "Đơn giá không hợp lệ.");
                    return;
                }

                String loai = cboLoai.getSelectedIndex() == 0 ? "DichVu" : "PhuTung";
                HangMucSuaChua hm = new HangMucSuaChua();
                hm.setLoaiHangMuc(loai);
                hm.setTenHangMuc(ten);
                hm.setSoLuong(sl);
                hm.setDonGia(gia);
                hm.setMucGiamGia(0);
                hm.setGhiChu(txtGhiChu.getText().trim());

                try {
                    bus.themHangMucPhatSinh(maPhieuSC, hm);
                    Msg.info(this, "Đã thêm hạng mục phát sinh thành công (ở trạng thái Chờ duyệt).");
                    if (onSuccessCallback != null) onSuccessCallback.run();
                    dispose();
                } catch (Exception ex) {
                    Msg.error(this, "Lỗi thêm hạng mục: " + ex.getMessage());
                }
            });
        }

        private JLabel fieldLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(Theme.font(Font.BOLD, 12));
            l.setForeground(Theme.TEXT);
            return l;
        }
    }
}

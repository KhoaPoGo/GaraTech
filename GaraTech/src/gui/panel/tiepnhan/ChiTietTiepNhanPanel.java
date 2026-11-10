package gui.panel.tiepnhan;

import bus.PhieuTiepNhanSuaChuaBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.DichVuDao;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.PhieuTiepNhanSuaChuaDao.PhieuTiepNhanSuaChuaRow;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.DichVu;
import entity.PhieuTiepNhanSuaChua;
import enums.TrangThaiPhieu;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Chi tiết phiếu tiếp nhận - sửa chữa.
 * Quản lý quy trình 4 trạng thái: CHO_SUA -> DANG_SUA <-> CHO_LINH_KIEN -> HOAN_TAT.
 * Khóa hoàn toàn khi phiếu HOAN_TAT để chống gian lận.
 */
public final class ChiTietTiepNhanPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private final Runnable onBackCallback;
    private final PhieuTiepNhanSuaChuaBus phieuBus = new PhieuTiepNhanSuaChuaBus();
    private final PhieuTiepNhanSuaChuaDao phieuDao = new PhieuTiepNhanSuaChuaDao();

    private PhieuTiepNhanSuaChuaRow currentTicketRow = null;
    private PhieuTiepNhanSuaChua currentTicketEntity = null;

    private final JLabel lblHeaderTitle = UI.h2("Thông tin phiếu tiếp nhận - sửa chữa");
    private final JLabel lblStatusBadge = new JLabel();

    // Fields
    private final JTextField txtKhachHang = new JTextField();
    private final JTextField txtPhuongTien = new JTextField();
    private final JTextField txtThoiGianTiepNhan = new JTextField();
    private final JTextField txtThoiGianHenTra = new JTextField();
    private final JTextField txtOdoNhienLieu = new JTextField();
    private final JTextField txtNhanVienTiepNhan = new JTextField();
    private final JTextField txtNhanVienKyThuat = new JTextField();

    // Table Đối chiếu Hiện trạng & Yêu cầu
    private final DefaultTableModel compareModel = UI.readOnlyModel("Nội dung", "Thông tin ghi nhận");
    private final JTable compareTable = UI.table(compareModel);

    // Dịch vụ
    private final JComboBox<String> cboDichVu = new JComboBox<>();
    private final JSpinner spnSoLuongDV = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JTextField txtGhiChuDV = new JTextField();
    private final JButton btnThemDV = UI.primaryButton("Thêm dịch vụ");
    private final JButton btnBoDV = UI.dangerButton("Bỏ dịch vụ");
    private final DefaultTableModel modelDichVu = new DefaultTableModel(
            new String[]{"STT", "Mã DV", "Tên dịch vụ", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDichVu = UI.table(modelDichVu);
    private final JLabel lblTongTienDichVu = UI.label("Tổng tiền dịch vụ: 0 đ", Font.BOLD, 13, Theme.NAVY);

    // Linh kiện / Phụ tùng
    private final DefaultTableModel modelLinhKien = new DefaultTableModel(
            new String[]{"STT", "Mã phụ tùng", "Tên phụ tùng", "Đơn vị tính", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblLinhKien = UI.table(modelLinhKien);
    private final JLabel lblTongTienLinhKien = UI.label("Tổng tiền linh kiện: 0 đ", Font.BOLD, 13, Theme.NAVY);
    private final JLabel lblTongChiPhiPhieu = UI.label("TỔNG CHI PHÍ: 0 đ", Font.BOLD, 16, new Color(0x027A48));

    // Nút điều khiển quy trình trạng thái
    private final JButton btnBatDauSua = UI.primaryButton("Bắt đầu sửa chữa");
    private final JButton btnChoLinhKien = UI.secondaryButton("Chờ linh kiện");
    private final JButton btnTiepTucSua = UI.primaryButton("Tiếp tục sửa chữa");
    private final JButton btnHoanTat = UI.primaryButton("Hoàn tất sửa chữa");
    private final JButton btnBack = UI.secondaryButton("Quay lại danh sách");
    private final JLabel lblLockNotice = UI.label("🔒 Phiếu đã hoàn tất - Bị khóa toàn diện chống gian lận.", Font.ITALIC, 13, new Color(0x027A48));

    private final List<DichVu> listAvailableServices = new ArrayList<>();

    public ChiTietTiepNhanPanel(Runnable onBackCallback) {
        this.onBackCallback = onBackCallback;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        loadAvailableServices();
        initEvents();
    }

    private void buildUI() {
        // 1. Card Thông tin tiếp nhận
        RoundedPanel cardInfo = new RoundedPanel(16, Theme.CARD);
        cardInfo.setLayout(new BorderLayout(0, 16));
        cardInfo.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(lblHeaderTitle, BorderLayout.WEST);
        headerPanel.add(lblStatusBadge, BorderLayout.EAST);
        cardInfo.add(headerPanel, BorderLayout.NORTH);

        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 24, 12));
        infoGrid.setOpaque(false);

        infoGrid.add(buildReadonlyField("Khách hàng", txtKhachHang));
        infoGrid.add(buildReadonlyField("Phương tiện", txtPhuongTien));
        infoGrid.add(buildReadonlyField("Thời gian tiếp nhận", txtThoiGianTiepNhan));
        infoGrid.add(buildReadonlyField("Hẹn trả xe", txtThoiGianHenTra));
        infoGrid.add(buildReadonlyField("ODO / Mức nhiên liệu", txtOdoNhienLieu));
        infoGrid.add(buildReadonlyField("Nhân viên tiếp nhận", txtNhanVienTiepNhan));
        infoGrid.add(buildReadonlyField("Kỹ thuật viên phụ trách", txtNhanVienKyThuat));

        cardInfo.add(infoGrid, BorderLayout.CENTER);
        cardInfo.setAlignmentX(LEFT_ALIGNMENT);
        add(cardInfo);

        add(Box.createVerticalStrut(16));

        // 2. Card Hiện trạng và yêu cầu
        RoundedPanel cardReq = new RoundedPanel(16, Theme.CARD);
        cardReq.setLayout(new BorderLayout(0, 14));
        cardReq.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        cardReq.add(UI.h2("Hiện trạng xe & Yêu cầu của khách hàng"), BorderLayout.NORTH);

        compareTable.setRowHeight(40);
        compareTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        compareTable.getColumnModel().getColumn(1).setPreferredWidth(600);

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < 2; i++) {
            compareTable.getColumnModel().getColumn(i).setCellRenderer(r);
        }

        JScrollPane scrollReq = new JScrollPane(compareTable);
        scrollReq.setPreferredSize(new Dimension(800, 125));
        scrollReq.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardReq.add(scrollReq, BorderLayout.CENTER);

        cardReq.setAlignmentX(LEFT_ALIGNMENT);
        add(cardReq);

        add(Box.createVerticalStrut(16));

        // 3. Card Dịch vụ thi công
        RoundedPanel cardServices = new RoundedPanel(16, Theme.CARD);
        cardServices.setLayout(new BorderLayout(0, 12));
        cardServices.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel pServiceTop = new JPanel(new BorderLayout(12, 0));
        pServiceTop.setOpaque(false);
        pServiceTop.add(UI.h2("Dịch vụ thi công"), BorderLayout.WEST);

        // Thanh thêm dịch vụ
        JPanel pAddBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pAddBar.setOpaque(false);

        cboDichVu.setPreferredSize(new Dimension(280, 36));
        cboDichVu.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        spnSoLuongDV.setPreferredSize(new Dimension(65, 36));
        spnSoLuongDV.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        txtGhiChuDV.putClientProperty("JTextField.placeholderText", "Ghi chú dịch vụ...");
        txtGhiChuDV.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        txtGhiChuDV.setPreferredSize(new Dimension(170, 36));

        btnThemDV.setPreferredSize(new Dimension(130, 36));
        btnBoDV.setPreferredSize(new Dimension(110, 36));

        pAddBar.add(cboDichVu);
        pAddBar.add(spnSoLuongDV);
        pAddBar.add(txtGhiChuDV);
        pAddBar.add(btnThemDV);
        pAddBar.add(btnBoDV);
        pServiceTop.add(pAddBar, BorderLayout.EAST);

        cardServices.add(pServiceTop, BorderLayout.NORTH);

        tblDichVu.setRowHeight(38);
        setupTableRenderers(tblDichVu, 7);

        JScrollPane scrollDv = new JScrollPane(tblDichVu);
        scrollDv.setPreferredSize(new Dimension(800, 140));
        scrollDv.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardServices.add(scrollDv, BorderLayout.CENTER);

        JPanel pDvFoot = new JPanel(new BorderLayout());
        pDvFoot.setOpaque(false);
        pDvFoot.add(lblTongTienDichVu, BorderLayout.EAST);
        cardServices.add(pDvFoot, BorderLayout.SOUTH);

        cardServices.setAlignmentX(LEFT_ALIGNMENT);
        add(cardServices);

        add(Box.createVerticalStrut(16));

        // 4. Card Linh kiện / Phụ tùng sử dụng
        RoundedPanel cardParts = new RoundedPanel(16, Theme.CARD);
        cardParts.setLayout(new BorderLayout(0, 12));
        cardParts.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        cardParts.add(UI.h2("Linh kiện & Phụ tùng sử dụng"), BorderLayout.NORTH);

        tblLinhKien.setRowHeight(38);
        setupTableRenderers(tblLinhKien, 8);

        JScrollPane scrollLk = new JScrollPane(tblLinhKien);
        scrollLk.setPreferredSize(new Dimension(800, 130));
        scrollLk.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardParts.add(scrollLk, BorderLayout.CENTER);

        JPanel pLkFoot = new JPanel(new BorderLayout());
        pLkFoot.setOpaque(false);
        pLkFoot.add(lblTongTienLinhKien, BorderLayout.EAST);
        cardParts.add(pLkFoot, BorderLayout.SOUTH);

        cardParts.setAlignmentX(LEFT_ALIGNMENT);
        add(cardParts);

        add(Box.createVerticalStrut(16));

        // 5. Card Tổng chi phí & Hành động
        RoundedPanel cardActions = new RoundedPanel(16, Theme.CARD);
        cardActions.setLayout(new BorderLayout(16, 0));
        cardActions.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        cardActions.add(lblTongChiPhiPhieu, BorderLayout.WEST);

        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pButtons.setOpaque(false);

        btnBatDauSua.setPreferredSize(new Dimension(160, 40));
        btnChoLinhKien.setPreferredSize(new Dimension(150, 40));
        btnTiepTucSua.setPreferredSize(new Dimension(160, 40));
        btnHoanTat.setPreferredSize(new Dimension(160, 40));
        btnBack.setPreferredSize(new Dimension(150, 40));

        pButtons.add(lblLockNotice);
        pButtons.add(btnBatDauSua);
        pButtons.add(btnChoLinhKien);
        pButtons.add(btnTiepTucSua);
        pButtons.add(btnHoanTat);
        pButtons.add(btnBack);

        cardActions.add(pButtons, BorderLayout.EAST);
        cardActions.setAlignmentX(LEFT_ALIGNMENT);
        add(cardActions);

        add(Box.createVerticalStrut(20));
    }

    private void setupTableRenderers(JTable tbl, int cols) {
        DefaultTableCellRenderer cellR = new DefaultTableCellRenderer();
        cellR.setHorizontalAlignment(SwingConstants.LEFT);
        cellR.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        for (int i = 0; i < cols; i++) {
            tbl.getColumnModel().getColumn(i).setCellRenderer(cellR);
        }

        TableCellRenderer headerRenderer = tbl.getTableHeader().getDefaultRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer headR) {
            headR.setHorizontalAlignment(SwingConstants.LEFT);
            headR.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }
    }

    private JPanel buildReadonlyField(String label, JTextField tf) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));

        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setFont(Theme.font(Font.PLAIN, 13));
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#E2E8F0");
        tf.setPreferredSize(new Dimension(300, 36));
        tf.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        p.add(tf);

        return p;
    }

    private void loadAvailableServices() {
        cboDichVu.removeAllItems();
        cboDichVu.addItem("-- Chọn dịch vụ thêm vào phiếu --");
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

    public void displayTicket(PhieuTiepNhanSuaChuaRow row) {
        this.currentTicketRow = row;
        if (row == null) return;

        try {
            this.currentTicketEntity = phieuDao.findById(row.maPhieu());
        } catch (Exception e) {
            e.printStackTrace();
        }

        lblHeaderTitle.setText("Phiếu Tiếp nhận - Sửa chữa: " + row.maPhieu());
        updateStatusBadge(row.trangThai());

        txtKhachHang.setText(String.format("%s (Mã KH: %s, SĐT: %s)",
                row.tenKH() != null ? row.tenKH() : "---",
                row.maKH() != null ? row.maKH() : "---",
                row.sdtKH() != null ? row.sdtKH() : "---"));

        txtPhuongTien.setText(String.format("%s · %s",
                row.bienSo(), row.thongTinXe() != null ? row.thongTinXe() : "---"));

        txtThoiGianTiepNhan.setText(row.ngayGioTiepNhan() != null ? row.ngayGioTiepNhan().format(DT_FMT) : "---");

        txtThoiGianHenTra.setText(currentTicketEntity != null && currentTicketEntity.getThoiGianHenTra() != null
                ? currentTicketEntity.getThoiGianHenTra().format(DT_FMT)
                : "Không hẹn giờ trả xe");

        txtOdoNhienLieu.setText(String.format("%,.0f km · Nhiên liệu: %.0f%%",
                row.soKmTiepNhan(), row.mucNhienLieu() * 100));

        txtNhanVienTiepNhan.setText(row.tenNVTiepNhan() != null
                ? String.format("%s (%s)", row.tenNVTiepNhan(), row.maNVTiepNhan())
                : row.maNVTiepNhan());

        txtNhanVienKyThuat.setText(row.tenNVKyThuat() != null
                ? String.format("%s (%s)", row.tenNVKyThuat(), row.maNVKyThuat())
                : "Chưa phân công");

        // Bảng đối chiếu
        compareModel.setRowCount(0);
        compareModel.addRow(new Object[]{"Yêu cầu ban đầu của khách", row.yeuCauBanDau() != null ? row.yeuCauBanDau() : "---"});
        compareModel.addRow(new Object[]{"Hiện trạng xe lúc tiếp nhận", row.tinhTrangXe() != null ? row.tinhTrangXe() : "---"});

        // Nạp bảng dịch vụ và linh kiện
        loadTicketDetails();

        // Cập nhật hiển thị nút hành động theo 4 trạng thái
        updateWorkflowButtons(row.trangThai());
    }

    private void updateStatusBadge(String status) {
        TrangThaiPhieu tt = TrangThaiPhieu.fromDb(status);
        String color = switch (tt) {
            case CHO_SUA -> "#B45309";
            case DANG_SUA -> "#175CD3";
            case CHO_LINH_KIEN -> "#7A5AF8";
            case HOAN_TAT -> "#027A48";
        };
        lblStatusBadge.setText("<html><span style='color:" + color + "; font-size:13px; font-weight:bold; " +
                "border:1px solid " + color + "; padding:4px 10px; border-radius:6px;'>" + tt.label().toUpperCase() + "</span></html>");
    }

    private void loadTicketDetails() {
        if (currentTicketEntity == null) return;

        // 1. Dịch vụ
        modelDichVu.setRowCount(0);
        double tongDv = 0;
        int sttDv = 1;
        List<ChiTietDichVu> dsDv = currentTicketEntity.getChiTietDichVu();
        if (dsDv != null) {
            for (ChiTietDichVu ct : dsDv) {
                double tt = ct.getThanhTien();
                tongDv += tt;
                modelDichVu.addRow(new Object[]{
                        sttDv++,
                        ct.getDichVu() != null ? ct.getDichVu().getMaDichVu() : "",
                        ct.getDichVu() != null ? ct.getDichVu().getTenDichVu() : "",
                        ct.getSoLuong(),
                        Fmt.money(ct.getDonGiaThucHien()),
                        Fmt.money(tt),
                        ct.getGhiChu() != null ? ct.getGhiChu() : ""
                });
            }
        }
        lblTongTienDichVu.setText("Tổng tiền dịch vụ: " + Fmt.money(tongDv));

        // 2. Linh kiện
        modelLinhKien.setRowCount(0);
        double tongLk = 0;
        int sttLk = 1;
        List<ChiTietLinhKien> dsLk = currentTicketEntity.getChiTietLinhKien();
        if (dsLk != null) {
            for (ChiTietLinhKien ct : dsLk) {
                double tt = ct.getThanhTien();
                tongLk += tt;
                modelLinhKien.addRow(new Object[]{
                        sttLk++,
                        ct.getPhuTung() != null ? ct.getPhuTung().getMaPhuTung() : "",
                        ct.getPhuTung() != null ? ct.getPhuTung().getTenPhuTung() : "",
                        ct.getPhuTung() != null ? ct.getPhuTung().getDonViTinh() : "",
                        ct.getSoLuong(),
                        Fmt.money(ct.getDonGiaXuat()),
                        Fmt.money(tt),
                        ct.getGhiChu() != null ? ct.getGhiChu() : ""
                });
            }
        }
        lblTongTienLinhKien.setText("Tổng tiền linh kiện: " + Fmt.money(tongLk));

        // 3. Tổng chi phí
        double tongChiPhi = tongDv + tongLk;
        lblTongChiPhiPhieu.setText("TỔNG CHI PHÍ: " + Fmt.money(tongChiPhi));
    }

    private void updateWorkflowButtons(String status) {
        TrangThaiPhieu tt = TrangThaiPhieu.fromDb(status);

        btnBatDauSua.setVisible(false);
        btnChoLinhKien.setVisible(false);
        btnTiepTucSua.setVisible(false);
        btnHoanTat.setVisible(false);
        lblLockNotice.setVisible(false);

        boolean isLocked = tt.isLocked();
        btnThemDV.setEnabled(!isLocked);
        btnBoDV.setEnabled(!isLocked);
        cboDichVu.setEnabled(!isLocked);
        spnSoLuongDV.setEnabled(!isLocked);
        txtGhiChuDV.setEnabled(!isLocked);

        switch (tt) {
            case CHO_SUA -> {
                btnBatDauSua.setVisible(true);
            }
            case DANG_SUA -> {
                btnChoLinhKien.setVisible(true);
                btnHoanTat.setVisible(true);
            }
            case CHO_LINH_KIEN -> {
                btnTiepTucSua.setVisible(true);
            }
            case HOAN_TAT -> {
                lblLockNotice.setVisible(true);
            }
        }
    }

    private void reloadCurrentTicket() {
        if (currentTicketRow == null) return;
        try {
            PhieuTiepNhanSuaChua p = phieuDao.findById(currentTicketRow.maPhieu());
            if (p != null) {
                List<PhieuTiepNhanSuaChuaRow> rows = phieuDao.layDanhSachChoTiepNhanVien(
                        p.getMaPhieu(), null, null, null
                );
                if (!rows.isEmpty()) {
                    displayTicket(rows.get(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleThemDichVu() {
        if (currentTicketRow == null) return;
        int idx = cboDichVu.getSelectedIndex();
        if (idx <= 0 || idx - 1 >= listAvailableServices.size()) {
            Msg.error(this, "Vui lòng chọn dịch vụ từ danh mục.");
            return;
        }

        DichVu dv = listAvailableServices.get(idx - 1);
        int sl = (int) spnSoLuongDV.getValue();
        String ghichu = txtGhiChuDV.getText().trim();

        try {
            phieuBus.themDichVu(currentTicketRow.maPhieu(), dv.getMaDichVu(), sl, ghichu.isEmpty() ? null : ghichu);
            Msg.info(this, "Đã thêm dịch vụ '" + dv.getTenDichVu() + "' vào phiếu.");
            txtGhiChuDV.setText("");
            spnSoLuongDV.setValue(1);
            reloadCurrentTicket();
        } catch (Exception ex) {
            Msg.error(this, ex.getMessage());
        }
    }

    private void handleBoDichVu() {
        if (currentTicketRow == null) return;
        int row = tblDichVu.getSelectedRow();
        if (row < 0 || currentTicketEntity == null || currentTicketEntity.getChiTietDichVu() == null) {
            Msg.error(this, "Vui lòng chọn dòng dịch vụ cần bỏ.");
            return;
        }

        ChiTietDichVu ct = currentTicketEntity.getChiTietDichVu().get(row);
        if (!Msg.confirm(this, "Bạn có chắc chắn muốn bỏ dịch vụ '" + ct.getDichVu().getTenDichVu() + "'?")) {
            return;
        }

        try {
            phieuBus.xoaDichVu(currentTicketRow.maPhieu(), ct.getDichVu().getMaDichVu());
            Msg.info(this, "Đã bỏ dịch vụ khỏi phiếu.");
            reloadCurrentTicket();
        } catch (Exception ex) {
            Msg.error(this, ex.getMessage());
        }
    }

    // =========================================================================
    // ĐEM TOÀN BỘ CÁC LUỒNG GỌI XỬ LÝ SỰ KIỆN XUỐNG CUỐI CÙNG (QUY TẮC BẮT BUỘC)
    // =========================================================================
    private void initEvents() {
        btnThemDV.addActionListener(e -> handleThemDichVu());
        btnBoDV.addActionListener(e -> handleBoDichVu());

        btnBack.addActionListener(e -> {
            if (onBackCallback != null) onBackCallback.run();
        });

        // 1. Chờ sửa chữa -> Bắt đầu sửa chữa (DANG_SUA)
        btnBatDauSua.addActionListener(e -> {
            if (currentTicketRow == null) return;
            if (!Msg.confirm(this, "Xác nhận chuyển trạng thái sang 'Đang sửa chữa' cho xe " + currentTicketRow.bienSo() + "?")) {
                return;
            }
            try {
                phieuBus.batDauSuaChua(currentTicketRow.maPhieu(), currentTicketRow.maNVKyThuat());
                Msg.info(this, "Đã cập nhật trạng thái sang 'Đang sửa chữa'.");
                reloadCurrentTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 2. Đang sửa chữa -> Chờ linh kiện (CHO_LINH_KIEN)
        btnChoLinhKien.addActionListener(e -> {
            if (currentTicketRow == null) return;
            if (!Msg.confirm(this, "Tạm dừng và chuyển phiếu sang trạng thái 'Chờ linh kiện'?")) {
                return;
            }
            try {
                phieuBus.choLinhKien(currentTicketRow.maPhieu());
                Msg.info(this, "Đã cập nhật trạng thái sang 'Chờ linh kiện'.");
                reloadCurrentTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 3. Chờ linh kiện -> Tiếp tục sửa chữa (DANG_SUA)
        btnTiepTucSua.addActionListener(e -> {
            if (currentTicketRow == null) return;
            if (!Msg.confirm(this, "Linh kiện đã sẵn sàng, tiếp tục công việc sửa chữa xe " + currentTicketRow.bienSo() + "?")) {
                return;
            }
            try {
                phieuBus.batDauSuaChua(currentTicketRow.maPhieu(), currentTicketRow.maNVKyThuat());
                Msg.info(this, "Đã cập nhật trạng thái sang 'Đang sửa chữa'.");
                reloadCurrentTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 4. Đang sửa chữa -> Hoàn tất sửa chữa (HOAN_TAT - Khóa toàn diện)
        btnHoanTat.addActionListener(e -> {
            if (currentTicketRow == null) return;
            if (!Msg.confirm(this, "Bạn có chắc chắn xe đã sửa chữa hoàn tất và nghiệm thu đạt chuẩn?\n" +
                    "LƯU Ý: Sau khi hoàn tất, phiếu sẽ bị KHÓA hoàn toàn để chống gian lận!")) {
                return;
            }
            try {
                phieuBus.hoanTatSuaChua(currentTicketRow.maPhieu());
                Msg.info(this, "Chúc mừng! Phiếu sửa chữa đã Hoàn tất. Hệ thống đã khóa an toàn.");
                reloadCurrentTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });
    }
}

package gui.panel.tiepnhan;

import bus.PhieuTiepNhanSuaChuaBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.PhieuTiepNhanSuaChuaDao.PhieuTiepNhanSuaChuaRow;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.KhachHang;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Hộp thoại Xem chi tiết phiếu tiếp nhận – sửa chữa (Modal Dialog).
 */
public final class ChiTietPhieuModalDialog extends JDialog {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private final String maPhieu;
    private final Runnable onDataChangedCallback;
    private final PhieuTiepNhanSuaChuaDao phieuDao = new PhieuTiepNhanSuaChuaDao();
    private final PhieuTiepNhanSuaChuaBus phieuBus = new PhieuTiepNhanSuaChuaBus();

    private PhieuTiepNhanSuaChua currentEntity;
    private PhieuTiepNhanSuaChuaRow currentRow;

    // Header
    private final JLabel lblHeaderTitle = UI.h1("Chi tiết phiếu tiếp nhận – sửa chữa");
    private final JLabel lblStatusBadge = new JLabel();

    // Fields Card 1
    private final JTextField txtKhachHang = new JTextField();
    private final JTextField txtPhuongTien = new JTextField();
    private final JTextField txtThoiGianTiepNhan = new JTextField();
    private final JTextField txtThoiGianHenTra = new JTextField();
    private final JTextField txtOdoNhienLieu = new JTextField();
    private final JTextField txtNhanVienTiepNhan = new JTextField();
    private final JTextField txtNhanVienKyThuat = new JTextField();

    // Card 2: Bảng đối chiếu
    private final DefaultTableModel compareModel = UI.readOnlyModel("Nội dung", "Thông tin ghi nhận");
    private final JTable compareTable = UI.table(compareModel);

    // Card 3: Dịch vụ (chỉ hiển thị)
    private final DefaultTableModel modelDichVu = new DefaultTableModel(
            new String[]{"STT", "Mã DV", "Tên dịch vụ", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDichVu = UI.table(modelDichVu);
    private final JLabel lblTongTienDichVu = UI.label("Tổng tiền dịch vụ: 0 đ", Font.BOLD, 13, Theme.NAVY);

    // Card 4: Linh kiện
    private final DefaultTableModel modelLinhKien = new DefaultTableModel(
            new String[]{"STT", "Mã phụ tùng", "Tên phụ tùng", "Đơn vị tính", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblLinhKien = UI.table(modelLinhKien);
    private final JLabel lblTongTienLinhKien = UI.label("Tổng tiền linh kiện: 0 đ", Font.BOLD, 13, Theme.NAVY);
    private final JLabel lblTongChiPhiPhieu = UI.label("TỔNG CHI PHÍ: 0 đ", Font.BOLD, 15, new Color(0x027A48));

    // Footer actions
    private final JButton btnSuaPhieu = UI.secondaryButton("Sửa phiếu này");
    private final JButton btnBatDauSua = UI.primaryButton("Bắt đầu sửa chữa");
    private final JButton btnChoLinhKien = UI.secondaryButton("Chờ linh kiện");
    private final JButton btnTiepTucSua = UI.primaryButton("Tiếp tục sửa chữa");
    private final JButton btnHoanTat = UI.primaryButton("Hoàn tất sửa chữa");
    private final JButton btnDong = UI.primaryButton("Đóng");
    private final JLabel lblLockNotice = UI.label("Phiếu đã hoàn tất - Bị khóa toàn diện chống gian lận.", Font.ITALIC, 13, new Color(0x027A48));

    public ChiTietPhieuModalDialog(Window parent, String maPhieu, Runnable onDataChangedCallback) {
        super(parent, "Chi tiết phiếu tiếp nhận – " + maPhieu, ModalityType.APPLICATION_MODAL);
        this.maPhieu = maPhieu;
        this.onDataChangedCallback = onDataChangedCallback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = ge.getMaximumWindowBounds();
        int dialogW = Math.min(1150, maxBounds.width - 40);
        int dialogH = Math.min(620, maxBounds.height - 40);
        setPreferredSize(new Dimension(dialogW, dialogH));
        setLayout(new BorderLayout());

        buildUI();
        loadTicketData();
        initEvents();

        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        // Header
        JPanel pHead = new JPanel(new BorderLayout(12, 0));
        pHead.setOpaque(false);
        pHead.add(lblHeaderTitle, BorderLayout.WEST);

        JPanel pHeadRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pHeadRight.setOpaque(false);
        pHeadRight.add(lblStatusBadge);
        pHead.add(pHeadRight, BorderLayout.EAST);
        root.add(pHead, BorderLayout.NORTH);

        // Body ScrollPane
        JPanel pBody = new JPanel();
        pBody.setOpaque(false);
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));

        pBody.add(buildCardGeneralInfo());
        pBody.add(Box.createVerticalStrut(10));
        pBody.add(buildCardCompareInfo());
        pBody.add(Box.createVerticalStrut(10));
        pBody.add(buildCardServices());
        pBody.add(Box.createVerticalStrut(10));
        pBody.add(buildCardParts());

        JScrollPane scroll = new JScrollPane(pBody);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        // Footer Actions
        JPanel pFoot = new JPanel(new BorderLayout(12, 0));
        pFoot.setOpaque(false);
        pFoot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));

        JPanel pFootLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pFootLeft.setOpaque(false);
        pFootLeft.add(btnSuaPhieu);
        pFootLeft.add(lblLockNotice);
        pFoot.add(pFootLeft, BorderLayout.WEST);

        JPanel pFootRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pFootRight.setOpaque(false);
        pFootRight.add(btnBatDauSua);
        pFootRight.add(btnChoLinhKien);
        pFootRight.add(btnTiepTucSua);
        pFootRight.add(btnHoanTat);
        btnDong.setPreferredSize(new Dimension(90, 36));
        pFootRight.add(btnDong);
        pFoot.add(pFootRight, BorderLayout.EAST);

        root.add(pFoot, BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);
    }

    private JPanel buildCardGeneralInfo() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Thông tin chung phiếu tiếp nhận"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 8));
        grid.setOpaque(false);

        grid.add(buildReadonlyField("Khách hàng", txtKhachHang));
        grid.add(buildReadonlyField("Phương tiện", txtPhuongTien));
        grid.add(buildReadonlyField("Thời gian tiếp nhận", txtThoiGianTiepNhan));
        grid.add(buildReadonlyField("Hạn trả xe dự kiến", txtThoiGianHenTra));
        grid.add(buildReadonlyField("ODO / Mức nhiên liệu", txtOdoNhienLieu));
        grid.add(buildReadonlyField("Nhân viên tiếp nhận", txtNhanVienTiepNhan));
        grid.add(buildReadonlyField("Kỹ thuật viên chính phụ trách", txtNhanVienKyThuat));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardCompareInfo() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Hiện trạng xe & Yêu cầu của khách hàng"), BorderLayout.NORTH);

        compareTable.setRowHeight(36);
        compareTable.getColumnModel().getColumn(0).setPreferredWidth(210);
        compareTable.getColumnModel().getColumn(1).setPreferredWidth(750);

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        compareTable.getColumnModel().getColumn(0).setCellRenderer(r);
        compareTable.getColumnModel().getColumn(1).setCellRenderer(r);

        JScrollPane sp = new JScrollPane(compareTable);
        sp.setPreferredSize(new Dimension(0, 95));
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildCardServices() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel pHead = new JPanel(new BorderLayout());
        pHead.setOpaque(false);
        pHead.add(UI.h2("Dịch vụ thi công"), BorderLayout.WEST);
        card.add(pHead, BorderLayout.NORTH);

        // Table
        tblDichVu.setRowHeight(30);
        tblDichVu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblDichVu.getColumnModel().getColumn(1).setPreferredWidth(85);
        tblDichVu.getColumnModel().getColumn(2).setPreferredWidth(260);
        tblDichVu.getColumnModel().getColumn(3).setPreferredWidth(65);
        tblDichVu.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblDichVu.getColumnModel().getColumn(5).setPreferredWidth(110);
        tblDichVu.getColumnModel().getColumn(6).setPreferredWidth(160);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblDichVu.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        tblDichVu.getColumnModel().getColumn(3).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDichVu.getColumnModel().getColumn(4).setCellRenderer(rightRender);
        tblDichVu.getColumnModel().getColumn(5).setCellRenderer(rightRender);

        JScrollPane sp = new JScrollPane(tblDichVu);
        sp.setPreferredSize(new Dimension(0, 110));
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(sp, BorderLayout.CENTER);

        JPanel pFoot = new JPanel(new BorderLayout());
        pFoot.setOpaque(false);
        pFoot.add(lblTongTienDichVu, BorderLayout.EAST);
        card.add(pFoot, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildCardParts() {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(UI.h2("Linh kiện / Phụ tùng xuất kho"), BorderLayout.NORTH);

        tblLinhKien.setRowHeight(30);
        tblLinhKien.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblLinhKien.getColumnModel().getColumn(1).setPreferredWidth(95);
        tblLinhKien.getColumnModel().getColumn(2).setPreferredWidth(230);
        tblLinhKien.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblLinhKien.getColumnModel().getColumn(4).setPreferredWidth(65);
        tblLinhKien.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblLinhKien.getColumnModel().getColumn(6).setPreferredWidth(110);
        tblLinhKien.getColumnModel().getColumn(7).setPreferredWidth(160);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblLinhKien.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        tblLinhKien.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        tblLinhKien.getColumnModel().getColumn(5).setCellRenderer(rightRender);
        tblLinhKien.getColumnModel().getColumn(6).setCellRenderer(rightRender);

        JScrollPane sp = new JScrollPane(tblLinhKien);
        sp.setPreferredSize(new Dimension(0, 110));
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.add(sp, BorderLayout.CENTER);

        JPanel pFoot = new JPanel(new BorderLayout(10, 0));
        pFoot.setOpaque(false);
        pFoot.add(lblTongTienLinhKien, BorderLayout.WEST);
        pFoot.add(lblTongChiPhiPhieu, BorderLayout.EAST);
        card.add(pFoot, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildReadonlyField(String label, JTextField tf) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = UI.label(label, Font.BOLD, 12, Theme.MUTED);
        p.add(l, BorderLayout.NORTH);

        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setFont(Theme.font(Font.PLAIN, 13));
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:8; margin:3,8,3,8; borderColor:#E2E8F0");
        tf.setPreferredSize(new Dimension(0, 34));
        p.add(tf, BorderLayout.CENTER);

        return p;
    }

    public void loadTicketData() {
        try {
            currentEntity = phieuDao.findById(maPhieu);
            List<PhieuTiepNhanSuaChuaRow> rows = phieuDao.layDanhSachChoTiepNhanVien(maPhieu, null, null, null);
            if (!rows.isEmpty()) {
                currentRow = rows.get(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (currentEntity == null) {
            Msg.error(this, "Không tìm thấy dữ liệu phiếu tiếp nhận: " + maPhieu);
            return;
        }

        lblHeaderTitle.setText("Chi tiết phiếu tiếp nhận – " + maPhieu);
        updateStatusBadge(currentEntity.getTrangThai());

        Xe xe = currentEntity.getXe();
        KhachHang kh = xe != null ? xe.getKhachHang() : null;

        txtKhachHang.setText(String.format("%s (Mã KH: %s, SĐT: %s)",
                kh != null ? kh.getHoTen() : "---",
                kh != null ? kh.getMaKH() : "---",
                kh != null ? kh.getSoDienThoai() : "---"));

        txtPhuongTien.setText(String.format("%s · %s %s",
                xe != null ? xe.getBienSo() : "---",
                xe != null && xe.getHangXe() != null ? xe.getHangXe() : "",
                xe != null && xe.getModel() != null ? xe.getModel() : ""));

        txtThoiGianTiepNhan.setText(currentEntity.getNgayGioTiepNhan() != null
                ? currentEntity.getNgayGioTiepNhan().format(DT_FMT) : "---");

        txtThoiGianHenTra.setText(currentEntity.getThoiGianHenTra() != null
                ? currentEntity.getThoiGianHenTra().format(DT_FMT) : "Không hẹn giờ trả xe");

        txtOdoNhienLieu.setText(String.format("%,.0f km · Nhiên liệu: %.0f%%",
                currentEntity.getSoKmTiepNhan(), currentEntity.getMucNhienLieu() * 100));

        txtNhanVienTiepNhan.setText(currentEntity.getNhanVienTiepNhan() != null
                ? String.format("%s (%s)", currentEntity.getNhanVienTiepNhan().getHoTen(), currentEntity.getNhanVienTiepNhan().getMaNV())
                : "---");

        txtNhanVienKyThuat.setText(currentEntity.getNhanVienKyThuat() != null
                ? String.format("%s (%s)", currentEntity.getNhanVienKyThuat().getHoTen(), currentEntity.getNhanVienKyThuat().getMaNV())
                : "Chưa phân công KTV");

        // Bảng đối chiếu
        compareModel.setRowCount(0);
        compareModel.addRow(new Object[]{"Yêu cầu ban đầu của khách", currentEntity.getYeuCauBanDau() != null ? currentEntity.getYeuCauBanDau() : "---"});
        compareModel.addRow(new Object[]{"Hiện trạng xe lúc tiếp nhận", currentEntity.getTinhTrangXe() != null ? currentEntity.getTinhTrangXe() : "---"});
        if (currentEntity.getGhiChuKyThuat() != null && !currentEntity.getGhiChuKyThuat().isBlank()) {
            compareModel.addRow(new Object[]{"Ghi chú kỹ thuật bổ sung", currentEntity.getGhiChuKyThuat()});
        }

        // Bảng dịch vụ
        modelDichVu.setRowCount(0);
        double tongDv = 0;
        int sttDv = 1;
        List<ChiTietDichVu> dsDv = currentEntity.getChiTietDichVu();
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

        // Bảng linh kiện
        modelLinhKien.setRowCount(0);
        double tongLk = 0;
        int sttLk = 1;
        List<ChiTietLinhKien> dsLk = currentEntity.getChiTietLinhKien();
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
        lblTongChiPhiPhieu.setText("TỔNG CHI PHÍ: " + Fmt.money(tongDv + tongLk));

        updateWorkflowButtons(currentEntity.getTrangThai());
    }

    private void updateStatusBadge(TrangThaiPhieu tt) {
        String text;
        String color;
        String bg;
        switch (tt) {
            case CHO_SUA -> {
                text = "Chờ sửa chữa";
                color = "#92400E";
                bg = "#FEF3C7";
            }
            case DANG_SUA -> {
                text = "Đang sửa chữa";
                color = "#1E40AF";
                bg = "#DBEAFE";
            }
            case CHO_LINH_KIEN -> {
                text = "Chờ linh kiện";
                color = "#6B21A8";
                bg = "#F3E8FF";
            }
            case HOAN_TAT -> {
                text = "Hoàn tất";
                color = "#065F46";
                bg = "#D1FAE5";
            }
            default -> {
                text = tt.name();
                color = "#475569";
                bg = "#F1F5F9";
            }
        }

        lblStatusBadge.setText("<html><span style='color:" + color + "; background-color:" + bg + "; " +
                "font-size:12px; font-weight:bold; border:1px solid " + color + "; padding:4px 10px; border-radius:6px;'>" +
                text.toUpperCase() + "</span></html>");
    }

    private void updateWorkflowButtons(TrangThaiPhieu tt) {
        btnBatDauSua.setVisible(false);
        btnChoLinhKien.setVisible(false);
        btnTiepTucSua.setVisible(false);
        btnHoanTat.setVisible(false);
        lblLockNotice.setVisible(false);

        boolean isLocked = (tt == TrangThaiPhieu.HOAN_TAT);
        btnSuaPhieu.setEnabled(!isLocked);
        if (isLocked) {
            btnSuaPhieu.setToolTipText("Phiếu đã hoàn tất và bị khóa toàn diện.");
        } else {
            btnSuaPhieu.setToolTipText("Chỉnh sửa thông tin phiếu tiếp nhận");
        }

        switch (tt) {
            case CHO_SUA -> btnBatDauSua.setVisible(true);
            case DANG_SUA -> {
                btnChoLinhKien.setVisible(true);
                btnHoanTat.setVisible(true);
            }
            case CHO_LINH_KIEN -> btnTiepTucSua.setVisible(true);
            case HOAN_TAT -> lblLockNotice.setVisible(true);
        }
    }

    private void openEditDialog() {
        if (currentEntity == null) return;
        if (currentEntity.getTrangThai() == TrangThaiPhieu.HOAN_TAT) {
            Msg.error(this, "Phiếu này đã Hoàn tất và bị khóa toàn diện, không thể chỉnh sửa.");
            return;
        }

        SuaPhieuModalDialog dlg = new SuaPhieuModalDialog(this, currentEntity, () -> {
            loadTicketData();
            if (onDataChangedCallback != null) onDataChangedCallback.run();
        });
        dlg.setVisible(true);
    }

    // =========================================================================
    // QUY TẮC BẮT BUỘC: TOÀN BỘ SỰ KIỆN NẰM TẠI initEvents() Ở CUỐI FILE
    // =========================================================================
    private void initEvents() {
        btnDong.addActionListener(e -> dispose());
        btnSuaPhieu.addActionListener(e -> openEditDialog());

        // 1. Bắt đầu sửa chữa
        btnBatDauSua.addActionListener(e -> {
            if (currentEntity == null) return;
            String ktv = currentEntity.getNhanVienKyThuat() != null ? currentEntity.getNhanVienKyThuat().getMaNV() : null;
            if (ktv == null || ktv.isBlank()) {
                Msg.error(this, "Phiếu chưa có Kỹ thuật viên phụ trách. Vui lòng bấm 'Sửa phiếu này' để phân công KTV trước.");
                return;
            }
            if (!Msg.confirm(this, "Xác nhận chuyển trạng thái sang 'Đang sửa chữa'?")) return;
            try {
                phieuBus.batDauSuaChua(maPhieu, ktv);
                Msg.info(this, "Đã cập nhật trạng thái sang 'Đang sửa chữa'.");
                loadTicketData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 2. Chờ linh kiện
        btnChoLinhKien.addActionListener(e -> {
            if (currentEntity == null) return;
            if (!Msg.confirm(this, "Tạm dừng và chuyển phiếu sang trạng thái 'Chờ linh kiện'?")) return;
            try {
                phieuBus.choLinhKien(maPhieu);
                Msg.info(this, "Đã cập nhật trạng thái sang 'Chờ linh kiện'.");
                loadTicketData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 3. Tiếp tục sửa chữa
        btnTiepTucSua.addActionListener(e -> {
            if (currentEntity == null) return;
            String ktv = currentEntity.getNhanVienKyThuat() != null ? currentEntity.getNhanVienKyThuat().getMaNV() : null;
            if (!Msg.confirm(this, "Linh kiện đã sẵn sàng, tiếp tục sửa chữa?")) return;
            try {
                phieuBus.batDauSuaChua(maPhieu, ktv);
                Msg.info(this, "Đã cập nhật trạng thái sang 'Đang sửa chữa'.");
                loadTicketData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // 4. Hoàn tất sửa chữa
        btnHoanTat.addActionListener(e -> {
            if (currentEntity == null) return;
            if (!Msg.confirm(this, "Bạn có chắc chắn xe đã sửa chữa hoàn tất và nghiệm thu đạt chuẩn?\n" +
                    "LƯU Ý: Sau khi hoàn tất, phiếu sẽ bị KHÓA hoàn toàn để chống gian lận!")) return;
            try {
                phieuBus.hoanTatSuaChua(maPhieu);
                Msg.info(this, "Chúc mừng! Phiếu sửa chữa đã Hoàn tất. Hệ thống đã khóa an toàn.");
                loadTicketData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });
    }
}

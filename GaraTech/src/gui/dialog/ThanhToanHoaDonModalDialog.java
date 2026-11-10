package gui.dialog;

import bus.HoaDonBus;
import com.formdev.flatlaf.FlatClientProperties;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.KhachHang;
import entity.KhuyenMai;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
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
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Hộp thoại Modal Lập hóa đơn và Xác nhận thanh toán thu ngân.
 */
public final class ThanhToanHoaDonModalDialog extends JDialog {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String maPhieu;
    private final Runnable onSuccess;
    private final HoaDonBus hoaDonBus = new HoaDonBus();

    private PhieuTiepNhanSuaChua phieu;
    private List<ChiTietDichVu> dsDv = new ArrayList<>();
    private List<ChiTietLinhKien> dsLk = new ArrayList<>();

    // Các trường hiển thị thông tin
    private final JTextField txtMaHD = taoReadOnlyField("HD-Tự động sinh khi thanh toán");
    private final JTextField txtMaPhieu = taoReadOnlyField("");
    private final JTextField txtTrangThai = taoReadOnlyField("Chờ thanh toán");
    private final JTextField txtKhachHang = taoReadOnlyField("");
    private final JTextField txtSdt = taoReadOnlyField("");
    private final JTextField txtThongTinXe = taoReadOnlyField("");

    // Bảng chi tiết
    private final DefaultTableModel modelChiTiet;
    private final JTable tblChiTiet;

    // Tính toán tiền
    private double tongTienDv = 0;
    private double tongTienLk = 0;
    private final JLabel lblTienDvVal = UI.label("0 ₫", Font.BOLD, 12, Theme.TEXT);
    private final JLabel lblTienLkVal = UI.label("0 ₫", Font.BOLD, 12, Theme.TEXT);
    private final JTextField txtChiPhiKhac = new JTextField("0");
    private final JComboBox<Object> cboKhuyenMai = new JComboBox<>();
    private final JTextField txtGiamGia = new JTextField("0");
    private final JComboBox<String> cboVat = new JComboBox<>(new String[]{"10% (Chuẩn)", "8%", "5%", "0% (Miễn thuế)"});
    private final JLabel lblTienVatVal = UI.label("0 ₫", Font.BOLD, 12, Theme.TEXT);
    private final JLabel lblTongThanhToanVal = UI.label("0 ₫", Font.BOLD, 18, Theme.NAVY);

    // Hình thức thu tiền: Chỉ 2 phương thức Tiền mặt & Chuyển khoản
    private final JComboBox<String> cboPhuongThuc = new JComboBox<>(new String[]{
            "Tiền mặt", "Chuyển khoản"
    });
    private final JTextField txtMaGiaoDich = new JTextField();
    private JPanel pnlMaGd;

    // Nút hành động
    private final JLabel lblCanhBao = UI.label("Kiểm tra kỹ các khoản tiền và phương thức trước khi xác nhận thu tiền.", Font.ITALIC, 11, Theme.MUTED);
    private final JButton btnHuy = UI.secondaryButton("Hủy bỏ");
    private final JButton btnXacNhan = UI.primaryButton("Xác nhận thanh toán");

    private boolean thanhToanThanhCong = false;
    private String maHoaDonTaoMoi = null;

    public ThanhToanHoaDonModalDialog(Window parent, String maPhieu, Runnable onSuccess) {
        super(parent, "Lập hóa đơn & thanh toán – " + maPhieu, ModalityType.APPLICATION_MODAL);
        this.maPhieu = maPhieu;
        this.onSuccess = onSuccess;

        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        // Khởi tạo bảng chi tiết
        String[] cols = {"STT", "Hạng mục / Phụ tùng", "Phân loại", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"};
        modelChiTiet = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChiTiet = UI.table(modelChiTiet);

        buildUI();
        initEvents();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();
        int w = Math.min(960, bounds.width - 40);
        int h = Math.min(840, bounds.height - 40);
        setSize(w, h);
        setLocationRelativeTo(parent);

        taiDuLieu();
    }

    private void buildUI() {
        // ── 1. Header ────────────────────────────────────────────────────────
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(Color.WHITE);
        pHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JPanel pLeftHead = new JPanel();
        pLeftHead.setLayout(new BoxLayout(pLeftHead, BoxLayout.Y_AXIS));
        pLeftHead.setOpaque(false);
        pLeftHead.add(UI.eyebrow("THU NGÂN & XUẤT HÓA ĐƠN"));
        pLeftHead.add(Box.createVerticalStrut(4));
        pLeftHead.add(UI.h1("Lập hóa đơn và thanh toán – " + maPhieu));
        pHeader.add(pLeftHead, BorderLayout.WEST);

        add(pHeader, BorderLayout.NORTH);

        // ── 2. Body Scrollable ───────────────────────────────────────────────
        JPanel pBody = new JPanel();
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));
        pBody.setOpaque(false);
        pBody.setBorder(BorderFactory.createEmptyBorder(16, 24, 20, 24));

        // Khối 1: Thông tin chung
        RoundedPanel card1 = new RoundedPanel(12, Color.WHITE);
        card1.setLayout(new BorderLayout(0, 8));
        card1.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card1.setAlignmentX(0.0f);

        card1.add(UI.h2("Thông tin phiếu & Khách hàng"), BorderLayout.NORTH);

        JPanel pRowFields = new JPanel(new GridLayout(2, 3, 14, 10));
        pRowFields.setOpaque(false);
        pRowFields.add(wrapField("Mã hóa đơn tự sinh", txtMaHD));
        pRowFields.add(wrapField("Mã phiếu sửa chữa", txtMaPhieu));
        pRowFields.add(wrapField("Trạng thái", txtTrangThai));
        pRowFields.add(wrapField("Khách hàng", txtKhachHang));
        pRowFields.add(wrapField("Số điện thoại", txtSdt));
        pRowFields.add(wrapField("Biển số xe & mẫu xe", txtThongTinXe));
        card1.add(pRowFields, BorderLayout.CENTER);

        pBody.add(card1);
        pBody.add(Box.createVerticalStrut(14));

        // Khối 2: Bảng chi tiết dịch vụ & linh kiện
        RoundedPanel card2 = new RoundedPanel(12, Color.WHITE);
        card2.setLayout(new BorderLayout(0, 8));
        card2.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card2.setAlignmentX(0.0f);

        card2.add(UI.h2("Chi tiết dịch vụ đã thực hiện & phụ tùng thay thế"), BorderLayout.NORTH);

        tblChiTiet.setShowGrid(true);
        tblChiTiet.setGridColor(new Color(0xF1, 0xF5, 0xF9));

        DefaultTableCellRenderer rCenter = new DefaultTableCellRenderer();
        rCenter.setHorizontalAlignment(SwingConstants.CENTER);
        tblChiTiet.getColumnModel().getColumn(0).setCellRenderer(rCenter);
        tblChiTiet.getColumnModel().getColumn(2).setCellRenderer(rCenter);
        tblChiTiet.getColumnModel().getColumn(3).setCellRenderer(rCenter);
        tblChiTiet.getColumnModel().getColumn(4).setCellRenderer(rCenter);

        DefaultTableCellRenderer rRight = new DefaultTableCellRenderer();
        rRight.setHorizontalAlignment(SwingConstants.RIGHT);
        tblChiTiet.getColumnModel().getColumn(5).setCellRenderer(rRight);
        tblChiTiet.getColumnModel().getColumn(6).setCellRenderer(rRight);

        tblChiTiet.getColumnModel().getColumn(0).setPreferredWidth(35);
        tblChiTiet.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblChiTiet.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblChiTiet.getColumnModel().getColumn(3).setPreferredWidth(50);
        tblChiTiet.getColumnModel().getColumn(4).setPreferredWidth(50);
        tblChiTiet.getColumnModel().getColumn(5).setPreferredWidth(110);
        tblChiTiet.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane spTbl = new JScrollPane(tblChiTiet);
        spTbl.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        spTbl.setPreferredSize(new Dimension(860, 160));
        card2.add(spTbl, BorderLayout.CENTER);

        pBody.add(card2);
        pBody.add(Box.createVerticalStrut(14));

        // Khối 3: Tính toán tiền & Khuyến mãi, Thuế
        RoundedPanel card3 = new RoundedPanel(12, Color.WHITE);
        card3.setLayout(new BorderLayout(24, 12));
        card3.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card3.setAlignmentX(0.0f);

        card3.add(UI.h2("Tính toán tổng tiền thanh toán & khuyến mãi"), BorderLayout.NORTH);

        // Bên trái: Chọn chương trình khuyến mãi (căn thẳng hàng với lề trái)
        JPanel pLeftCalc = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pLeftCalc.setOpaque(false);

        JPanel pPromoWrap = new JPanel(new BorderLayout(0, 6));
        pPromoWrap.setOpaque(false);

        JLabel lblPromoTitle = UI.label("Chương trình khuyến mãi áp dụng:", Font.BOLD, 12, Theme.TEXT);
        pPromoWrap.add(lblPromoTitle, BorderLayout.NORTH);

        cboKhuyenMai.setPreferredSize(new Dimension(380, 36));
        pPromoWrap.add(cboKhuyenMai, BorderLayout.CENTER);

        JLabel lblPromoDesc = UI.label("Hệ thống sẽ tự động tính toán số tiền giảm giá và trừ vào hóa đơn.", Font.ITALIC, 11, Theme.MUTED);
        pPromoWrap.add(lblPromoDesc, BorderLayout.SOUTH);

        pLeftCalc.add(pPromoWrap);
        card3.add(pLeftCalc, BorderLayout.CENTER);

        // Bên phải: Các dòng số liệu
        JPanel pCalc = new JPanel(new GridLayout(6, 2, 12, 6));
        pCalc.setOpaque(false);
        pCalc.setPreferredSize(new Dimension(380, 180));

        pCalc.add(UI.label("Tổng tiền dịch vụ:", Font.PLAIN, 12, Theme.MUTED));
        lblTienDvVal.setHorizontalAlignment(SwingConstants.RIGHT);
        pCalc.add(lblTienDvVal);

        pCalc.add(UI.label("Tổng tiền phụ tùng:", Font.PLAIN, 12, Theme.MUTED));
        lblTienLkVal.setHorizontalAlignment(SwingConstants.RIGHT);
        pCalc.add(lblTienLkVal);

        pCalc.add(UI.label("Chi phí khác (₫):", Font.PLAIN, 12, Theme.MUTED));
        txtChiPhiKhac.setHorizontalAlignment(SwingConstants.RIGHT);
        pCalc.add(txtChiPhiKhac);

        pCalc.add(UI.label("Giảm giá (₫):", Font.PLAIN, 12, Theme.SUCCESS));
        txtGiamGia.setHorizontalAlignment(SwingConstants.RIGHT);
        pCalc.add(txtGiamGia);

        pCalc.add(UI.label("Thuế VAT:", Font.PLAIN, 12, Theme.MUTED));
        pCalc.add(cboVat);

        pCalc.add(UI.label("TỔNG THANH TOÁN:", Font.BOLD, 13, Theme.NAVY));
        lblTongThanhToanVal.setHorizontalAlignment(SwingConstants.RIGHT);
        pCalc.add(lblTongThanhToanVal);

        card3.add(pCalc, BorderLayout.EAST);
        pBody.add(card3);
        pBody.add(Box.createVerticalStrut(14));

        // Khối 4: Hình thức thu ngân (Chỉ Tiền mặt và Chuyển khoản)
        RoundedPanel card4 = new RoundedPanel(12, Color.WHITE);
        card4.setLayout(new BorderLayout(0, 10));
        card4.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card4.setAlignmentX(0.0f);

        card4.add(UI.h2("Hình thức thu ngân"), BorderLayout.NORTH);

        JPanel pMethod = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        pMethod.setOpaque(false);

        pMethod.add(UI.label("Phương thức thanh toán:", Font.BOLD, 12, Theme.TEXT));
        cboPhuongThuc.setPreferredSize(new Dimension(160, 34));
        pMethod.add(cboPhuongThuc);

        // Khung mã giao dịch (khi chuyển khoản)
        pnlMaGd = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlMaGd.setOpaque(false);
        pnlMaGd.add(UI.label("Mã giao dịch ngân hàng *:", Font.PLAIN, 12, Theme.TEXT));
        txtMaGiaoDich.setPreferredSize(new Dimension(240, 34));
        txtMaGiaoDich.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "VD: VCB9834123, MBB771234...");
        pnlMaGd.add(txtMaGiaoDich);
        pnlMaGd.setVisible(false);
        pMethod.add(pnlMaGd);

        card4.add(pMethod, BorderLayout.CENTER);
        pBody.add(card4);

        JScrollPane scroll = new JScrollPane(pBody);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // ── 3. Footer ────────────────────────────────────────────────────────
        JPanel pFooter = new JPanel(new BorderLayout(10, 0));
        pFooter.setBackground(Color.WHITE);
        pFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        pFooter.add(lblCanhBao, BorderLayout.WEST);

        JPanel pBtnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pBtnGroup.setOpaque(false);
        btnHuy.setPreferredSize(new Dimension(90, 36));
        btnXacNhan.setPreferredSize(new Dimension(180, 36));
        pBtnGroup.add(btnHuy);
        pBtnGroup.add(btnXacNhan);
        pFooter.add(pBtnGroup, BorderLayout.EAST);

        add(pFooter, BorderLayout.SOUTH);
    }

    private JPanel wrapField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(UI.label(label, Font.PLAIN, 11, Theme.MUTED), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JTextField taoReadOnlyField(String val) {
        JTextField tf = new JTextField(val);
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8, 0xFA, 0xFC));
        tf.setForeground(Theme.TEXT);
        tf.setFont(Theme.font(Font.PLAIN, 12));
        tf.setPreferredSize(new Dimension(140, 34));
        return tf;
    }

    private void taiDuLieu() {
        new SwingWorker<Void, Void>() {
            private List<KhuyenMai> dsKmHieuLuc = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                phieu = hoaDonBus.layPhieuTheoMa(maPhieu);
                dsDv = hoaDonBus.layChiTietDichVu(maPhieu);
                dsLk = hoaDonBus.layChiTietLinhKien(maPhieu);
                dsKmHieuLuc = hoaDonBus.layDanhSachKhuyenMaiHieuLuc();
                return null;
            }

            @Override
            protected void done() {
                // Nạp danh sách khuyến mãi vào ComboBox
                cboKhuyenMai.removeAllItems();
                cboKhuyenMai.addItem("-- Không áp dụng khuyến mãi --");
                if (dsKmHieuLuc != null) {
                    for (KhuyenMai km : dsKmHieuLuc) {
                        cboKhuyenMai.addItem(km);
                    }
                }

                if (phieu != null) {
                    txtMaPhieu.setText(phieu.getMaPhieu());
                    Xe x = phieu.getXe();
                    KhachHang kh = x != null ? x.getKhachHang() : null;
                    txtKhachHang.setText(kh != null ? kh.getHoTen() : "--");
                    txtSdt.setText(kh != null ? kh.getSoDienThoai() : "--");
                    String desc = x != null ? x.getBienSo() + " (" + (x.getHangXe() != null ? x.getHangXe() + " " + x.getModel() : "") + ")" : "--";
                    txtThongTinXe.setText(desc);
                }

                modelChiTiet.setRowCount(0);
                tongTienDv = 0;
                tongTienLk = 0;
                int stt = 1;

                for (ChiTietDichVu dv : dsDv) {
                    double tt = dv.getSoLuong() * dv.getDonGiaThucHien();
                    tongTienDv += tt;
                    modelChiTiet.addRow(new Object[]{
                            stt++,
                            dv.getDichVu().getTenDichVu(),
                            "Dịch vụ",
                            "Lần",
                            dv.getSoLuong(),
                            Fmt.money(dv.getDonGiaThucHien()),
                            Fmt.money(tt)
                    });
                }

                for (ChiTietLinhKien lk : dsLk) {
                    double tt = lk.getSoLuong() * lk.getDonGiaXuat();
                    tongTienLk += tt;
                    modelChiTiet.addRow(new Object[]{
                            stt++,
                            lk.getPhuTung().getTenPhuTung(),
                            "Phụ tùng",
                            lk.getPhuTung().getDonViTinh() != null ? lk.getPhuTung().getDonViTinh() : "Cái",
                            lk.getSoLuong(),
                            Fmt.money(lk.getDonGiaXuat()),
                            Fmt.money(tt)
                    });
                }

                lblTienDvVal.setText(Fmt.money(tongTienDv));
                lblTienLkVal.setText(Fmt.money(tongTienLk));

                tinhToanTongTien();
            }
        }.execute();
    }

    private void apDungKhuyenMai() {
        Object sel = cboKhuyenMai.getSelectedItem();
        if (sel instanceof KhuyenMai km) {
            double giam = 0;
            if (km.apDungTatCa()) {
                giam = (tongTienDv + tongTienLk) * (km.getMucGiamPhanTram() / 100.0);
            } else {
                for (ChiTietDichVu dv : dsDv) {
                    String maDv = dv.getDichVu() != null ? dv.getDichVu().getMaDichVu() : null;
                    if (maDv != null && (km.getDsDichVuApDung().isEmpty() || km.getDsDichVuApDung().contains(maDv))) {
                        giam += (dv.getSoLuong() * dv.getDonGiaThucHien()) * (km.getMucGiamPhanTram() / 100.0);
                    }
                }
                for (ChiTietLinhKien lk : dsLk) {
                    String maLk = lk.getPhuTung() != null ? lk.getPhuTung().getMaPhuTung() : null;
                    if (maLk != null && (km.getDsPhuTungApDung().isEmpty() || km.getDsPhuTungApDung().contains(maLk))) {
                        giam += (lk.getSoLuong() * lk.getDonGiaXuat()) * (km.getMucGiamPhanTram() / 100.0);
                    }
                }
            }
            txtGiamGia.setText(String.valueOf(Math.round(giam)));
        } else {
            txtGiamGia.setText("0");
        }
        tinhToanTongTien();
    }

    private void tinhToanTongTien() {
        double cpKhac = 0;
        try {
            cpKhac = Double.parseDouble(txtChiPhiKhac.getText().trim().replace(".", "").replace(",", ""));
            if (cpKhac < 0) cpKhac = 0;
        } catch (Exception ignored) {}

        double giamGia = 0;
        try {
            giamGia = Double.parseDouble(txtGiamGia.getText().trim().replace(".", "").replace(",", ""));
            if (giamGia < 0) giamGia = 0;
        } catch (Exception ignored) {}

        double vatRate = switch (cboVat.getSelectedIndex()) {
            case 1 -> 8.0;
            case 2 -> 5.0;
            case 3 -> 0.0;
            default -> 10.0;
        };

        double truocThue = tongTienDv + tongTienLk + cpKhac - giamGia;
        if (truocThue < 0) truocThue = 0;
        double tienVat = Math.round(truocThue * (vatRate / 100.0));
        double tongThanhToan = truocThue + tienVat;

        lblTienVatVal.setText(Fmt.money(tienVat));
        lblTongThanhToanVal.setText(Fmt.money(tongThanhToan));
    }

    private void xacNhanThanhToan() {
        String pt = (String) cboPhuongThuc.getSelectedItem();
        boolean isCk = "Chuyển khoản".equalsIgnoreCase(pt);
        String maGd = txtMaGiaoDich.getText().trim();

        if (isCk && maGd.isEmpty()) {
            lblCanhBao.setText("Phương thức Chuyển khoản bắt buộc phải nhập mã giao dịch ngân hàng.");
            lblCanhBao.setForeground(Theme.DANGER);
            txtMaGiaoDich.requestFocusInWindow();
            return;
        }

        String maKM = null;
        if (cboKhuyenMai.getSelectedItem() instanceof KhuyenMai km) {
            maKM = km.getMaKM();
        }

        double cpKhac = 0;
        try {
            cpKhac = Double.parseDouble(txtChiPhiKhac.getText().trim().replace(".", "").replace(",", ""));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Chi phí khác phải là số hợp lệ.", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double giamGia = 0;
        try {
            giamGia = Double.parseDouble(txtGiamGia.getText().trim().replace(".", "").replace(",", ""));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Giảm giá phải là số hợp lệ.", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double vatRate = switch (cboVat.getSelectedIndex()) {
            case 1 -> 8.0;
            case 2 -> 5.0;
            case 3 -> 0.0;
            default -> 10.0;
        };

        final String finalMaKM = maKM;
        final double finalCpKhac = cpKhac;
        final double finalGiamGia = giamGia;
        final double finalVat = vatRate;
        final String finalPt = pt;
        final String finalMaGd = maGd;

        btnXacNhan.setEnabled(false);
        btnXacNhan.setText("Đang xử lý...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return hoaDonBus.lapHoaDonVaThanhToan(
                        maPhieu, null, finalMaKM, finalCpKhac, finalGiamGia, finalVat, finalPt, finalMaGd
                );
            }

            @Override
            protected void done() {
                btnXacNhan.setEnabled(true);
                btnXacNhan.setText("Xác nhận thanh toán");
                try {
                    String maHD = get();
                    thanhToanThanhCong = true;
                    maHoaDonTaoMoi = maHD;

                    if (onSuccess != null) onSuccess.run();

                    int opt = JOptionPane.showConfirmDialog(
                            ThanhToanHoaDonModalDialog.this,
                            "Đã thanh toán thành công hóa đơn " + maHD + ".\nBạn có muốn xem trước và in hóa đơn ngay không?",
                            "Thanh toán thành công",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    dispose();

                    if (opt == JOptionPane.YES_OPTION) {
                        InHoaDonDialog dlg = new InHoaDonDialog(getOwner(), maHD);
                        dlg.setVisible(true);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblCanhBao.setText("Lỗi: " + cause.getMessage());
                    lblCanhBao.setForeground(Theme.DANGER);
                    JOptionPane.showMessageDialog(
                            ThanhToanHoaDonModalDialog.this,
                            cause.getMessage(),
                            "Lỗi thanh toán",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN Ở CUỐI CÙNG THEO QUY TẮC BẮT BUỘC
    // =========================================================================
    private void initEvents() {
        btnHuy.addActionListener(e -> dispose());
        btnXacNhan.addActionListener(e -> xacNhanThanhToan());

        cboKhuyenMai.addActionListener(e -> apDungKhuyenMai());

        cboPhuongThuc.addActionListener(e -> {
            boolean isCk = cboPhuongThuc.getSelectedIndex() == 1;
            pnlMaGd.setVisible(isCk);
            if (isCk) {
                txtMaGiaoDich.requestFocusInWindow();
            }
            revalidate();
            repaint();
        });

        cboVat.addActionListener(e -> tinhToanTongTien());

        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { tinhToanTongTien(); }
            @Override public void removeUpdate(DocumentEvent e) { tinhToanTongTien(); }
            @Override public void changedUpdate(DocumentEvent e) { tinhToanTongTien(); }
        };
        txtChiPhiKhac.getDocument().addDocumentListener(docListener);
        txtGiamGia.getDocument().addDocumentListener(docListener);
    }
}

package gui.dialog;

import bus.HoaDonBus;
import entity.ChiTietDichVu;
import entity.ChiTietHoaDon;
import entity.ChiTietLinhKien;
import entity.HoaDon;
import entity.KhachHang;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Hộp thoại xem trước và in hóa đơn thanh toán dịch vụ.
 */
public final class InHoaDonDialog extends JDialog implements Printable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String maHoaDon;
    private final HoaDonBus hoaDonBus = new HoaDonBus();
    private JPanel printPanel;

    private final JButton btnIn = UI.primaryButton("In hóa đơn");
    private final JButton btnDong = UI.secondaryButton("Đóng");

    public InHoaDonDialog(Window parent, String maHoaDon) {
        super(parent, "Xem trước hóa đơn – " + maHoaDon, ModalityType.APPLICATION_MODAL);
        this.maHoaDon = maHoaDon;

        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0xE2, 0xE8, 0xF0));

        buildUI();
        initEvents();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();
        int w = Math.min(840, bounds.width - 60);
        int h = Math.min(900, bounds.height - 60);
        setSize(w, h);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        // Thanh công cụ trên
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));

        JLabel lblTitle = UI.label("Xem trước hóa đơn thanh toán", Font.BOLD, 15, Theme.TEXT);
        toolbar.add(lblTitle, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnGroup.setOpaque(false);
        btnIn.setPreferredSize(new Dimension(130, 36));
        btnDong.setPreferredSize(new Dimension(90, 36));
        btnGroup.add(btnIn);
        btnGroup.add(btnDong);
        toolbar.add(btnGroup, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);

        // Vùng chứa hóa đơn (tờ giấy in màu trắng ở giữa nền xám)
        JPanel pnlWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        pnlWrapper.setOpaque(false);

        printPanel = buildInvoiceSheet();
        pnlWrapper.add(printPanel);

        JScrollPane scroll = new JScrollPane(pnlWrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildInvoiceSheet() {
        HoaDon hd = hoaDonBus.timHoaDonTheoMa(maHoaDon);

        JPanel sheet = new JPanel();
        sheet.setLayout(new BoxLayout(sheet, BoxLayout.Y_AXIS));
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(30, 36, 36, 36)
        ));
        sheet.setPreferredSize(new Dimension(740, 920));
        sheet.setMaximumSize(new Dimension(740, 1200));

        if (hd == null) {
            sheet.add(UI.label("Không tìm thấy thông tin hóa đơn " + maHoaDon, Font.BOLD, 14, Theme.DANGER));
            return sheet;
        }

        PhieuTiepNhanSuaChua phieu = hd.getPhieuSuaChua();
        Xe xe = phieu != null ? phieu.getXe() : null;
        KhachHang kh = xe != null ? xe.getKhachHang() : null;

        // 1. Header gara
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setOpaque(false);

        JPanel pLeftHead = new JPanel();
        pLeftHead.setLayout(new BoxLayout(pLeftHead, BoxLayout.Y_AXIS));
        pLeftHead.setOpaque(false);

        JLabel lblBrand = UI.label("GARATECH - HỆ THỐNG GARA Ô TÔ", Font.BOLD, 16, Theme.NAVY);
        JLabel lblAddr = UI.label("Địa chỉ: 12 Nguyễn Văn Bảo, Phường 4, Q. Gò Vấp, TP. HCM", Font.PLAIN, 12, Theme.MUTED);
        JLabel lblHotline = UI.label("Hotline: 1900 6868 · Website: www.garatech.vn", Font.PLAIN, 12, Theme.MUTED);

        pLeftHead.add(lblBrand);
        pLeftHead.add(Box.createVerticalStrut(3));
        pLeftHead.add(lblAddr);
        pLeftHead.add(Box.createVerticalStrut(2));
        pLeftHead.add(lblHotline);
        pHeader.add(pLeftHead, BorderLayout.WEST);

        JPanel pRightHead = new JPanel();
        pRightHead.setLayout(new BoxLayout(pRightHead, BoxLayout.Y_AXIS));
        pRightHead.setOpaque(false);

        JLabel lblSoHD = UI.label("Số HĐ: " + hd.getMaHoaDon(), Font.BOLD, 13, Theme.TEXT);
        JLabel lblNgay = UI.label("Ngày: " + (hd.getNgayLap() != null ? hd.getNgayLap().format(TIME_FMT) : "--"), Font.PLAIN, 12, Theme.MUTED);
        JLabel lblThuNgan = UI.label("Thu ngân: " + (hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Trần Thị Bình"), Font.PLAIN, 12, Theme.MUTED);

        pRightHead.add(lblSoHD);
        pRightHead.add(Box.createVerticalStrut(3));
        pRightHead.add(lblNgay);
        pRightHead.add(Box.createVerticalStrut(2));
        pRightHead.add(lblThuNgan);
        pHeader.add(pRightHead, BorderLayout.EAST);

        sheet.add(pHeader);
        sheet.add(Box.createVerticalStrut(14));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sheet.add(sep);
        sheet.add(Box.createVerticalStrut(14));

        // 2. Tiêu đề hóa đơn
        JLabel lblTitle = UI.label("HÓA ĐƠN THANH TOÁN DỊCH VỤ", Font.BOLD, 18, Theme.TEXT);
        lblTitle.setAlignmentX(CENTER_ALIGNMENT);
        sheet.add(lblTitle);
        sheet.add(Box.createVerticalStrut(14));

        // 3. Thông tin khách hàng & xe
        JPanel pInfo = new JPanel(new GridLayout(4, 2, 16, 6));
        pInfo.setOpaque(false);
        pInfo.setAlignmentX(CENTER_ALIGNMENT);

        pInfo.add(UI.label("Khách hàng: " + (kh != null ? kh.getHoTen() : "--"), Font.PLAIN, 12, Theme.TEXT));
        pInfo.add(UI.label("Biển số xe: " + (xe != null ? xe.getBienSo() : "--"), Font.BOLD, 12, Theme.TEXT));

        pInfo.add(UI.label("Số điện thoại: " + (kh != null ? kh.getSoDienThoai() : "--"), Font.PLAIN, 12, Theme.TEXT));
        String xeDesc = xe != null && xe.getHangXe() != null ? xe.getHangXe() + " " + xe.getModel() : "--";
        pInfo.add(UI.label("Dòng xe: " + xeDesc, Font.PLAIN, 12, Theme.TEXT));

        pInfo.add(UI.label("Mã phiếu SC: " + (phieu != null ? phieu.getMaPhieu() : "--"), Font.PLAIN, 12, Theme.TEXT));
        pInfo.add(UI.label("KTV phụ trách: " + (phieu != null && phieu.getNhanVienKyThuat() != null ? phieu.getNhanVienKyThuat().getHoTen() : "Lê Minh Cường"), Font.PLAIN, 12, Theme.TEXT));

        String ptDesc = hd.getPhuongThucTT() != null ? hd.getPhuongThucTT().label() : "Tiền mặt";
        if ("Chuyển khoản".equalsIgnoreCase(ptDesc) && hd.getMaGiaoDich() != null && !hd.getMaGiaoDich().isBlank()) {
            ptDesc += " (Mã GD: " + hd.getMaGiaoDich() + ")";
        }
        pInfo.add(UI.label("Thanh toán: " + ptDesc, Font.PLAIN, 12, Theme.TEXT));

        String kmStr = hd.getKhuyenMai() != null ? hd.getKhuyenMai().getTenChuongTrinh() : (hd.getMaKM() != null ? hd.getMaKM() : "Không");
        pInfo.add(UI.label("Khuyến mãi: " + kmStr, Font.PLAIN, 12, Theme.TEXT));

        sheet.add(pInfo);
        sheet.add(Box.createVerticalStrut(16));

        // 4. Bảng chi tiết dịch vụ & linh kiện
        String[] cols = {"STT", "Hạng mục / Phụ tùng", "Loại", "ĐVT", "SL", "Đơn giá", "Thành tiền"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        int stt = 1;
        if (hd.getDsChiTiet() != null && !hd.getDsChiTiet().isEmpty()) {
            for (ChiTietHoaDon ct : hd.getDsChiTiet()) {
                String loaiStr = "DichVu".equalsIgnoreCase(ct.getLoaiChiTiet()) ? "Dịch vụ" : "Phụ tùng";
                String dvt = ct.getDonViTinh() != null ? ct.getDonViTinh() : ("DichVu".equalsIgnoreCase(ct.getLoaiChiTiet()) ? "Lần" : "Cái");
                model.addRow(new Object[]{
                        stt++,
                        ct.getTenChiTiet(),
                        loaiStr,
                        dvt,
                        ct.getSoLuong(),
                        Fmt.money(ct.getDonGia()),
                        Fmt.money(ct.getThanhTien())
                });
            }
        } else if (phieu != null) {
            List<ChiTietDichVu> dvs = hoaDonBus.layChiTietDichVu(phieu.getMaPhieu());
            for (ChiTietDichVu dv : dvs) {
                double tt = dv.getSoLuong() * dv.getDonGiaThucHien();
                model.addRow(new Object[]{
                        stt++,
                        dv.getDichVu().getTenDichVu(),
                        "Dịch vụ",
                        "Lần",
                        dv.getSoLuong(),
                        Fmt.money(dv.getDonGiaThucHien()),
                        Fmt.money(tt)
                });
            }
            List<ChiTietLinhKien> lks = hoaDonBus.layChiTietLinhKien(phieu.getMaPhieu());
            for (ChiTietLinhKien lk : lks) {
                double tt = lk.getSoLuong() * lk.getDonGiaXuat();
                model.addRow(new Object[]{
                        stt++,
                        lk.getPhuTung().getTenPhuTung(),
                        "Phụ tùng",
                        lk.getPhuTung().getDonViTinh() != null ? lk.getPhuTung().getDonViTinh() : "Cái",
                        lk.getSoLuong(),
                        Fmt.money(lk.getDonGiaXuat()),
                        Fmt.money(tt)
                });
            }
        }

        JTable tbl = new JTable(model);
        tbl.setFont(Theme.font(Font.PLAIN, 12));
        tbl.setRowHeight(26);
        tbl.setShowGrid(true);
        tbl.setGridColor(new Color(0xE2, 0xE8, 0xF0));
        tbl.getTableHeader().setFont(Theme.font(Font.BOLD, 12));
        tbl.getTableHeader().setBackground(new Color(0xF1, 0xF5, 0xF9));

        // Căn lề
        DefaultTableCellRenderer rCenter = new DefaultTableCellRenderer();
        rCenter.setHorizontalAlignment(SwingConstants.CENTER);
        tbl.getColumnModel().getColumn(0).setCellRenderer(rCenter);
        tbl.getColumnModel().getColumn(2).setCellRenderer(rCenter);
        tbl.getColumnModel().getColumn(3).setCellRenderer(rCenter);
        tbl.getColumnModel().getColumn(4).setCellRenderer(rCenter);

        DefaultTableCellRenderer rRight = new DefaultTableCellRenderer();
        rRight.setHorizontalAlignment(SwingConstants.RIGHT);
        tbl.getColumnModel().getColumn(5).setCellRenderer(rRight);
        tbl.getColumnModel().getColumn(6).setCellRenderer(rRight);

        tbl.getColumnModel().getColumn(0).setPreferredWidth(35);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(230);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(70);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(50);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(45);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(6).setPreferredWidth(110);

        JScrollPane spTbl = new JScrollPane(tbl);
        spTbl.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        spTbl.setPreferredSize(new Dimension(670, 180));
        spTbl.setMaximumSize(new Dimension(670, 240));
        spTbl.setAlignmentX(CENTER_ALIGNMENT);
        sheet.add(spTbl);
        sheet.add(Box.createVerticalStrut(14));

        // 5. Tổng kết chi phí
        JPanel pSummaryWrap = new JPanel(new BorderLayout());
        pSummaryWrap.setOpaque(false);
        pSummaryWrap.setAlignmentX(CENTER_ALIGNMENT);

        JPanel pSum = new JPanel(new GridLayout(6, 2, 10, 4));
        pSum.setOpaque(false);
        pSum.setPreferredSize(new Dimension(320, 140));

        pSum.add(UI.label("Tiền dịch vụ:", Font.PLAIN, 12, Theme.MUTED));
        pSum.add(right(UI.label(Fmt.money(hd.getTongTienDichVu()), Font.PLAIN, 12, Theme.TEXT)));

        pSum.add(UI.label("Tiền phụ tùng:", Font.PLAIN, 12, Theme.MUTED));
        pSum.add(right(UI.label(Fmt.money(hd.getTongTienPhuTung()), Font.PLAIN, 12, Theme.TEXT)));

        if (hd.getChiPhiKhac() > 0) {
            pSum.add(UI.label("Chi phí khác:", Font.PLAIN, 12, Theme.MUTED));
            pSum.add(right(UI.label(Fmt.money(hd.getChiPhiKhac()), Font.PLAIN, 12, Theme.TEXT)));
        }

        if (hd.getGiamGia() > 0) {
            pSum.add(UI.label("Giảm giá:", Font.PLAIN, 12, Theme.SUCCESS));
            pSum.add(right(UI.label("-" + Fmt.money(hd.getGiamGia()), Font.PLAIN, 12, Theme.SUCCESS)));
        }

        pSum.add(UI.label("Thuế VAT (" + (int) hd.getThueVAT() + "%):", Font.PLAIN, 12, Theme.MUTED));
        double thue = Math.round((hd.getTongTienDichVu() + hd.getTongTienPhuTung() + hd.getChiPhiKhac() - hd.getGiamGia()) * (hd.getThueVAT() / 100.0));
        pSum.add(right(UI.label(Fmt.money(thue), Font.PLAIN, 12, Theme.TEXT)));

        pSum.add(UI.label("TỔNG THANH TOÁN:", Font.BOLD, 13, Theme.NAVY));
        pSum.add(right(UI.label(Fmt.money(hd.getTongTienThanhToan()), Font.BOLD, 15, Theme.NAVY)));

        pSummaryWrap.add(pSum, BorderLayout.EAST);
        sheet.add(pSummaryWrap);
        sheet.add(Box.createVerticalStrut(10));

        // 6. Số tiền bằng chữ
        long tongTienLong = Math.round(hd.getTongTienThanhToan());
        JLabel lblBangChu = UI.label("Bằng chữ: " + Fmt.moneyInWords(tongTienLong), Font.ITALIC, 12, Theme.TEXT);
        lblBangChu.setAlignmentX(CENTER_ALIGNMENT);
        sheet.add(lblBangChu);
        sheet.add(Box.createVerticalStrut(24));

        // 7. Chữ ký
        JPanel pSign = new JPanel(new GridLayout(1, 2, 40, 0));
        pSign.setOpaque(false);
        pSign.setAlignmentX(CENTER_ALIGNMENT);

        JPanel pSignLeft = new JPanel();
        pSignLeft.setLayout(new BoxLayout(pSignLeft, BoxLayout.Y_AXIS));
        pSignLeft.setOpaque(false);
        JLabel lblSignKH = UI.label("Khách hàng", Font.BOLD, 12, Theme.TEXT);
        JLabel lblSubKH = UI.label("(Ký và ghi rõ họ tên)", Font.ITALIC, 11, Theme.MUTED);
        lblSignKH.setAlignmentX(CENTER_ALIGNMENT);
        lblSubKH.setAlignmentX(CENTER_ALIGNMENT);
        pSignLeft.add(lblSignKH);
        pSignLeft.add(lblSubKH);
        pSignLeft.add(Box.createVerticalStrut(50));
        JLabel lblTenKH = UI.label(kh != null ? kh.getHoTen() : "", Font.PLAIN, 12, Theme.TEXT);
        lblTenKH.setAlignmentX(CENTER_ALIGNMENT);
        pSignLeft.add(lblTenKH);

        JPanel pSignRight = new JPanel();
        pSignRight.setLayout(new BoxLayout(pSignRight, BoxLayout.Y_AXIS));
        pSignRight.setOpaque(false);
        JLabel lblSignNV = UI.label("Người lập hóa đơn", Font.BOLD, 12, Theme.TEXT);
        JLabel lblSubNV = UI.label("(Ký và ghi rõ họ tên)", Font.ITALIC, 11, Theme.MUTED);
        lblSignNV.setAlignmentX(CENTER_ALIGNMENT);
        lblSubNV.setAlignmentX(CENTER_ALIGNMENT);
        pSignRight.add(lblSignNV);
        pSignRight.add(lblSubNV);
        pSignRight.add(Box.createVerticalStrut(50));
        JLabel lblTenNV = UI.label(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Trần Thị Bình", Font.PLAIN, 12, Theme.TEXT);
        lblTenNV.setAlignmentX(CENTER_ALIGNMENT);
        pSignRight.add(lblTenNV);

        pSign.add(pSignLeft);
        pSign.add(pSignRight);
        sheet.add(pSign);

        return sheet;
    }

    private static JLabel right(JLabel l) {
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        double scaleX = pageFormat.getImageableWidth() / printPanel.getWidth();
        double scaleY = pageFormat.getImageableHeight() / printPanel.getHeight();
        double scale = Math.min(scaleX, scaleY);
        g2d.scale(scale, scale);

        printPanel.printAll(g2d);
        return PAGE_EXISTS;
    }

    private void doPrint() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        boolean ok = job.printDialog();
        if (ok) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Hóa đơn " + maHoaDon + " đã được gửi tới máy in.", "In hóa đơn", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi in: " + ex.getMessage(), "Lỗi máy in", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN Ở CUỐI CÙNG THEO QUY TẮC BẮT BUỘC
    // =========================================================================
    private void initEvents() {
        btnIn.addActionListener(e -> doPrint());
        btnDong.addActionListener(e -> dispose());
    }
}

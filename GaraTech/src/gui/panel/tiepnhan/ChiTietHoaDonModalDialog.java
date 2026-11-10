package gui.panel.tiepnhan;

import bus.HoaDonBus;
import entity.ChiTietDichVu;
import entity.ChiTietHoaDon;
import entity.ChiTietLinhKien;
import entity.GiaoDichThanhToan;
import entity.HoaDon;
import entity.KhachHang;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
 * Hộp thoại Modal xem chi tiết hóa đơn đã thanh toán.
 */
public final class ChiTietHoaDonModalDialog extends JDialog {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String maHoaDon;
    private final HoaDonBus hoaDonBus = new HoaDonBus();

    private final JButton btnIn = UI.primaryButton("In hóa đơn");
    private final JButton btnDong = UI.secondaryButton("Đóng");

    public ChiTietHoaDonModalDialog(Window parent, String maHoaDon) {
        super(parent, "Chi tiết hóa đơn – " + maHoaDon, ModalityType.APPLICATION_MODAL);
        this.maHoaDon = maHoaDon;

        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        buildUI();
        initEvents();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();
        int w = Math.min(940, bounds.width - 60);
        int h = Math.min(800, bounds.height - 60);
        setSize(w, h);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        // Header
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(Color.WHITE);
        pHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JPanel pLeftHead = new JPanel();
        pLeftHead.setLayout(new BoxLayout(pLeftHead, BoxLayout.Y_AXIS));
        pLeftHead.setOpaque(false);
        pLeftHead.add(UI.eyebrow("HÓA ĐƠN & THANH TOÁN"));
        pLeftHead.add(Box.createVerticalStrut(4));
        pLeftHead.add(UI.h1("Chi tiết hóa đơn – " + maHoaDon));
        pHeader.add(pLeftHead, BorderLayout.WEST);

        add(pHeader, BorderLayout.NORTH);

        // Body scrollable
        JPanel pBody = new JPanel();
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));
        pBody.setOpaque(false);
        pBody.setBorder(BorderFactory.createEmptyBorder(16, 24, 20, 24));

        HoaDon hd = hoaDonBus.timHoaDonTheoMa(maHoaDon);
        if (hd == null) {
            pBody.add(UI.label("Không tìm thấy thông tin hóa đơn " + maHoaDon, Font.BOLD, 14, Theme.DANGER));
            add(new JScrollPane(pBody), BorderLayout.CENTER);
            return;
        }

        PhieuTiepNhanSuaChua phieu = hd.getPhieuSuaChua();
        Xe xe = phieu != null ? phieu.getXe() : null;
        KhachHang kh = xe != null ? xe.getKhachHang() : null;

        // ── 1. Thẻ thông tin chung ───────────────────────────────────────────
        RoundedPanel cardInfo = new RoundedPanel(12, Color.WHITE);
        cardInfo.setLayout(new GridLayout(5, 2, 16, 10));
        cardInfo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        cardInfo.setAlignmentX(0.0f);

        cardInfo.add(taoRowInfo("Mã hóa đơn:", hd.getMaHoaDon()));
        cardInfo.add(taoRowInfo("Trạng thái:", hd.getTrangThai() != null ? hd.getTrangThai() : "Đã thanh toán"));

        cardInfo.add(taoRowInfo("Mã phiếu sửa chữa:", phieu != null ? phieu.getMaPhieu() : hd.getMaPhieu()));
        cardInfo.add(taoRowInfo("Thời gian lập / thanh toán:", hd.getNgayLap() != null ? hd.getNgayLap().format(TIME_FMT) : "--"));

        cardInfo.add(taoRowInfo("Khách hàng:", (kh != null ? kh.getHoTen() : "--") + " (" + (kh != null ? kh.getSoDienThoai() : "--") + ")"));
        String xeDesc = xe != null && xe.getHangXe() != null ? xe.getBienSo() + " - " + xe.getHangXe() + " " + xe.getModel() : (xe != null ? xe.getBienSo() : "--");
        cardInfo.add(taoRowInfo("Biển số & xe:", xeDesc));

        String pt = hd.getPhuongThucTT() != null ? hd.getPhuongThucTT() : (!hd.getDsGiaoDich().isEmpty() ? hd.getDsGiaoDich().get(0).getPhuongThuc() : "Tiền mặt");
        cardInfo.add(taoRowInfo("Hình thức thanh toán:", pt));
        cardInfo.add(taoRowInfo("Thu ngân thực hiện:", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Trần Thị Bình"));

        String kmStr = hd.getKhuyenMai() != null ? hd.getKhuyenMai().getTenChuongTrinh() + " (Giảm " + (int) hd.getKhuyenMai().getMucGiamPhanTram() + "%)" : (hd.getMaKM() != null ? hd.getMaKM() : "Không áp dụng");
        cardInfo.add(taoRowInfo("Khuyến mãi áp dụng:", kmStr));
        String maGdStr = (hd.getMaGiaoDich() != null && !hd.getMaGiaoDich().isBlank()) ? hd.getMaGiaoDich() : "--";
        cardInfo.add(taoRowInfo("Mã giao dịch ngân hàng:", maGdStr));

        pBody.add(cardInfo);
        pBody.add(Box.createVerticalStrut(14));

        // ── 2. Bảng chi tiết dịch vụ & linh kiện ──────────────────────────────
        RoundedPanel cardItems = new RoundedPanel(12, Color.WHITE);
        cardItems.setLayout(new BorderLayout(0, 10));
        cardItems.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        cardItems.setAlignmentX(0.0f);

        cardItems.add(UI.h2("Chi tiết dịch vụ đã làm & phụ tùng thay thế"), BorderLayout.NORTH);

        String[] cols = {"STT", "Hạng mục / Phụ tùng", "Phân loại", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

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

        JTable tbl = UI.table(model);
        tbl.setShowGrid(true);
        tbl.setGridColor(new Color(0xF1, 0xF5, 0xF9));

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

        tbl.getColumnModel().getColumn(0).setPreferredWidth(40);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(250);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(80);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(60);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(60);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(110);
        tbl.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane spTbl = new JScrollPane(tbl);
        spTbl.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        spTbl.setPreferredSize(new Dimension(860, 180));
        cardItems.add(spTbl, BorderLayout.CENTER);

        pBody.add(cardItems);
        pBody.add(Box.createVerticalStrut(14));

        // ── 3. Tổng kết chi phí & Bằng chữ ────────────────────────────────────
        RoundedPanel cardMoney = new RoundedPanel(12, Color.WHITE);
        cardMoney.setLayout(new BorderLayout(20, 10));
        cardMoney.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        cardMoney.setAlignmentX(0.0f);

        JPanel pBangChu = new JPanel();
        pBangChu.setLayout(new BoxLayout(pBangChu, BoxLayout.Y_AXIS));
        pBangChu.setOpaque(false);
        pBangChu.add(UI.label("Số tiền viết bằng chữ:", Font.BOLD, 12, Theme.MUTED));
        pBangChu.add(Box.createVerticalStrut(4));
        long tongTienLong = Math.round(hd.getTongTienThanhToan());
        JLabel lblWords = UI.label(Fmt.moneyInWords(tongTienLong), Font.BOLD, 13, Theme.NAVY);
        pBangChu.add(lblWords);
        cardMoney.add(pBangChu, BorderLayout.CENTER);

        JPanel pSum = new JPanel(new GridLayout(6, 2, 10, 4));
        pSum.setOpaque(false);
        pSum.setPreferredSize(new Dimension(340, 130));

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
        pSum.add(right(UI.label(Fmt.money(hd.getTongTienThanhToan()), Font.BOLD, 16, Theme.NAVY)));

        cardMoney.add(pSum, BorderLayout.EAST);
        pBody.add(cardMoney);

        JScrollPane scroll = new JScrollPane(pBody);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel pFooter = new JPanel(new BorderLayout());
        pFooter.setBackground(Color.WHITE);
        pFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        JPanel pFooterBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pFooterBtns.setOpaque(false);
        btnIn.setPreferredSize(new Dimension(130, 36));
        btnDong.setPreferredSize(new Dimension(90, 36));
        pFooterBtns.add(btnIn);
        pFooterBtns.add(btnDong);
        pFooter.add(pFooterBtns, BorderLayout.EAST);

        add(pFooter, BorderLayout.SOUTH);
    }

    private JPanel taoRowInfo(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        JLabel l = UI.label(label, Font.PLAIN, 12, Theme.MUTED);
        l.setPreferredSize(new Dimension(150, 20));
        p.add(l);
        JLabel v = UI.label(value != null ? value : "--", Font.BOLD, 12, Theme.TEXT);
        p.add(v);
        return p;
    }

    private static JLabel right(JLabel l) {
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN Ở CUỐI CÙNG THEO QUY TẮC BẮT BUỘC
    // =========================================================================
    private void initEvents() {
        btnIn.addActionListener(e -> {
            InHoaDonDialog dlg = new InHoaDonDialog(this, maHoaDon);
            dlg.setVisible(true);
        });

        btnDong.addActionListener(e -> dispose());
    }
}

package gui.panel;

import bus.HoaDonBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.HoaDonDao.HoaDonChoThanhToanRow;
import dao.HoaDonDao.HoaDonLichSuRow;
import gui.component.BasePanel;
import gui.component.Theme;
import gui.component.UI;
import gui.dialog.ChiTietHoaDonModalDialog;
import gui.dialog.InHoaDonDialog;
import gui.dialog.ThanhToanHoaDonModalDialog;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Quản lý Hóa đơn và Thanh toán thu ngân.
 */
public final class HoaDonPanel extends BasePanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HoaDonBus hoaDonBus = new HoaDonBus();

    private JTabbedPane tabbedPane;

    // ── TAB 1: XE HOÀN TẤT CHỜ THANH TOÁN ────────────────────────────────────
    private List<HoaDonChoThanhToanRow> dsChoThanhToan = new ArrayList<>();
    private final DefaultTableModel modelChoTT;
    private final JTable tblChoTT;
    private final JTextField txtTimKiemChoTT = new JTextField();
    private final JButton btnTimChoTT = UI.secondaryButton("Tìm kiếm");
    private final JButton btnLamMoiChoTT = UI.secondaryButton("Làm mới");
    private final JButton btnThanhToan = UI.primaryButton("Lập hóa đơn & Thanh toán");
    private final JLabel lblSoLuongChoTT = UI.muted("Đang tải dữ liệu...");

    // ── TAB 2: LỊCH SỬ HÓA ĐƠN ĐÃ THANH TOÁN ─────────────────────────────────
    private List<HoaDonLichSuRow> dsLichSu = new ArrayList<>();
    private final DefaultTableModel modelLichSu;
    private final JTable tblLichSu;
    private final JTextField txtTimKiemLichSu = new JTextField();
    private final JComboBox<String> cboLocPhuongThuc = new JComboBox<>(new String[]{
            "-- Tất cả phương thức --", "Tiền mặt", "Chuyển khoản"
    });
    private final JButton btnTimLichSu = UI.secondaryButton("Tìm kiếm");
    private final JButton btnLamMoiLichSu = UI.secondaryButton("Làm mới");
    private final JButton btnXemChiTietHD = UI.secondaryButton("Xem chi tiết hóa đơn");
    private final JButton btnInHD = UI.primaryButton("In hóa đơn");
    private final JLabel lblSoLuongLichSu = UI.muted("Đang tải dữ liệu...");

    public HoaDonPanel() {
        super("", "Quản lý hóa đơn & thanh toán",
                "");

        // Bảng Tab 1
        String[] colsChoTT = {
                "STT", "Mã phiếu", "Thời gian hoàn thành", "Biển số xe",
                "Hãng & Dòng xe", "Khách hàng", "Số điện thoại", "Kỹ thuật viên chính", "Chi phí tạm tính"
        };
        modelChoTT = new DefaultTableModel(colsChoTT, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChoTT = UI.table(modelChoTT);

        // Bảng Tab 2
        String[] colsLichSu = {
                "STT", "Mã hóa đơn", "Mã phiếu", "Thời gian thanh toán", "Khách hàng", "Biển số xe",
                "Tiền dịch vụ", "Tiền phụ tùng", "Giảm giá", "Thuế VAT", "Tổng thanh toán", "Phương thức"
        };
        modelLichSu = new DefaultTableModel(colsLichSu, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLichSu = UI.table(modelLichSu);

        buildContent();
        initShortcuts();
        initEvents();

        taiDuLieuTab1();
        taiDuLieuTab2();
    }

    @Override
    public void onShow() {
        setHeader("TIẾP NHẬN / THU NGÂN", "Quản lý hóa đơn & thanh toán",
                "Lập hóa đơn thanh toán cho xe đã hoàn tất sửa chữa và quản lý lịch sử thu tiền của gara.");
        taiDuLieuTab1();
        taiDuLieuTab2();
    }

    private void buildContent() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.font(Font.BOLD, 13));

        tabbedPane.addTab("Xe hoàn tất chờ thanh toán", buildTab1ChoThanhToan());
        tabbedPane.addTab("Lịch sử hóa đơn đã thanh toán", buildTab2LichSu());

        body.add(tabbedPane, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 1: XE HOÀN TẤT CHỜ THANH TOÁN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTab1ChoThanhToan() {
        JPanel pnl = new JPanel(new BorderLayout(0, 12));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // Thanh công cụ trên
        RoundedPanel cardToolbar = new RoundedPanel(12, Color.WHITE);
        cardToolbar.setLayout(new BorderLayout(12, 0));
        cardToolbar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel pLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pLeft.setOpaque(false);

        txtTimKiemChoTT.setPreferredSize(new Dimension(320, 36));
        txtTimKiemChoTT.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm mã phiếu, biển số xe, khách hàng, SĐT...");
        pLeft.add(txtTimKiemChoTT);

        btnTimChoTT.setPreferredSize(new Dimension(100, 36));
        pLeft.add(btnTimChoTT);

        btnLamMoiChoTT.setPreferredSize(new Dimension(95, 36));
        pLeft.add(btnLamMoiChoTT);

        cardToolbar.add(pLeft, BorderLayout.WEST);

        JPanel pRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pRight.setOpaque(false);
        btnThanhToan.setPreferredSize(new Dimension(200, 36));
        btnThanhToan.setEnabled(false);
        pRight.add(btnThanhToan);

        cardToolbar.add(pRight, BorderLayout.EAST);
        pnl.add(cardToolbar, BorderLayout.NORTH);

        // Bảng dữ liệu Tab 1
        RoundedPanel cardTable = new RoundedPanel(12, Color.WHITE);
        cardTable.setLayout(new BorderLayout(0, 10));
        cardTable.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel pHeadTbl = new JPanel(new BorderLayout());
        pHeadTbl.setOpaque(false);
        pHeadTbl.add(UI.h2("Danh sách xe đã hoàn tất sửa chữa cần thanh toán"), BorderLayout.WEST);
        pHeadTbl.add(lblSoLuongChoTT, BorderLayout.EAST);
        cardTable.add(pHeadTbl, BorderLayout.NORTH);

        tblChoTT.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblChoTT.setRowHeight(36);
        tblChoTT.setShowGrid(false);

        DefaultTableCellRenderer rCenter = new DefaultTableCellRenderer();
        rCenter.setHorizontalAlignment(SwingConstants.CENTER);
        tblChoTT.getColumnModel().getColumn(0).setCellRenderer(rCenter);
        tblChoTT.getColumnModel().getColumn(1).setCellRenderer(rCenter);
        tblChoTT.getColumnModel().getColumn(2).setCellRenderer(rCenter);
        tblChoTT.getColumnModel().getColumn(3).setCellRenderer(rCenter);

        DefaultTableCellRenderer rRight = new DefaultTableCellRenderer();
        rRight.setHorizontalAlignment(SwingConstants.RIGHT);
        tblChoTT.getColumnModel().getColumn(8).setCellRenderer(rRight);

        tblChoTT.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblChoTT.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblChoTT.getColumnModel().getColumn(2).setPreferredWidth(140);
        tblChoTT.getColumnModel().getColumn(3).setPreferredWidth(110);
        tblChoTT.getColumnModel().getColumn(4).setPreferredWidth(170);
        tblChoTT.getColumnModel().getColumn(5).setPreferredWidth(140);
        tblChoTT.getColumnModel().getColumn(6).setPreferredWidth(110);
        tblChoTT.getColumnModel().getColumn(7).setPreferredWidth(140);
        tblChoTT.getColumnModel().getColumn(8).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(tblChoTT);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardTable.add(scroll, BorderLayout.CENTER);

        pnl.add(cardTable, BorderLayout.CENTER);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 2: LỊCH SỬ HÓA ĐƠN ĐÃ THANH TOÁN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTab2LichSu() {
        JPanel pnl = new JPanel(new BorderLayout(0, 12));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // Thanh công cụ trên
        RoundedPanel cardToolbar = new RoundedPanel(12, Color.WHITE);
        cardToolbar.setLayout(new BorderLayout(12, 0));
        cardToolbar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel pLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pLeft.setOpaque(false);

        txtTimKiemLichSu.setPreferredSize(new Dimension(280, 36));
        txtTimKiemLichSu.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm mã hóa đơn, mã phiếu, biển số, KH...");
        pLeft.add(txtTimKiemLichSu);

        cboLocPhuongThuc.setPreferredSize(new Dimension(180, 36));
        pLeft.add(cboLocPhuongThuc);

        btnTimLichSu.setPreferredSize(new Dimension(100, 36));
        pLeft.add(btnTimLichSu);

        btnLamMoiLichSu.setPreferredSize(new Dimension(95, 36));
        pLeft.add(btnLamMoiLichSu);

        cardToolbar.add(pLeft, BorderLayout.WEST);

        JPanel pRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pRight.setOpaque(false);
        btnXemChiTietHD.setPreferredSize(new Dimension(170, 36));
        btnXemChiTietHD.setEnabled(false);
        pRight.add(btnXemChiTietHD);

        btnInHD.setPreferredSize(new Dimension(110, 36));
        btnInHD.setEnabled(false);
        pRight.add(btnInHD);

        cardToolbar.add(pRight, BorderLayout.EAST);
        pnl.add(cardToolbar, BorderLayout.NORTH);

        // Bảng dữ liệu Tab 2
        RoundedPanel cardTable = new RoundedPanel(12, Color.WHITE);
        cardTable.setLayout(new BorderLayout(0, 10));
        cardTable.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel pHeadTbl = new JPanel(new BorderLayout());
        pHeadTbl.setOpaque(false);
        pHeadTbl.add(UI.h2("Lịch sử hóa đơn đã thanh toán thành công"), BorderLayout.WEST);
        pHeadTbl.add(lblSoLuongLichSu, BorderLayout.EAST);
        cardTable.add(pHeadTbl, BorderLayout.NORTH);

        tblLichSu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLichSu.setRowHeight(36);
        tblLichSu.setShowGrid(false);

        DefaultTableCellRenderer rCenter = new DefaultTableCellRenderer();
        rCenter.setHorizontalAlignment(SwingConstants.CENTER);
        tblLichSu.getColumnModel().getColumn(0).setCellRenderer(rCenter);
        tblLichSu.getColumnModel().getColumn(1).setCellRenderer(rCenter);
        tblLichSu.getColumnModel().getColumn(2).setCellRenderer(rCenter);
        tblLichSu.getColumnModel().getColumn(3).setCellRenderer(rCenter);
        tblLichSu.getColumnModel().getColumn(5).setCellRenderer(rCenter);
        tblLichSu.getColumnModel().getColumn(11).setCellRenderer(rCenter);

        DefaultTableCellRenderer rRight = new DefaultTableCellRenderer();
        rRight.setHorizontalAlignment(SwingConstants.RIGHT);
        tblLichSu.getColumnModel().getColumn(6).setCellRenderer(rRight);
        tblLichSu.getColumnModel().getColumn(7).setCellRenderer(rRight);
        tblLichSu.getColumnModel().getColumn(8).setCellRenderer(rRight);
        tblLichSu.getColumnModel().getColumn(9).setCellRenderer(rRight);
        tblLichSu.getColumnModel().getColumn(10).setCellRenderer(rRight);

        tblLichSu.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblLichSu.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblLichSu.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblLichSu.getColumnModel().getColumn(3).setPreferredWidth(140);
        tblLichSu.getColumnModel().getColumn(4).setPreferredWidth(140);
        tblLichSu.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblLichSu.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblLichSu.getColumnModel().getColumn(7).setPreferredWidth(100);
        tblLichSu.getColumnModel().getColumn(8).setPreferredWidth(90);
        tblLichSu.getColumnModel().getColumn(9).setPreferredWidth(90);
        tblLichSu.getColumnModel().getColumn(10).setPreferredWidth(130);
        tblLichSu.getColumnModel().getColumn(11).setPreferredWidth(110);

        JScrollPane scroll = new JScrollPane(tblLichSu);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardTable.add(scroll, BorderLayout.CENTER);

        pnl.add(cardTable, BorderLayout.CENTER);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TẢI DỮ LIỆU
    // ─────────────────────────────────────────────────────────────────────────
    public void taiDuLieuTab1() {
        lblSoLuongChoTT.setText("Đang tải dữ liệu...");
        new Thread(() -> {
            try {
                String kw = txtTimKiemChoTT.getText();
                List<HoaDonChoThanhToanRow> list = hoaDonBus.layDanhSachChoThanhToan(kw);
                SwingUtilities.invokeLater(() -> renderTab1(list));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> lblSoLuongChoTT.setText("Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    private void renderTab1(List<HoaDonChoThanhToanRow> list) {
        this.dsChoThanhToan = list != null ? list : List.of();
        modelChoTT.setRowCount(0);
        int stt = 1;

        for (HoaDonChoThanhToanRow r : dsChoThanhToan) {
            modelChoTT.addRow(new Object[]{
                    stt++,
                    r.maPhieu(),
                    r.thoiGianHoanThanh() != null ? r.thoiGianHoanThanh().format(TIME_FMT) : "--",
                    r.bienSo(),
                    r.thongTinXe(),
                    r.tenKH(),
                    r.sdtKH(),
                    r.tenKTV(),
                    Fmt.money(r.chiPhiTamTinh())
            });
        }

        lblSoLuongChoTT.setText("Hiển thị " + dsChoThanhToan.size() + " xe chờ thanh toán");
        btnThanhToan.setEnabled(false);
    }

    public void taiDuLieuTab2() {
        lblSoLuongLichSu.setText("Đang tải dữ liệu...");
        new Thread(() -> {
            try {
                String kw = txtTimKiemLichSu.getText();
                String pt = (String) cboLocPhuongThuc.getSelectedItem();
                List<HoaDonLichSuRow> list = hoaDonBus.layDanhSachLichSu(kw, pt);
                SwingUtilities.invokeLater(() -> renderTab2(list));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> lblSoLuongLichSu.setText("Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    private void renderTab2(List<HoaDonLichSuRow> list) {
        this.dsLichSu = list != null ? list : List.of();
        modelLichSu.setRowCount(0);
        int stt = 1;

        for (HoaDonLichSuRow r : dsLichSu) {
            modelLichSu.addRow(new Object[]{
                    stt++,
                    r.maHoaDon(),
                    r.maPhieu(),
                    r.ngayLap() != null ? r.ngayLap().format(TIME_FMT) : "--",
                    r.tenKH(),
                    r.bienSo(),
                    Fmt.money(r.tongTienDichVu()),
                    Fmt.money(r.tongTienPhuTung()),
                    r.giamGia() > 0 ? ("-" + Fmt.money(r.giamGia())) : "0 ₫",
                    Fmt.money(r.thueVAT()),
                    Fmt.money(r.tongTienThanhToan()),
                    r.phuongThuc()
            });
        }

        lblSoLuongLichSu.setText("Hiển thị " + dsLichSu.size() + " hóa đơn đã thanh toán");
        btnXemChiTietHD.setEnabled(false);
        btnInHD.setEnabled(false);
    }

    private void moDialogThanhToan() {
        int row = tblChoTT.getSelectedRow();
        if (row < 0 || row >= dsChoThanhToan.size()) return;

        HoaDonChoThanhToanRow item = dsChoThanhToan.get(row);
        ThanhToanHoaDonModalDialog dlg = new ThanhToanHoaDonModalDialog(
                SwingUtilities.getWindowAncestor(this),
                item.maPhieu(),
                () -> {
                    taiDuLieuTab1();
                    taiDuLieuTab2();
                }
        );
        dlg.setVisible(true);
    }

    private void moDialogXemChiTiet() {
        int row = tblLichSu.getSelectedRow();
        if (row < 0 || row >= dsLichSu.size()) return;

        HoaDonLichSuRow item = dsLichSu.get(row);
        ChiTietHoaDonModalDialog dlg = new ChiTietHoaDonModalDialog(
                SwingUtilities.getWindowAncestor(this),
                item.maHoaDon()
        );
        dlg.setVisible(true);
    }

    private void moDialogInHoaDon() {
        int row = tblLichSu.getSelectedRow();
        if (row < 0 || row >= dsLichSu.size()) return;

        HoaDonLichSuRow item = dsLichSu.get(row);
        InHoaDonDialog dlg = new InHoaDonDialog(
                SwingUtilities.getWindowAncestor(this),
                item.maHoaDon()
        );
        dlg.setVisible(true);
    }

    private void initShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "actRefreshAll");
        getActionMap().put("actRefreshAll", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                taiDuLieuTab1();
                taiDuLieuTab2();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "actThanhToan");
        getActionMap().put("actThanhToan", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 0 && tblChoTT.getSelectedRow() >= 0) {
                    moDialogThanhToan();
                }
            }
        });
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN 
    // =========================================================================
    private void initEvents() {
        // Tab 1 Events
        btnTimChoTT.addActionListener(e -> taiDuLieuTab1());
        txtTimKiemChoTT.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) taiDuLieuTab1();
            }
        });
        btnLamMoiChoTT.addActionListener(e -> {
            txtTimKiemChoTT.setText("");
            taiDuLieuTab1();
        });

        btnThanhToan.addActionListener(e -> moDialogThanhToan());

        tblChoTT.getSelectionModel().addListSelectionListener(e -> {
            btnThanhToan.setEnabled(tblChoTT.getSelectedRow() >= 0);
        });

        tblChoTT.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblChoTT.getSelectedRow() >= 0) {
                    moDialogThanhToan();
                }
            }
        });

        // Tab 2 Events
        btnTimLichSu.addActionListener(e -> taiDuLieuTab2());
        txtTimKiemLichSu.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) taiDuLieuTab2();
            }
        });
        cboLocPhuongThuc.addActionListener(e -> taiDuLieuTab2());
        btnLamMoiLichSu.addActionListener(e -> {
            txtTimKiemLichSu.setText("");
            cboLocPhuongThuc.setSelectedIndex(0);
            taiDuLieuTab2();
        });

        btnXemChiTietHD.addActionListener(e -> moDialogXemChiTiet());
        btnInHD.addActionListener(e -> moDialogInHoaDon());

        tblLichSu.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = tblLichSu.getSelectedRow() >= 0;
            btnXemChiTietHD.setEnabled(hasSel);
            btnInHD.setEnabled(hasSel);
        });

        tblLichSu.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblLichSu.getSelectedRow() >= 0) {
                    moDialogXemChiTiet();
                }
            }
        });
    }
}

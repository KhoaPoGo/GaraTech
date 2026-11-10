package gui.panel;

import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.PhieuTiepNhanSuaChuaDao.PhieuTiepNhanSuaChuaRow;
import entity.KhachHang;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
import enums.TrangThaiPhieu;
import gui.Navigator;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import gui.dialog.ChiTietPhieuModalDialog;
import gui.dialog.LapPhieuModalDialog;
import gui.dialog.SuaPhieuModalDialog;
import gui.dialog.TraCuuKhachHangDialog;
import util.RoundedPanel;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Quản lý phiếu tiếp nhận – sửa chữa.
 * Hiển thị trực tiếp danh sách các phiếu với bộ lọc và tìm kiếm.
 * Các thao tác tạo mới, xem chi tiết và chỉnh sửa phiếu đều mở dạng Modal Dialog.
 */
public final class TiepNhanXePanel extends BasePanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PhieuTiepNhanSuaChuaDao phieuDao = new PhieuTiepNhanSuaChuaDao();

    // Dữ liệu bảng
    private List<PhieuTiepNhanSuaChuaRow> dsPhieu = new ArrayList<>();
    private final DefaultTableModel modelPhieu;
    private final JTable tblPhieu;

    // Components thanh công cụ
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboLocTrangThai = new JComboBox<>(new String[]{
            "-- Tất cả trạng thái --",
            "Chờ sửa chữa (CHO_SUA)",
            "Đang sửa chữa (DANG_SUA)",
            "Chờ linh kiện (CHO_LINH_KIEN)",
            "Hoàn tất (HOAN_TAT)"
    });
    private final JButton btnTim = UI.primaryButton("Tìm kiếm");
    private final JButton btnLamMoi = UI.secondaryButton("Làm mới");

    // Action buttons
    private final JButton btnXemChiTiet = UI.secondaryButton("Xem chi tiết");
    private final JButton btnThemPhieu = UI.primaryButton("+ Tạo phiếu tiếp nhận");

    private final JLabel lblSoLuong = UI.muted("Đang tải dữ liệu...");

    public TiepNhanXePanel() {
        super("", "Quản lý phiếu tiếp nhận – sửa chữa",
                "");

        // Nút hành động trên tiêu đề
        btnXemChiTiet.setEnabled(false);
        actions.add(btnXemChiTiet);
        actions.add(btnThemPhieu);

        // Khởi tạo bảng danh sách phiếu
        String[] cols = {
                "STT", "Mã phiếu", "Thời gian tiếp nhận", "Biển số xe",
                "Hãng & Dòng xe", "Khách hàng", "Số điện thoại", "Kỹ thuật viên chính", "Trạng thái"
        };
        modelPhieu = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPhieu = UI.table(modelPhieu);

        buildContent();
        initShortcuts();
        initEvents();

        loadData();
    }

    private void buildContent() {
        JPanel pMain = new JPanel(new BorderLayout(0, 12));
        pMain.setOpaque(false);

        // 1. Thanh tìm kiếm & bộ lọc
        RoundedPanel cardFilter = new RoundedPanel(14, Theme.CARD);
        cardFilter.setLayout(new BorderLayout(12, 0));
        cardFilter.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel pLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pLeft.setOpaque(false);

        txtTimKiem.setFont(Theme.font(Font.PLAIN, 13));
        txtTimKiem.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm mã phiếu, biển số xe, khách hàng, SĐT...");
        txtTimKiem.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        txtTimKiem.setPreferredSize(new Dimension(320, 36));
        pLeft.add(txtTimKiem);

        cboLocTrangThai.setFont(Theme.font(Font.PLAIN, 13));
        cboLocTrangThai.setPreferredSize(new Dimension(210, 36));
        cboLocTrangThai.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        pLeft.add(cboLocTrangThai);

        btnTim.setPreferredSize(new Dimension(115, 36));
        btnLamMoi.setPreferredSize(new Dimension(115, 36));
        pLeft.add(btnTim);
        pLeft.add(btnLamMoi);
        cardFilter.add(pLeft, BorderLayout.CENTER);

        pMain.add(cardFilter, BorderLayout.NORTH);

        // 2. Bảng dữ liệu
        RoundedPanel cardTable = new RoundedPanel(14, Theme.CARD);
        cardTable.setLayout(new BorderLayout(0, 10));
        cardTable.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel pHead = new JPanel(new BorderLayout());
        pHead.setOpaque(false);
        pHead.add(UI.h2("Danh sách phiếu tiếp nhận sửa chữa"), BorderLayout.WEST);
        pHead.add(lblSoLuong, BorderLayout.EAST);
        cardTable.add(pHead, BorderLayout.NORTH);

        // Thiết lập bảng
        tblPhieu.setRowHeight(36);
        tblPhieu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblPhieu.getColumnModel().getColumn(1).setPreferredWidth(125);
        tblPhieu.getColumnModel().getColumn(2).setPreferredWidth(135);
        tblPhieu.getColumnModel().getColumn(3).setPreferredWidth(105);
        tblPhieu.getColumnModel().getColumn(4).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(5).setPreferredWidth(145);
        tblPhieu.getColumnModel().getColumn(6).setPreferredWidth(105);
        tblPhieu.getColumnModel().getColumn(7).setPreferredWidth(140);
        tblPhieu.getColumnModel().getColumn(8).setPreferredWidth(130);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblPhieu.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        tblPhieu.getColumnModel().getColumn(8).setCellRenderer(new TrangThaiBadgeRenderer());

        JScrollPane scroll = new JScrollPane(tblPhieu);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardTable.add(scroll, BorderLayout.CENTER);

        pMain.add(cardTable, BorderLayout.CENTER);
        body.add(pMain, BorderLayout.CENTER);
    }

    private void initShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "themPhieuF2");
        am.put("themPhieuF2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moModalLapPhieu(null, null);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoiF5");
        am.put("lamMoiF5", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadData();
            }
        });
    }

    public void loadData() {
        String kw = txtTimKiem.getText().trim();
        int idx = cboLocTrangThai.getSelectedIndex();

        TrangThaiPhieu tt = switch (idx) {
            case 1 -> TrangThaiPhieu.CHO_SUA;
            case 2 -> TrangThaiPhieu.DANG_SUA;
            case 3 -> TrangThaiPhieu.CHO_LINH_KIEN;
            case 4 -> TrangThaiPhieu.HOAN_TAT;
            default -> null;
        };

        lblSoLuong.setText("Đang tải dữ liệu...");
        btnXemChiTiet.setEnabled(false);

        try {
            dsPhieu = phieuDao.layDanhSachChoTiepNhanVien(kw, tt, null, null);
            hienThiLenBang();
        } catch (Exception ex) {
            lblSoLuong.setText("Lỗi nạp dữ liệu: " + ex.getMessage());
        }
    }

    private void hienThiLenBang() {
        modelPhieu.setRowCount(0);
        int stt = 1;

        if (dsPhieu == null || dsPhieu.isEmpty()) {
            lblSoLuong.setText("Không tìm thấy phiếu tiếp nhận nào phù hợp.");
            btnXemChiTiet.setEnabled(false);
            return;
        }

        for (PhieuTiepNhanSuaChuaRow row : dsPhieu) {
            modelPhieu.addRow(new Object[]{
                    stt++,
                    row.maPhieu(),
                    row.ngayGioTiepNhan() != null ? row.ngayGioTiepNhan().format(TIME_FMT) : "--",
                    row.bienSo() != null ? row.bienSo() : "--",
                    row.thongTinXe() != null ? row.thongTinXe() : "--",
                    row.tenKH() != null ? row.tenKH() : "--",
                    row.sdtKH() != null ? row.sdtKH() : "--",
                    row.tenNVKyThuat() != null ? row.tenNVKyThuat() : "-- Chưa phân công --",
                    row.trangThai() != null ? row.trangThai() : "CHO_SUA"
            });
        }

        lblSoLuong.setText("Hiển thị " + dsPhieu.size() + " phiếu tiếp nhận");
        btnXemChiTiet.setEnabled(false);
    }

    public void apDungBoLocTuNgoai(TrangThaiPhieu tt, String tuKhoa) {
        if (tt != null) {
            switch (tt) {
                case CHO_SUA -> cboLocTrangThai.setSelectedIndex(1);
                case DANG_SUA -> cboLocTrangThai.setSelectedIndex(2);
                case CHO_LINH_KIEN -> cboLocTrangThai.setSelectedIndex(3);
                case HOAN_TAT -> cboLocTrangThai.setSelectedIndex(4);
            }
        } else {
            cboLocTrangThai.setSelectedIndex(0);
        }

        if (tuKhoa != null) {
            txtTimKiem.setText(tuKhoa);
        }

        loadData();
    }

    public void moModalLapPhieu(KhachHang kh, Xe xe) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        LapPhieuModalDialog dlg = new LapPhieuModalDialog(parent, kh, xe);
        dlg.setVisible(true);
        if (dlg.isThanhCong()) {
            loadData();
        }
    }

    public void moModalXemChiTiet(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return;
        Window parent = SwingUtilities.getWindowAncestor(this);
        ChiTietPhieuModalDialog dlg = new ChiTietPhieuModalDialog(parent, maPhieu, this::loadData);
        dlg.setVisible(true);
    }

    private void onXemChiTietSelected() {
        int row = tblPhieu.getSelectedRow();
        if (row < 0 || row >= dsPhieu.size()) return;
        moModalXemChiTiet(dsPhieu.get(row).maPhieu());
    }

    @Override
    public void onShow() {
        loadData();
        Object payload = Navigator.consumePayload();
        if (payload instanceof Xe xe) {
            moModalLapPhieu(xe.getKhachHang(), xe);
        } else if (payload instanceof String maPhieu) {
            moModalXemChiTiet(maPhieu);
        }
    }

    // =========================================================================
    // QUY TẮC BẮT BUỘC: TOÀN BỘ SỰ KIỆN NẰM TẠI initEvents() Ở CUỐI FILE
    // =========================================================================
    private void initEvents() {
        btnTim.addActionListener(e -> loadData());

        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cboLocTrangThai.setSelectedIndex(0);
            loadData();
        });

        cboLocTrangThai.addActionListener(e -> loadData());

        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loadData();
                }
            }
        });

        tblPhieu.getSelectionModel().addListSelectionListener(e -> {
            int row = tblPhieu.getSelectedRow();
            boolean coChon = (row >= 0 && row < dsPhieu.size());
            btnXemChiTiet.setEnabled(coChon);
        });

        tblPhieu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblPhieu.getSelectedRow() >= 0) {
                    onXemChiTietSelected();
                }
            }
        });

        btnXemChiTiet.addActionListener(e -> onXemChiTietSelected());
        btnThemPhieu.addActionListener(e -> moModalLapPhieu(null, null));
    }

    // =========================================================================
    // CELL RENDERER TRẠNG THÁI BADGE BO GÓC MÀU SẮC
    // =========================================================================
    private static class TrangThaiBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String val = value != null ? value.toString() : "";
            JLabel lbl = new JLabel();
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);

            String text;
            Color bg;
            Color fg;

            switch (val) {
                case "CHO_SUA" -> {
                    text = "Chờ sửa chữa";
                    bg = new Color(0xFEF3C7);
                    fg = new Color(0x92400E);
                }
                case "DANG_SUA" -> {
                    text = "Đang sửa chữa";
                    bg = new Color(0xDBEAFE);
                    fg = new Color(0x1E40AF);
                }
                case "CHO_LINH_KIEN" -> {
                    text = "Chờ linh kiện";
                    bg = new Color(0xF3E8FF);
                    fg = new Color(0x6B21A8);
                }
                case "HOAN_TAT" -> {
                    text = "Hoàn tất";
                    bg = new Color(0xD1FAE5);
                    fg = new Color(0x065F46);
                }
                default -> {
                    text = val;
                    bg = new Color(0xF1F5F9);
                    fg = new Color(0x475569);
                }
            }

            lbl.setText(text);
            lbl.setBackground(bg);
            lbl.setForeground(fg);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bg, 1),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));

            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            if (isSelected) {
                wrapper.setBackground(table.getSelectionBackground());
            } else {
                wrapper.setBackground(table.getBackground());
            }
            wrapper.add(lbl);
            return wrapper;
        }
    }
}

package gui.panel.tiepnhan;

import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.PhieuTiepNhanSuaChuaDao.PhieuTiepNhanSuaChuaRow;
import enums.TrangThaiPhieu;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Danh sách phiếu tiếp nhận - sửa chữa.
 * Hỗ trợ bộ lọc gọn gàng (khoảng ngày, từ khóa, trạng thái) và xem chi tiết phiếu.
 * Áp dụng 4 trạng thái chuẩn: Chờ sửa chữa, Đang sửa chữa, Chờ linh kiện, Hoàn tất.
 */
public final class DanhSachTiepNhanPanel extends JPanel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM · HH:mm");

    private final Runnable onCreateNewCallback;
    private final Consumer<PhieuTiepNhanSuaChuaRow> onViewDetailCallback;
    private final PhieuTiepNhanSuaChuaDao phieuDao = new PhieuTiepNhanSuaChuaDao();

    // Filters
    private final JTextField txtFromDate = new JTextField(LocalDate.now().minusDays(30).format(DMY));
    private final JTextField txtToDate = new JTextField(LocalDate.now().format(DMY));
    private final JTextField txtKeyword = new JTextField();
    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{
            "Tất cả trạng thái",
            "Chờ sửa chữa (CHO_SUA)",
            "Đang sửa chữa (DANG_SUA)",
            "Chờ linh kiện (CHO_LINH_KIEN)",
            "Hoàn tất (HOAN_TAT)"
    });
    private final JButton btnFilter = UI.primaryButton("Tìm kiếm");
    private final JButton btnResetFilter = UI.secondaryButton("Đặt lại");

    // Table
    private final DefaultTableModel tableModel = UI.readOnlyModel(
            "Mã phiếu", "Biển số / khách", "Kỹ thuật viên", "Tiếp nhận", "Tổng tiền", "Trạng thái"
    );
    private final JTable table = UI.table(tableModel);
    private final JLabel lblPagingStatus = UI.muted("Trang 1 / 1 · Hiển thị 0 phiếu");

    private List<PhieuTiepNhanSuaChuaRow> currentRows = new ArrayList<>();

    public DanhSachTiepNhanPanel(Runnable onCreateNewCallback, Consumer<PhieuTiepNhanSuaChuaRow> onViewDetailCallback) {
        this.onCreateNewCallback = onCreateNewCallback;
        this.onViewDetailCallback = onViewDetailCallback;

        setOpaque(false);
        setLayout(new BorderLayout(0, 16));

        buildUI();
        initEvents();
        loadData();
    }

    private void buildUI() {
        // 1. Card Bộ lọc (Chiều cao gọn gàng ~90px, tự co giãn theo nội dung)
        RoundedPanel cardFilter = new RoundedPanel(14, Theme.CARD);
        cardFilter.setLayout(new BorderLayout(0, 10));
        cardFilter.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        cardFilter.add(UI.h2("Bộ lọc tìm kiếm"), BorderLayout.NORTH);

        JPanel pFilterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pFilterRow.setOpaque(false);

        // Từ ngày / Đến ngày
        pFilterRow.add(compactFieldWrap("Từ ngày", txtFromDate, 105));
        pFilterRow.add(compactFieldWrap("Đến ngày", txtToDate, 105));

        // Từ khóa
        txtKeyword.putClientProperty("JTextField.placeholderText", "Biển số, SĐT, khách hàng, mã...");
        pFilterRow.add(compactFieldWrap("Từ khóa", txtKeyword, 230));

        // Trạng thái
        pFilterRow.add(compactFieldWrap("Trạng thái", cboStatus, 195));

        // Nút tìm kiếm & Đặt lại
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pBtns.setOpaque(false);
        btnFilter.setPreferredSize(new Dimension(105, 36));
        btnResetFilter.setPreferredSize(new Dimension(85, 36));
        pBtns.add(btnFilter);
        pBtns.add(btnResetFilter);
        pFilterRow.add(compactFieldWrap(" ", pBtns, 205));

        cardFilter.add(pFilterRow, BorderLayout.CENTER);
        add(cardFilter, BorderLayout.NORTH);

        // 2. Card Danh sách phiếu
        RoundedPanel cardTable = new RoundedPanel(16, Theme.CARD);
        cardTable.setLayout(new BorderLayout(0, 14));
        cardTable.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        cardTable.add(UI.h2("Danh sách phiếu tiếp nhận - sửa chữa"), BorderLayout.NORTH);

        // Setup Table
        table.setRowHeight(44);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(160);

        DefaultTableCellRenderer cellR = new DefaultTableCellRenderer();
        cellR.setHorizontalAlignment(SwingConstants.LEFT);
        cellR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < 6; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellR);
        }

        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer headR) {
            headR.setHorizontalAlignment(SwingConstants.LEFT);
            headR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        cardTable.add(scroll, BorderLayout.CENTER);

        // Footer Card: Chỉ hiển thị số lượng / phân trang
        JPanel pFooter = new JPanel(new BorderLayout());
        pFooter.setOpaque(false);
        pFooter.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        pFooter.add(lblPagingStatus, BorderLayout.WEST);

        cardTable.add(pFooter, BorderLayout.SOUTH);
        add(cardTable, BorderLayout.CENTER);
    }

    private JPanel compactFieldWrap(String label, JComponent comp, int width) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));

        comp.setFont(Theme.font(Font.PLAIN, 13));
        if (comp instanceof JTextField) {
            comp.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8; borderColor:#D5DBE5");
        } else if (comp instanceof JComboBox) {
            comp.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        }
        comp.setPreferredSize(new Dimension(width, 36));
        comp.setMaximumSize(new Dimension(width, 36));
        comp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(comp);

        return p;
    }

    public void handleViewSelectedDetail() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < currentRows.size()) {
            if (onViewDetailCallback != null) {
                onViewDetailCallback.accept(currentRows.get(row));
            }
        } else {
            gui.component.Msg.error(this, "Vui lòng chọn một phiếu từ bảng để xem chi tiết.");
        }
    }

    public void loadData() {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        try {
            String fromStr = txtFromDate.getText().trim();
            if (!fromStr.isEmpty()) fromDate = LocalDate.parse(fromStr, DMY);
        } catch (Exception ignored) {}

        try {
            String toStr = txtToDate.getText().trim();
            if (!toStr.isEmpty()) toDate = LocalDate.parse(toStr, DMY);
        } catch (Exception ignored) {}

        String kw = txtKeyword.getText().trim();

        TrangThaiPhieu tt = null;
        int selIdx = cboStatus.getSelectedIndex();
        if (selIdx == 1) tt = TrangThaiPhieu.CHO_SUA;
        else if (selIdx == 2) tt = TrangThaiPhieu.DANG_SUA;
        else if (selIdx == 3) tt = TrangThaiPhieu.CHO_LINH_KIEN;
        else if (selIdx == 4) tt = TrangThaiPhieu.HOAN_TAT;

        try {
            currentRows = phieuDao.layDanhSachChoTiepNhanVien(kw, tt, fromDate, toDate);
        } catch (Exception e) {
            e.printStackTrace();
            currentRows = new ArrayList<>();
        }

        tableModel.setRowCount(0);

        for (PhieuTiepNhanSuaChuaRow r : currentRows) {
            String maHtml = "<html><span style='color:#24508F; font-weight:bold'>" + r.maPhieu() + "</span></html>";
            String custVeh = String.format("<html><b>%s</b> · <span style='color:#6B7A90'>%s</span></html>",
                    r.bienSo(), r.tenKH() != null ? r.tenKH() : "---");
            String ktvStr = r.tenNVKyThuat() != null ? r.tenNVKyThuat() : "<html><i style='color:#94A3B8'>Chưa phân công</i></html>";
            String timeStr = r.ngayGioTiepNhan() != null ? r.ngayGioTiepNhan().format(DATE_TIME_FMT) : "---";
            String tienStr = Fmt.money(r.tongChiPhi());
            String statusHtml = formatStatusBadge(r.trangThai());

            tableModel.addRow(new Object[]{
                    maHtml,
                    custVeh,
                    ktvStr,
                    timeStr,
                    tienStr,
                    statusHtml
            });
        }

        lblPagingStatus.setText(String.format("Trang 1 / 1 · Hiển thị %d phiếu", currentRows.size()));

        if (!currentRows.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private String formatStatusBadge(String status) {
        TrangThaiPhieu tt = TrangThaiPhieu.fromDb(status);
        String color = switch (tt) {
            case CHO_SUA -> "#B45309";       // Cam đậm / Amber
            case DANG_SUA -> "#175CD3";      // Lam / Blue
            case CHO_LINH_KIEN -> "#7A5AF8"; // Tím / Purple
            case HOAN_TAT -> "#027A48";      // Xanh lá / Green
        };
        return "<html><span style='color:" + color + "; font-weight:bold'>" + tt.label() + "</span></html>";
    }

    // =========================================================================
    // ĐEM TOÀN BỘ CÁC LUỒNG GỌI XỬ LÝ SỰ KIỆN XUỐNG CUỐI CÙNG
    // =========================================================================
    private void initEvents() {
        btnFilter.addActionListener(e -> loadData());

        btnResetFilter.addActionListener(e -> {
            txtFromDate.setText(LocalDate.now().minusDays(30).format(DMY));
            txtToDate.setText(LocalDate.now().format(DMY));
            txtKeyword.setText("");
            cboStatus.setSelectedIndex(0);
            loadData();
        });

        txtKeyword.addActionListener(e -> loadData());

        cboStatus.addActionListener(e -> {
            if (isShowing()) {
                loadData();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleViewSelectedDetail();
                }
            }
        });
    }
}

package gui.panel.tiepnhan;

import bus.TiepNhanXeBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuTiepNhanDao.PhieuTiepNhanRow;
import gui.component.Theme;
import gui.component.UI;
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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Danh sách phiếu tiếp nhận (Figma Frame 9:9).
 * Hỗ trợ bộ lọc khoảng ngày, tìm kiếm theo từ khóa/trạng thái và xem chi tiết phiếu.
 */
public final class DanhSachTiepNhanPanel extends JPanel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM · HH:mm");

    private final Runnable onCreateNewCallback;
    private final Consumer<PhieuTiepNhanRow> onViewDetailCallback;

    // Filters
    private final JTextField txtFromDate = new JTextField(LocalDate.now().minusDays(30).format(DMY));
    private final JTextField txtToDate = new JTextField(LocalDate.now().format(DMY));
    private final JTextField txtKeyword = new JTextField();
    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{
            "Tất cả trạng thái",
            "Chờ khách hàng xác nhận",
            "Đã xác nhận",
            "Đã hủy"
    });
    private final JButton btnFilter = UI.primaryButton("Tìm kiếm");
    private final JButton btnResetFilter = UI.secondaryButton("Đặt lại");

    // Table
    private final DefaultTableModel tableModel = UI.readOnlyModel("Mã phiếu", "Biển số / khách", "Tiếp nhận", "Trạng thái");
    private final JTable table = UI.table(tableModel);
    private final JLabel lblPagingStatus = UI.muted("Trang 1 / 1 · Hiển thị 0 phiếu");
    private final JButton btnViewDetail = UI.secondaryButton("Xem chi tiết phiếu");
    private final JButton btnCreateNew = UI.primaryButton("+ Lập phiếu mới");

    private List<PhieuTiepNhanRow> currentRows = new ArrayList<>();

    public DanhSachTiepNhanPanel(Runnable onCreateNewCallback, Consumer<PhieuTiepNhanRow> onViewDetailCallback) {
        this.onCreateNewCallback = onCreateNewCallback;
        this.onViewDetailCallback = onViewDetailCallback;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        initEvents();
        loadData();
    }

    private void buildUI() {
        // 1. Card Bộ lọc
        RoundedPanel cardFilter = new RoundedPanel(16, Theme.CARD);
        cardFilter.setLayout(new BorderLayout(0, 14));
        cardFilter.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        cardFilter.add(UI.h2("Bộ lọc tìm kiếm"), BorderLayout.NORTH);

        JPanel pFilterRow = new JPanel(new GridLayout(1, 3, 20, 0));
        pFilterRow.setOpaque(false);

        // Cột 1: Từ ngày - Đến ngày
        JPanel pDates = new JPanel(new GridLayout(1, 2, 10, 0));
        pDates.setOpaque(false);
        pDates.add(formFieldWrap("Từ ngày", txtFromDate));
        pDates.add(formFieldWrap("Đến ngày", txtToDate));
        pFilterRow.add(pDates);

        // Cột 2: Từ khóa
        pFilterRow.add(formFieldWrap("Biển số / SĐT / Khách hàng / Mã PTN", txtKeyword));

        // Cột 3: Trạng thái & Nút tìm
        JPanel pStatusAndBtn = new JPanel(new BorderLayout(10, 0));
        pStatusAndBtn.setOpaque(false);
        pStatusAndBtn.add(formFieldWrap("Trạng thái phiếu", cboStatus), BorderLayout.CENTER);

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 22));
        pBtns.setOpaque(false);
        pBtns.add(btnFilter);
        pBtns.add(btnResetFilter);
        pStatusAndBtn.add(pBtns, BorderLayout.EAST);

        pFilterRow.add(pStatusAndBtn);

        cardFilter.add(pFilterRow, BorderLayout.CENTER);
        cardFilter.setAlignmentX(LEFT_ALIGNMENT);
        add(cardFilter);

        add(Box.createVerticalStrut(18));

        // 2. Card Danh sách phiếu
        RoundedPanel cardTable = new RoundedPanel(16, Theme.CARD);
        cardTable.setLayout(new BorderLayout(0, 14));
        cardTable.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        cardTable.add(UI.h2("Danh sách phiếu tiếp nhận"), BorderLayout.NORTH);

        // Setup Table
        table.setRowHeight(44);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);

        DefaultTableCellRenderer cellR = new DefaultTableCellRenderer();
        cellR.setHorizontalAlignment(SwingConstants.LEFT);
        cellR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < 4; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellR);
        }

        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer headR) {
            headR.setHorizontalAlignment(SwingConstants.LEFT);
            headR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        sp.setPreferredSize(new Dimension(800, 360));
        cardTable.add(sp, BorderLayout.CENTER);

        // Footer table: Phân trang + Action buttons
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(lblPagingStatus, BorderLayout.WEST);

        JPanel botActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botActions.setOpaque(false);
        botActions.add(btnCreateNew);
        botActions.add(btnViewDetail);
        footer.add(botActions, BorderLayout.EAST);

        cardTable.add(footer, BorderLayout.SOUTH);
        cardTable.setAlignmentX(LEFT_ALIGNMENT);
        add(cardTable);
    }

    private void initEvents() {
        btnFilter.addActionListener(e -> loadData());

        btnResetFilter.addActionListener(e -> {
            txtFromDate.setText(LocalDate.now().minusDays(30).format(DMY));
            txtToDate.setText(LocalDate.now().format(DMY));
            txtKeyword.setText("");
            cboStatus.setSelectedIndex(0);
            loadData();
        });

        btnCreateNew.addActionListener(e -> {
            if (onCreateNewCallback != null) {
                onCreateNewCallback.run();
            }
        });

        btnViewDetail.addActionListener(e -> handleViewSelectedDetail());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleViewSelectedDetail();
                }
            }
        });
    }

    private void handleViewSelectedDetail() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < currentRows.size()) {
            if (onViewDetailCallback != null) {
                onViewDetailCallback.accept(currentRows.get(row));
            }
        } else {
            gui.component.Msg.error(this, "Vui lòng chọn một phiếu tiếp nhận từ bảng để xem chi tiết.");
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
        String st = cboStatus.getSelectedItem() != null ? cboStatus.getSelectedItem().toString() : "";
        if (st.startsWith("Tất cả")) st = null;

        currentRows = TiepNhanXeBus.traCuuDanhSach(fromDate, toDate, kw, st);
        tableModel.setRowCount(0);

        for (PhieuTiepNhanRow r : currentRows) {
            String maHtml = "<html><span style='color:#24508F; font-weight:bold'>" + r.maPhieuTN() + " ↗</span></html>";
            String custVeh = String.format("<html><b>%s</b> · <span style='color:#6B7A90'>%s</span></html>",
                    r.bienSo(), r.tenKhachHang());
            String timeStr = r.thoiGianTiepNhan() != null ? r.thoiGianTiepNhan().format(DATE_TIME_FMT) : "---";

            String statusHtml = formatStatusBadge(r.trangThai());

            tableModel.addRow(new Object[]{
                    maHtml,
                    custVeh,
                    timeStr,
                    statusHtml
            });
        }

        lblPagingStatus.setText(String.format("Trang 1 / 1 · Hiển thị %d phiếu tiếp nhận", currentRows.size()));

        if (!currentRows.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private String formatStatusBadge(String status) {
        if (status == null) status = "Chờ khách hàng xác nhận";
        String color = switch (status) {
            case "Đã xác nhận" -> "#027A48";
            case "Đã hủy" -> "#B42318";
            default -> "#B45309"; // Chờ khách hàng xác nhận
        };
        return "<html><span style='color:" + color + "; font-weight:bold'>" + status + "</span></html>";
    }

    private JPanel formFieldWrap(String label, JComponent comp) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        comp.setFont(Theme.font(Font.PLAIN, 13));
        if (comp instanceof JTextField) {
            comp.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        } else {
            comp.putClientProperty(FlatClientProperties.STYLE, "arc:10; borderColor:#D5DBE5");
        }
        comp.setPreferredSize(new Dimension(150, 38));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        comp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(comp);

        return p;
    }
}

package gui.panel.phieusuachua;

import bus.PhieuSuaChuaBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuSuaChuaDao.PhieuSuaChuaRow;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Danh sách phiếu sửa chữa (SV2 - Tiếp nhận).
 * Tra cứu, lọc theo từ khóa, trạng thái, khoảng ngày và xem chi tiết phiếu.
 */
public final class DanhSachPhieuSuaChuaPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PhieuSuaChuaBus bus = new PhieuSuaChuaBus();
    private final Runnable onToCreateCallback;
    private final Consumer<PhieuSuaChuaRow> onDetailCallback;

    // Bộ lọc
    private final JTextField txtKeyword = new JTextField(18);
    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{
            "Tất cả", "Chờ khách duyệt", "Đã xác nhận", "Đang sửa chữa", "Chờ kiểm tra", "Hoàn thành", "Đã bàn giao", "Đã hủy"
    });
    private final JTextField txtFromDate = new JTextField(9);
    private final JTextField txtToDate = new JTextField(9);
    private final JButton btnSearch = UI.primaryButton("🔍 Tìm kiếm");
    private final JButton btnReset = UI.secondaryButton("🔄 Đặt lại");

    // Bảng dữ liệu
    private final String[] COLS = {
            "Mã phiếu", "Mã tiếp nhận", "Biển số xe", "Phương tiện", "Khách hàng",
            "KTV phụ trách", "Hẹn hoàn thành", "Hạng mục", "Tổng chi phí", "Trạng thái"
    };
    private final DefaultTableModel model = UI.readOnlyModel(COLS);
    private final JTable table = UI.table(model);
    private List<PhieuSuaChuaRow> currentList = new ArrayList<>();

    private final JLabel lblCount = UI.muted("Tổng số: 0 phiếu");
    private final JButton btnDetail = UI.primaryButton("👁️ Xem chi tiết phiếu");

    public DanhSachPhieuSuaChuaPanel(Runnable onToCreateCallback, Consumer<PhieuSuaChuaRow> onDetailCallback) {
        this.onToCreateCallback = onToCreateCallback;
        this.onDetailCallback = onDetailCallback;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        buildUI();
        initEvents();
    }

    private void buildUI() {
        // 1. Thẻ Bộ lọc
        RoundedPanel cardFilter = new RoundedPanel(16, Theme.CARD);
        cardFilter.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        cardFilter.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        txtKeyword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mã PSC, biển số, tên khách, SĐT...");
        txtKeyword.setPreferredSize(new Dimension(240, 36));

        cboStatus.setPreferredSize(new Dimension(160, 36));

        txtFromDate.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Từ: dd/MM/yyyy");
        txtFromDate.setPreferredSize(new Dimension(115, 36));

        txtToDate.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Đến: dd/MM/yyyy");
        txtToDate.setPreferredSize(new Dimension(115, 36));

        cardFilter.add(new JLabel("Từ khóa:"));
        cardFilter.add(txtKeyword);
        cardFilter.add(new JLabel("Trạng thái:"));
        cardFilter.add(cboStatus);
        cardFilter.add(new JLabel("Thời gian:"));
        cardFilter.add(txtFromDate);
        cardFilter.add(txtToDate);
        cardFilter.add(btnSearch);
        cardFilter.add(btnReset);

        add(cardFilter, BorderLayout.NORTH);

        // 2. Thẻ Danh sách bảng
        RoundedPanel cardTable = new RoundedPanel(16, Theme.CARD);
        cardTable.setLayout(new BorderLayout(0, 12));
        cardTable.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        setupTable();
        JScrollPane scroll = new JScrollPane(table);
        cardTable.add(scroll, BorderLayout.CENTER);

        // Thanh công cụ dưới bảng
        JPanel botBar = new JPanel(new BorderLayout());
        botBar.setOpaque(false);
        botBar.add(lblCount, BorderLayout.WEST);

        JPanel pActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pActions.setOpaque(false);
        pActions.add(btnDetail);
        botBar.add(pActions, BorderLayout.EAST);

        cardTable.add(botBar, BorderLayout.SOUTH);
        add(cardTable, BorderLayout.CENTER);
    }

    private void setupTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(36);

        table.getColumnModel().getColumn(0).setPreferredWidth(125); // Mã PSC
        table.getColumnModel().getColumn(1).setPreferredWidth(125); // Mã PTN
        table.getColumnModel().getColumn(2).setPreferredWidth(95);  // Biển số
        table.getColumnModel().getColumn(3).setPreferredWidth(160); // Xe
        table.getColumnModel().getColumn(4).setPreferredWidth(150); // Khách hàng
        table.getColumnModel().getColumn(5).setPreferredWidth(130); // KTV
        table.getColumnModel().getColumn(6).setPreferredWidth(125); // Hẹn xong
        table.getColumnModel().getColumn(7).setPreferredWidth(85);  // Hạng mục
        table.getColumnModel().getColumn(8).setPreferredWidth(120); // Chi phí
        table.getColumnModel().getColumn(9).setPreferredWidth(140); // Trạng thái

        // Renderer tiền tệ
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
        table.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        // Renderer căn giữa
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        // Renderer trạng thái badge màu sắc
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String st = value != null ? value.toString() : "";
                String color = switch (st) {
                    case "Chờ khách duyệt" -> "#B45309"; // Cam ấm
                    case "Đã xác nhận" -> "#1D4ED8";    // Xanh dương
                    case "Đang sửa chữa" -> "#0284C7";   // Xanh biển
                    case "Chờ kiểm tra" -> "#7C3AED";    // Tím
                    case "Hoàn thành" -> "#059669";      // Xanh lá
                    case "Đã bàn giao" -> "#15803D";     // Xanh đậm
                    case "Đã hủy" -> "#DC2626";          // Đỏ
                    default -> "#475569";
                };
                lbl.setText("<html><span style='color:" + color + "; font-weight:bold'>● " + st + "</span></html>");
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });
    }

    private void initEvents() {
        btnSearch.addActionListener(e -> loadData());
        txtKeyword.addActionListener(e -> loadData());
        cboStatus.addActionListener(e -> loadData());

        btnReset.addActionListener(e -> {
            txtKeyword.setText("");
            cboStatus.setSelectedIndex(0);
            txtFromDate.setText("");
            txtToDate.setText("");
            loadData();
        });

        btnDetail.addActionListener(e -> viewSelectedDetail());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelectedDetail();
                }
            }
        });
    }

    public void loadData() {
        String kw = txtKeyword.getText().trim();
        String st = (String) cboStatus.getSelectedItem();

        LocalDate from = parseDate(txtFromDate.getText().trim());
        LocalDate to = parseDate(txtToDate.getText().trim());

        try {
            currentList = bus.timKiem(from, to, kw, st);
            model.setRowCount(0);

            for (PhieuSuaChuaRow r : currentList) {
                String ktv = r.tenKTV() != null ? r.tenKTV() : (r.maKTV() != null ? r.maKTV() : "Chưa phân công");
                String henXong = r.thoiGianDuKien() != null ? r.thoiGianDuKien().format(DT_FMT) : "---";
                String tienDo = r.tongHangMuc() > 0 ? (r.hangMucXong() + "/" + r.tongHangMuc() + " HM") : "0 HM";

                model.addRow(new Object[]{
                        r.maPhieuSC(),
                        r.maPhieuTN(),
                        r.bienSo(),
                        r.thongTinXe(),
                        r.tenKH(),
                        ktv,
                        henXong,
                        tienDo,
                        r.tongTien(),
                        r.trangThai()
                });
            }
            lblCount.setText("Tổng số: " + currentList.size() + " phiếu sửa chữa");
        } catch (SQLException ex) {
            Msg.error(this, "Lỗi tải danh sách phiếu sửa chữa: " + ex.getMessage());
        }
    }

    private void viewSelectedDetail() {
        int sel = table.getSelectedRow();
        if (sel >= 0 && sel < currentList.size()) {
            if (onDetailCallback != null) {
                onDetailCallback.accept(currentList.get(sel));
            }
        } else {
            Msg.info(this, "Vui lòng chọn một phiếu sửa chữa để xem chi tiết.");
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s, D_FMT);
        } catch (Exception ex) {
            return null;
        }
    }
}

package gui.panel.tiepnhan;

import bus.KhachHangXeBus;
import bus.KhachHangXeBus.KhachHangOverview;
import com.formdev.flatlaf.FlatClientProperties;
import entity.KhachHang;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hộp thoại tra cứu nhanh khách hàng để tiếp nhận xe.
 */
public final class TraCuuKhachHangDialog extends JDialog {

    private final JTextField txtFilter = new JTextField();
    private final DefaultTableModel tableModel = UI.readOnlyModel("Mã KH", "Họ và tên", "Số điện thoại", "Xe liên kết", "Điểm");
    private final JTable table = UI.table(tableModel);
    private final JLabel lblCount = UI.muted("0 khách hàng");

    private final JButton btnSelect = UI.primaryButton("Chọn khách hàng");
    private final JButton btnAddNew = UI.secondaryButton("Thêm khách mới");
    private final JButton btnCancel = UI.secondaryButton("Đóng");

    private final Consumer<String> onAddNewCallback;
    private List<KhachHangOverview> currentList = new ArrayList<>();
    private KhachHang selectedCustomer = null;

    public TraCuuKhachHangDialog(Window owner, String initialKeyword, Consumer<String> onAddNewCallback) {
        super(owner, "Tra cứu khách hàng", ModalityType.APPLICATION_MODAL);
        this.onAddNewCallback = onAddNewCallback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(720, 500));
        setLayout(new BorderLayout());

        buildUI();
        initEvents();

        if (initialKeyword != null && !initialKeyword.isBlank()) {
            txtFilter.setText(initialKeyword.trim());
        }

        loadData();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // 1. Top Search Bar
        RoundedPanel pTop = new RoundedPanel(12, Theme.CARD);
        pTop.setLayout(new BorderLayout(12, 0));
        pTop.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel lblSearch = UI.label("Tìm kiếm:", Font.BOLD, 12, Theme.TEXT);
        txtFilter.setFont(Theme.font(Font.PLAIN, 13));
        txtFilter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Gõ số điện thoại, tên khách hàng hoặc biển số xe để lọc tức thì...");
        txtFilter.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        txtFilter.setPreferredSize(new Dimension(0, 38));

        pTop.add(lblSearch, BorderLayout.WEST);
        pTop.add(txtFilter, BorderLayout.CENTER);
        root.add(pTop, BorderLayout.NORTH);

        // 2. Table
        RoundedPanel pCenter = new RoundedPanel(14, Theme.CARD);
        pCenter.setLayout(new BorderLayout(0, 8));
        pCenter.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        table.setRowHeight(36);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(210);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        pCenter.add(sp, BorderLayout.CENTER);

        JPanel pFoot = new JPanel(new BorderLayout());
        pFoot.setOpaque(false);
        pFoot.add(lblCount, BorderLayout.WEST);
        pCenter.add(pFoot, BorderLayout.SOUTH);

        root.add(pCenter, BorderLayout.CENTER);

        // 3. Bottom Actions
        JPanel pBot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pBot.setOpaque(false);
        btnAddNew.setPreferredSize(new Dimension(130, 38));
        btnCancel.setPreferredSize(new Dimension(90, 38));
        btnSelect.setPreferredSize(new Dimension(150, 38));
        pBot.add(btnAddNew);
        pBot.add(btnCancel);
        pBot.add(btnSelect);
        root.add(pBot, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private void doSearch() {
        String q = txtFilter.getText().trim();
        loadDataFiltered(q);
    }

    private void loadData() {
        loadDataFiltered(txtFilter.getText().trim());
    }

    private void loadDataFiltered(String kw) {
        currentList = KhachHangXeBus.search(kw, kw);
        tableModel.setRowCount(0);

        for (KhachHangOverview item : currentList) {
            KhachHang kh = item.khachHang();
            tableModel.addRow(new Object[]{
                    kh.getMaKH(),
                    kh.getHoTen(),
                    kh.getSoDienThoai(),
                    item.xeSummary(),
                    kh.getDiemTichLuy()
            });
        }

        lblCount.setText(String.format("Tìm thấy %d khách hàng phù hợp", currentList.size()));

        if (!currentList.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            btnSelect.setEnabled(true);
        } else {
            btnSelect.setEnabled(false);
        }
    }

    private void handleSelectRow() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < currentList.size()) {
            this.selectedCustomer = currentList.get(row).khachHang();
            dispose();
        }
    }

    public KhachHang getSelectedCustomer() {
        return selectedCustomer;
    }

    private void initEvents() {
        btnCancel.addActionListener(e -> dispose());

        btnSelect.addActionListener(e -> handleSelectRow());

        btnAddNew.addActionListener(e -> {
            String q = txtFilter.getText().trim();
            dispose();
            if (onAddNewCallback != null) {
                onAddNewCallback.accept(q);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            btnSelect.setEnabled(table.getSelectedRow() >= 0);
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    handleSelectRow();
                }
            }
        });

        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { doSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { doSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { doSearch(); }
        });
    }
}

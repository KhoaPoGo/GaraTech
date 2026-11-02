package gui.panel;

import connectDB.ConnectDB;
import enums.Role;
import gui.component.BasePanel;
import util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class PhuTungTonKhoPanel extends BasePanel {

    private JTextField txtSearch;
    private JButton btnThem, btnSua, btnLamMoi;
    private JTable tblPhuTung;
    private DefaultTableModel modelPhuTung;
    private JTable tblLichSu;
    private DefaultTableModel modelLichSu;
    private JLabel lblSubHistory;

    private final DecimalFormat dfMoney = new DecimalFormat("#,### đ");
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public PhuTungTonKhoPanel() {
        super("Phụ tùng & tồn kho", "Quản lý phụ tùng và tồn kho kho hàng", "");
        initUI();
        loadData();
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 15, 15, 15));

        // Top Toolbar
        JPanel pnlTop = new JPanel(new BorderLayout(10, 0));
        pnlTop.setOpaque(false);

        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlSearch.setOpaque(false);
        txtSearch = new JTextField(20);
        txtSearch.setPreferredSize(new Dimension(220, 32));
        JButton btnTim = new JButton("Tìm kiếm");
        btnLamMoi = new JButton("Làm mới");
        pnlSearch.add(new JLabel("Tìm kiếm:"));
        pnlSearch.add(txtSearch);
        pnlSearch.add(btnTim);
        pnlSearch.add(btnLamMoi);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);
        btnThem = new JButton("+ Thêm phụ tùng");
        btnThem.setBackground(new Color(37, 99, 235));
        btnThem.setForeground(Color.WHITE);
        btnSua = new JButton("Cập nhật");
        pnlActions.add(btnThem);
        pnlActions.add(btnSua);

        try {
            if (Session.hasRole(Role.KY_THUAT_VIEN)) {
                btnThem.setVisible(false);
                btnSua.setVisible(false);
            }
        } catch (Exception ignored) {}

        pnlTop.add(pnlSearch, BorderLayout.WEST);
        pnlTop.add(pnlActions, BorderLayout.EAST);
        content.add(pnlTop, BorderLayout.NORTH);

        // Center Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);

        String[] colsPT = {"Mã PT", "Tên phụ tùng", "Loại PT", "ĐVT", "Tồn kho", "Mức cảnh báo", "Giá nhập", "Giá bán", "Vị trí", "Tình trạng"};
        modelPhuTung = new DefaultTableModel(colsPT, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPhuTung = new JTable(modelPhuTung);
        tblPhuTung.setRowHeight(28);
        tblPhuTung.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblPhuTung.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                String val = value != null ? value.toString() : "";
                if (!isSelected) {
                    if ("Hết hàng".equals(val)) {
                        lbl.setForeground(new Color(220, 38, 38));
                        lbl.setBackground(new Color(254, 226, 226));
                    } else if ("Sắp hết".equals(val)) {
                        lbl.setForeground(new Color(217, 119, 6));
                        lbl.setBackground(new Color(254, 243, 199));
                    } else {
                        lbl.setForeground(new Color(22, 163, 74));
                        lbl.setBackground(new Color(220, 252, 231));
                    }
                }
                return lbl;
            }
        });
        JScrollPane scrollPT = new JScrollPane(tblPhuTung);
        scrollPT.setBorder(new TitledBorder("Danh sách phụ tùng & tồn kho"));

        String[] colsLS = {"Thời gian", "Loại GD", "Mã phiếu", "Số lượng", "Đơn giá", "Thành tiền", "Người thực hiện", "Ghi chú"};
        modelLichSu = new DefaultTableModel(colsLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLichSu = new JTable(modelLichSu);
        tblLichSu.setRowHeight(26);
        JScrollPane scrollLS = new JScrollPane(tblLichSu);
        lblSubHistory = new JLabel("Lịch sử nhập / xuất kho (Bấm vào 1 phụ tùng ở trên để xem)");
        lblSubHistory.setBorder(new EmptyBorder(4, 4, 4, 4));
        scrollLS.setBorder(new TitledBorder("Lịch sử giao dịch kho"));

        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);
        pnlBottom.add(lblSubHistory, BorderLayout.NORTH);
        pnlBottom.add(scrollLS, BorderLayout.CENTER);

        splitPane.setTopComponent(scrollPT);
        splitPane.setBottomComponent(pnlBottom);
        content.add(splitPane, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // Events
        btnTim.addActionListener(e -> loadData());
        txtSearch.addActionListener(e -> loadData());
        btnLamMoi.addActionListener(e -> {
            txtSearch.setText("");
            loadData();
            modelLichSu.setRowCount(0);
        });
        tblPhuTung.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblPhuTung.getSelectedRow() != -1) {
                String maPT = modelPhuTung.getValueAt(tblPhuTung.getSelectedRow(), 0).toString();
                loadHistory(maPT);
            }
        });
        btnThem.addActionListener(e -> showDialog(null));
        btnSua.addActionListener(e -> {
            int r = tblPhuTung.getSelectedRow();
            if (r == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 phụ tùng để sửa!");
                return;
            }
            showDialog(r);
        });
    }

    private void loadData() {
        modelPhuTung.setRowCount(0);
        String kw = "%" + txtSearch.getText().trim() + "%";
        String sql = "SELECT maPhuTung, tenPhuTung, loaiPhuTung, donViTinh, soLuongTon, tonKhoToiThieu, donGiaNhap, donGiaBan, viTriLuuKho "
                   + "FROM PhuTung WHERE maPhuTung LIKE ? OR tenPhuTung LIKE ? OR loaiPhuTung LIKE ?";

        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ton = rs.getInt("soLuongTon");
                    int cb = rs.getInt("tonKhoToiThieu");
                    String tt = (ton <= 0) ? "Hết hàng" : (ton <= cb ? "Sắp hết" : "Đủ hàng");

                    modelPhuTung.addRow(new Object[]{
                        rs.getString("maPhuTung"),
                        rs.getString("tenPhuTung"),
                        rs.getString("loaiPhuTung"),
                        rs.getString("donViTinh"),
                        ton,
                        cb,
                        dfMoney.format(rs.getDouble("donGiaNhap")),
                        dfMoney.format(rs.getDouble("donGiaBan")),
                        rs.getString("viTriLuuKho"),
                        tt
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách phụ tùng: " + e.getMessage());
        }
    }

    private void loadHistory(String maPT) {
        modelLichSu.setRowCount(0);
        lblSubHistory.setText("Lịch sử giao dịch của phụ tùng: " + maPT);

        // Truy vấn lịch sử Nhập kho từ ChiTietNhapKho & PhieuNhapKho
        String sqlNhap = "SELECT pn.ngayNhap AS thoiGian, N'Nhập kho' AS loaiGD, pn.maPhieuNhap AS maPhieu, "
                       + "ct.soLuongNhap AS soLuong, ct.donGiaNhap AS donGia, ct.thanhTien, nv.hoTen AS nguoiTH, ncc.tenNCC AS ghiChu "
                       + "FROM ChiTietNhapKho ct "
                       + "JOIN PhieuNhapKho pn ON ct.maPhieuNhap = pn.maPhieuNhap "
                       + "LEFT JOIN NhanVien nv ON pn.maNVKho = nv.maNV "
                       + "LEFT JOIN NhaCungCap ncc ON pn.maNCC = ncc.maNCC "
                       + "WHERE ct.maPhuTung = ? ORDER BY pn.ngayNhap DESC";

        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlNhap)) {
            ps.setString(1, maPT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("thoiGian");
                    modelLichSu.addRow(new Object[]{
                        ts != null ? sdfDate.format(ts) : "",
                        rs.getString("loaiGD"),
                        rs.getString("maPhieu"),
                        "+" + rs.getInt("soLuong"),
                        dfMoney.format(rs.getDouble("donGia")),
                        dfMoney.format(rs.getDouble("thanhTien")),
                        rs.getString("nguoiTH"),
                        "NCC: " + (rs.getString("ghiChu") != null ? rs.getString("ghiChu") : "")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showDialog(Integer row) {
        boolean isNew = (row == null);
        JTextField txtMa = new JTextField(isNew ? "" : modelPhuTung.getValueAt(row, 0).toString());
        txtMa.setEditable(isNew);
        JTextField txtTen = new JTextField(isNew ? "" : modelPhuTung.getValueAt(row, 1).toString());
        JTextField txtLoai = new JTextField(isNew ? "Động cơ" : String.valueOf(modelPhuTung.getValueAt(row, 2)));
        JTextField txtDVT = new JTextField(isNew ? "Cái" : modelPhuTung.getValueAt(row, 3).toString());
        JTextField txtCanhBao = new JTextField(isNew ? "5" : modelPhuTung.getValueAt(row, 5).toString());
        JTextField txtGiaNhap = new JTextField(isNew ? "0" : modelPhuTung.getValueAt(row, 6).toString().replaceAll("[^0-9]", ""));
        JTextField txtGiaBan = new JTextField(isNew ? "0" : modelPhuTung.getValueAt(row, 7).toString().replaceAll("[^0-9]", ""));
        JTextField txtViTri = new JTextField(isNew ? "Kệ A1" : String.valueOf(modelPhuTung.getValueAt(row, 8)));

        Object[] msg = {
            "Mã PT (VD: PT0005):", txtMa,
            "Tên phụ tùng:", txtTen,
            "Loại phụ tùng:", txtLoai,
            "Đơn vị tính:", txtDVT,
            "Mức cảnh báo tồn:", txtCanhBao,
            "Đơn giá nhập:", txtGiaNhap,
            "Đơn giá bán:", txtGiaBan,
            "Vị trí lưu kho:", txtViTri
        };

        if (JOptionPane.showConfirmDialog(this, msg, isNew ? "Thêm phụ tùng" : "Cập nhật phụ tùng", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String ma = txtMa.getText().trim();
                String ten = txtTen.getText().trim();
                String loai = txtLoai.getText().trim();
                String dvt = txtDVT.getText().trim();
                int cb = Integer.parseInt(txtCanhBao.getText().trim().replaceAll("[^0-9]", ""));
                double gNhap = Double.parseDouble(txtGiaNhap.getText().trim().replaceAll("[^0-9]", ""));
                double gBan = Double.parseDouble(txtGiaBan.getText().trim().replaceAll("[^0-9]", ""));
                String viTri = txtViTri.getText().trim();

                if (ma.isEmpty() || ten.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Mã và tên phụ tùng không được để trống!");
                    return;
                }

                String sql = isNew
                    ? "INSERT INTO PhuTung(maPhuTung, tenPhuTung, loaiPhuTung, donViTinh, donGiaNhap, donGiaBan, soLuongTon, tonKhoToiThieu, viTriLuuKho, thoiGianBaoHanh) VALUES(?,?,?,?,?,?,0,?,?,6)"
                    : "UPDATE PhuTung SET tenPhuTung=?, loaiPhuTung=?, donViTinh=?, donGiaNhap=?, donGiaBan=?, tonKhoToiThieu=?, viTriLuuKho=? WHERE maPhuTung=?";

                try (Connection conn = ConnectDB.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (isNew) {
                        ps.setString(1, ma);
                        ps.setString(2, ten);
                        ps.setString(3, loai);
                        ps.setString(4, dvt);
                        ps.setDouble(5, gNhap);
                        ps.setDouble(6, gBan);
                        ps.setInt(7, cb);
                        ps.setString(8, viTri);
                    } else {
                        ps.setString(1, ten);
                        ps.setString(2, loai);
                        ps.setString(3, dvt);
                        ps.setDouble(4, gNhap);
                        ps.setDouble(5, gBan);
                        ps.setInt(6, cb);
                        ps.setString(7, viTri);
                        ps.setString(8, ma);
                    }
                    ps.executeUpdate();
                    loadData();
                    JOptionPane.showMessageDialog(this, "Lưu phụ tùng thành công!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi lưu dữ liệu: " + ex.getMessage());
            }
        }
    }
}
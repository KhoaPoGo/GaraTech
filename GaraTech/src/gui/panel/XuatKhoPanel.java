package gui.panel;

import connectDB.ConnectDB;
import gui.component.BasePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class XuatKhoPanel extends BasePanel {

    private JTable tblYeuCau;
    private DefaultTableModel modelYeuCau;
    private JTable tblChiTiet;
    private DefaultTableModel modelChiTiet;
    private JLabel lblThongBao;
    private JButton btnDuyet, btnTuChoi;

    private final DecimalFormat df = new DecimalFormat("#,### đ");
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public XuatKhoPanel() {
        super("Xuất kho", "Duyệt yêu cầu xuất kho từ KTV", "");
        initUI();
        loadYeuCau();
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 15, 15, 15));

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setOpaque(false);
        JLabel lblTitle = new JLabel("Danh sách yêu cầu xuất phụ tùng từ Kỹ thuật viên");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlTopBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlTopBtns.setOpaque(false);
        JButton btnRefresh = new JButton("Tải lại");
        pnlTopBtns.add(btnRefresh);
        pnlTop.add(pnlTopBtns, BorderLayout.EAST);
        content.add(pnlTop, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.45);
        split.setBorder(null);

        String[] colsYC = {"Mã Phiếu Xuất", "Mã Phiếu SC", "Người lập / KTV", "Ngày xuất / YC", "Trạng thái"};
        modelYeuCau = new DefaultTableModel(colsYC, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblYeuCau = new JTable(modelYeuCau);
        tblYeuCau.setRowHeight(28);
        tblYeuCau.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollYC = new JScrollPane(tblYeuCau);
        scrollYC.setBorder(new TitledBorder("1. Chọn phiếu xuất kho cần duyệt"));

        String[] colsCT = {"Mã PT", "Tên phụ tùng", "ĐVT", "SL Yêu cầu", "Tồn kho hiện tại", "Tồn sau xuất", "Đơn giá bán", "Kiểm tra tồn"};
        modelChiTiet = new DefaultTableModel(colsCT, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChiTiet = new JTable(modelChiTiet);
        tblChiTiet.setRowHeight(28);
        tblChiTiet.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    if (row >= 0 && row < table.getRowCount()) {
                        int sl = Integer.parseInt(table.getValueAt(row, 3).toString());
                        int ton = Integer.parseInt(table.getValueAt(row, 4).toString());
                        if (sl > ton) {
                            c.setForeground(new Color(220, 38, 38));
                            if (!isSelected) c.setBackground(new Color(254, 226, 226));
                        } else {
                            c.setForeground(new Color(22, 101, 52));
                            if (!isSelected) c.setBackground(Color.WHITE);
                        }
                    }
                } catch (Exception ignored) {}
                return c;
            }
        });
        JScrollPane scrollCT = new JScrollPane(tblChiTiet);
        scrollCT.setBorder(new TitledBorder("2. Chi tiết phụ tùng trong phiếu"));

        split.setTopComponent(scrollYC);
        split.setBottomComponent(scrollCT);
        content.add(split, BorderLayout.CENTER);

        JPanel pnlFoot = new JPanel(new BorderLayout());
        pnlFoot.setBackground(Color.WHITE);
        pnlFoot.setBorder(new EmptyBorder(10, 15, 10, 15));
        lblThongBao = new JLabel("Chọn 1 phiếu ở bảng trên để kiểm tra tồn kho.");
        lblThongBao.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtns.setOpaque(false);
        btnTuChoi = new JButton("Từ chối / Hủy");
        btnDuyet = new JButton("XÁC NHẬN DUYỆT XUẤT (TRANSACTION)");
        btnDuyet.setBackground(new Color(37, 99, 235));
        btnDuyet.setForeground(Color.WHITE);
        btnDuyet.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnDuyet.setPreferredSize(new Dimension(300, 38));
        btnDuyet.setEnabled(false);
        btnTuChoi.setEnabled(false);

        pnlBtns.add(btnTuChoi);
        pnlBtns.add(btnDuyet);

        pnlFoot.add(lblThongBao, BorderLayout.WEST);
        pnlFoot.add(pnlBtns, BorderLayout.EAST);
        content.add(pnlFoot, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadYeuCau());
        tblYeuCau.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblYeuCau.getSelectedRow() != -1) {
                loadChiTiet(modelYeuCau.getValueAt(tblYeuCau.getSelectedRow(), 0).toString());
            }
        });
        btnDuyet.addActionListener(e -> duyetXuat());
        btnTuChoi.addActionListener(e -> tuChoi());
    }

    private void loadYeuCau() {
        modelYeuCau.setRowCount(0);
        modelChiTiet.setRowCount(0);
        btnDuyet.setEnabled(false);
        btnTuChoi.setEnabled(false);

        // Sử dụng SELECT * kết hợp ResultSetMetaData để tương thích mọi tên cột trong PhieuXuatKho của nhóm
        String sql = "SELECT px.*, nv.hoTen FROM PhieuXuatKho px "
                   + "LEFT JOIN NhanVien nv ON px.maNVKho = nv.maNV "
                   + "ORDER BY px.ngayXuat DESC";
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                String maPX = rs.getString("maPhieuXuat");
                String maPSC = hasColumn(meta, "maPhieuSuaChua") ? rs.getString("maPhieuSuaChua")
                             : (hasColumn(meta, "maPhieuSC") ? rs.getString("maPhieuSC") : "");
                String hoTen = rs.getString("hoTen") != null ? rs.getString("hoTen") : "KTV yêu cầu";
                Timestamp ngay = hasColumn(meta, "ngayXuat") ? rs.getTimestamp("ngayXuat")
                               : (hasColumn(meta, "ngayYeuCau") ? rs.getTimestamp("ngayYeuCau") : null);
                String trangThai = hasColumn(meta, "trangThai") ? rs.getString("trangThai") : "Chờ duyệt";

                modelYeuCau.addRow(new Object[]{
                    maPX, maPSC, hoTen,
                    ngay != null ? sdf.format(ngay) : "",
                    trangThai
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean hasColumn(ResultSetMetaData meta, String colName) throws SQLException {
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            if (meta.getColumnName(i).equalsIgnoreCase(colName)) return true;
        }
        return false;
    }

    private void loadChiTiet(String maPX) {
        modelChiTiet.setRowCount(0);
        boolean vuotTon = false;

        // Lấy chi tiết xuất kho kết hợp với bảng PhuTung (dùng đúng cột donGiaBan)
        String sql = "SELECT ct.*, pt.tenPhuTung, pt.donViTinh, pt.soLuongTon, pt.donGiaBan "
                   + "FROM ChiTietXuatKho ct "
                   + "JOIN PhuTung pt ON ct.maPhuTung = pt.maPhuTung "
                   + "WHERE ct.maPhieuXuat = ?";
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maPX);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    int sl = hasColumn(meta, "soLuongXuat") ? rs.getInt("soLuongXuat")
                           : (hasColumn(meta, "soLuong") ? rs.getInt("soLuong") : 1);
                    int ton = rs.getInt("soLuongTon");
                    int tonSau = ton - sl;
                    String kq = (sl > ton) ? "KHÔNG ĐỦ TỒN (Thiếu " + (sl - ton) + ")" : "Đủ tồn kho";
                    if (sl > ton) vuotTon = true;

                    modelChiTiet.addRow(new Object[]{
                        rs.getString("maPhuTung"),
                        rs.getString("tenPhuTung"),
                        rs.getString("donViTinh"),
                        sl,
                        ton,
                        tonSau,
                        df.format(rs.getDouble("donGiaBan")),
                        kq
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int selectedRow = tblYeuCau.getSelectedRow();
        String trangThai = selectedRow != -1 ? String.valueOf(modelYeuCau.getValueAt(selectedRow, 4)) : "";
        boolean daXuat = trangThai.equalsIgnoreCase("Đã xuất") || trangThai.equalsIgnoreCase("DA_XUAT");

        if (daXuat) {
            btnDuyet.setEnabled(false);
            btnTuChoi.setEnabled(false);
            lblThongBao.setText("Phiếu này đã được xuất kho.");
            lblThongBao.setForeground(new Color(37, 99, 235));
        } else if (vuotTon) {
            btnDuyet.setEnabled(false);
            btnTuChoi.setEnabled(true);
            lblThongBao.setText("CẢNH BÁO: Phụ tùng vượt quá tồn kho! Đã khóa nút xác nhận.");
            lblThongBao.setForeground(new Color(220, 38, 38));
        } else if (modelChiTiet.getRowCount() > 0) {
            btnDuyet.setEnabled(true);
            btnTuChoi.setEnabled(true);
            lblThongBao.setText("Đủ tồn kho, có thể xác nhận duyệt xuất.");
            lblThongBao.setForeground(new Color(22, 163, 74));
        } else {
            btnDuyet.setEnabled(false);
            btnTuChoi.setEnabled(false);
            lblThongBao.setText("Phiếu này chưa có chi tiết phụ tùng.");
            lblThongBao.setForeground(Color.DARK_GRAY);
        }
    }

    private void duyetXuat() {
        int r = tblYeuCau.getSelectedRow();
        if (r == -1) return;
        String maPX = modelYeuCau.getValueAt(r, 0).toString();

        int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận duyệt xuất kho và trừ tồn kho?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Connection conn = null;
        try {
            conn = ConnectDB.getConnection();
            conn.setAutoCommit(false);

            String maNV = "NV26004"; // Phạm Thu Dung - Nhân viên kho

            String sqlTru = "UPDATE PhuTung SET soLuongTon = soLuongTon - ? WHERE maPhuTung = ? AND soLuongTon >= ?";
            try (PreparedStatement psTru = conn.prepareStatement(sqlTru)) {
                for (int i = 0; i < modelChiTiet.getRowCount(); i++) {
                    String pt = modelChiTiet.getValueAt(i, 0).toString();
                    int sl = Integer.parseInt(modelChiTiet.getValueAt(i, 3).toString());

                    psTru.setInt(1, sl);
                    psTru.setString(2, pt);
                    psTru.setInt(3, sl);
                    if (psTru.executeUpdate() == 0) {
                        throw new SQLException("Phụ tùng " + pt + " không đủ tồn kho!");
                    }
                }
            }

            // Cập nhật trạng thái phiếu xuất kho
            String sqlUpdPX = "UPDATE PhieuXuatKho SET trangThai = N'Đã xuất', maNVKho = ?, ngayXuat = GETDATE() WHERE maPhieuXuat = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdPX)) {
                ps.setString(1, maNV);
                ps.setString(2, maPX);
                ps.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Đã duyệt xuất kho thành công!");
            loadYeuCau();

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException e1) { e1.printStackTrace(); }
            }
            JOptionPane.showMessageDialog(this, "Lỗi xuất kho (Đã Rollback): " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void tuChoi() {
        int r = tblYeuCau.getSelectedRow();
        if (r == -1) return;
        String maPX = modelYeuCau.getValueAt(r, 0).toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE PhieuXuatKho SET trangThai = N'Hủy' WHERE maPhieuXuat = ?")) {
            ps.setString(1, maPX);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Đã hủy phiếu xuất kho.");
            loadYeuCau();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
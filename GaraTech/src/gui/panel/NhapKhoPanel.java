package gui.panel;

import connectDB.ConnectDB;
import gui.component.BasePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NhapKhoPanel extends BasePanel {

    private JComboBox<String> cboNCC;
    private JComboBox<String> cboPT;
    private JSpinner spnSL;
    private JTextField txtGia, txtGhiChu;
    private JLabel lblThanhTien, lblTongLo;
    private DefaultTableModel model;
    private double tongTien = 0;
    private final DecimalFormat df = new DecimalFormat("#,### đ");

    public NhapKhoPanel() {
        super("Nhập kho", "Lập phiếu nhập kho phụ tùng", "");
        // Bắt buộc set BorderLayout cho chính Panel này để không bị mất thanh nút bấm ở SOUTH
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(10, 15, 10, 15));

        initUI();
        loadData();
    }

    private void initUI() {
        // ================= 1. FORM LẬP PHIẾU (NORTH) =================
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBackground(Color.WHITE);
        pnlForm.setBorder(new TitledBorder("Lập phiếu nhập kho"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cboNCC = new JComboBox<>();
        cboPT = new JComboBox<>();
        spnSL = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        txtGia = new JTextField("0", 12);
        txtGhiChu = new JTextField(15);
        lblThanhTien = new JLabel("0 đ");
        lblThanhTien.setForeground(new Color(37, 99, 235));
        lblThanhTien.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton btnThem = new JButton("+ Thêm vào lô");
        btnThem.setBackground(new Color(37, 99, 235));
        btnThem.setForeground(Color.WHITE);
        JButton btnXoa = new JButton("Xóa dòng");

        gbc.gridx = 0; gbc.gridy = 0; pnlForm.add(new JLabel("Nhà cung cấp:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4; pnlForm.add(cboNCC, gbc);
        gbc.gridx = 2; gbc.weightx = 0; pnlForm.add(new JLabel("Ghi chú:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6; pnlForm.add(txtGhiChu, gbc);

        gbc.gridx = 0; gbc.gridy = 1; pnlForm.add(new JLabel("Phụ tùng:"), gbc);
        gbc.gridx = 1; pnlForm.add(cboPT, gbc);
        gbc.gridx = 2; pnlForm.add(new JLabel("Số lượng:"), gbc);
        gbc.gridx = 3; pnlForm.add(spnSL, gbc);

        gbc.gridx = 0; gbc.gridy = 2; pnlForm.add(new JLabel("Đơn giá nhập:"), gbc);
        gbc.gridx = 1; pnlForm.add(txtGia, gbc);
        gbc.gridx = 2; pnlForm.add(new JLabel("Thành tiền:"), gbc);
        gbc.gridx = 3; pnlForm.add(lblThanhTien, gbc);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBtns.setOpaque(false);
        pnlBtns.add(btnXoa);
        pnlBtns.add(btnThem);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        pnlForm.add(pnlBtns, gbc);

        add(pnlForm, BorderLayout.NORTH);

        // ================= 2. BẢNG LÔ HÀNG NHẬP (CENTER) =================
        String[] cols = {"Mã PT", "Tên phụ tùng", "Số lượng", "Đơn giá", "Thành tiền", "Tồn trước", "Tồn sau"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(600, 220));
        add(scroll, BorderLayout.CENTER);

        // ================= 3. THANH NÚT LẬP PHIẾU & XUẤT PDF (SOUTH) =================
        JPanel pnlFoot = new JPanel(new BorderLayout(10, 0));
        pnlFoot.setBackground(Color.WHITE);
        pnlFoot.setBorder(new EmptyBorder(10, 15, 10, 15));

        lblTongLo = new JLabel("Tổng lô hàng: 0 đ");
        lblTongLo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTongLo.setForeground(new Color(220, 38, 38));

        JButton btnLuu = new JButton("LẬP PHIẾU NHẬP KHO & XUẤT HÓA ĐƠN PDF");
        btnLuu.setBackground(new Color(22, 163, 74));
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLuu.setPreferredSize(new Dimension(340, 40));

        pnlFoot.add(lblTongLo, BorderLayout.WEST);
        pnlFoot.add(btnLuu, BorderLayout.EAST);
        add(pnlFoot, BorderLayout.SOUTH);

        // ================= 4. SỰ KIỆN TÍNH TIỀN & NÚT BẤM =================
        Runnable calc = () -> {
            try {
                int sl = (int) spnSL.getValue();
                String rawGia = txtGia.getText().trim().replaceAll("[^0-9]", "");
                double g = rawGia.isEmpty() ? 0 : Double.parseDouble(rawGia);
                lblThanhTien.setText(df.format(sl * g));
            } catch (Exception e) {
                lblThanhTien.setText("0 đ");
            }
        };
        spnSL.addChangeListener(e -> calc.run());
        txtGia.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calc.run(); }
            public void removeUpdate(DocumentEvent e) { calc.run(); }
            public void changedUpdate(DocumentEvent e) { calc.run(); }
        });

        cboPT.addActionListener(e -> {
            if (cboPT.getSelectedItem() != null) {
                String s = cboPT.getSelectedItem().toString();
                int idx = s.lastIndexOf(" - Giá: ");
                if (idx != -1) {
                    String giaStr = s.substring(idx + 8).replaceAll("[^0-9]", "");
                    txtGia.setText(giaStr);
                    calc.run();
                }
            }
        });

        btnThem.addActionListener(e -> themMon());
        btnXoa.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1) {
                model.removeRow(r);
                tinhTong();
            }
        });
        btnLuu.addActionListener(e -> luuPhieuNhapVaXuatPDF());
    }

    private void loadData() {
        cboNCC.removeAllItems();
        cboPT.removeAllItems();
        try (Connection conn = ConnectDB.getConnection()) {
            String sqlNCC = "SELECT maNCC, tenNCC FROM NhaCungCap";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlNCC)) {
                while (rs.next()) {
                    cboNCC.addItem(rs.getString("maNCC") + " - " + rs.getString("tenNCC"));
                }
            }

            String sqlPT = "SELECT maPhuTung, tenPhuTung, soLuongTon, donGiaNhap FROM PhuTung";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlPT)) {
                while (rs.next()) {
                    String maPT = rs.getString("maPhuTung");
                    String tenPT = rs.getString("tenPhuTung");
                    int ton = rs.getInt("soLuongTon");
                    long gia = Math.round(rs.getDouble("donGiaNhap"));
                    cboPT.addItem(maPT + " - " + tenPT + " - Tồn: " + ton + " - Giá: " + gia);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void themMon() {
        if (cboPT.getSelectedItem() == null) return;
        try {
            String sel = cboPT.getSelectedItem().toString();
            int firstDash = sel.indexOf(" - ");
            int tonIndex = sel.lastIndexOf(" - Tồn: ");
            int giaIndex = sel.lastIndexOf(" - Giá: ");

            String maPT = sel.substring(0, firstDash).trim();
            String tenPT = sel.substring(firstDash + 3, tonIndex).trim();
            int tonTruoc = Integer.parseInt(sel.substring(tonIndex + 8, giaIndex).trim());

            int sl = (int) spnSL.getValue();
            String rawGia = txtGia.getText().trim().replaceAll("[^0-9]", "");
            if (rawGia.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đơn giá hợp lệ!");
                return;
            }
            double gia = Double.parseDouble(rawGia);
            int tonSau = tonTruoc + sl;
            double tt = sl * gia;

            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.getValueAt(i, 0).toString().equals(maPT)) {
                    int oldSL = Integer.parseInt(model.getValueAt(i, 2).toString());
                    int newSL = oldSL + sl;
                    model.setValueAt(newSL, i, 2);
                    model.setValueAt(df.format(gia), i, 3);
                    model.setValueAt(df.format(newSL * gia), i, 4);
                    model.setValueAt(tonTruoc + newSL, i, 6);
                    tinhTong();
                    return;
                }
            }

            model.addRow(new Object[]{maPT, tenPT, sl, df.format(gia), df.format(tt), tonTruoc, tonSau});
            tinhTong();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Dữ liệu nhập không hợp lệ: " + ex.getMessage());
        }
    }

    private void tinhTong() {
        tongTien = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            String val = model.getValueAt(i, 4).toString().replaceAll("[^0-9]", "");
            tongTien += val.isEmpty() ? 0 : Double.parseDouble(val);
        }
        lblTongLo.setText("Tổng lô hàng: " + df.format(tongTien));
    }

    private void luuPhieuNhapVaXuatPDF() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Lô hàng trống! Vui lòng bấm '+ Thêm vào lô' trước.");
            return;
        }
        if (cboNCC.getSelectedItem() == null) return;

        String selNCC = cboNCC.getSelectedItem().toString();
        String maNCC = selNCC.substring(0, selNCC.indexOf(" - ")).trim();
        String tenNCC = selNCC.substring(selNCC.indexOf(" - ") + 3).trim();
        String ghiChu = txtGhiChu.getText().trim();

        // Lưu lại danh sách các dòng trước khi xóa bảng để in ra PDF
        List<String[]> danhSachIn = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            danhSachIn.add(new String[]{
                model.getValueAt(i, 0).toString(),
                model.getValueAt(i, 1).toString(),
                model.getValueAt(i, 2).toString(),
                model.getValueAt(i, 3).toString(),
                model.getValueAt(i, 4).toString()
            });
        }
        double tongTienIn = this.tongTien;

        Connection conn = null;
        try {
            conn = ConnectDB.getConnection();
            conn.setAutoCommit(false);

            String maPN = "PN" + (System.currentTimeMillis() % 1000000);
            String maNVKho = "NV26004"; // Phạm Thu Dung - Nhân viên kho

            // 1. Lưu phiếu nhập kho vào CSDL
            String sqlPN = "INSERT INTO PhieuNhapKho(maPhieuNhap, maNCC, maNVKho, ngayNhap) VALUES(?,?,?,GETDATE())";
            try (PreparedStatement ps = conn.prepareStatement(sqlPN)) {
                ps.setString(1, maPN);
                ps.setString(2, maNCC);
                ps.setString(3, maNVKho);
                ps.executeUpdate();
            }

            // 2. Lưu chi tiết & Cập nhật tồn kho
            String sqlCT = "INSERT INTO ChiTietNhapKho(maPhieuNhap, maPhuTung, soLuongNhap, donGiaNhap) VALUES(?,?,?,?)";
            String sqlUpd = "UPDATE PhuTung SET soLuongTon = soLuongTon + ?, donGiaNhap = ? WHERE maPhuTung = ?";

            try (PreparedStatement psCT = conn.prepareStatement(sqlCT);
                 PreparedStatement psUpd = conn.prepareStatement(sqlUpd)) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    String maPT = model.getValueAt(i, 0).toString();
                    int sl = Integer.parseInt(model.getValueAt(i, 2).toString());
                    String dgStr = model.getValueAt(i, 3).toString().replaceAll("[^0-9]", "");
                    double dg = Double.parseDouble(dgStr);

                    psCT.setString(1, maPN);
                    psCT.setString(2, maPT);
                    psCT.setInt(3, sl);
                    psCT.setDouble(4, dg);
                    psCT.addBatch();

                    psUpd.setInt(1, sl);
                    psUpd.setDouble(2, dg);
                    psUpd.setString(3, maPT);
                    psUpd.addBatch();
                }
                psCT.executeBatch();
                psUpd.executeBatch();
            }

            conn.commit();

            // Làm mới giao diện
            model.setRowCount(0);
            txtGhiChu.setText("");
            tinhTong();
            loadData();

            // Hỏi xuất Hóa đơn PDF
            int chon = JOptionPane.showConfirmDialog(
                this,
                "Đã lưu Phiếu Nhập Kho (" + maPN + ") vào CSDL thành công!\nBạn có muốn xuất Hóa Đơn Nhập Kho ra file PDF ngay không?",
                "Nhập kho thành công",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );

            if (chon == JOptionPane.YES_OPTION) {
                xuatHoaDonPDF(maPN, tenNCC, maNVKho + " - Phạm Thu Dung", ghiChu, danhSachIn, tongTienIn);
            }

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException e1) { e1.printStackTrace(); }
            }
            JOptionPane.showMessageDialog(this, "Lỗi nhập kho (Đã Rollback): " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Xuất Hóa đơn Nhập Kho ra file PDF chuẩn (Sử dụng PrinterJob tích hợp sẵn của Java + Microsoft Print to PDF)
     * Không cần thư viện ngoài (như iText), đảm bảo không bao giờ lỗi thiếu thư viện trên máy bất kỳ thành viên nào.
     */
    private void xuatHoaDonPDF(String maPN, String tenNCC, String nhanVien, String ghiChu, List<String[]> items, double tongCong) {
        try {
            // Tự động tạo thư mục "hoa_don" nằm ngay trong thư mục gốc của project GaraTech
            java.io.File folder = new java.io.File("hoa_don");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Đường dẫn file hóa đơn (dạng HTML chuẩn in ấn, mở trực tiếp trên trình duyệt để In/Lưu PDF 1 click)
            java.io.File file = new java.io.File(folder, "HoaDonNhapKho_" + maPN + ".html");
            String ngayLap = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset='UTF-8'><title>Hóa đơn ").append(maPN).append("</title>");
            sb.append("<style>");
            sb.append("body { font-family: Arial, sans-serif; padding: 30px; max-width: 800px; margin: auto; }");
            sb.append("h2, h1 { text-align: center; margin: 5px 0; }");
            sb.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
            sb.append("th, td { border: 1px solid #333; padding: 10px; text-align: left; }");
            sb.append("th { background-color: #f2f2f2; }");
            sb.append(".total { text-align: right; font-size: 18px; font-weight: bold; color: #d9534f; margin-top: 15px; }");
            sb.append(".sign { display: flex; justify-content: space-between; margin-top: 40px; text-align: center; }");
            sb.append("@media print { .no-print { display: none; } }");
            sb.append("</style></head><body onload='window.print()'>");

            sb.append("<div class='no-print' style='text-align:right; margin-bottom:15px;'>");
            sb.append("<button onclick='window.print()' style='padding:8px 16px; background:#16a34a; color:#fff; border:none; border-radius:4px; cursor:pointer; font-weight:bold;'>In / Lưu thành PDF</button>");
            sb.append("</div>");

            sb.append("<h2>HỆ THỐNG QUẢN LÝ GARA Ô TÔ - GARATECH</h2>");
            sb.append("<h1>HÓA ĐƠN NHẬP KHO PHỤ TÙNG</h1><hr>");
            sb.append("<p><b>Mã phiếu nhập:</b> ").append(maPN).append(" &nbsp;&nbsp;|&nbsp;&nbsp; <b>Ngày lập:</b> ").append(ngayLap).append("</p>");
            sb.append("<p><b>Nhà cung cấp:</b> ").append(tenNCC).append("</p>");
            sb.append("<p><b>Nhân viên kho:</b> ").append(nhanVien).append("</p>");
            if (!ghiChu.isEmpty()) {
                sb.append("<p><b>Ghi chú:</b> ").append(ghiChu).append("</p>");
            }

            sb.append("<table><thead><tr><th>Mã PT</th><th>Tên phụ tùng</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>");
            for (String[] r : items) {
                sb.append("<tr><td>").append(r[0]).append("</td><td>").append(r[1]).append("</td><td>")
                  .append(r[2]).append("</td><td>").append(r[3]).append("</td><td>").append(r[4]).append("</td></tr>");
            }
            sb.append("</tbody></table>");
            sb.append("<div class='total'>TỔNG TIỀN THANH TOÁN: ").append(df.format(tongCong)).append("</div>");

            sb.append("<div class='sign'>");
            sb.append("<div><b>Đại diện Nhà cung cấp</b><br><i>(Ký, ghi rõ họ tên)</i></div>");
            sb.append("<div><b>Nhân viên lập phiếu</b><br><i>(Ký, ghi rõ họ tên)</i><br><br><br><b>Phạm Thu Dung</b></div>");
            sb.append("</div></body></html>");

            java.nio.file.Files.writeString(file.toPath(), sb.toString(), java.nio.charset.StandardCharsets.UTF_8);

            // Thông báo đường dẫn chính xác và tự động mở hóa đơn lên màn hình
            JOptionPane.showMessageDialog(this, "Đã lưu hóa đơn tại:\n" + file.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất hóa đơn: " + ex.getMessage());
        }
    }
}
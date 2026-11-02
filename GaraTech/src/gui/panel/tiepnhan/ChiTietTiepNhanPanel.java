package gui.panel.tiepnhan;

import bus.TiepNhanXeBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuTiepNhanDao.PhieuTiepNhanRow;
import gui.Navigator;
import gui.Screen;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình Chi tiết phiếu tiếp nhận (Figma Frame 9:10).
 * Hiển thị toàn bộ thông tin tiếp nhận, bảng đối chiếu hiện trạng - yêu cầu,
 * và thanh hành động xử lý 3 trạng thái: Chờ khách hàng xác nhận, Đã xác nhận, Đã hủy.
 */
public final class ChiTietTiepNhanPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private final Runnable onBackCallback;
    private PhieuTiepNhanRow currentTicket = null;

    private final JLabel lblHeaderTitle = UI.h2("Thông tin tiếp nhận");
    private final JLabel lblStatusBadge = new JLabel();

    // Contact & Info fields
    private final JTextField txtKhachHang = new JTextField();
    private final JTextField txtPhuongTien = new JTextField();
    private final JTextField txtThoiGianTiepNhan = new JTextField();
    private final JTextField txtThoiGianHenTra = new JTextField();
    private final JTextField txtOdoNhienLieu = new JTextField();
    private final JTextField txtNhanVien = new JTextField();

    // Table đối chiếu
    private final DefaultTableModel compareModel = UI.readOnlyModel("Nội dung", "Thông tin ghi nhận");
    private final JTable compareTable = UI.table(compareModel);

    // Actions (3 trạng thái)
    private final JButton btnConfirmTicket = UI.primaryButton("✓ Khách đã xác nhận");
    private final JButton btnCreatePsc = UI.primaryButton("Lập phiếu sửa chữa ↗");
    private final JButton btnCancelTicket = UI.dangerButton("Hủy phiếu");
    private final JButton btnBack = UI.secondaryButton("Quay lại danh sách");

    public ChiTietTiepNhanPanel(Runnable onBackCallback) {
        this.onBackCallback = onBackCallback;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
        initEvents();
    }

    private void buildUI() {
        // 1. Card Thông tin tiếp nhận
        RoundedPanel cardInfo = new RoundedPanel(16, Theme.CARD);
        cardInfo.setLayout(new BorderLayout(0, 16));
        cardInfo.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(lblHeaderTitle, BorderLayout.WEST);
        headerPanel.add(lblStatusBadge, BorderLayout.EAST);
        cardInfo.add(headerPanel, BorderLayout.NORTH);

        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 24, 12));
        infoGrid.setOpaque(false);

        infoGrid.add(buildReadonlyField("Khách hàng", txtKhachHang));
        infoGrid.add(buildReadonlyField("Phương tiện", txtPhuongTien));
        infoGrid.add(buildReadonlyField("Thời gian tiếp nhận", txtThoiGianTiepNhan));
        infoGrid.add(buildReadonlyField("Hẹn trả xe", txtThoiGianHenTra));
        infoGrid.add(buildReadonlyField("ODO / Mức nhiên liệu", txtOdoNhienLieu));
        infoGrid.add(buildReadonlyField("Nhân viên tiếp nhận", txtNhanVien));

        cardInfo.add(infoGrid, BorderLayout.CENTER);
        cardInfo.setAlignmentX(LEFT_ALIGNMENT);
        add(cardInfo);

        add(Box.createVerticalStrut(18));

        // 2. Card Hiện trạng và yêu cầu
        RoundedPanel cardReq = new RoundedPanel(16, Theme.CARD);
        cardReq.setLayout(new BorderLayout(0, 14));
        cardReq.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        cardReq.add(UI.h2("Hiện trạng và yêu cầu"), BorderLayout.NORTH);

        compareTable.setRowHeight(48);
        compareTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        compareTable.getColumnModel().getColumn(1).setPreferredWidth(600);

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < 2; i++) {
            compareTable.getColumnModel().getColumn(i).setCellRenderer(r);
        }

        TableCellRenderer headerRenderer = compareTable.getTableHeader().getDefaultRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer headR) {
            headR.setHorizontalAlignment(SwingConstants.LEFT);
            headR.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        JPanel tableBlock = UI.tableBlock(compareTable);
        cardReq.add(tableBlock, BorderLayout.CENTER);
        cardReq.setAlignmentX(LEFT_ALIGNMENT);
        add(cardReq);

        add(Box.createVerticalStrut(20));

        // 3. Actions Row
        JPanel botActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        botActions.setOpaque(false);
        botActions.add(btnConfirmTicket);
        botActions.add(btnCreatePsc);
        botActions.add(btnCancelTicket);
        botActions.add(btnBack);
        botActions.setAlignmentX(LEFT_ALIGNMENT);
        add(botActions);
    }

    private void initEvents() {
        btnBack.addActionListener(e -> {
            if (onBackCallback != null) onBackCallback.run();
        });

        // Khách xác nhận: Chờ khách hàng xác nhận -> Đã xác nhận
        btnConfirmTicket.addActionListener(e -> {
            if (currentTicket == null) return;
            if (!Msg.confirm(this, "Khách hàng đã đồng ý xác nhận phương án sửa chữa cho xe " + currentTicket.bienSo() + "?")) {
                return;
            }
            try {
                TiepNhanXeBus.xacNhanPhieuTiepNhan(currentTicket.maPhieuTN());
                Msg.info(this, "Đã cập nhật trạng thái phiếu sang 'Đã xác nhận' thành công.");
                reloadTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });

        // Lập phiếu sửa chữa: Chuyển sang màn hình PhieuSuaChua
        btnCreatePsc.addActionListener(e -> {
            Navigator.go(Screen.PHIEU_SUA_CHUA);
        });

        // Hủy phiếu
        btnCancelTicket.addActionListener(e -> {
            if (currentTicket == null) return;
            if (!Msg.confirm(this, "Bạn có chắc chắn muốn hủy phiếu tiếp nhận " + currentTicket.maPhieuTN() + "?")) {
                return;
            }
            try {
                TiepNhanXeBus.huyPhieuTiepNhan(currentTicket.maPhieuTN());
                Msg.info(this, "Đã hủy phiếu tiếp nhận " + currentTicket.maPhieuTN() + " thành công.");
                reloadTicket();
            } catch (Exception ex) {
                Msg.error(this, ex.getMessage());
            }
        });
    }

    private void reloadTicket() {
        if (currentTicket != null) {
            PhieuTiepNhanRow updated = TiepNhanXeBus.layChiTietPhieu(currentTicket.maPhieuTN());
            displayTicket(updated != null ? updated : currentTicket);
        }
    }

    public void displayTicket(PhieuTiepNhanRow r) {
        this.currentTicket = r;
        if (r == null) return;

        lblHeaderTitle.setText("Thông tin tiếp nhận · " + r.maPhieuTN());
        lblStatusBadge.setText(formatStatusBadge(r.trangThai()));

        txtKhachHang.setText(String.format("%s · SĐT: %s", r.tenKhachHang(), r.soDienThoai()));
        txtPhuongTien.setText(String.format("%s · %s", r.bienSo(), r.thongTinXe()));
        txtThoiGianTiepNhan.setText(r.thoiGianTiepNhan() != null ? r.thoiGianTiepNhan().format(DT_FMT) : "---");
        txtThoiGianHenTra.setText(r.thoiGianHenTra() != null ? r.thoiGianHenTra().format(DT_FMT) : "Chưa hẹn");

        String fuelStr = String.format("%.0f%% bình", r.mucNhienLieu() * 100);
        txtOdoNhienLieu.setText(String.format("%,.0f km · %s", r.soKmTiepNhan(), fuelStr));
        txtNhanVien.setText(r.tenNhanVien() != null ? r.tenNhanVien() : r.maNV());

        // Bảng đối chiếu
        compareModel.setRowCount(0);
        compareModel.addRow(new Object[]{
                "<html><b>Yêu cầu của khách</b></html>",
                r.yeuCauKhachHang() != null ? r.yeuCauKhachHang() : "---"
        });
        compareModel.addRow(new Object[]{
                "<html><b>Hiện trạng bàn giao</b></html>",
                r.tinhTrangBanDau() != null ? r.tinhTrangBanDau() : "---"
        });

        // Ẩn toàn bộ nút hành động
        btnConfirmTicket.setVisible(false);
        btnCreatePsc.setVisible(false);
        btnCancelTicket.setVisible(false);

        // Hiển thị nút tương ứng với 3 trạng thái
        String st = r.trangThai() != null ? r.trangThai().trim() : "";
        if ("Đã xác nhận".equalsIgnoreCase(st)) {
            btnCreatePsc.setVisible(true);
        } else if ("Đã hủy".equalsIgnoreCase(st)) {
            // Không có nút thao tác nào ngoài "Quay lại danh sách"
        } else {
            // Chờ khách hàng xác nhận (hoặc Chờ khách xác nhận)
            btnConfirmTicket.setVisible(true);
            btnCancelTicket.setVisible(true);
        }
    }

    private String formatStatusBadge(String status) {
        if (status == null) status = "Chờ khách hàng xác nhận";
        String color = switch (status) {
            case "Đã xác nhận" -> "#027A48"; // Xanh lá
            case "Đã hủy" -> "#B42318";     // Đỏ
            default -> "#B45309";            // Cam ấm (Chờ khách hàng xác nhận)
        };
        return "<html><span style='color:" + color + "; font-size:14px; font-weight:bold'>● " + status + "</span></html>";
    }

    private JPanel buildReadonlyField(String label, JTextField tf) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        tf.setFont(Theme.font(Font.PLAIN, 13));
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setForeground(Theme.TEXT);
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        tf.setPreferredSize(new Dimension(200, 38));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        p.add(tf);

        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }
}

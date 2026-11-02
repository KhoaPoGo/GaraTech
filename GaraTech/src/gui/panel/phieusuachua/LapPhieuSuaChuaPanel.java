package gui.panel.phieusuachua;

import bus.BusinessException;
import bus.PhieuSuaChuaBus;
import com.formdev.flatlaf.FlatClientProperties;
import dao.PhieuSuaChuaDao.PhieuSuaChuaRow;
import dao.PhieuTiepNhanDao.PhieuTiepNhanRow;
import entity.DichVu;
import entity.HangMucSuaChua;
import entity.PhieuSuaChua;
import entity.PhuTung;
import enums.User;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.Fmt;
import util.RoundedPanel;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Lập phiếu sửa chữa & Lên bảng báo giá (SV2 - Tiếp nhận).
 * Không sử dụng các thành phần rườm rà (không header phụ, không thẻ xe minh họa,
 * không dropdown 50+ item, không checkbox thừa).
 */
public final class LapPhieuSuaChuaPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PhieuSuaChuaBus bus = new PhieuSuaChuaBus();
    private final Runnable onToListCallback;
    private final Consumer<String> onTicketCreatedCallback;

    // Thông tin phiếu tiếp nhận liên kết
    private PhieuTiepNhanRow selectedTicket = null;
    private final JTextField txtMaPhieuTN = new JTextField();
    private final JTextField txtBienSo = new JTextField();
    private final JTextField txtKhachHang = new JTextField();
    private final JTextField txtSoDienThoai = new JTextField();
    private final JTextField txtThongTinXe = new JTextField();
    private final JTextField txtOdo = new JTextField();
    private final JTextArea txaYeuCauKhach = new JTextArea(2, 20);
    private final JButton btnTraCuuPTN = UI.primaryButton("🔍 Chọn phiếu tiếp nhận");

    // Bảng hạng mục sửa chữa
    private final String[] COL_NAMES = {
            "STT", "Loại", "Mã", "Tên hạng mục", "ĐVT", "Số lượng", "Đơn giá", "Giảm giá", "Thành tiền", "Ghi chú"
    };
    private final DefaultTableModel itemModel = new DefaultTableModel(COL_NAMES, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 5 || col == 7 || col == 9; // SL, Giảm giá, Ghi chú
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 0 -> Integer.class;
                case 5 -> Integer.class;
                case 6, 7, 8 -> Double.class;
                default -> String.class;
            };
        }
    };
    private final JTable itemTable = new JTable(itemModel);
    private final List<HangMucSuaChua> currentItems = new ArrayList<>();

    // Các trường thời gian & ghi chú kỹ thuật
    private final JTextField txtThoiGianDuKien = new JTextField();
    private final JTextArea txaGhiChuKyThuat = new JTextArea(3, 20);
    private final JTextField txtNhanVienLap = new JTextField();

    // Thẻ tóm tắt chi phí
    private final JLabel lblSumMaPTN = new JLabel("---");
    private final JLabel lblSumKhachHang = new JLabel("---");
    private final JLabel lblSumBienSo = new JLabel("---");
    private final JLabel lblTienDichVu = new JLabel("0 đ");
    private final JLabel lblTienPhuTung = new JLabel("0 đ");
    private final JLabel lblTongGiamGia = new JLabel("0 đ");
    private final JLabel lblTongChiPhi = new JLabel("0 đ");

    private final JButton btnSaveDraft = UI.secondaryButton("💾 Lưu phiếu (Chờ khách duyệt)");
    private final JButton btnConfirmSave = UI.primaryButton("✓ Khách duyệt & Xác nhận sửa");
    private final JButton btnReset = UI.secondaryButton("🔄 Làm mới form");

    public LapPhieuSuaChuaPanel(Runnable onToListCallback, Consumer<String> onTicketCreatedCallback) {
        this.onToListCallback = onToListCallback;
        this.onTicketCreatedCallback = onTicketCreatedCallback;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        buildUI();
        initEvents();
        resetForm();
    }

    private void buildUI() {
        JPanel mainSplit = new JPanel(new BorderLayout(24, 0));
        mainSplit.setOpaque(false);

        // CỘT TRÁI: Dữ liệu chính
        JPanel leftCol = new JPanel();
        leftCol.setOpaque(false);
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));

        // Khối 1: Thông tin tiếp nhận xe liên kết
        RoundedPanel cardTicket = new RoundedPanel(16, Theme.CARD);
        cardTicket.setLayout(new BorderLayout(0, 14));
        cardTicket.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel headTicket = new JPanel(new BorderLayout());
        headTicket.setOpaque(false);
        JLabel lblCard1 = UI.h2("Thông tin tiếp nhận & phương tiện");
        headTicket.add(lblCard1, BorderLayout.WEST);
        headTicket.add(btnTraCuuPTN, BorderLayout.EAST);
        cardTicket.add(headTicket, BorderLayout.NORTH);

        JPanel pTicketFields = new JPanel(new GridLayout(3, 2, 16, 10));
        pTicketFields.setOpaque(false);

        setupReadonlyField(txtMaPhieuTN);
        setupReadonlyField(txtBienSo);
        setupReadonlyField(txtKhachHang);
        setupReadonlyField(txtSoDienThoai);
        setupReadonlyField(txtThongTinXe);
        setupReadonlyField(txtOdo);

        pTicketFields.add(formFieldWrap("Mã tiếp nhận", txtMaPhieuTN));
        pTicketFields.add(formFieldWrap("Biển số xe", txtBienSo));
        pTicketFields.add(formFieldWrap("Chủ phương tiện", txtKhachHang));
        pTicketFields.add(formFieldWrap("Số điện thoại liên hệ", txtSoDienThoai));
        pTicketFields.add(formFieldWrap("Thông số xe", txtThongTinXe));
        pTicketFields.add(formFieldWrap("Số km tiếp nhận (ODO)", txtOdo));

        JPanel pYeuCau = new JPanel(new BorderLayout(0, 6));
        pYeuCau.setOpaque(false);
        JLabel lblYc = createLabel("Yêu cầu ban đầu của khách & tình trạng xe");
        txaYeuCauKhach.setEditable(false);
        txaYeuCauKhach.setLineWrap(true);
        txaYeuCauKhach.setWrapStyleWord(true);
        txaYeuCauKhach.setBackground(new Color(0xF8FAFC));
        JScrollPane scrYc = new JScrollPane(txaYeuCauKhach);
        scrYc.setPreferredSize(new Dimension(100, 50));
        pYeuCau.add(lblYc, BorderLayout.NORTH);
        pYeuCau.add(scrYc, BorderLayout.CENTER);

        JPanel pBodyCard1 = new JPanel(new BorderLayout(0, 10));
        pBodyCard1.setOpaque(false);
        pBodyCard1.add(pTicketFields, BorderLayout.NORTH);
        pBodyCard1.add(pYeuCau, BorderLayout.CENTER);
        cardTicket.add(pBodyCard1, BorderLayout.CENTER);

        leftCol.add(cardTicket);
        leftCol.add(Box.createVerticalStrut(18));

        // Khối 2: Bảng Hạng mục Sửa chữa & Báo giá
        RoundedPanel cardItems = new RoundedPanel(16, Theme.CARD);
        cardItems.setLayout(new BorderLayout(0, 14));
        cardItems.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel headItems = new JPanel(new BorderLayout());
        headItems.setOpaque(false);
        headItems.add(UI.h2("Hạng mục công việc & Phụ tùng thay thế"), BorderLayout.WEST);

        JPanel pItemBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pItemBtns.setOpaque(false);
        JButton btnAddDichVu = UI.primaryButton("+ Thêm dịch vụ");
        JButton btnAddPhuTung = UI.primaryButton("+ Thêm phụ tùng");
        JButton btnDelItem = UI.dangerButton("Xóa dòng");
        pItemBtns.add(btnAddDichVu);
        pItemBtns.add(btnAddPhuTung);
        pItemBtns.add(btnDelItem);
        headItems.add(pItemBtns, BorderLayout.EAST);
        cardItems.add(headItems, BorderLayout.NORTH);

        // Cấu hình bảng
        setupItemTable();
        JScrollPane scrTable = new JScrollPane(itemTable);
        scrTable.setPreferredSize(new Dimension(100, 240));
        cardItems.add(scrTable, BorderLayout.CENTER);

        // Khối thời gian dự kiến & Ghi chú kỹ thuật
        JPanel pBotDetails = new JPanel(new GridLayout(1, 2, 16, 0));
        pBotDetails.setOpaque(false);

        txtThoiGianDuKien.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/MM/yyyy HH:mm");
        JPanel pTg = formFieldWrap("Dự kiến hoàn thành (dd/MM/yyyy HH:mm) *", txtThoiGianDuKien);

        txaGhiChuKyThuat.setLineWrap(true);
        txaGhiChuKyThuat.setWrapStyleWord(true);
        JScrollPane scrGhiChu = new JScrollPane(txaGhiChuKyThuat);
        scrGhiChu.setPreferredSize(new Dimension(100, 54));
        JPanel pGc = new JPanel(new BorderLayout(0, 6));
        pGc.setOpaque(false);
        pGc.add(createLabel("Ghi chú kỹ thuật cho thợ"), BorderLayout.NORTH);
        pGc.add(scrGhiChu, BorderLayout.CENTER);

        pBotDetails.add(pTg);
        pBotDetails.add(pGc);
        cardItems.add(pBotDetails, BorderLayout.SOUTH);

        leftCol.add(cardItems);
        mainSplit.add(leftCol, BorderLayout.CENTER);

        // CỘT PHẢI: Thẻ Tóm tắt chi phí & Nút hành động
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setPreferredSize(new Dimension(340, 10));

        RoundedPanel cardSummary = new RoundedPanel(16, Theme.CARD);
        cardSummary.setLayout(new BoxLayout(cardSummary, BoxLayout.Y_AXIS));
        cardSummary.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JLabel lblSumTitle = UI.h2("Tóm tắt báo giá");
        lblSumTitle.setAlignmentX(LEFT_ALIGNMENT);
        cardSummary.add(lblSumTitle);
        cardSummary.add(Box.createVerticalStrut(14));

        cardSummary.add(buildSummaryItem("Phiếu tiếp nhận", lblSumMaPTN));
        cardSummary.add(Box.createVerticalStrut(10));
        cardSummary.add(buildSummaryItem("Khách hàng", lblSumKhachHang));
        cardSummary.add(Box.createVerticalStrut(10));
        cardSummary.add(buildSummaryItem("Biển số xe", lblSumBienSo));
        cardSummary.add(Box.createVerticalStrut(16));

        // Phân cách
        JPanel line = new JPanel();
        line.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        line.setBackground(new Color(0xE2E8F0));
        cardSummary.add(line);
        cardSummary.add(Box.createVerticalStrut(16));

        cardSummary.add(buildSummaryItem("Tiền dịch vụ", lblTienDichVu));
        cardSummary.add(Box.createVerticalStrut(10));
        cardSummary.add(buildSummaryItem("Tiền phụ tùng", lblTienPhuTung));
        cardSummary.add(Box.createVerticalStrut(10));
        cardSummary.add(buildSummaryItem("Tổng giảm giá", lblTongGiamGia));
        cardSummary.add(Box.createVerticalStrut(16));

        // Tổng chi phí dự kiến
        RoundedPanel pTotalBox = new RoundedPanel(12, new Color(0xEFF6FF));
        pTotalBox.setLayout(new BorderLayout(8, 6));
        pTotalBox.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel lblTotalTitle = new JLabel("TỔNG CHI PHÍ DỰ KIẾN");
        lblTotalTitle.setFont(Theme.font(11, Font.BOLD));
        lblTotalTitle.setForeground(new Color(0x1E40AF));

        lblTongChiPhi.setFont(Theme.font(20, Font.BOLD));
        lblTongChiPhi.setForeground(new Color(0x1E3A8A));
        lblTongChiPhi.setHorizontalAlignment(SwingConstants.RIGHT);

        pTotalBox.add(lblTotalTitle, BorderLayout.NORTH);
        pTotalBox.add(lblTongChiPhi, BorderLayout.CENTER);
        pTotalBox.setAlignmentX(LEFT_ALIGNMENT);
        cardSummary.add(pTotalBox);
        cardSummary.add(Box.createVerticalStrut(22));

        // Nút hành động
        btnConfirmSave.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        btnConfirmSave.setAlignmentX(LEFT_ALIGNMENT);
        cardSummary.add(btnConfirmSave);
        cardSummary.add(Box.createVerticalStrut(10));

        btnSaveDraft.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        btnSaveDraft.setAlignmentX(LEFT_ALIGNMENT);
        cardSummary.add(btnSaveDraft);
        cardSummary.add(Box.createVerticalStrut(10));

        btnReset.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        btnReset.setAlignmentX(LEFT_ALIGNMENT);
        cardSummary.add(btnReset);

        rightCol.add(cardSummary);
        mainSplit.add(rightCol, BorderLayout.EAST);

        add(mainSplit, BorderLayout.CENTER);

        // Events cho nút thêm/xóa hạng mục
        btnAddDichVu.addActionListener(e -> openChonDichVuDialog());
        btnAddPhuTung.addActionListener(e -> openChonPhuTungDialog());
        btnDelItem.addActionListener(e -> removeItemRow());
    }

    private void setupItemTable() {
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setRowHeight(36);
        itemTable.setShowGrid(true);
        itemTable.setGridColor(new Color(0xF1F5F9));

        itemTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // STT
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(90);  // Loại
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Mã
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(210); // Tên
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // ĐVT
        itemTable.getColumnModel().getColumn(5).setPreferredWidth(75);  // SL
        itemTable.getColumnModel().getColumn(6).setPreferredWidth(105); // Đơn giá
        itemTable.getColumnModel().getColumn(7).setPreferredWidth(95);  // Giảm giá
        itemTable.getColumnModel().getColumn(8).setPreferredWidth(115); // Thành tiền
        itemTable.getColumnModel().getColumn(9).setPreferredWidth(140); // Ghi chú

        // Canh lề và định dạng tiền
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
        itemTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        itemTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
        itemTable.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        itemTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        // Lắng nghe chỉnh sửa ô số lượng, giảm giá
        itemModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && row < currentItems.size() && (col == 5 || col == 7 || col == 9)) {
                    HangMucSuaChua hm = currentItems.get(row);
                    try {
                        Object slVal = itemModel.getValueAt(row, 5);
                        int sl = slVal instanceof Number ? ((Number) slVal).intValue() : Integer.parseInt(slVal.toString().trim());
                        if (sl <= 0) sl = 1;
                        hm.setSoLuong(sl);

                        Object ggVal = itemModel.getValueAt(row, 7);
                        double gg = ggVal instanceof Number ? ((Number) ggVal).doubleValue() : Double.parseDouble(ggVal.toString().trim());
                        if (gg < 0) gg = 0;
                        if (gg > sl * hm.getDonGia()) gg = sl * hm.getDonGia();
                        hm.setMucGiamGia(gg);

                        Object gcVal = itemModel.getValueAt(row, 9);
                        hm.setGhiChu(gcVal != null ? gcVal.toString() : "");

                        hm.tinhThanhTien();

                        // Cập nhật lại cột Thành tiền mà không gây loop
                        itemModel.setValueAt(hm.getThanhTien(), row, 8);
                    } catch (Exception ignored) {
                    }
                    recalculateSummary();
                }
            }
        });
    }

    private void initEvents() {
        btnTraCuuPTN.addActionListener(e -> openTraCuuPhieuTiepNhanDialog());

        btnConfirmSave.addActionListener(e -> handleSubmit(true));
        btnSaveDraft.addActionListener(e -> handleSubmit(false));
        btnReset.addActionListener(e -> resetForm());
    }

    private void openTraCuuPhieuTiepNhanDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        TraCuuPhieuTiepNhanDialog dlg = new TraCuuPhieuTiepNhanDialog(owner, bus, this::setTicket);
        dlg.setVisible(true);
    }

    private void openChonDichVuDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        ChonDichVuDialog dlg = new ChonDichVuDialog(owner, bus, dv -> {
            // Kiểm tra trùng dịch vụ
            for (HangMucSuaChua hm : currentItems) {
                if ("DichVu".equalsIgnoreCase(hm.getLoaiHangMuc()) && dv.getMaDichVu().equals(hm.getMaDichVu())) {
                    Msg.info(this, "Dịch vụ '" + dv.getTenDichVu() + "' đã có trong bảng.");
                    return;
                }
            }
            HangMucSuaChua hm = new HangMucSuaChua();
            hm.setLoaiHangMuc("DichVu");
            hm.setMaDichVu(dv.getMaDichVu());
            hm.setTenHangMuc(dv.getTenDichVu());
            hm.setSoLuong(1);
            hm.setDonGia(dv.getTienCong());
            hm.setMucGiamGia(0);
            hm.tinhThanhTien();
            hm.setLaPhatSinh(false);

            addHangMucToTable(hm, "Lần");
        });
        dlg.setVisible(true);
    }

    private void openChonPhuTungDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        ChonPhuTungDialog dlg = new ChonPhuTungDialog(owner, bus, pt -> {
            for (HangMucSuaChua hm : currentItems) {
                if ("PhuTung".equalsIgnoreCase(hm.getLoaiHangMuc()) && pt.getMaPhuTung().equals(hm.getMaPhuTung())) {
                    Msg.info(this, "Phụ tùng '" + pt.getTenPhuTung() + "' đã có trong bảng.");
                    return;
                }
            }
            HangMucSuaChua hm = new HangMucSuaChua();
            hm.setLoaiHangMuc("PhuTung");
            hm.setMaPhuTung(pt.getMaPhuTung());
            hm.setTenHangMuc(pt.getTenPhuTung());
            hm.setSoLuong(1);
            hm.setDonGia(pt.getDonGiaBan());
            hm.setMucGiamGia(0);
            hm.tinhThanhTien();
            hm.setLaPhatSinh(false);

            addHangMucToTable(hm, pt.getDonViTinh());
        });
        dlg.setVisible(true);
    }

    private void addHangMucToTable(HangMucSuaChua hm, String dvt) {
        currentItems.add(hm);
        int stt = currentItems.size();
        String loaiStr = "DichVu".equalsIgnoreCase(hm.getLoaiHangMuc()) ? "Dịch vụ" : "Phụ tùng";
        String ma = hm.getMaDichVu() != null ? hm.getMaDichVu() : hm.getMaPhuTung();

        itemModel.addRow(new Object[]{
                stt,
                loaiStr,
                ma,
                hm.getTenHangMuc(),
                dvt != null ? dvt : "---",
                hm.getSoLuong(),
                hm.getDonGia(),
                hm.getMucGiamGia(),
                hm.getThanhTien(),
                hm.getGhiChu() != null ? hm.getGhiChu() : ""
        });

        recalculateSummary();
    }

    private void removeItemRow() {
        int sel = itemTable.getSelectedRow();
        if (sel < 0) {
            Msg.info(this, "Vui lòng chọn một dòng hạng mục để xóa.");
            return;
        }
        currentItems.remove(sel);
        itemModel.removeRow(sel);

        // Đánh lại số thứ tự
        for (int i = 0; i < itemModel.getRowCount(); i++) {
            itemModel.setValueAt(i + 1, i, 0);
        }

        recalculateSummary();
    }

    private void recalculateSummary() {
        PhieuSuaChuaBus.ChiPhiSummary sum = PhieuSuaChuaBus.tinhChiPhi(currentItems);
        lblTienDichVu.setText(Fmt.money((long) sum.tienDichVu()) + " đ");
        lblTienPhuTung.setText(Fmt.money((long) sum.tienPhuTung()) + " đ");
        lblTongGiamGia.setText(Fmt.money((long) sum.tongGiamGia()) + " đ");
        lblTongChiPhi.setText(Fmt.money((long) sum.tongThanhTien()) + " đ");
    }

    public void setTicket(PhieuTiepNhanRow row) {
        this.selectedTicket = row;
        if (row == null) {
            txtMaPhieuTN.setText("");
            txtBienSo.setText("");
            txtKhachHang.setText("");
            txtSoDienThoai.setText("");
            txtThongTinXe.setText("");
            txtOdo.setText("");
            txaYeuCauKhach.setText("");
            lblSumMaPTN.setText("---");
            lblSumKhachHang.setText("---");
            lblSumBienSo.setText("---");
            return;
        }

        txtMaPhieuTN.setText(row.maPhieuTN());
        txtBienSo.setText(row.bienSo());
        txtKhachHang.setText(row.tenKhachHang());
        txtSoDienThoai.setText(row.soDienThoai());
        txtThongTinXe.setText(row.thongTinXe());
        txtOdo.setText(String.format("%,.0f km", row.soKmTiepNhan()));
        txaYeuCauKhach.setText(row.yeuCauKhachHang() != null ? row.yeuCauKhachHang() : "");

        lblSumMaPTN.setText(row.maPhieuTN());
        lblSumKhachHang.setText(row.tenKhachHang());
        lblSumBienSo.setText(row.bienSo());

        // Đặt mặc định thời gian hẹn trả từ phiếu tiếp nhận nếu có
        if (row.thoiGianHenTra() != null) {
            txtThoiGianDuKien.setText(row.thoiGianHenTra().format(DT_FMT));
        } else {
            txtThoiGianDuKien.setText(LocalDateTime.now().plusHours(4).format(DT_FMT));
        }
    }

    private void handleSubmit(boolean khachDuyetNgay) {
        if (selectedTicket == null) {
            Msg.error(this, "Vui lòng chọn phiếu tiếp nhận cần lập sửa chữa.");
            return;
        }
        if (currentItems.isEmpty()) {
            Msg.error(this, "Vui lòng thêm ít nhất 1 hạng mục dịch vụ hoặc phụ tùng vào báo giá.");
            return;
        }

        String tgDkStr = txtThoiGianDuKien.getText().trim();
        LocalDateTime thoiGianDuKien = null;
        if (!tgDkStr.isBlank()) {
            try {
                thoiGianDuKien = LocalDateTime.parse(tgDkStr, DT_FMT);
            } catch (Exception ex) {
                Msg.error(this, "Định dạng thời gian dự kiến không hợp lệ. Vui lòng nhập theo mẫu dd/MM/yyyy HH:mm");
                txtThoiGianDuKien.requestFocus();
                return;
            }
        }

        User u = Session.user();
        String maNVLap = u != null ? u.maNV() : "NV26002";

        PhieuSuaChua psc = new PhieuSuaChua();
        psc.setMaPhieuTN(selectedTicket.maPhieuTN());
        psc.setMaNVLap(maNVLap);
        psc.setThoiGianDuKien(thoiGianDuKien);
        psc.setGhiChuKyThuat(txaGhiChuKyThuat.getText().trim());

        String prompt = khachDuyetNgay
                ? "Khách hàng đã đồng ý báo giá.\nXác nhận tạo phiếu sửa chữa và chuyển sang trạng thái 'Đã xác nhận'?"
                : "Tạo phiếu sửa chữa ở trạng thái 'Chờ khách duyệt'?";

        if (!Msg.confirm(this, prompt)) {
            return;
        }

        try {
            String maPsc = bus.lapPhieuSuaChua(psc, currentItems, khachDuyetNgay);
            Msg.info(this, "Lập phiếu sửa chữa " + maPsc + " thành công!");
            resetForm();
            if (onTicketCreatedCallback != null) {
                onTicketCreatedCallback.accept(maPsc);
            }
        } catch (BusinessException be) {
            Msg.error(this, be.getMessage());
        } catch (SQLException ex) {
            Msg.error(this, "Lỗi cơ sở dữ liệu: " + ex.getMessage());
        }
    }

    public void resetForm() {
        setTicket(null);
        currentItems.clear();
        itemModel.setRowCount(0);
        txtThoiGianDuKien.setText(LocalDateTime.now().plusHours(4).format(DT_FMT));
        txaGhiChuKyThuat.setText("");

        User u = Session.user();
        txtNhanVienLap.setText(u != null ? String.format("%s · %s", u.maNV(), u.hoTen()) : "NV26002 · Tiếp nhận");

        recalculateSummary();
    }

    private void setupReadonlyField(JTextField tf) {
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private JPanel formFieldWrap(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(createLabel(label), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSummaryItem(String label, JLabel lblValue) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel lblL = UI.muted(label);
        lblValue.setFont(Theme.font(13, Font.BOLD));
        lblValue.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(lblL, BorderLayout.WEST);
        p.add(lblValue, BorderLayout.CENTER);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    // =========================================================================
    // DIALOG TRA CỨU PHIẾU TIẾP NHẬN (EMBEDDED STATIC INNER CLASS TRÁNH LỖI ECLIPSE)
    // =========================================================================
    public static final class TraCuuPhieuTiepNhanDialog extends JDialog {
        private final PhieuSuaChuaBus bus;
        private final Consumer<PhieuTiepNhanRow> onSelectCallback;

        private final JTextField txtFilter = new JTextField();
        private final DefaultTableModel model = UI.readOnlyModel(
                "Mã PTN", "Biển số", "Khách hàng", "Số điện thoại", "Phương tiện", "Thời gian TN", "Tình trạng"
        );
        private final JTable table = UI.table(model);
        private List<PhieuTiepNhanRow> data = new ArrayList<>();

        public TraCuuPhieuTiepNhanDialog(Window owner, PhieuSuaChuaBus bus, Consumer<PhieuTiepNhanRow> onSelectCallback) {
            super(owner, "Chọn phiếu tiếp nhận để lập sửa chữa", ModalityType.APPLICATION_MODAL);
            this.bus = bus;
            this.onSelectCallback = onSelectCallback;

            setSize(880, 500);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 12));
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

            buildUI();
            loadData();
        }

        private void buildUI() {
            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);
            txtFilter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mã phiếu, biển số, tên khách hàng hoặc SĐT...");
            JButton btnSearch = UI.primaryButton("Tìm kiếm");
            top.add(txtFilter, BorderLayout.CENTER);
            top.add(btnSearch, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(32);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bot.setOpaque(false);
            JButton btnChoose = UI.primaryButton("Chọn phiếu này");
            JButton btnCancel = UI.secondaryButton("Đóng");
            bot.add(btnChoose, BorderLayout.WEST);
            bot.add(btnCancel, BorderLayout.EAST);
            add(bot, BorderLayout.SOUTH);

            btnSearch.addActionListener(e -> loadData());
            txtFilter.addActionListener(e -> loadData());

            btnChoose.addActionListener(e -> chooseSelected());
            btnCancel.addActionListener(e -> dispose());

            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        chooseSelected();
                    }
                }
            });
        }

        private void loadData() {
            try {
                data = bus.layDanhSachPhieuTiepNhanHopLe(txtFilter.getText().trim());
                model.setRowCount(0);
                for (PhieuTiepNhanRow r : data) {
                    model.addRow(new Object[]{
                            r.maPhieuTN(),
                            r.bienSo(),
                            r.tenKhachHang(),
                            r.soDienThoai(),
                            r.thongTinXe(),
                            r.thoiGianTiepNhan() != null ? r.thoiGianTiepNhan().format(DT_FMT) : "",
                            r.tinhTrangBanDau()
                    });
                }
            } catch (SQLException ex) {
                Msg.error(this, "Lỗi tải phiếu tiếp nhận: " + ex.getMessage());
            }
        }

        private void chooseSelected() {
            int sel = table.getSelectedRow();
            if (sel >= 0 && sel < data.size()) {
                if (onSelectCallback != null) {
                    onSelectCallback.accept(data.get(sel));
                }
                dispose();
            } else {
                Msg.info(this, "Vui lòng chọn một phiếu tiếp nhận từ bảng.");
            }
        }
    }

    // =========================================================================
    // DIALOG CHỌN DỊCH VỤ (EMBEDDED STATIC INNER CLASS)
    // =========================================================================
    public static final class ChonDichVuDialog extends JDialog {
        private final PhieuSuaChuaBus bus;
        private final Consumer<DichVu> onSelectCallback;

        private final JTextField txtFilter = new JTextField();
        private final DefaultTableModel model = UI.readOnlyModel("Mã DV", "Tên dịch vụ", "Cấp bảo dưỡng", "Đơn giá công", "Thời gian ước tính");
        private final JTable table = UI.table(model);
        private List<DichVu> allData = new ArrayList<>();
        private List<DichVu> filteredData = new ArrayList<>();

        public ChonDichVuDialog(Window owner, PhieuSuaChuaBus bus, Consumer<DichVu> onSelectCallback) {
            super(owner, "Danh mục Dịch vụ đang cung cấp", ModalityType.APPLICATION_MODAL);
            this.bus = bus;
            this.onSelectCallback = onSelectCallback;

            setSize(780, 480);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 12));
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

            buildUI();
            loadData();
        }

        private void buildUI() {
            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);
            txtFilter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm theo mã DV hoặc tên dịch vụ...");
            top.add(txtFilter, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(32);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bot.setOpaque(false);
            JButton btnSelect = UI.primaryButton("Chọn dịch vụ");
            JButton btnClose = UI.secondaryButton("Đóng");
            bot.add(btnSelect);
            bot.add(btnClose);
            add(bot, BorderLayout.SOUTH);

            txtFilter.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    applyFilter();
                }
            });

            btnSelect.addActionListener(e -> selectItem());
            btnClose.addActionListener(e -> dispose());
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) selectItem();
                }
            });
        }

        private void loadData() {
            try {
                allData = bus.layDanhSachDichVuHoatDong();
                applyFilter();
            } catch (SQLException ex) {
                Msg.error(this, "Lỗi tải dịch vụ: " + ex.getMessage());
            }
        }

        private void applyFilter() {
            String kw = txtFilter.getText().trim().toLowerCase();
            filteredData = allData.stream().filter(d ->
                    kw.isEmpty() || d.getMaDichVu().toLowerCase().contains(kw) || d.getTenDichVu().toLowerCase().contains(kw)
            ).toList();

            model.setRowCount(0);
            for (DichVu d : filteredData) {
                model.addRow(new Object[]{
                        d.getMaDichVu(),
                        d.getTenDichVu(),
                        d.getCapDoBaoDuong() != null ? d.getCapDoBaoDuong() : "---",
                        Fmt.money((long) d.getTienCong()) + " đ",
                        d.getThoiGianUocTinh() + " giờ"
                });
            }
        }

        private void selectItem() {
            int sel = table.getSelectedRow();
            if (sel >= 0 && sel < filteredData.size()) {
                if (onSelectCallback != null) onSelectCallback.accept(filteredData.get(sel));
                dispose();
            } else {
                Msg.info(this, "Vui lòng chọn một dịch vụ từ bảng.");
            }
        }
    }

    // =========================================================================
    // DIALOG CHỌN PHỤ TÙNG (EMBEDDED STATIC INNER CLASS)
    // =========================================================================
    public static final class ChonPhuTungDialog extends JDialog {
        private final PhieuSuaChuaBus bus;
        private final Consumer<PhuTung> onSelectCallback;

        private final JTextField txtFilter = new JTextField();
        private final DefaultTableModel model = UI.readOnlyModel("Mã PT", "Tên phụ tùng", "Loại", "ĐVT", "Đơn giá bán", "Tồn kho");
        private final JTable table = UI.table(model);
        private List<PhuTung> allData = new ArrayList<>();
        private List<PhuTung> filteredData = new ArrayList<>();

        public ChonPhuTungDialog(Window owner, PhieuSuaChuaBus bus, Consumer<PhuTung> onSelectCallback) {
            super(owner, "Danh mục Phụ tùng trong kho", ModalityType.APPLICATION_MODAL);
            this.bus = bus;
            this.onSelectCallback = onSelectCallback;

            setSize(800, 480);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 12));
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

            buildUI();
            loadData();
        }

        private void buildUI() {
            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);
            txtFilter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm theo mã PT, tên phụ tùng hoặc loại...");
            top.add(txtFilter, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(32);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bot.setOpaque(false);
            JButton btnSelect = UI.primaryButton("Chọn phụ tùng");
            JButton btnClose = UI.secondaryButton("Đóng");
            bot.add(btnSelect);
            bot.add(btnClose);
            add(bot, BorderLayout.SOUTH);

            txtFilter.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    applyFilter();
                }
            });

            btnSelect.addActionListener(e -> selectItem());
            btnClose.addActionListener(e -> dispose());
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) selectItem();
                }
            });
        }

        private void loadData() {
            try {
                allData = bus.layDanhSachPhuTungKhaDung();
                applyFilter();
            } catch (SQLException ex) {
                Msg.error(this, "Lỗi tải phụ tùng: " + ex.getMessage());
            }
        }

        private void applyFilter() {
            String kw = txtFilter.getText().trim().toLowerCase();
            filteredData = allData.stream().filter(p ->
                    kw.isEmpty() || p.getMaPhuTung().toLowerCase().contains(kw)
                            || p.getTenPhuTung().toLowerCase().contains(kw)
                            || p.getLoaiPhuTung().toLowerCase().contains(kw)
            ).toList();

            model.setRowCount(0);
            for (PhuTung p : filteredData) {
                model.addRow(new Object[]{
                        p.getMaPhuTung(),
                        p.getTenPhuTung(),
                        p.getLoaiPhuTung(),
                        p.getDonViTinh(),
                        Fmt.money(p.getDonGiaBan().longValue()) + " đ",
                        p.getSoLuongTon() > 0 ? p.getSoLuongTon() : "Hết hàng"
                });
            }
        }

        private void selectItem() {
            int sel = table.getSelectedRow();
            if (sel >= 0 && sel < filteredData.size()) {
                PhuTung pt = filteredData.get(sel);
                if (pt.getSoLuongTon() <= 0) {
                    if (!Msg.confirm(this, "Phụ tùng '" + pt.getTenPhuTung() + "' hiện đang hết hàng tồn kho.\nBạn vẫn muốn đưa vào báo giá để đặt hàng bổ sung?")) {
                        return;
                    }
                }
                if (onSelectCallback != null) onSelectCallback.accept(pt);
                dispose();
            } else {
                Msg.info(this, "Vui lòng chọn một phụ tùng từ bảng.");
            }
        }
    }
}

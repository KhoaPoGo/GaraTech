package gui.panel;

import bus.KhachHangXeBus;
import com.formdev.flatlaf.FlatClientProperties;
import entity.KhachHang;
import entity.Xe;
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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Form thêm xe cho khách hàng hiện có.
 */
public final class FormThemXePanel extends JPanel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Consumer<Xe> onSaved;
    private final Runnable onCancel;

    private KhachHang currentCustomer;

    // Header banner labels
    private final JLabel lblAddVehCustName = UI.h2("Khách hàng");
    private final JLabel lblAddVehCustSub = UI.muted("Mã KH · SĐT");
    private final JLabel lblAddVehPlatesCompare = UI.muted("Biển số đã có để đối chiếu: ---");
    private final JLabel lblAddVehPointsStat = UI.h2("0");
    private final JLabel lblAddVehCountStat = UI.h2("0 xe");

    // Form inputs
    private final JTextField txtAddVehPlate = new JTextField();
    private final JTextField txtAddVehMakeModel = new JTextField();
    private final JTextField txtAddVehYear = new JTextField();
    private final JTextField txtAddVehColor = new JTextField();
    private final JTextField txtAddVehOdo = new JTextField("0");
    private final JTextField txtAddVehRegDate = new JTextField();

    public FormThemXePanel(Consumer<Xe> onSaved, Runnable onCancel) {
        this.onSaved = Objects.requireNonNull(onSaved, "onSaved must not be null");
        this.onCancel = Objects.requireNonNull(onCancel, "onCancel must not be null");

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildUI();
    }

    private void buildUI() {
        // 1. Customer summary header banner card
        RoundedPanel banner = new RoundedPanel(14, Theme.CARD);
        banner.setLayout(new BorderLayout(20, 0));
        banner.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel bLeft = new JPanel();
        bLeft.setOpaque(false);
        bLeft.setLayout(new BoxLayout(bLeft, BoxLayout.Y_AXIS));
        bLeft.add(lblAddVehCustName);
        bLeft.add(Box.createVerticalStrut(4));
        bLeft.add(lblAddVehCustSub);
        bLeft.add(Box.createVerticalStrut(4));
        bLeft.add(lblAddVehPlatesCompare);
        banner.add(bLeft, BorderLayout.CENTER);

        JPanel bRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 0));
        bRight.setOpaque(false);

        JPanel statPoints = new JPanel(new BorderLayout(0, 2));
        statPoints.setOpaque(false);
        statPoints.add(lblAddVehPointsStat, BorderLayout.NORTH);
        statPoints.add(UI.muted("Điểm tích lũy"), BorderLayout.SOUTH);

        JPanel statCount = new JPanel(new BorderLayout(0, 2));
        statCount.setOpaque(false);
        statCount.add(lblAddVehCountStat, BorderLayout.NORTH);
        statCount.add(UI.muted("Đang thuộc khách hàng"), BorderLayout.SOUTH);

        bRight.add(statPoints);
        bRight.add(statCount);
        banner.add(bRight, BorderLayout.EAST);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        add(banner);

        add(Box.createVerticalStrut(18));

        // 2. Vehicle form card
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel formHead = new JPanel(new BorderLayout());
        formHead.setOpaque(false);
        formHead.add(UI.h2("Thông tin phương tiện"), BorderLayout.WEST);
        formHead.add(UI.muted("* Thông tin bắt buộc"), BorderLayout.EAST);
        card.add(formHead, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 2, 20, 16));
        grid.setOpaque(false);

        grid.add(formFieldInput("Biển số *", txtAddVehPlate, "Chữ in hoa; 6~12 ký tự; không trùng bất kỳ xe đã có."));
        grid.add(formFieldInput("Hãng / dòng xe *", txtAddVehMakeModel, "Nhập hãng / dòng xe (ví dụ: Kia / Seltos)"));

        grid.add(formFieldInput("Năm sản xuất *", txtAddVehYear, "Từ 1980 đến năm hiện tại"));
        grid.add(formFieldInput("Màu sơn *", txtAddVehColor, "Nhập màu sơn"));

        grid.add(formFieldInput("ODO hiện tại *", txtAddVehOdo, "Số km không âm"));
        grid.add(formFieldInput("Ngày đăng ký *", txtAddVehRegDate, "Không sau ngày hiện tại"));

        // Bottom Actions: Lưu xe | Hủy
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        bot.setOpaque(false);

        JButton btnSaveVeh = UI.primaryButton("Lưu xe");
        JButton btnCancelVeh = UI.secondaryButton("Hủy");

        btnSaveVeh.addActionListener(e -> handleSaveVehicle());
        btnCancelVeh.addActionListener(e -> onCancel.run());

        bot.add(btnSaveVeh);
        bot.add(btnCancelVeh);
        bot.setAlignmentX(LEFT_ALIGNMENT);

        JPanel vContent = new JPanel();
        vContent.setOpaque(false);
        vContent.setLayout(new BoxLayout(vContent, BoxLayout.Y_AXIS));
        vContent.add(grid);
        vContent.add(Box.createVerticalStrut(20));
        vContent.add(bot);

        card.add(vContent, BorderLayout.NORTH);
        card.setAlignmentX(LEFT_ALIGNMENT);
        add(card);
    }

    public void setCustomer(KhachHang kh, String pendingPlate) {
        this.currentCustomer = kh;
        if (kh == null) return;

        lblAddVehCustName.setText(kh.getHoTen());
        lblAddVehCustSub.setText(kh.getMaKH() + " · SĐT: " + kh.getSoDienThoai());

        List<Xe> existingVehicles = KhachHangXeBus.getDanhSachXe(kh.getMaKH());
        StringBuilder sbPlates = new StringBuilder("Biển số đã có để đối chiếu: ");
        if (existingVehicles.isEmpty()) {
            sbPlates.append("Chưa có xe nào");
        } else {
            for (int i = 0; i < existingVehicles.size(); i++) {
                sbPlates.append(existingVehicles.get(i).getBienSo());
                if (i < existingVehicles.size() - 1) sbPlates.append(" · ");
            }
        }
        lblAddVehPlatesCompare.setText(sbPlates.toString());

        lblAddVehPointsStat.setText(String.valueOf(kh.getDiemTichLuy()));
        lblAddVehCountStat.setText(existingVehicles.size() + " xe");

        // Clear & populate inputs
        txtAddVehPlate.setText(pendingPlate != null ? pendingPlate : "");
        txtAddVehMakeModel.setText("");
        txtAddVehYear.setText(String.valueOf(LocalDate.now().getYear()));
        txtAddVehColor.setText("");
        txtAddVehOdo.setText("0");
        txtAddVehRegDate.setText(LocalDate.now().format(DMY));

        txtAddVehPlate.requestFocusInWindow();
    }

    private void handleSaveVehicle() {
        if (currentCustomer == null) return;
        try {
            String plate = txtAddVehPlate.getText().trim().toUpperCase();
            String makeModel = txtAddVehMakeModel.getText().trim();
            String hangXe = "";
            String model = "";
            if (makeModel.contains("/")) {
                String[] p = makeModel.split("/", 2);
                hangXe = p[0].trim();
                model = p[1].trim();
            } else if (makeModel.contains(" ")) {
                int idx = makeModel.indexOf(" ");
                hangXe = makeModel.substring(0, idx).trim();
                model = makeModel.substring(idx + 1).trim();
            } else {
                hangXe = makeModel;
                model = makeModel;
            }

            int year = Integer.parseInt(txtAddVehYear.getText().trim());
            String color = txtAddVehColor.getText().trim();
            float odo = Float.parseFloat(txtAddVehOdo.getText().trim().replaceAll("[^0-9.]", ""));
            String engine = "";
            LocalDateTime regDate = LocalDate.parse(txtAddVehRegDate.getText().trim(), DMY).atStartOfDay();

            Xe newXe = new Xe(plate, currentCustomer, hangXe, model, year, color, engine, odo, regDate);
            KhachHangXeBus.themXeChoKhachHang(newXe);

            Msg.info(this, "Đã thêm phương tiện " + plate + " cho khách hàng " + currentCustomer.getHoTen() + "!");
            onSaved.accept(newXe);
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi lưu phương tiện: " + ex.getMessage());
        }
    }

    private JPanel formFieldInput(String label, JTextField tf, String hint) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT);
        l.setPreferredSize(new Dimension(200, 20));
        l.setMinimumSize(new Dimension(50, 20));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        styleInput(tf, null);
        tf.setAlignmentX(LEFT_ALIGNMENT);
        p.add(tf);

        p.add(Box.createVerticalStrut(4));
        JLabel h = new JLabel(hint != null && !hint.isEmpty() ? hint : " ");
        h.setFont(Theme.font(Font.PLAIN, 11));
        h.setForeground(hint != null && !hint.isEmpty() ? Theme.MUTED : new Color(0, 0, 0, 0));
        h.setPreferredSize(new Dimension(200, 16));
        h.setMinimumSize(new Dimension(50, 16));
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        h.setAlignmentX(LEFT_ALIGNMENT);
        p.add(h);

        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private void styleInput(JTextField tf, String placeholder) {
        tf.setFont(Theme.font(Font.PLAIN, 13));
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:6,10,6,10; borderColor:#D5DBE5");
        tf.setPreferredSize(new Dimension(200, 38));
        tf.setMinimumSize(new Dimension(50, 38));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        if (placeholder != null) {
            tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
    }
}

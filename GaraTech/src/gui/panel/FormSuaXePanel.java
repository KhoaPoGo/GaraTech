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
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Form chỉnh sửa thông tin phương tiện.
 */
public final class FormSuaXePanel extends JPanel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Consumer<Xe> onUpdated;
    private final Runnable onCancel;
    private final Consumer<Xe> onDelete;

    private Xe vehicleToEdit;
    private float editVehPreviousOdo = 0f;

    private final JTextField txtEditVehOwner = new JTextField();
    private final JTextField txtEditVehPlate = new JTextField();
    private final JTextField txtEditVehMakeModel = new JTextField();
    private final JTextField txtEditVehYear = new JTextField();
    private final JTextField txtEditVehColor = new JTextField();
    private final JTextField txtEditVehEngine = new JTextField();
    private final JTextField txtEditVehOdo = new JTextField();
    private final JLabel lblEditVehOdoHint = new JLabel("Không nhỏ hơn số km lần trước");
    private final JTextField txtEditVehRegDate = new JTextField();

    public FormSuaXePanel(Consumer<Xe> onUpdated, Runnable onCancel, Consumer<Xe> onDelete) {
        this.onUpdated = Objects.requireNonNull(onUpdated, "onUpdated must not be null");
        this.onCancel = Objects.requireNonNull(onCancel, "onCancel must not be null");
        this.onDelete = Objects.requireNonNull(onDelete, "onDelete must not be null");

        setOpaque(false);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        card.add(UI.h2("Thông tin phương tiện"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 16));
        grid.setOpaque(false);

        grid.add(formFieldReadonly("Chủ xe *", txtEditVehOwner, null));
        grid.add(formFieldReadonly("Biển số *", txtEditVehPlate, "Chữ in hoa; 6~12 ký tự; duy nhất"));

        grid.add(formFieldInput("Hãng / dòng xe *", txtEditVehMakeModel, null));
        grid.add(formFieldInput("Năm sản xuất *", txtEditVehYear, "Từ 1980 đến năm hiện tại"));

        grid.add(formFieldInput("Màu sơn *", txtEditVehColor, null));
        grid.add(formFieldInput("Số máy", txtEditVehEngine, "5~20 chữ in hoa và chữ số"));

        grid.add(formFieldInputWithCustomHint("ODO hiện tại *", txtEditVehOdo, lblEditVehOdoHint));
        grid.add(formFieldInput("Ngày đăng ký *", txtEditVehRegDate, "Không sau ngày hiện tại"));

        // Bottom Actions: Lưu phương tiện | Hủy | Xóa phương tiện
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        bot.setOpaque(false);

        JButton btnSaveEditVeh = UI.primaryButton("Lưu phương tiện");
        JButton btnCancelEditVeh = UI.secondaryButton("Hủy");
        JButton btnDeleteEditVeh = UI.dangerButton("Xóa phương tiện");

        btnSaveEditVeh.addActionListener(e -> handleUpdateVehicle());
        btnCancelEditVeh.addActionListener(e -> onCancel.run());
        btnDeleteEditVeh.addActionListener(e -> handleDeleteVehicle());

        bot.add(btnSaveEditVeh);
        bot.add(btnCancelEditVeh);
        bot.add(btnDeleteEditVeh);
        bot.setAlignmentX(LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(grid);
        content.add(Box.createVerticalStrut(20));
        content.add(bot);

        card.add(content, BorderLayout.NORTH);
        add(card, BorderLayout.NORTH);
    }

    public void setVehicle(Xe xe) {
        this.vehicleToEdit = xe;
        if (xe == null) return;
        this.editVehPreviousOdo = xe.getSoKmHienTai();

        KhachHang kh = xe.getKhachHang();
        txtEditVehOwner.setText(kh != null ? kh.getHoTen() + " · " + kh.getSoDienThoai() : "---");
        txtEditVehPlate.setText(xe.getBienSo());
        txtEditVehMakeModel.setText(xe.getHangXe() + " / " + xe.getModel());
        txtEditVehYear.setText(String.valueOf(xe.getNamSanXuat()));
        txtEditVehColor.setText(xe.getMauXe());
        txtEditVehEngine.setText(xe.getSoMay() != null ? xe.getSoMay() : "");
        txtEditVehOdo.setText(String.format("%,.0f km", xe.getSoKmHienTai()));
        lblEditVehOdoHint.setText(String.format("Không nhỏ hơn %,.0f km lần trước", editVehPreviousOdo));
        txtEditVehRegDate.setText(xe.getNgayDangKy() != null ? xe.getNgayDangKy().format(DMY) : "");

        txtEditVehMakeModel.requestFocusInWindow();
    }

    private void handleUpdateVehicle() {
        if (vehicleToEdit == null) return;
        try {
            String makeModel = txtEditVehMakeModel.getText().trim();
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

            int year = Integer.parseInt(txtEditVehYear.getText().trim());
            String color = txtEditVehColor.getText().trim();
            float newOdo = Float.parseFloat(txtEditVehOdo.getText().trim().replaceAll("[^0-9.]", ""));
            String engine = txtEditVehEngine.getText().trim();
            LocalDateTime regDate = LocalDate.parse(txtEditVehRegDate.getText().trim(), DMY).atStartOfDay();

            vehicleToEdit.setHangXe(hangXe);
            vehicleToEdit.setModel(model);
            vehicleToEdit.setNamSanXuat(year);
            vehicleToEdit.setMauXe(color);
            vehicleToEdit.setSoKmHienTai(newOdo);
            vehicleToEdit.setSoMay(engine);
            vehicleToEdit.setNgayDangKy(regDate);

            // Cập nhật với kiểm tra ràng buộc ODO không nhỏ hơn số km lần trước
            KhachHangXeBus.capNhatPhuongTien(vehicleToEdit, editVehPreviousOdo);

            Msg.info(this, "Cập nhật phương tiện " + vehicleToEdit.getBienSo() + " thành công!");
            onUpdated.accept(vehicleToEdit);
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi cập nhật phương tiện: " + ex.getMessage());
        }
    }

    private void handleDeleteVehicle() {
        if (vehicleToEdit == null) {
            Msg.error(this, "Vui lòng chọn phương tiện cần xóa.");
            return;
        }
        if (!Msg.confirm(this, "Bạn có chắc chắn muốn xóa phương tiện có biển số " + vehicleToEdit.getBienSo() + " không?")) {
            return;
        }
        try {
            KhachHangXeBus.xoaXe(vehicleToEdit.getBienSo());
            Msg.info(this, "Đã xóa phương tiện " + vehicleToEdit.getBienSo() + " thành công.");
            onDelete.accept(vehicleToEdit);
        } catch (IllegalArgumentException ex) {
            Msg.error(this, ex.getMessage());
        } catch (Exception ex) {
            Msg.error(this, "Lỗi khi xóa phương tiện: " + ex.getMessage());
        }
    }

    private JPanel formFieldInput(String label, JTextField tf, String hint) {
        JLabel h = new JLabel(hint != null && !hint.isEmpty() ? hint : " ");
        h.setFont(Theme.font(Font.PLAIN, 11));
        h.setForeground(hint != null && !hint.isEmpty() ? Theme.MUTED : new Color(0, 0, 0, 0));
        return formFieldInputWithCustomHint(label, tf, h);
    }

    private JPanel formFieldInputWithCustomHint(String label, JTextField tf, JLabel hintLabel) {
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
        hintLabel.setFont(Theme.font(Font.PLAIN, 11));
        hintLabel.setForeground(Theme.MUTED);
        hintLabel.setPreferredSize(new Dimension(200, 16));
        hintLabel.setMinimumSize(new Dimension(50, 16));
        hintLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        hintLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hintLabel);

        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JPanel formFieldReadonly(String label, JTextField tf, String hint) {
        JPanel p = formFieldInput(label, tf, hint);
        tf.setEditable(false);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setForeground(Theme.TEXT);
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

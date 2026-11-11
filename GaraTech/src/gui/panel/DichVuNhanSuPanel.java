package gui.panel;

import bus.DichVuService;
import bus.NhanSuService;
import dao.DichVuDao;
import dao.TaiKhoanQuanLyDao.TaiKhoanRow;
import entity.DichVu;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.Role;
import gui.Screen;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.StatusCellRenderer;
import gui.component.Theme;
import gui.component.UI;
import gui.dialog.FormDialog;
import util.Async;
import util.Fmt;
import util.RoundedPanel;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Màn "Nhân sự & tài khoản" (quản lý): nhân viên, tài khoản và bảng phân quyền.
 * Không xóa dữ liệu: dịch vụ chuyển "Ngừng cung cấp", nhân viên "Nghỉ việc", tài khoản "Đang khóa".
 */
public final class DichVuNhanSuPanel extends BasePanel {
    private static final String TT_NGHI = "Đã nghỉ việc";

    private final DichVuService dvService = new DichVuService();
    private final NhanSuService nsService = new NhanSuService();

    private final NhanVienTab nhanVienTab = new NhanVienTab();
    private final TaiKhoanTab taiKhoanTab = new TaiKhoanTab();

    public DichVuNhanSuPanel() {
        super("Quản trị", "Nhân sự & tài khoản",
                "Quản lý hồ sơ nhân viên, tài khoản đăng nhập và phân quyền theo vai trò.");
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.font(Font.BOLD, 13));
        tabs.addTab("Nhân viên", nhanVienTab);
        tabs.addTab("Tài khoản", taiKhoanTab);
        tabs.addTab("Bảng phân quyền", new PhanQuyenTab());
        // chiều cao theo tab đang chọn (trang cuộn dọc nên không để tab ngắn bị kéo theo tab dài)
        tabs.addChangeListener(e -> { tabs.revalidate(); tabs.repaint(); });
        body.add(tabs, BorderLayout.NORTH);
    }

    @Override
    public void onShow() {
        nhanVienTab.reload();
        taiKhoanTab.reload();
    }

    // ======================================================================== khung chung của một tab bảng
    private abstract class TableTab extends JPanel {
        final DefaultTableModel model;
        final JTable table;
        final JTextField search = new JTextField(22);
        final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        final TableRowSorter<DefaultTableModel> sorter;
        final JLabel count = UI.muted("");

        TableTab(String searchHint, String... columns) {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            setBorder(UI.pad(14, 0, 0, 0));
            model = UI.readOnlyModel(columns);
            table = UI.table(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.setFocusable(true);
            table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onSelect(); });

            search.putClientProperty("JTextField.placeholderText", searchHint);
            search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                void apply() {
                    String q = search.getText().trim();
                    sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(q)));
                    updateCount();
                }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { apply(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { apply(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { apply(); }
            });

            bar.setOpaque(false);
            bar.add(search);
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(bar, BorderLayout.WEST);
            top.add(count, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            RoundedPanel card = new RoundedPanel();
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            card.add(UI.tableBlock(table), BorderLayout.CENTER);
            add(card, BorderLayout.CENTER);
        }

        JButton addButton(JButton b) { bar.add(b); return b; }

        /** Chỉ số dòng của dữ liệu gốc (theo thứ tự nạp), hoặc -1 nếu chưa chọn. */
        int selected() {
            int r = table.getSelectedRow();
            return r < 0 ? -1 : table.convertRowIndexToModel(r);
        }

        void updateCount() { count.setText(table.getRowCount() + " / " + model.getRowCount() + " dòng"); }

        void filled() {
            updateCount();
            onSelect();
            revalidate();
            DichVuNhanSuPanel.this.revalidate();
        }

        void onSelect() { }
        abstract void reload();
    }

    // ======================================================================== Tab 1: Dịch vụ
    private final class DichVuTab extends TableTab {
        private List<DichVu> data = new ArrayList<>();
        private final JButton btnSua = UI.secondaryButton("Sửa");
        private final JButton btnNgung = UI.secondaryButton("Ngừng cung cấp");

        DichVuTab() {
            super("Tìm theo mã, tên, cấp độ…", "Mã", "Tên dịch vụ", "Mô tả", "Đơn giá công", "Giờ ước tính", "Cấp độ", "Trạng thái");
            JButton them = addButton(UI.primaryButton("+ Thêm dịch vụ"));
            addButton(btnSua);
            addButton(btnNgung);
            them.addActionListener(e -> form(null));
            btnSua.addActionListener(e -> { DichVu d = current(); if (d != null) form(d); });
            btnNgung.addActionListener(e -> toggle());

            table.getColumnModel().getColumn(0).setPreferredWidth(70);
            table.getColumnModel().getColumn(1).setPreferredWidth(220);
            table.getColumnModel().getColumn(2).setPreferredWidth(280);
            table.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
            rightAlign(table, 3, 4);
            onSelect();
        }

        private DichVu current() {
            int i = selected();
            return i < 0 || i >= data.size() ? null : data.get(i);
        }

        @Override void onSelect() {
            DichVu d = current();
            btnSua.setEnabled(d != null);
            btnNgung.setEnabled(d != null);
            if (d != null) btnNgung.setText(DichVuDao.NGUNG.equals(d.getTrangThai()) ? "Mở lại dịch vụ" : "Ngừng cung cấp");
        }

        @Override void reload() {
            Async.run(this, dvService::danhSach, list -> {
                data = list;
                model.setRowCount(0);
                for (DichVu d : list) {
                    model.addRow(new Object[]{d.getMaDichVu(), d.getTenDichVu(), nz(d.getMoTa()),
                            Fmt.money(Math.round(d.getTienCong())), gio(d.getThoiGianUocTinh()),
                            nz(d.getCapDoBaoDuong()), d.getTrangThai()});
                }
                filled();
            });
        }

        private void toggle() {
            DichVu d = current();
            if (d == null) return;
            boolean ngung = !DichVuDao.NGUNG.equals(d.getTrangThai());
            String hoi = ngung
                    ? "Ngừng cung cấp dịch vụ \"" + d.getTenDichVu() + "\"?\nDịch vụ sẽ không còn xuất hiện khi lập phiếu mới; phiếu cũ không bị ảnh hưởng."
                    : "Mở lại dịch vụ \"" + d.getTenDichVu() + "\"?";
            if (!Msg.confirm(this, hoi)) return;
            Async.run(this, () -> { dvService.ngungCungCap(d.getMaDichVu(), ngung); return null; }, x -> reload());
        }

        private void form(DichVu old) {
            JTextField ten = new JTextField(old == null ? "" : old.getTenDichVu(), 26);
            JTextArea moTa = new JTextArea(old == null ? "" : nz(old.getMoTa()), 3, 26);
            moTa.setLineWrap(true);
            moTa.setWrapStyleWord(true);
            JTextField gia = new JTextField(old == null ? "" : String.valueOf(Math.round(old.getTienCong())), 26);
            JTextField tg = new JTextField(old == null ? "" : gio(old.getThoiGianUocTinh()), 26);
            JComboBox<String> cap = new JComboBox<>();
            cap.addItem("(Không có)");
            DichVuService.CAP_DO.forEach(cap::addItem);
            if (old != null && old.getCapDoBaoDuong() != null) cap.setSelectedItem(old.getCapDoBaoDuong());

            FormDialog.Form f = new FormDialog.Form()
                    .add("Tên dịch vụ *", ten)
                    .add("Mô tả", new JScrollPane(moTa))
                    .add("Đơn giá công (₫) *", gia)
                    .add("Giờ ước tính *", tg)
                    .add("Cấp độ bảo dưỡng", cap);
            Double[] gioHolder = new Double[1];
            Long[] giaHolder = new Long[1];
            boolean ok = FormDialog.show(this, old == null ? "Thêm dịch vụ" : "Sửa dịch vụ " + old.getMaDichVu(), f, () -> {
                giaHolder[0] = parseMoney(gia.getText());
                if (giaHolder[0] == null) return "Đơn giá công phải là số nguyên không âm (ví dụ 150000).";
                gioHolder[0] = parseDouble(tg.getText());
                if (gioHolder[0] == null) return "Giờ ước tính phải là số (ví dụ 0,5 hoặc 2).";
                return null;
            });
            if (!ok) return;
            String capDo = cap.getSelectedIndex() <= 0 ? null : (String) cap.getSelectedItem();
            Async.run(this, () -> {
                if (old == null) return "Đã thêm dịch vụ " + dvService.them(ten.getText(), moTa.getText(), giaHolder[0], gioHolder[0], capDo) + ".";
                dvService.sua(old.getMaDichVu(), ten.getText(), moTa.getText(), giaHolder[0], gioHolder[0], capDo);
                return "Đã cập nhật dịch vụ " + old.getMaDichVu() + ".";
            }, msg -> { Msg.info(this, msg); reload(); });
        }
    }

    // ======================================================================== Tab 2: Nhân viên
    private final class NhanVienTab extends TableTab {
        private List<NhanVien> data = new ArrayList<>();
        private final JButton btnSua = UI.secondaryButton("Sửa");
        private final JButton btnNghi = UI.secondaryButton("Cho nghỉ việc");

        NhanVienTab() {
            super("Tìm theo mã, tên, SĐT, vai trò…", "Mã NV", "Họ tên", "Vai trò", "Số điện thoại", "Email", "Hệ số", "Lương cơ bản", "Trạng thái");
            JButton them = addButton(UI.primaryButton("+ Thêm nhân viên"));
            addButton(btnSua);
            addButton(btnNghi);
            them.addActionListener(e -> form(null));
            btnSua.addActionListener(e -> { NhanVien n = current(); if (n != null) form(n); });
            btnNghi.addActionListener(e -> toggle());
            table.getColumnModel().getColumn(0).setPreferredWidth(80);
            table.getColumnModel().getColumn(1).setPreferredWidth(160);
            table.getColumnModel().getColumn(2).setPreferredWidth(150);
            table.getColumnModel().getColumn(4).setPreferredWidth(200);
            table.getColumnModel().getColumn(7).setCellRenderer(new StatusCellRenderer());
            rightAlign(table, 5, 6);
            onSelect();
        }

        private NhanVien current() {
            int i = selected();
            return i < 0 || i >= data.size() ? null : data.get(i);
        }

        @Override void onSelect() {
            NhanVien n = current();
            btnSua.setEnabled(n != null);
            btnNghi.setEnabled(n != null);
            if (n != null) btnNghi.setText(n.isDangLam() ? "Cho nghỉ việc" : "Cho làm lại");
        }

        @Override void reload() {
            Async.run(this, nsService::danhSachNhanVien, list -> {
                data = list;
                model.setRowCount(0);
                for (NhanVien n : list) {
                    model.addRow(new Object[]{n.getMaNV(), n.getHoTen(), n.getRole() == null ? "?" : n.getRole().displayName(),
                            n.getSoDienThoai(), n.getEmail(), String.format(Locale.ROOT, "%.2f", n.getHeSoLuong()).replace('.', ','),
                            Fmt.money(n.getLuongCoBan()), n.isDangLam() ? "Hoạt động" : TT_NGHI});
                }
                filled();
            });
        }

        private void toggle() {
            NhanVien n = current();
            if (n == null) return;
            boolean nghi = n.isDangLam();
            String hoi = nghi
                    ? "Cho " + n.getHoTen() + " (" + n.getMaNV() + ") nghỉ việc?\nNhân viên sẽ không đăng nhập được nữa nhưng dữ liệu cũ vẫn được giữ."
                    : "Cho " + n.getHoTen() + " (" + n.getMaNV() + ") làm việc trở lại?";
            if (!Msg.confirm(this, hoi)) return;
            Async.run(this, () -> { nsService.datTrangThaiLamViec(n.getMaNV(), !nghi); return null; }, x -> {
                reload();
                taiKhoanTab.reload();
            });
        }

        private void form(NhanVien old) {
            JTextField ten = new JTextField(old == null ? "" : old.getHoTen(), 26);
            JTextField sdt = new JTextField(old == null ? "" : old.getSoDienThoai(), 26);
            JTextField email = new JTextField(old == null ? "" : old.getEmail(), 26);
            JTextField diaChi = new JTextField(old == null ? "" : old.getDiaChi(), 26);
            JComboBox<Role> vaiTro = new JComboBox<>(Role.values());
            vaiTro.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l, Object v, int i, boolean s, boolean f) {
                    return super.getListCellRendererComponent(l, v instanceof Role r ? r.displayName() : v, i, s, f);
                }
            });
            if (old != null && old.getRole() != null) vaiTro.setSelectedItem(old.getRole());
            JTextField heSo = new JTextField(old == null ? "2,0" : String.format(Locale.ROOT, "%.2f", old.getHeSoLuong()).replace('.', ','), 26);
            JTextField luong = new JTextField(old == null ? "" : String.valueOf(old.getLuongCoBan()), 26);

            FormDialog.Form f = new FormDialog.Form()
                    .add("Họ tên *", ten).add("Số điện thoại *", sdt).add("Email *", email).add("Địa chỉ *", diaChi)
                    .add("Vai trò *", vaiTro).add("Hệ số lương * (1,0 – 5,0)", heSo).add("Lương cơ bản (₫) *", luong);
            if (old == null) f.note("Sau khi thêm, vào tab <b>Tài khoản</b> để cấp tên đăng nhập và mật khẩu cho nhân viên.");
            Double[] heSoHolder = new Double[1];
            Long[] luongHolder = new Long[1];
            boolean ok = FormDialog.show(this, old == null ? "Thêm nhân viên" : "Sửa nhân viên " + old.getMaNV(), f, () -> {
                heSoHolder[0] = parseDouble(heSo.getText());
                if (heSoHolder[0] == null) return "Hệ số lương phải là số (ví dụ 2,5).";
                luongHolder[0] = parseMoney(luong.getText());
                if (luongHolder[0] == null) return "Lương cơ bản phải là số nguyên (ví dụ 9000000).";
                return null;
            });
            if (!ok) return;
            Role r = (Role) vaiTro.getSelectedItem();
            Async.run(this, () -> {
                if (old == null) return "Đã thêm nhân viên " + nsService.themNhanVien(ten.getText(), sdt.getText(), email.getText(),
                        diaChi.getText(), r, heSoHolder[0], luongHolder[0]) + ".";
                nsService.suaNhanVien(old.getMaNV(), ten.getText(), sdt.getText(), email.getText(), diaChi.getText(), r,
                        heSoHolder[0], luongHolder[0]);
                return "Đã cập nhật nhân viên " + old.getMaNV() + ".";
            }, msg -> { Msg.info(this, msg); reload(); taiKhoanTab.reload(); });
        }
    }

    // ======================================================================== Tab 3: Tài khoản
    private final class TaiKhoanTab extends TableTab {
        private List<TaiKhoanRow> data = new ArrayList<>();
        private final JButton btnReset = UI.secondaryButton("Đặt lại mật khẩu");
        private final JButton btnKhoa = UI.secondaryButton("Khóa tài khoản");

        TaiKhoanTab() {
            super("Tìm theo tên đăng nhập, nhân viên…", "Tên đăng nhập", "Nhân viên", "Mã NV", "Vai trò", "Trạng thái");
            JButton cap = addButton(UI.primaryButton("+ Cấp tài khoản"));
            addButton(btnReset);
            addButton(btnKhoa);
            cap.addActionListener(e -> capMoi());
            btnReset.addActionListener(e -> datLai());
            btnKhoa.addActionListener(e -> khoa());
            table.getColumnModel().getColumn(4).setCellRenderer(new StatusCellRenderer());
            onSelect();
        }

        private TaiKhoanRow current() {
            int i = selected();
            return i < 0 || i >= data.size() ? null : data.get(i);
        }

        @Override void onSelect() {
            TaiKhoanRow t = current();
            btnReset.setEnabled(t != null);
            btnKhoa.setEnabled(t != null);
            if (t != null) btnKhoa.setText(TaiKhoan.DANG_KHOA.equals(t.trangThai()) ? "Mở khóa" : "Khóa tài khoản");
        }

        @Override void reload() {
            Async.run(this, nsService::danhSachTaiKhoan, list -> {
                data = list;
                model.setRowCount(0);
                for (TaiKhoanRow t : list) {
                    model.addRow(new Object[]{t.tenDangNhap(), t.hoTen() + (t.dangLam() ? "" : " (đã nghỉ)"), t.maNV(),
                            t.vaiTro() == null ? "?" : t.vaiTro().displayName(), t.trangThai()});
                }
                filled();
            });
        }

        private void capMoi() {
            Async.run(this, nsService::nhanVienChuaCoTaiKhoan, list -> {
                if (list.isEmpty()) {
                    Msg.info(this, "Tất cả nhân viên đang làm việc đều đã có tài khoản.");
                    return;
                }
                JComboBox<NhanVien> nv = new JComboBox<>(new DefaultComboBoxModel<>(list.toArray(new NhanVien[0])));
                nv.setRenderer(new javax.swing.DefaultListCellRenderer() {
                    @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l, Object v, int i, boolean s, boolean f) {
                        String t = v instanceof NhanVien n ? n.getMaNV() + " · " + n.getHoTen() + " · " + (n.getRole() == null ? "?" : n.getRole().displayName()) : "";
                        return super.getListCellRendererComponent(l, t, i, s, f);
                    }
                });
                JTextField user = new JTextField(22);
                JPasswordField p1 = new JPasswordField(22), p2 = new JPasswordField(22);
                FormDialog.Form f = new FormDialog.Form().add("Nhân viên *", nv).add("Tên đăng nhập *", user)
                        .add("Mật khẩu * (≥ 8 ký tự)", p1).add("Nhập lại mật khẩu *", p2)
                        .note("Tên đăng nhập 5–50 ký tự, chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới. Mật khẩu được lưu dưới dạng mã hóa PBKDF2.");
                if (!FormDialog.show(this, "Cấp tài khoản", f, () -> null)) return;
                NhanVien chon = (NhanVien) nv.getSelectedItem();
                String m1 = new String(p1.getPassword()), m2 = new String(p2.getPassword());
                Async.run(this, () -> {
                    nsService.capTaiKhoan(chon.getMaNV(), user.getText(), m1, m2);
                    return null;
                }, x -> { Msg.info(this, "Đã cấp tài khoản cho " + chon.getHoTen() + "."); reload(); });
            });
        }

        private void datLai() {
            TaiKhoanRow t = current();
            if (t == null) return;
            JPasswordField p1 = new JPasswordField(22), p2 = new JPasswordField(22);
            FormDialog.Form f = new FormDialog.Form().add("Mật khẩu mới * (≥ 8 ký tự)", p1).add("Nhập lại *", p2);
            if (!FormDialog.show(this, "Đặt lại mật khẩu cho " + t.tenDangNhap(), f, () -> null)) return;
            String m1 = new String(p1.getPassword()), m2 = new String(p2.getPassword());
            Async.run(this, () -> { nsService.datLaiMatKhau(t.tenDangNhap(), m1, m2); return null; },
                    x -> Msg.info(this, "Đã đặt lại mật khẩu cho " + t.tenDangNhap() + ". Hãy báo mật khẩu mới cho nhân viên."));
        }

        private void khoa() {
            TaiKhoanRow t = current();
            if (t == null) return;
            boolean khoa = !TaiKhoan.DANG_KHOA.equals(t.trangThai());
            if (khoa && Session.user() != null && t.tenDangNhap().equalsIgnoreCase(Session.user().tenDangNhap())) {
                Msg.error(this, "Bạn không thể tự khóa tài khoản đang đăng nhập.");
                return;
            }
            if (!Msg.confirm(this, (khoa ? "Khóa" : "Mở khóa") + " tài khoản \"" + t.tenDangNhap() + "\"?")) return;
            Async.run(this, () -> { nsService.khoaTaiKhoan(t.tenDangNhap(), khoa); return null; }, x -> reload());
        }
    }

    // ======================================================================== Tab 4: Phân quyền (chỉ xem)
    private static final class PhanQuyenTab extends JPanel {
        PhanQuyenTab() {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            setBorder(UI.pad(14, 0, 0, 0));
            add(UI.muted("Quyền truy cập được cố định theo vai trò (khai báo trong Screen.java). Muốn đổi quyền của một người, hãy đổi vai trò ở tab Nhân viên."),
                    BorderLayout.NORTH);
            Role[] roles = Role.values();
            String[] cols = new String[roles.length + 1];
            cols[0] = "Chức năng";
            for (int i = 0; i < roles.length; i++) cols[i + 1] = roles[i].displayName();
            DefaultTableModel m = UI.readOnlyModel(cols);
            for (Screen s : Screen.values()) {
                Object[] row = new Object[cols.length];
                row[0] = s.title();
                for (int i = 0; i < roles.length; i++) row[i + 1] = s.allows(roles[i]) ? "✓" : "";
                m.addRow(row);
            }
            JTable t = UI.table(m);
            t.setRowHeight(38);
            DefaultTableCellRenderer center = new DefaultTableCellRenderer();
            center.setHorizontalAlignment(SwingConstants.CENTER);
            center.setForeground(Theme.SUCCESS);
            center.setFont(Theme.font(Font.BOLD, 14));
            for (int i = 1; i < cols.length; i++) t.getColumnModel().getColumn(i).setCellRenderer(center);
            RoundedPanel card = new RoundedPanel();
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            card.add(UI.tableBlock(t), BorderLayout.CENTER);
            add(card, BorderLayout.CENTER);
        }
    }

    // ======================================================================== tiện ích nhỏ
    private static String nz(String s) { return s == null ? "" : s; }

    private static String gio(double h) {
        return (h == Math.floor(h) ? String.valueOf((long) h) : String.valueOf(h).replace('.', ',')) + " giờ";
    }

    /** "150.000", "150,000", "150000" -> 150000; sai -> null; âm -> null. */
    static Long parseMoney(String s) {
        if (s == null) return null;
        String x = s.trim().replace(".", "").replace(",", "").replace(" ", "").replace("₫", "");
        if (!x.matches("\\d{1,15}")) return null;
        return Long.parseLong(x);
    }

    /** "2,5" hoặc "2.5" -> 2.5; sai -> null. */
    static Double parseDouble(String s) {
        if (s == null) return null;
        String x = s.trim().replace(',', '.').replace("giờ", "").trim();
        if (!x.matches("\\d{1,9}(\\.\\d{1,4})?")) return null;
        return Double.parseDouble(x);
    }

    private static void rightAlign(JTable t, int... cols) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : cols) t.getColumnModel().getColumn(c).setCellRenderer(r);
    }
}

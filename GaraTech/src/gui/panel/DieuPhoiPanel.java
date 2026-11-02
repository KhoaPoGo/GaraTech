package gui.panel;

import bus.DieuPhoiService;
import dao.DieuPhoiDao.KtvLoad;
import dao.DieuPhoiDao.PhieuCard;
import enums.TrangThaiPhieuSuaChua;
import gui.Navigator;
import gui.Screen;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.StatusBadge;
import gui.component.Theme;
import gui.component.UI;
import gui.dialog.FormDialog;
import util.Async;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn "Điều phối & tiến độ" (quản lý): bảng Kanban 5 cột theo trạng thái phiếu sửa chữa; chọn một phiếu để
 * phân công kỹ thuật viên (hiện Rảnh / Bận), đặt hạn hoàn thành, bắt đầu sửa hoặc hủy phiếu có lý do.
 */
public final class DieuPhoiPanel extends BasePanel {
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DieuPhoiService service = new DieuPhoiService();

    private final JPanel board = new JPanel(new GridLayout(1, DieuPhoiService.CAC_COT.length, 12, 0));
    private final JLabel warn = UI.label("", Font.BOLD, 12, Theme.DANGER);

    // ---- vùng chi tiết / thao tác ----
    private final JLabel lbTitle = UI.h2("Chọn một phiếu trên bảng");
    private final StatusBadge badge = new StatusBadge("");
    private final JTextArea lbInfo = new JTextArea();
    private final JComboBox<KtvLoad> cbKtv = new JComboBox<>();
    private final JTextField tfHan = new JTextField(16);
    private final JButton btnLuu = UI.primaryButton("Lưu phân công");
    private final JButton btnBatDau = UI.secondaryButton("Bắt đầu sửa");
    private final JButton btnHuy = UI.secondaryButton("Hủy phiếu");
    private final JButton btnKiemTra = UI.secondaryButton("Mở kiểm tra chất lượng");

    private List<PhieuCard> phieu = new ArrayList<>();
    private List<KtvLoad> ktv = new ArrayList<>();
    private String selectedId;

    public DieuPhoiPanel() {
        super("Điều hành gara", "Điều phối & tiến độ",
                "Theo dõi phiếu sửa chữa theo trạng thái, phân công kỹ thuật viên và kiểm soát hạn hoàn thành.");
        JButton refresh = UI.secondaryButton("Làm mới");
        refresh.addActionListener(e -> reload());
        actions.add(refresh);

        board.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        warn.setBorder(UI.pad(0, 2, 0, 0));
        top.add(warn, BorderLayout.NORTH);
        top.add(board, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(top, BorderLayout.NORTH);
        content.add(detailCard(), BorderLayout.CENTER);
        body.add(content, BorderLayout.NORTH);

        cbKtv.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                String t = "";
                if (v instanceof KtvLoad k) t = k.hoTen() + "  ·  " + (k.ranh() ? "Rảnh" : "Bận (" + k.soPhieuDangGiu() + " phiếu)");
                return super.getListCellRendererComponent(l, t, i, s, f);
            }
        });
        tfHan.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy HH:mm");
        btnLuu.addActionListener(e -> luuPhanCong());
        btnBatDau.addActionListener(e -> batDau());
        btnHuy.addActionListener(e -> huy());
        btnKiemTra.addActionListener(e -> Navigator.go(Screen.KIEM_TRA_CL));
        showDetail(null);
    }

    @Override
    public void onShow() { reload(); }

    // ====================================================================== nạp dữ liệu
    private record Data(List<PhieuCard> phieu, List<KtvLoad> ktv) {}

    private void reload() {
        Async.run(this, () -> new Data(service.phieuTrenBang(), service.danhSachKtv()), d -> {
            phieu = d.phieu();
            ktv = d.ktv();
            buildBoard();
            PhieuCard sel = find(selectedId);
            if (sel == null) selectedId = null;
            showDetail(sel);
        });
    }

    private PhieuCard find(String id) {
        if (id == null) return null;
        return phieu.stream().filter(p -> p.maPhieuSC().equals(id)).findFirst().orElse(null);
    }

    private void buildBoard() {
        board.removeAll();
        LocalDateTime now = LocalDateTime.now();
        long quaHan = phieu.stream().filter(p -> p.quaHan(now)).count();
        warn.setText(quaHan > 0 ? "⚠ " + quaHan + " phiếu đã quá hạn hoàn thành dự kiến" : " ");
        for (TrangThaiPhieuSuaChua tt : DieuPhoiService.CAC_COT) {
            List<PhieuCard> ds = phieu.stream().filter(p -> p.trangThai() == tt).toList();
            board.add(column(tt, ds, now));
        }
        board.revalidate();
        board.repaint();
        revalidate();
    }

    private JComponent column(TrangThaiPhieuSuaChua tt, List<PhieuCard> ds, LocalDateTime now) {
        RoundedPanel col = new RoundedPanel(14, new Color(0xEBEEF4));
        col.setLayout(new BorderLayout(0, 8));
        col.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(new StatusBadge(tt.label()), BorderLayout.WEST);
        head.add(UI.label(String.valueOf(ds.size()), Font.BOLD, 13, Theme.MUTED), BorderLayout.EAST);
        col.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (ds.isEmpty()) {
            JLabel e = UI.muted("Không có phiếu");
            e.setBorder(UI.pad(10, 4, 10, 4));
            list.add(e);
        }
        for (PhieuCard p : ds) {
            JComponent c = card(p, now);
            c.setAlignmentX(LEFT_ALIGNMENT);
            list.add(c);
            list.add(Box.createVerticalStrut(8));
        }
        col.add(list, BorderLayout.CENTER);
        col.setPreferredSize(new Dimension(150, col.getPreferredSize().height));
        return col;
    }

    private JComponent card(PhieuCard p, LocalDateTime now) {
        boolean sel = p.maPhieuSC().equals(selectedId);
        RoundedPanel c = new RoundedPanel(10, Color.WHITE);
        if (sel) c.borderColor(Theme.YELLOW);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.add(left(UI.label(p.maPhieuSC(), Font.BOLD, 12, Theme.TEXT)));
        c.add(left(UI.label(p.bienSo() + " · " + p.tenXe(), Font.PLAIN, 12, Theme.MUTED)));
        c.add(left(UI.label(p.tenKhach(), Font.PLAIN, 12, Theme.MUTED)));
        c.add(Box.createVerticalStrut(4));
        c.add(left(UI.label(p.tenKTV() == null ? "Chưa phân công" : "KTV: " + p.tenKTV(), Font.BOLD, 12,
                p.tenKTV() == null ? Theme.WARN : Theme.TEXT)));
        if (p.duKien() != null) {
            c.add(left(UI.label("Hạn " + Fmt.dateTime(p.duKien()), Font.PLAIN, 11, p.quaHan(now) ? Theme.DANGER : Theme.MUTED)));
        }
        if (p.trangThai() == TrangThaiPhieuSuaChua.DANG_SUA_CHUA) {
            c.add(left(UI.label(p.tienDo() + "% hoàn thành", Font.PLAIN, 11, Theme.MUTED)));
        }
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectedId = p.maPhieuSC();
                buildBoard();
                showDetail(p);
            }
        };
        attach(c, click);
        return c;
    }

    private static void attach(Component c, MouseAdapter l) {
        c.addMouseListener(l);
        if (c instanceof java.awt.Container k) for (Component x : k.getComponents()) attach(x, l);
    }

    private static JComponent left(JComponent c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    // ====================================================================== vùng chi tiết
    private JComponent detailCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(24, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        head.add(lbTitle);
        head.add(badge);
        head.setAlignmentX(LEFT_ALIGNMENT);
        lbInfo.setEditable(false);
        lbInfo.setOpaque(false);
        lbInfo.setLineWrap(true);
        lbInfo.setWrapStyleWord(true);
        lbInfo.setFont(Theme.font(Font.PLAIN, 13));
        lbInfo.setForeground(Theme.MUTED);
        lbInfo.setAlignmentX(LEFT_ALIGNMENT);
        info.add(head);
        info.add(Box.createVerticalStrut(8));
        info.add(lbInfo);
        card.add(info, BorderLayout.CENTER);

        FormDialog.Form f = new FormDialog.Form();
        f.add("Kỹ thuật viên", cbKtv).add("Hạn hoàn thành", tfHan);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnLuu);
        btns.add(btnBatDau);
        btns.add(btnHuy);
        btns.add(btnKiemTra);
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(f, BorderLayout.CENTER);
        right.add(btns, BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(520, right.getPreferredSize().height));
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private void showDetail(PhieuCard p) {
        boolean has = p != null;
        lbTitle.setText(has ? p.maPhieuSC() : "Chọn một phiếu trên bảng");
        badge.setVisible(has);
        cbKtv.setModel(new DefaultComboBoxModel<>(ktv.toArray(new KtvLoad[0])));
        cbKtv.setSelectedItem(null);
        tfHan.setText("");
        if (!has) {
            lbInfo.setText("Bấm vào một thẻ phiếu để xem chi tiết, phân công kỹ thuật viên và đặt hạn hoàn thành.");
            setActions(false, false, false, false);
            return;
        }
        badge.setStatus(p.trangThai().label());
        StringBuilder sb = new StringBuilder();
        sb.append("Xe: ").append(p.bienSo()).append(" · ").append(p.tenXe()).append('\n');
        sb.append("Khách hàng: ").append(p.tenKhach()).append('\n');
        sb.append("Yêu cầu: ").append(p.yeuCau() == null ? "" : p.yeuCau()).append('\n');
        sb.append("Hạng mục: ").append(p.hangMucXong()).append('/').append(p.tongHangMuc()).append(" đã xong (")
                .append(p.tienDo()).append("%)\n");
        sb.append("Bắt đầu sửa: ").append(p.batDau() == null ? "chưa" : Fmt.dateTime(p.batDau()));
        lbInfo.setText(sb.toString());

        if (p.maKTV() != null) {
            for (KtvLoad k : ktv) if (k.maNV().equals(p.maKTV())) cbKtv.setSelectedItem(k);
        }
        LocalDateTime han = p.duKien() != null ? p.duKien() : LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0);
        tfHan.setText(han.format(DT));

        TrangThaiPhieuSuaChua s = p.trangThai();
        boolean phanCongDuoc = s == TrangThaiPhieuSuaChua.DA_XAC_NHAN || s == TrangThaiPhieuSuaChua.DANG_SUA_CHUA;
        setActions(phanCongDuoc, s == TrangThaiPhieuSuaChua.DA_XAC_NHAN && p.maKTV() != null,
                s.canMoveTo(TrangThaiPhieuSuaChua.DA_HUY), s == TrangThaiPhieuSuaChua.CHO_KIEM_TRA);
    }

    private void setActions(boolean luu, boolean batDau, boolean huy, boolean kiemTra) {
        cbKtv.setEnabled(luu);
        tfHan.setEnabled(luu);
        btnLuu.setEnabled(luu);
        btnBatDau.setEnabled(batDau);
        btnHuy.setEnabled(huy);
        btnKiemTra.setVisible(kiemTra);
    }

    // ====================================================================== thao tác
    private void luuPhanCong() {
        PhieuCard p = find(selectedId);
        if (p == null) return;
        KtvLoad k = (KtvLoad) cbKtv.getSelectedItem();
        if (k == null) { Msg.error(this, "Vui lòng chọn kỹ thuật viên."); return; }
        LocalDateTime han;
        try {
            han = LocalDateTime.parse(tfHan.getText().trim(), DT);
        } catch (DateTimeParseException ex) {
            Msg.error(this, "Hạn hoàn thành phải theo dạng dd/MM/yyyy HH:mm (ví dụ 06/11/2026 17:00).");
            return;
        }
        Async.run(this, () -> { service.phanCong(p.maPhieuSC(), k.maNV(), han); return null; }, x -> {
            Msg.info(this, "Đã phân công " + k.hoTen() + " cho phiếu " + p.maPhieuSC() + ".");
            reload();
        });
    }

    private void batDau() {
        PhieuCard p = find(selectedId);
        if (p == null || !Msg.confirm(this, "Bắt đầu sửa chữa phiếu " + p.maPhieuSC() + "?\nPhiếu sẽ chuyển sang \"Đang sửa chữa\".")) return;
        Async.run(this, () -> { service.batDauSua(p.maPhieuSC()); return null; }, x -> reload());
    }

    private void huy() {
        PhieuCard p = find(selectedId);
        if (p == null) return;
        JTextArea ly = new JTextArea(3, 28);
        ly.setLineWrap(true);
        ly.setWrapStyleWord(true);
        FormDialog.Form f = new FormDialog.Form().add("Lý do hủy *", new javax.swing.JScrollPane(ly));
        if (!FormDialog.show(this, "Hủy phiếu " + p.maPhieuSC(), f, "Xác nhận hủy",
                () -> ly.getText().isBlank() ? "Vui lòng nhập lý do hủy phiếu." : null)) return;
        Async.run(this, () -> { service.huyPhieu(p.maPhieuSC(), ly.getText()); return null; }, x -> {
            selectedId = null;
            reload();
        });
    }
}

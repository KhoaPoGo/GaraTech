package gui.panel;

import bus.DieuPhoiService;
import dao.DieuPhoiDao.HangMuc;
import dao.DieuPhoiDao.PhieuCard;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.ProgressCellRenderer;
import gui.component.StatusCellRenderer;
import gui.component.Theme;
import gui.component.UI;
import gui.dialog.FormDialog;
import util.Async;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn "Kiểm tra chất lượng" (quản lý): hai cột đối chiếu — kỹ thuật viên báo gì / quản lý kiểm gì.
 * Chỉ cho "Duyệt hoàn thành" khi tick đủ checklist bắt buộc và mọi hạng mục đã xong; nếu chưa đạt thì trả về làm lại.
 */
public final class KiemTraChatLuongPanel extends BasePanel {
    private final DieuPhoiService service = new DieuPhoiService();

    private final DefaultTableModel listModel = UI.readOnlyModel("Phiếu / phương tiện", "Khách hàng", "Kỹ thuật viên", "Tiến độ", "Hạn dự kiến");
    private final JTable listTable = UI.table(listModel);
    private final JLabel emptyHint = UI.muted("Schema hiện tại không có trạng thái KCS riêng; màn này chưa có phiếu để duyệt.");

    private final JLabel lbTitle = UI.h2("Chọn một phiếu ở trên để kiểm tra");
    private final JTextArea ktvNote = new JTextArea(4, 20);
    private final DefaultTableModel hmModel = UI.readOnlyModel("Hạng mục", "Loại", "SL", "Trạng thái");
    private final JLabel hmWarn = UI.label(" ", Font.BOLD, 12, Theme.DANGER);

    private final JCheckBox ckChayThu = new JCheckBox("Đã chạy thử xe, vận hành bình thường");
    private final JCheckBox ckRoRi = new JCheckBox("Không rò rỉ nhớt / dầu / nước làm mát");
    private final JCheckBox ckVeSinh = new JCheckBox("Đã vệ sinh xe, trả đủ đồ cho khách");
    private final JTextArea qlNote = new JTextArea(3, 20);
    private final JButton btnDuyet = UI.primaryButton("Duyệt hoàn thành");
    private final JButton btnTraVe = UI.secondaryButton("Trả về làm lại");

    private List<PhieuCard> phieu = new ArrayList<>();
    private int chuaXong = 0;
    private String selectedId;

    public KiemTraChatLuongPanel() {
        super("Điều hành gara", "Kiểm tra chất lượng",
                "Đối chiếu kết quả kỹ thuật viên báo với kiểm tra thực tế trước khi xác nhận xe hoàn thành.");
        JButton refresh = UI.secondaryButton("Làm mới");
        refresh.addActionListener(e -> reload());
        actions.add(refresh);

        listTable.setRowHeight(52);
        listTable.setFocusable(true);
        listTable.getColumnModel().getColumn(3).setCellRenderer(new ProgressCellRenderer());
        listTable.getColumnModel().getColumn(0).setPreferredWidth(240);
        listTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int r = listTable.getSelectedRow();
            if (r >= 0 && r < phieu.size()) choose(phieu.get(r));
        });

        RoundedPanel listCard = new RoundedPanel();
        listCard.setLayout(new BorderLayout(0, 8));
        listCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        JPanel lh = new JPanel(new BorderLayout());
        lh.setOpaque(false);
        lh.add(UI.h2("Phiếu chờ kiểm tra"), BorderLayout.WEST);
        lh.add(emptyHint, BorderLayout.EAST);
        listCard.add(lh, BorderLayout.NORTH);
        listCard.add(UI.tableBlock(listTable), BorderLayout.CENTER);

        JPanel two = new JPanel(new GridLayout(1, 2, 16, 0));
        two.setOpaque(false);
        two.add(ktvColumn());
        two.add(qlColumn());

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        JPanel stack = new JPanel(new BorderLayout(0, 18));
        stack.setOpaque(false);
        stack.add(listCard, BorderLayout.NORTH);
        stack.add(lbTitleBar(), BorderLayout.CENTER);
        content.add(stack, BorderLayout.NORTH);
        content.add(two, BorderLayout.CENTER);
        body.add(content, BorderLayout.NORTH);

        Runnable upd = this::updateButtons;
        ckChayThu.addActionListener(e -> upd.run());
        ckRoRi.addActionListener(e -> upd.run());
        ckVeSinh.addActionListener(e -> upd.run());
        btnDuyet.addActionListener(e -> duyet());
        btnTraVe.addActionListener(e -> traVe());
        clearDetail();
    }

    private JComponent lbTitleBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(lbTitle, BorderLayout.WEST);
        return p;
    }

    private JComponent ktvColumn() {
        RoundedPanel c = new RoundedPanel();
        c.setLayout(new BorderLayout(0, 10));
        c.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        c.add(UI.h2("Kỹ thuật viên báo"), BorderLayout.NORTH);

        ktvNote.setEditable(false);
        ktvNote.setLineWrap(true);
        ktvNote.setWrapStyleWord(true);
        ktvNote.setFont(Theme.font(Font.PLAIN, 13));
        JScrollPane sn = new JScrollPane(ktvNote);
        sn.setPreferredSize(new Dimension(100, 96));

        JTable t = UI.table(hmModel);
        t.setRowHeight(36);
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(2).setPreferredWidth(40);
        t.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.add(left(UI.muted("Ghi chú kỹ thuật")));
        mid.add(Box.createVerticalStrut(4));
        mid.add(left(sn));
        mid.add(Box.createVerticalStrut(12));
        mid.add(left(UI.muted("Hạng mục đã thực hiện")));
        mid.add(Box.createVerticalStrut(4));
        mid.add(left(UI.tableBlock(t)));
        mid.add(Box.createVerticalStrut(6));
        mid.add(left(hmWarn));
        c.add(mid, BorderLayout.CENTER);
        return c;
    }

    private JComponent qlColumn() {
        RoundedPanel c = new RoundedPanel();
        c.setLayout(new BorderLayout(0, 10));
        c.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        c.add(UI.h2("Quản lý kiểm tra"), BorderLayout.NORTH);

        for (JCheckBox k : new JCheckBox[]{ckChayThu, ckRoRi, ckVeSinh}) k.setFont(Theme.font(Font.PLAIN, 13));
        qlNote.setLineWrap(true);
        qlNote.setWrapStyleWord(true);
        JScrollPane sn = new JScrollPane(qlNote);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.add(left(UI.muted("Checklist bắt buộc (phải tick đủ mới được duyệt)")));
        mid.add(Box.createVerticalStrut(6));
        mid.add(left(ckChayThu));
        mid.add(left(ckRoRi));
        mid.add(left(ckVeSinh));
        mid.add(Box.createVerticalStrut(12));
        mid.add(left(UI.muted("Nhận xét của quản lý (tùy chọn, sẽ ghi vào ghi chú kỹ thuật)")));
        mid.add(Box.createVerticalStrut(4));
        mid.add(left(sn));
        c.add(mid, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnDuyet);
        btns.add(btnTraVe);
        c.add(btns, BorderLayout.SOUTH);
        return c;
    }

    private static JComponent left(JComponent c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    // ====================================================================== dữ liệu
    @Override
    public void onShow() { reload(); }

    private void reload() {
        Async.run(this, service::phieuChoKiemTra, list -> {
            phieu = list;
            listModel.setRowCount(0);
            for (PhieuCard p : list) {
                String two = "<html><b>" + p.maPhieuSC() + "</b><br><span style='color:#6B7A90'>" + p.bienSo() + " · " + p.tenXe() + "</span></html>";
                listModel.addRow(new Object[]{two, p.tenKhach(), p.tenKTV() == null ? "—" : p.tenKTV(), p.tienDo(), Fmt.dateTime(p.duKien())});
            }
            emptyHint.setVisible(list.isEmpty());
            int keep = -1;
            for (int i = 0; i < list.size(); i++) if (list.get(i).maPhieuSC().equals(selectedId)) keep = i;
            if (keep >= 0) listTable.setRowSelectionInterval(keep, keep);
            else { selectedId = null; clearDetail(); }
            revalidate();
        });
    }

    private void choose(PhieuCard p) {
        selectedId = p.maPhieuSC();
        lbTitle.setText("Kiểm tra phiếu " + p.maPhieuSC() + " · " + p.bienSo() + " · " + p.tenXe());
        ckChayThu.setSelected(false);
        ckRoRi.setSelected(false);
        ckVeSinh.setSelected(false);
        qlNote.setText("");
        Async.run(this, () -> new Object[]{service.ghiChuKyThuat(p.maPhieuSC()), service.hangMuc(p.maPhieuSC())}, r -> {
            if (!p.maPhieuSC().equals(selectedId)) return; // người dùng đã chọn phiếu khác
            @SuppressWarnings("unchecked") List<HangMuc> hms = (List<HangMuc>) r[1];
            String note = (String) r[0];
            ktvNote.setText(note == null || note.isBlank() ? "(Kỹ thuật viên chưa ghi chú)" : note);
            ktvNote.setCaretPosition(0);
            hmModel.setRowCount(0);
            chuaXong = 0;
            for (HangMuc h : hms) {
                hmModel.addRow(new Object[]{h.ten() + (h.phatSinh() ? " (phát sinh)" : ""),
                        "DichVu".equals(h.loai()) ? "Dịch vụ" : "Phụ tùng", h.soLuong(), h.trangThai()});
                if (!"Hoàn thành".equals(h.trangThai()) && !"Từ chối".equals(h.trangThai())) chuaXong++;
            }
            hmWarn.setText(chuaXong > 0 ? "⚠ Còn " + chuaXong + " hạng mục chưa hoàn thành — chưa thể duyệt." : " ");
            updateButtons();
            revalidate();
        });
    }

    private void clearDetail() {
        lbTitle.setText("Chọn một phiếu ở trên để kiểm tra");
        ktvNote.setText("");
        hmModel.setRowCount(0);
        hmWarn.setText(" ");
        ckChayThu.setSelected(false);
        ckRoRi.setSelected(false);
        ckVeSinh.setSelected(false);
        qlNote.setText("");
        chuaXong = 0;
        updateButtons();
    }

    private void updateButtons() {
        boolean has = selectedId != null;
        for (JCheckBox k : new JCheckBox[]{ckChayThu, ckRoRi, ckVeSinh}) k.setEnabled(has);
        qlNote.setEnabled(has);
        btnTraVe.setEnabled(has);
        btnDuyet.setEnabled(has && chuaXong == 0 && ckChayThu.isSelected() && ckRoRi.isSelected() && ckVeSinh.isSelected());
    }

    // ====================================================================== thao tác
    private void duyet() {
        if (selectedId == null) return;
        String id = selectedId;
        if (!Msg.confirm(this, "Xác nhận phiếu " + id + " đã đạt chất lượng và chuyển sang \"Hoàn thành\"?")) return;
        String note = qlNote.getText();
        Async.run(this, () -> { service.duyetHoanThanh(id, note); return null; }, x -> {
            Msg.info(this, "Phiếu " + id + " đã hoàn thành. Nhân viên tiếp nhận có thể lập hóa đơn.");
            selectedId = null;
            reload();
        });
    }

    private void traVe() {
        if (selectedId == null) return;
        String id = selectedId;
        JTextArea ly = new JTextArea(qlNote.getText(), 3, 28);
        ly.setLineWrap(true);
        ly.setWrapStyleWord(true);
        FormDialog.Form f = new FormDialog.Form().add("Cần làm lại *", new JScrollPane(ly));
        if (!FormDialog.show(this, "Trả phiếu " + id + " về làm lại", f, "Trả về",
                () -> ly.getText().isBlank() ? "Vui lòng ghi rõ cần làm lại những gì." : null)) return;
        Async.run(this, () -> { service.traVeLamLai(id, ly.getText()); return null; }, x -> {
            selectedId = null;
            reload();
        });
    }
}

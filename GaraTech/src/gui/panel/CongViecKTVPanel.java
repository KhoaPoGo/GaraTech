package gui.panel;

import bus.YeuCauPhuTungService;
import dao.CongViecKTVDao;
import dao.PhuTungDao;
import entity.PhuTung;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;
import util.Session;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Màn hình công việc kỹ thuật viên.
 * Tất cả dữ liệu hiển thị được tải từ SQL Server, không tạo dữ liệu demo.
 */
public final class CongViecKTVPanel extends BasePanel {

    // Các nhãn này được CongViecKTVDao ánh xạ từ 4 mã trạng thái trong SQL Server.
    private static final String CHO_SUA = "Chờ sửa chữa";
    private static final String DANG_SUA_CHUA = "Đang sửa chữa";
    private static final String CHO_LINH_KIEN = "Chờ linh kiện";
    private static final String HOAN_THANH = "Hoàn thành";

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private final CongViecKTVDao congViecDao = new CongViecKTVDao();
    private final PhuTungDao phuTungDao = new PhuTungDao();
    private final YeuCauPhuTungService yeuCauService = new YeuCauPhuTungService();

    private final List<Ticket> tickets = new ArrayList<>();
    private final JPanel ticketList = column();
    private final JLabel listTitle = heading("Công việc của tôi · 0");
    private final JLabel detailTitle = heading("Chọn phiếu sửa chữa");
    private final JLabel customer = text("—");
    private final JLabel car = text("—");
    private final JLabel lastVisit = text("—");
    private final JLabel state = text("—");
    private final JLabel diagnosis = new WrappedLabel("Chưa có dữ liệu.", 650);
    private final JPanel checklist = column();

    private final DefaultTableModel partsModel = UI.readOnlyModel(
            "Mã / tên phụ tùng", "SL yêu cầu", "Đã xuất", "Đã dùng", "Liên kết");
    private final JTable partsTable = UI.table(partsModel);

    private final JTextField linkedTicket = new JTextField();
    private final JComboBox<PartOption> requestedPart = new JComboBox<>();
    private final JTextField quantity = new JTextField("1");
    private final JTextField partReason = new JTextField("Phục vụ sửa chữa");
    private final JTextField percent = new JTextField("0%");
    private final JTextField updatedAt = new JTextField();
    private final JTextArea note = new JTextArea(3, 30);

    private final JLabel feedback = muted(" ");

    // Ba nút trạng thái được đặt trong khu vực Cập nhật kết quả.
    private final JButton startWork = UI.secondaryButton("Đang sửa chữa");
    private final JButton waitingParts = UI.secondaryButton("Chờ linh kiện");
    private final JButton complete = UI.primaryButton("Hoàn tất");

    private final JButton sendRequest = UI.secondaryButton("Gửi yêu cầu kho");
    private final JButton shortage = UI.secondaryButton("Báo thiếu phụ tùng");
    private final JButton requestLink = link("+ Yêu cầu phụ tùng");
    private final JButton saveProgress = UI.primaryButton("Lưu kết quả");

    private final JPanel resultCard;
    private Ticket selected;
    private boolean loading;

    public CongViecKTVPanel() {
        super("KỸ THUẬT VIÊN", "Công việc kỹ thuật viên",
                "Theo dõi phiếu được phân công, cập nhật hạng mục và yêu cầu phụ tùng.");

        // Panel này giữ bố cục riêng theo thiết kế CongViecKTVPanel.
        removeAll();
        setLayout(new BorderLayout(0, 24));
        setBorder(UI.pad(26, 28, 24, 24));
        add(buildHeader(), BorderLayout.NORTH);

        JPanel right = column();
        addStack(right, buildDetailCard(), 16);
        addStack(right, buildPartsCard(), 16);
        resultCard = buildResultCard();
        addStack(right, resultCard, 0);

        JPanel left = column();
        RoundedPanel listCard = card();
        listCard.add(listTitle, BorderLayout.NORTH);
        listCard.add(ticketList, BorderLayout.CENTER);
        addStack(left, listCard, 16);
        addStack(left, infoBox(
                "Chỉ hiển thị phiếu sửa chữa được phân công cho tài khoản đang đăng nhập. "
                        + "Các thao tác được lưu trực tiếp vào CSDL.", false), 0);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.25;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 0, 16);
        left.setPreferredSize(new Dimension(270, 0));
        left.setMinimumSize(new Dimension(205, 0));
        columns.add(left, c);

        c.gridx = 1;
        c.weightx = 0.75;
        c.insets = new Insets(0, 0, 0, 0);
        right.setMinimumSize(new Dimension(500, 0));
        columns.add(right, c);

        JPanel center = new JPanel(new BorderLayout(0, 26));
        center.setOpaque(false);
        center.add(columns, BorderLayout.NORTH);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(small("GaraTech · Dữ liệu lấy từ cơ sở dữ liệu"), BorderLayout.WEST);
        footer.add(small("Chi nhánh Trung tâm"), BorderLayout.EAST);
        center.add(footer, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        saveProgress.addActionListener(e -> saveUpdate());
        startWork.addActionListener(e -> startSelectedWork());
        waitingParts.addActionListener(e -> markSelectedItemWaitingForParts());
        complete.addActionListener(e -> submitForInspection());
        requestLink.addActionListener(e -> {
            requestedPart.requestFocusInWindow();
            requestedPart.showPopup();
        });
        sendRequest.addActionListener(e -> sendPartsRequest(false));
        shortage.addActionListener(e -> sendPartsRequest(true));

        note.getDocument().addDocumentListener(changed(this::updateButtons));

        // Nạp dữ liệu thật ngay khi mở màn hình.
        reloadFromDatabase(null, true);
    }

    // ---------------------------------------------------------------------
    // DỰNG GIAO DIỆN
    // ---------------------------------------------------------------------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel labels = column();
        String userId = Session.maNV() == null ? "Chưa đăng nhập" : Session.maNV();
        String name = Session.user() == null ? "Kỹ thuật viên" : Session.user().hoTen();

        labels.add(small(userId + " · CÔNG VIỆC ĐƯỢC PHÂN CÔNG"));
        labels.add(Box.createVerticalStrut(8));
        labels.add(UI.label("Công việc kỹ thuật viên", Font.BOLD, 27, Theme.TEXT));
        labels.add(Box.createVerticalStrut(8));
        labels.add(muted(name + " · Dữ liệu được đồng bộ từ GaraTechDB."));
        header.add(labels, BorderLayout.CENTER);

        // Nút bắt đầu thi công đã được chuyển xuống Cập nhật kết quả.
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        action.setOpaque(false);
        action.add(saveProgress);
        header.add(action, BorderLayout.EAST);
        return header;
    }

    private JPanel buildDetailCard() {
        RoundedPanel panel = card();
        JPanel top = transparent(new BorderLayout(8, 0));
        top.add(detailTitle, BorderLayout.CENTER);

        JButton history = link("Lịch sử xe ↗");
        history.addActionListener(e -> showHistory());
        top.add(history, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        JPanel stack = column();
        JPanel info = transparent(new GridBagLayout());
        addInfo(info, 0, "Khách hàng / biển số", customer, state);
        addInfo(info, 1, "Phương tiện / ODO", car, null);
        addInfo(info, 2, "Phiếu tiếp nhận", lastVisit, null);
        addStack(stack, info, 14);

        RoundedPanel diagnostic = new RoundedPanel(5, Theme.INFO_BG).borderColor(null);
        diagnostic.setLayout(new BorderLayout(9, 0));
        diagnostic.setBorder(UI.pad(10, 12, 10, 12));
        diagnostic.add(new JLabel(new SymbolIcon(true)), BorderLayout.WEST);
        diagnosis.setFont(Theme.font(Font.PLAIN, 11));
        diagnosis.setForeground(Theme.INFO);
        diagnostic.add(diagnosis, BorderLayout.CENTER);
        addStack(stack, diagnostic, 10);
        addStack(stack, checklist, 10);

        panel.add(stack, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPartsCard() {
        RoundedPanel panel = card();
        JPanel head = transparent(new BorderLayout());
        head.add(heading("Phụ tùng yêu cầu / đã xuất"), BorderLayout.WEST);
        head.add(requestLink, BorderLayout.EAST);
        panel.add(head, BorderLayout.NORTH);

        JPanel content = column();
        partsTable.setRowHeight(49);
        partsTable.setFont(Theme.font(Font.PLAIN, 11));
        partsTable.getTableHeader().setFont(Theme.font(Font.PLAIN, 10));
        partsTable.getTableHeader().setBackground(new Color(0xF6F7FA));
        partsTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        partsTable.setRowSelectionAllowed(false);
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer();
        cell.setBorder(UI.pad(0, 8, 0, 8));
        partsTable.setDefaultRenderer(Object.class, cell);
        int[] widths = {190, 115, 120, 120, 170};
        for (int i = 0; i < widths.length; i++) {
            partsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        addStack(content, UI.tableBlock(partsTable), 16);

        linkedTicket.setEditable(false);
        linkedTicket.setBackground(Color.WHITE);
        JPanel form = transparent(new GridLayout(2, 2, 14, 12));
        form.add(fieldBlock("Phiếu sửa chữa liên kết", linkedTicket));
        form.add(fieldBlock("Phụ tùng yêu cầu", requestedPart));
        form.add(fieldBlock("Số lượng yêu cầu", quantity));
        form.add(fieldBlock("Lý do yêu cầu", partReason));
        addStack(content, form, 14);

        JPanel buttons = transparent(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.add(sendRequest);
        buttons.add(shortage);
        addStack(content, buttons, 0);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Khu vực cập nhật kết quả gồm tiến độ, thời gian bắt đầu, ghi chú
     * và ba nút trạng thái: Đang sửa chữa, Chờ linh kiện, Hoàn tất.
     */
    private JPanel buildResultCard() {
        RoundedPanel panel = card();
        panel.add(heading("Cập nhật kết quả"), BorderLayout.NORTH);

        JPanel content = column();

        JPanel form = transparent(new GridLayout(1, 2, 16, 0));
        percent.setEditable(false);
        percent.setBackground(new Color(0xF6F7FA));
        form.add(fieldBlock("Tiến độ phiếu sửa chữa", percent));

        updatedAt.setEditable(false);
        updatedAt.setBackground(new Color(0xF6F7FA));
        form.add(fieldBlock("Thời gian bắt đầu", updatedAt));
        addStack(content, form, 16);

        note.setFont(Theme.font(Font.PLAIN, 12));
        note.setForeground(Theme.TEXT);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setBorder(UI.pad(10, 10, 10, 10));
        note.getAccessibleContext().setAccessibleName("Kết quả và ghi chú kỹ thuật");
        JScrollPane noteScroll = new JScrollPane(note);
        noteScroll.setPreferredSize(new Dimension(0, 100));
        addStack(content,
                fieldBlock("Kết quả / ghi chú kỹ thuật (tối đa 500 ký tự)", noteScroll),
                14);

        addStack(content, infoBox(
                "Database hiện quản lý trạng thái ở cấp phiếu sửa chữa: "
                        + "Chờ sửa chữa, Đang sửa chữa, Chờ linh kiện và Hoàn thành. "
                        + "Khi thiếu linh kiện, phiếu được tạm chuyển sang Chờ linh kiện; "
                        + "khi có đủ linh kiện, chọn Đang sửa chữa để tiếp tục.",
                true), 14);

        // Ba nút cùng nằm trong khu vực Cập nhật kết quả.
        JPanel statusButtons = transparent(new GridLayout(1, 3, 10, 0));
        statusButtons.add(startWork);
        statusButtons.add(waitingParts);
        statusButtons.add(complete);
        addStack(content, statusButtons, 10);

        feedback.setFont(Theme.font(Font.PLAIN, 11));
        addStack(content, feedback, 0);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------------
    // NẠP DỮ LIỆU TỪ SQL SERVER
    // ---------------------------------------------------------------------

    private void reloadFromDatabase(String maPhieuCanChon, boolean showError) {
        String maKTV = Session.maNV();
        if (maKTV == null || maKTV.isBlank()) {
            tickets.clear();
            selected = null;
            showEmpty("Không xác định được nhân viên đăng nhập. Vui lòng đăng nhập lại.");
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            List<PhuTung> catalog = phuTungDao.findAll();
            loadPartOptions(catalog);

            List<CongViecKTVDao.TicketRow> rows = congViecDao.findByTechnician(maKTV);
            tickets.clear();
            for (CongViecKTVDao.TicketRow row : rows) {
                Ticket ticket = new Ticket(row);
                ticket.items.addAll(congViecDao.findWorkItems(row.maPhieuSC()));
                ticket.parts.addAll(congViecDao.findPartsByRepair(row.maPhieuSC()));
                tickets.add(ticket);
            }

            rebuildTicketList();
            Ticket target = null;
            if (maPhieuCanChon != null) {
                target = tickets.stream()
                        .filter(t -> t.row.maPhieuSC().equals(maPhieuCanChon))
                        .findFirst().orElse(null);
            }
            if (target == null && selected != null) {
                String oldId = selected.row.maPhieuSC();
                target = tickets.stream()
                        .filter(t -> t.row.maPhieuSC().equals(oldId))
                        .findFirst().orElse(null);
            }
            if (target == null && !tickets.isEmpty()) {
                target = tickets.get(0);
            }

            if (target == null) {
                selected = null;
                showEmpty("Hiện chưa có phiếu sửa chữa nào được phân công cho bạn.");
            } else {
                selectTicket(target);
            }
        } catch (Exception ex) {
            if (showError) {
                Msg.error(this,
                        "Không thể tải dữ liệu công việc từ CSDL.\n" + errorMessage(ex));
            }
            tickets.clear();
            selected = null;
            showEmpty("Không thể tải dữ liệu từ GaraTechDB.");
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void loadPartOptions(List<PhuTung> catalog) {
        DefaultComboBoxModel<PartOption> model = new DefaultComboBoxModel<>();
        for (PhuTung pt : catalog) {
            model.addElement(new PartOption(
                    pt.getMaPhuTung(), pt.getTenPhuTung(),
                    pt.getDonViTinh(), pt.getSoLuongTon()));
        }
        requestedPart.setModel(model);
        requestedPart.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof PartOption option) {
                    label.setText(option.toString());
                }
                return label;
            }
        });
    }

    private void selectTicket(Ticket ticket) {
        if (ticket == null) return;
        if (selected != null && selected != ticket && isWorking(selected)) {
            String oldNote = selected.row.ghiChuKyThuat() == null
                    ? "" : selected.row.ghiChuKyThuat();
            if (!Objects.equals(note.getText().trim(), oldNote)
                    && !Msg.confirm(this,
                    "Ghi chú của phiếu " + selected.row.maPhieuSC()
                            + " chưa được lưu. Bạn có muốn bỏ qua thay đổi này không?")) {
                return;
            }
        }

        selected = ticket;
        loading = true;
        try {
            CongViecKTVDao.TicketRow row = ticket.row;
            detailTitle.setText("Chi tiết sửa chữa · " + row.maPhieuSC());
            customer.setText(nullToDash(row.tenKhach()) + " · " + nullToDash(row.bienSo())
                    + (row.soDienThoai() == null ? "" : " · " + row.soDienThoai()));
            car.setText(nullToDash(row.thongTinXe()) + " · ODO "
                    + formatNumber(row.odo()) + " km");
            lastVisit.setText("Tiếp nhận " + formatTime(row.thoiGianTiepNhan())
                    + " · " + row.maPhieuTN());
            state.setText(row.trangThai());
            state.setOpaque(true);
            state.setFont(Theme.font(Font.BOLD, 11));
            state.setBorder(UI.pad(5, 8, 5, 8));
            Color[] colors = Theme.statusColors(row.trangThai());
            state.setBackground(colors[0]);
            state.setForeground(colors[1]);

            String diagnosticText = "Tình trạng ban đầu: "
                    + nullToDash(row.tinhTrangBanDau())
                    + "\nYêu cầu khách hàng: "
                    + nullToDash(row.yeuCauKhachHang());
            diagnosis.setText("<html>" + escapeHtml(diagnosticText)
                    .replace("\n", "<br>") + "</html>");

            linkedTicket.setText(row.maPhieuSC());
            percent.setText(ticket.tienDo() + "%");
            updatedAt.setText(row.thoiGianBatDau() == null
                    ? "Chưa bắt đầu" : formatTime(row.thoiGianBatDau()));
            note.setText(row.ghiChuKyThuat() == null ? "" : row.ghiChuKyThuat());
            note.setCaretPosition(0);
            rebuildChecklist();
            rebuildParts();
            rebuildTicketList();
            feedback.setForeground(Theme.MUTED);
            feedback.setText(isWorking(ticket)
                    ? "Dữ liệu đang được đồng bộ từ CSDL."
                    : "Phiếu ở chế độ chỉ xem hoặc chờ bắt đầu.");
        } finally {
            loading = false;
        }
        updateButtons();
        revalidate();
        repaint();
    }

    private void rebuildChecklist() {
        checklist.removeAll();

        if (selected == null || selected.items.isEmpty()) {
            addStack(checklist, muted("Phiếu chưa có dịch vụ hoặc linh kiện chi tiết."), 0);
            return;
        }

        for (CongViecKTVDao.WorkItemRow item : selected.items) {
            JPanel row = transparent(new BorderLayout(0, 0));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    UI.pad(10, 0, 11, 0)));

            JPanel labels = column();
            labels.add(UI.label(item.tenHangMuc(), Font.BOLD, 12, Theme.TEXT));

            String description = item.loaiHangMuc()
                    + " · Số lượng: " + item.soLuong()
                    + (item.ghiChu() == null || item.ghiChu().isBlank()
                    ? "" : " · " + item.ghiChu());
            labels.add(small(description));
            row.add(labels, BorderLayout.CENTER);
            addStack(checklist, row, 0);
        }
    }

    private void rebuildParts() {
        partsModel.setRowCount(0);
        if (selected == null) return;

        for (CongViecKTVDao.PartRow part : selected.parts) {
            String requestedText = part.soLuongYeuCau()
                    + " " + nullToDash(part.donViTinh());
            String issuedText = part.soLuongDaXuat() > 0
                    ? part.soLuongDaXuat() + " " + nullToDash(part.donViTinh())
                    : "Chưa xuất";
            String reference = part.maPhieuXK() + " · " + part.trangThaiPhieu();
            partsModel.addRow(new Object[]{
                    part.maPhuTung() + " · " + part.tenPhuTung(),
                    requestedText,
                    issuedText,
                    "Chưa theo dõi",
                    reference
            });
        }
    }

    private void rebuildTicketList() {
        ticketList.removeAll();
        listTitle.setText("Công việc của tôi · " + tickets.size());

        for (Ticket ticket : tickets) {
            CongViecKTVDao.TicketRow row = ticket.row;
            JPanel item = column();
            item.setBorder(UI.pad(0, 0, 18, 0));
            if (ticket == selected) {
                JLabel tag = UI.label("Đang chọn", Font.PLAIN, 11, Theme.WARN);
                tag.setOpaque(true);
                tag.setBackground(Theme.YELLOW_SOFT);
                tag.setBorder(UI.pad(4, 7, 4, 7));
                item.add(tag);
                item.add(Box.createVerticalStrut(12));
            }
            item.add(UI.label(row.bienSo(), Font.BOLD,
                    ticket == selected ? 20 : 17, Theme.TEXT));
            item.add(Box.createVerticalStrut(8));
            item.add(UI.label(row.maPhieuSC(), Font.PLAIN, 12, Theme.INFO));
            item.add(Box.createVerticalStrut(8));
            item.add(UI.label(row.tenKhach() + " · " + row.thongTinXe(),
                    Font.PLAIN, 11, Theme.TEXT));
            item.add(Box.createVerticalStrut(10));
            item.add(small(row.trangThai() + " · " + ticket.tienDo() + "%"));

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(ticket.tienDo());
            bar.setForeground(Theme.YELLOW);
            bar.setBackground(Theme.TRACK);
            bar.setBorderPainted(false);
            bar.setPreferredSize(new Dimension(0, 5));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
            item.add(bar);
            item.add(Box.createVerticalStrut(10));
            item.add(small("Dự kiến: " + formatTime(row.thoiGianDuKien())));

            installClick(item, () -> selectTicket(ticket));
            addStack(ticketList, item, 0);
        }
        ticketList.revalidate();
        ticketList.repaint();
    }

    // ---------------------------------------------------------------------
    // NGHIỆP VỤ: BẮT ĐẦU, CHECKLIST, GHI CHÚ, CHỜ LINH KIỆN, HOÀN TẤT
    // ---------------------------------------------------------------------

    private void startSelectedWork() {
        if (selected == null
                || !(CHO_SUA.equals(selected.row.trangThai())
                || CHO_LINH_KIEN.equals(selected.row.trangThai()))) {
            return;
        }

        String action = CHO_LINH_KIEN.equals(selected.row.trangThai())
                ? "Tiếp tục sửa chữa sau khi đã có linh kiện cho phiếu "
                : "Bắt đầu sửa chữa phiếu ";
        if (!Msg.confirm(this, action + selected.row.maPhieuSC() + "?")) {
            return;
        }

        String maPhieu = selected.row.maPhieuSC();
        try {
            boolean updated = congViecDao.startWork(maPhieu, Session.maNV());
            if (!updated) {
                throw new SQLException(
                        "Phiếu không còn ở trạng thái Chờ sửa chữa/Chờ linh kiện "
                                + "hoặc không thuộc kỹ thuật viên này.");
            }
            reloadFromDatabase(maPhieu, true);
            Msg.info(this, "Phiếu đã chuyển sang Đang sửa chữa.");
        } catch (Exception ex) {
            Msg.error(this, errorMessage(ex));
        }
    }

    /** Chuyển trạng thái toàn bộ phiếu sang CHO_LINH_KIEN theo schema mới. */
    private void markSelectedItemWaitingForParts() {
        if (selected == null || !DANG_SUA_CHUA.equals(selected.row.trangThai())) {
            return;
        }

        if (!Msg.confirm(this,
                "Chuyển phiếu " + selected.row.maPhieuSC()
                        + " sang trạng thái Chờ linh kiện?")) {
            return;
        }

        String maPhieu = selected.row.maPhieuSC();
        try {
            boolean updated = congViecDao.markWaitingForParts(
                    maPhieu, Session.maNV());
            if (!updated) {
                throw new SQLException(
                        "Không thể chuyển phiếu sang Chờ linh kiện. "
                                + "Phiếu có thể đã thay đổi trạng thái hoặc không thuộc bạn.");
            }

            reloadFromDatabase(maPhieu, true);
            feedback.setForeground(Theme.SUCCESS);
            feedback.setText("Phiếu đã được lưu trạng thái Chờ linh kiện vào CSDL.");
            Msg.info(this, "Phiếu đã chuyển sang Chờ linh kiện.");
        } catch (Exception ex) {
            Msg.error(this, errorMessage(ex));
            reloadFromDatabase(maPhieu, false);
        }
    }

    private void saveUpdate() {
        if (selected == null || !isWorking(selected)) return;

        String content = note.getText() == null ? "" : note.getText().trim();
        if (content.length() > 500) {
            Msg.error(this, "Ghi chú kỹ thuật không được vượt quá 500 ký tự.");
            return;
        }

        try {
            boolean updated = congViecDao.saveTechnicalNote(
                    selected.row.maPhieuSC(), Session.maNV(), content);
            if (!updated) {
                throw new SQLException(
                        "Không thể lưu ghi chú. Phiếu có thể đã đổi trạng thái.");
            }
            feedback.setForeground(Theme.SUCCESS);
            feedback.setText("Đã lưu ghi chú kỹ thuật vào CSDL.");
        } catch (Exception ex) {
            Msg.error(this, errorMessage(ex));
        }
    }

    /** Hoàn tất thi công: schema mới chuyển trực tiếp phiếu sang HOAN_TAT. */
    private void submitForInspection() {
        if (selected == null || !DANG_SUA_CHUA.equals(selected.row.trangThai())) {
            return;
        }
        if (note.getText().trim().isBlank()) {
            Msg.error(this, "Vui lòng nhập kết quả/ghi chú kỹ thuật trước khi hoàn tất.");
            return;
        }
        if (!Msg.confirm(this,
                "Xác nhận hoàn tất phiếu " + selected.row.maPhieuSC()
                        + "? Phiếu sẽ chuyển sang trạng thái HOAN_TAT.")) {
            return;
        }

        String maPhieu = selected.row.maPhieuSC();
        try {
            boolean updated = congViecDao.submitForInspection(
                    maPhieu, Session.maNV(), note.getText());
            if (!updated) {
                throw new SQLException(
                        "Không thể hoàn tất phiếu. Kiểm tra phiếu có dịch vụ/linh kiện "
                                + "và đang ở trạng thái Đang sửa chữa hay không.");
            }
            reloadFromDatabase(maPhieu, true);
            Msg.info(this, "Đã hoàn tất sửa chữa. Trạng thái phiếu: HOAN_TAT.");
        } catch (Exception ex) {
            Msg.error(this, errorMessage(ex));
        }
    }

    // ---------------------------------------------------------------------
    // NGHIỆP VỤ: YÊU CẦU / BÁO THIẾU PHỤ TÙNG
    // ---------------------------------------------------------------------

    private void sendPartsRequest(boolean isShortage) {
        if (selected == null || !isWorking(selected)) return;

        PartOption part = (PartOption) requestedPart.getSelectedItem();
        if (part == null) {
            Msg.error(this, "CSDL chưa có danh mục phụ tùng để chọn.");
            return;
        }

        final int qty;
        try {
            String raw = quantity.getText().trim();
            if (!raw.matches("[1-9][0-9]*")) {
                throw new NumberFormatException();
            }
            qty = Integer.parseInt(raw);
            if (qty <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            Msg.error(this,
                    "Số lượng phải là số nguyên dương, ví dụ 1 hoặc 2.");
            return;
        }

        String reason = partReason.getText() == null
                ? "" : partReason.getText().trim();
        if (reason.isBlank()) {
            Msg.error(this, "Vui lòng nhập lý do yêu cầu phụ tùng.");
            return;
        }
        if (isShortage && !reason.toLowerCase().contains("thiếu")) {
            reason = "Báo thiếu phụ tùng: " + reason;
        }

        String confirmText = (isShortage
                ? "Gửi báo thiếu phụ tùng " : "Gửi yêu cầu kho ")
                + part.tenPhuTung() + " · Số lượng: " + qty + " "
                + part.donViTinh()
                + "?\nPhiếu yêu cầu sẽ ở trạng thái Chờ xác nhận; tồn kho chưa bị trừ.";
        if (!Msg.confirm(this, confirmText)) return;

        try {
            List<YeuCauPhuTungService.ChiTietYeuCauInput> details = List.of(
                    new YeuCauPhuTungService.ChiTietYeuCauInput(
                            part.maPhuTung(), qty, reason));

            String maPhieuXK = yeuCauService.guiYeuCauXuatPhuTung(
                    selected.row.maPhieuSC(), Session.maNV(), reason, details);
            String maPhieuSC = selected.row.maPhieuSC();
            reloadFromDatabase(maPhieuSC, true);
            feedback.setForeground(Theme.SUCCESS);
            feedback.setText(
                    "Đã ghi yêu cầu " + maPhieuXK + " vào CSDL · Chờ xác nhận.");
            Msg.info(this,
                    "Gửi yêu cầu phụ tùng thành công.\nMã phiếu xuất kho: "
                            + maPhieuXK
                            + "\nTrạng thái: Chờ xác nhận."
                            + "\nTồn kho chỉ được trừ khi kho duyệt.");
        } catch (Exception ex) {
            Msg.error(this, errorMessage(ex));
        }
    }

    // ---------------------------------------------------------------------
    // LỊCH SỬ XE, TRẠNG THÁI NÚT VÀ TRẠNG THÁI RỖNG
    // ---------------------------------------------------------------------

    private void showHistory() {
        if (selected == null) return;

        try {
            List<CongViecKTVDao.HistoryRow> historyRows =
                    congViecDao.findVehicleHistory(
                            selected.row.bienSo(), selected.row.maPhieuSC());
            StringBuilder text = new StringBuilder("LỊCH SỬ XE ")
                    .append(selected.row.bienSo()).append("\n\n");

            if (historyRows.isEmpty()) {
                text.append("Chưa có phiếu sửa chữa trước đó trong CSDL.");
            } else {
                for (CongViecKTVDao.HistoryRow row : historyRows) {
                    text.append(row.maPhieuSC()).append(" · ")
                            .append(row.trangThai()).append(" · ")
                            .append(formatTime(row.thoiGian())).append("\n")
                            .append("Yêu cầu: ")
                            .append(nullToDash(row.yeuCau())).append("\n\n");
                }
            }

            JTextArea area = new JTextArea(text.toString());
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(Theme.font(Font.PLAIN, 13));
            area.setBorder(UI.pad(14, 14, 14, 14));
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(540, 300));
            JOptionPane.showMessageDialog(
                    this, scroll, "Lịch sử xe", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            Msg.error(this,
                    "Không thể tải lịch sử xe.\n" + errorMessage(ex));
        }
    }

    private void updateButtons() {
        if (loading) return;

        boolean working = selected != null && isWorking(selected);
        boolean canStart = selected != null
                && (CHO_SUA.equals(selected.row.trangThai())
                || CHO_LINH_KIEN.equals(selected.row.trangThai()));
        boolean canWaitForParts = selected != null
                && DANG_SUA_CHUA.equals(selected.row.trangThai());
        boolean canComplete = selected != null
                && DANG_SUA_CHUA.equals(selected.row.trangThai());

        startWork.setEnabled(canStart);
        waitingParts.setEnabled(canWaitForParts);
        complete.setEnabled(canComplete && !note.getText().isBlank());

        percent.setEditable(false);
        note.setEditable(working);
        requestedPart.setEnabled(working);
        quantity.setEditable(working);
        partReason.setEditable(working);
        sendRequest.setEnabled(working);
        shortage.setEnabled(working);
        requestLink.setEnabled(working);
        saveProgress.setEnabled(working);
    }

    private void showEmpty(String message) {
        selected = null;
        loading = true;
        detailTitle.setText("Không có phiếu được phân công");
        customer.setText("—");
        car.setText("—");
        lastVisit.setText("—");
        state.setText("—");
        diagnosis.setText("<html>" + escapeHtml(message) + "</html>");
        linkedTicket.setText("");
        percent.setText("0%");
        updatedAt.setText("");
        note.setText("");
        checklist.removeAll();
        partsModel.setRowCount(0);
        ticketList.removeAll();
        listTitle.setText("Công việc của tôi · 0");
        loading = false;
        updateButtons();
    }

    @Override
    public void onShow() {
        reloadFromDatabase(
                selected == null ? null : selected.row.maPhieuSC(), true);
    }

    // ---------------------------------------------------------------------
    // HÀM TIỆN ÍCH
    // ---------------------------------------------------------------------

    private int tienDo(Ticket ticket) {
        return ticket.tienDo();
    }

    private static boolean isWorking(Ticket ticket) {
        return ticket != null
                && (DANG_SUA_CHUA.equals(ticket.row.trangThai())
                || CHO_LINH_KIEN.equals(ticket.row.trangThai()));
    }

    private static String formatTime(LocalDateTime value) {
        return value == null ? "Chưa xác định" : TIME.format(value);
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format("%,.0f", value);
        }
        return String.format("%,.1f", value);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String errorMessage(Throwable ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank()
                ? ex.getClass().getSimpleName() : message;
    }

    private static DocumentListener changed(Runnable task) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                task.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                task.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                task.run();
            }
        };
    }

    private static void addInfo(
            JPanel panel, int row, String label, JLabel value, JLabel extra) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 10, 14);
        panel.add(muted(label), g);

        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        value.setFont(Theme.font(Font.BOLD, 12));
        panel.add(value, g);

        if (extra != null) {
            g.gridx = 2;
            g.weightx = 0;
            g.insets = new Insets(0, 0, 10, 0);
            panel.add(extra, g);
        }
    }

    private static JPanel fieldBlock(String title, JComponent field) {
        JPanel p = transparent(new BorderLayout(0, 7));
        p.add(UI.label(title, Font.BOLD, 11, Theme.TEXT), BorderLayout.NORTH);
        if (!(field instanceof JScrollPane)) {
            field.setFont(Theme.font(Font.PLAIN, 12));
            field.setPreferredSize(new Dimension(0, 39));
            field.getAccessibleContext().setAccessibleName(title);
        }
        p.add(field, BorderLayout.CENTER);
        p.setMinimumSize(new Dimension(0, 0));
        return p;
    }

    private static RoundedPanel infoBox(String message, boolean warning) {
        RoundedPanel p = new RoundedPanel(
                5, warning ? Theme.YELLOW_SOFT : Theme.INFO_BG).borderColor(null);
        p.setLayout(new BorderLayout(9, 0));
        p.setBorder(UI.pad(12, 12, 12, 12));
        Color fg = warning ? Theme.WARN : Theme.INFO;
        JLabel icon = new JLabel(new SymbolIcon(true));
        icon.setForeground(fg);
        icon.setVerticalAlignment(SwingConstants.TOP);
        p.add(icon, BorderLayout.WEST);
        JLabel label = new WrappedLabel(message, warning ? 400 : 220);
        label.setFont(Theme.font(Font.PLAIN, 11));
        label.setForeground(fg);
        p.add(label, BorderLayout.CENTER);
        return p;
    }

    private static RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(10, Color.WHITE);
        p.setLayout(new BorderLayout(0, 16));
        p.setBorder(UI.pad(18, 18, 18, 18));
        p.setMinimumSize(new Dimension(0, 0));
        return p;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    private static void addStack(JPanel parent, JComponent child, int gap) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE,
                        getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.add(child, BorderLayout.CENTER);
        parent.add(wrapper);
        if (gap > 0) {
            parent.add(Box.createVerticalStrut(gap));
        }
    }

    private static JLabel text(String s) {
        return UI.label(s, Font.PLAIN, 12, Theme.TEXT);
    }

    private static JLabel muted(String s) {
        return UI.label(s, Font.PLAIN, 12, Theme.MUTED);
    }

    private static JLabel small(String s) {
        return UI.label(s, Font.PLAIN, 10, Theme.MUTED);
    }

    private static JLabel heading(String s) {
        return UI.label(s, Font.BOLD, 16, Theme.TEXT);
    }

    private static JButton link(String title) {
        JButton b = new JButton(title);
        b.setBorder(UI.pad(0, 0, 0, 0));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(Theme.font(Font.PLAIN, 11));
        b.setForeground(Theme.INFO);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void installClick(Component component, Runnable action) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installClick(child, action);
            }
        }
    }

    private static final class WrappedLabel extends JLabel {
        private final int defaultWidth;

        WrappedLabel(String text, int defaultWidth) {
            super("<html>" + text + "</html>");
            this.defaultWidth = defaultWidth;
        }

        @Override
        public Dimension getPreferredSize() {
            Object html = getClientProperty(
                    javax.swing.plaf.basic.BasicHTML.propertyKey);
            if (html instanceof javax.swing.text.View view) {
                int width = getWidth() > 0 ? getWidth() : defaultWidth;
                view.setSize(width, 0);
                return new Dimension(defaultWidth,
                        (int) Math.ceil(view.getPreferredSpan(
                                javax.swing.text.View.Y_AXIS)));
            }
            return super.getPreferredSize();
        }
    }

    private static final class SymbolIcon implements Icon {
        private final boolean info;

        SymbolIcon(boolean info) {
            this.info = info;
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(c.getForeground() == null
                    ? Theme.INFO : c.getForeground());
            p.setStroke(new BasicStroke(1.3f));
            if (info) {
                p.drawOval(x + 1, y + 1, 13, 13);
                p.setFont(new Font("Dialog", Font.BOLD, 11));
                p.drawString("i", x + 6, y + 12);
            } else {
                p.drawRoundRect(x + 2, y + 2, 12, 12, 2, 2);
                p.drawRect(x + 5, y + 2, 6, 4);
                p.drawRect(x + 5, y + 9, 6, 5);
            }
            p.dispose();
        }
    }

    private static final class PartOption {
        final String maPhuTung;
        final String tenPhuTung;
        final String donViTinh;
        final int soLuongTon;

        PartOption(
                String maPhuTung, String tenPhuTung,
                String donViTinh, int soLuongTon) {
            this.maPhuTung = maPhuTung;
            this.tenPhuTung = tenPhuTung;
            this.donViTinh = donViTinh;
            this.soLuongTon = soLuongTon;
        }

        String maPhuTung() {
            return maPhuTung;
        }

        String tenPhuTung() {
            return tenPhuTung;
        }

        String donViTinh() {
            return donViTinh;
        }

        @Override
        public String toString() {
            return maPhuTung + " · " + tenPhuTung
                    + " · Tồn: " + soLuongTon + " " + donViTinh;
        }
    }

    private static final class Ticket {
        final CongViecKTVDao.TicketRow row;
        final List<CongViecKTVDao.WorkItemRow> items = new ArrayList<>();
        final List<CongViecKTVDao.PartRow> parts = new ArrayList<>();

        Ticket(CongViecKTVDao.TicketRow row) {
            this.row = row;
        }

        int tienDo() {
            long total = items.size();
            if (total == 0) return 0;
            long completed = items.stream()
                    .filter(i -> HOAN_THANH.equals(i.trangThai()))
                    .count();
            return (int) (completed * 100 / total);
        }
    }
}

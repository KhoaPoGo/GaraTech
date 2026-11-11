package gui.panel;

import bus.YeuCauPhuTungService;
import dao.PhuTungDao;
import dao.PhieuXuatKhoDao;
import dao.YeuCauPhuTungDao;
import entity.PhuTung;
import enums.TrangThaiXuatKho;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.StatCard;
import gui.component.StatusBadge;
import gui.component.StatusCellRenderer;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;
import util.Session;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Màn hình yêu cầu phụ tùng của kỹ thuật viên.
 * Dữ liệu danh mục, phiếu sửa chữa, lịch sử và chi tiết đều được đọc từ SQL Server.
 * Tạo yêu cầu/hủy yêu cầu được ghi qua BUS và DAO, không lưu dữ liệu demo trong bộ nhớ.
 */
public final class YeuCauPhuTungPanel extends BasePanel {

    protected YeuCauPhuTungPanel(String eyebrow, String title, String subtitle) {
		super(eyebrow, title, subtitle);
		// TODO Auto-generated constructor stub
	}

	private static final String PENDING = TrangThaiXuatKho.CHO_XAC_NHAN.label();
    private static final String ISSUED = TrangThaiXuatKho.DA_XUAT_KHO.label();
    private static final String CANCELED = TrangThaiXuatKho.DA_HUY.label();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final YeuCauPhuTungDao queryDao = new YeuCauPhuTungDao();
    private final PhuTungDao phuTungDao = new PhuTungDao();
    private final PhieuXuatKhoDao phieuXuatKhoDao = new PhieuXuatKhoDao();
    private final YeuCauPhuTungService service = new YeuCauPhuTungService(phieuXuatKhoDao);

    private final List<RepairTicket> tickets = new ArrayList<>();
    private final List<Part> parts = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();
    private final List<Request> visible = new ArrayList<>();
    private final List<Line> draft = new ArrayList<>();

    private final JComboBox<RepairTicket> ticket = new JComboBox<>();
    private final JComboBox<Part> part = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final JLabel stock = small(" ");
    private final JTextArea reason = new JTextArea(3, 20);

    private final DefaultTableModel draftModel = UI.readOnlyModel("Phụ tùng", "Số lượng");
    private final JTable draftTable = UI.table(draftModel);
    private final JButton addLine = UI.secondaryButton("+ Thêm vào yêu cầu");
    private final JButton removeLine = UI.secondaryButton("Xóa dòng");
    private final JButton send = UI.primaryButton("Gửi yêu cầu kho");

    private final JTextField search = new JTextField();
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Tất cả trạng thái", PENDING, ISSUED, CANCELED});
    private final DefaultTableModel historyModel = UI.readOnlyModel(
            "Yêu cầu / ngày gửi", "Phiếu / biển số", "Trạng thái");
    private final JTable historyTable = UI.table(historyModel);
    private final JLabel count = small(" ");

    private final JLabel detailTitle = UI.h2("Chi tiết yêu cầu");
    private final StatusBadge badge = new StatusBadge("Chưa chọn");
    private final JLabel detailTicket = small(" ");
    private final JLabel detailTime = small(" ");
    private final WrappedLabel detailReason = new WrappedLabel("", 500);
    private final WrappedLabel warehouseNote = new WrappedLabel("", 500);
    private final DefaultTableModel detailModel = UI.readOnlyModel(
            "Phụ tùng", "Yêu cầu", "Đã xuất");
    private final JTable detailTable = UI.table(detailModel);
    private final JButton cancel = UI.secondaryButton("Hủy yêu cầu");

    private final JLabel feedback = small("Dữ liệu được đồng bộ trực tiếp với GaraTechDB.");
    private final StatCard pendingCard = new StatCard("Chờ xác nhận", "0", "Kho đang tiếp nhận yêu cầu");
    private final StatCard issuedCard = new StatCard("Đã xuất kho", "0", "Kho đã xác nhận và trừ tồn");
    private final StatCard canceledCard = new StatCard("Đã hủy", "0", "Theo dõi yêu cầu đã hủy");

    private Request selected;
    private boolean refreshing;
    private boolean loading;
    private String owner;

    public YeuCauPhuTungPanel() {
        super("KHÔNG GIAN KỸ THUẬT VIÊN", "Yêu cầu phụ tùng",
                "Gửi yêu cầu vật tư theo phiếu sửa chữa và theo dõi tình trạng xử lý của kho.");

        JButton newRequest = UI.primaryButton("+ Tạo yêu cầu mới");
        newRequest.addActionListener(e -> startNewDraft());
        actions.add(newRequest);

        JPanel content = column();
        JPanel stats = transparent(new GridLayout(1, 3, 14, 0));
        stats.add(pendingCard);
        stats.add(issuedCard);
        stats.add(canceledCard);
        stack(content, stats, 18);

        JPanel left = column();
        stack(left, buildForm(), 14);
        stack(left, notice("Tồn kho chỉ để tham khảo. Bạn vẫn có thể gửi yêu cầu khi thiếu hàng; "
                + "kho sẽ kiểm tra và xác nhận trước khi xuất. Tồn kho chỉ bị trừ sau khi kho duyệt.", false, 280), 0);

        JPanel right = column();
        stack(right, buildHistory(), 16);
        stack(right, buildDetail(), 0);

        JPanel columns = transparent(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.35;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        g.anchor = GridBagConstraints.NORTH;
        g.insets = new Insets(0, 0, 16, 16);
        left.setPreferredSize(new Dimension(340, 0));
        left.setMinimumSize(new Dimension(280, 0));
        columns.add(left, g);
        g.gridx = 1;
        g.weightx = 0.65;
        g.insets = new Insets(0, 0, 16, 0);
        right.setMinimumSize(new Dimension(450, 0));
        columns.add(right, g);

        stack(content, columns, 8);
        stack(content, feedback, 14);
        stack(content, small("GaraTech · Lịch sử yêu cầu của kỹ thuật viên đang đăng nhập"), 0);
        body.add(content, BorderLayout.NORTH);

        part.addActionListener(e -> updateStock());
        ticket.addActionListener(e -> updateButtons());
        addLine.addActionListener(e -> addDraftLine());
        removeLine.addActionListener(e -> removeDraftLine());
        send.addActionListener(e -> sendDraft());
        cancel.addActionListener(e -> cancelSelected());
        reason.getDocument().addDocumentListener(changed(this::updateButtons));
        search.getDocument().addDocumentListener(changed(this::refreshHistory));
        statusFilter.addActionListener(e -> refreshHistory());
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!refreshing && !e.getValueIsAdjusting()) showSelected();
        });
        draftTable.getSelectionModel().addListSelectionListener(e -> updateButtons());

        owner = Session.maNV();
        reloadFromDatabase(true);
    }

    private JComponent buildForm() {
        RoundedPanel card = card("Tạo yêu cầu xuất phụ tùng");
        JPanel form = column();
        stack(form, field("Phiếu sửa chữa được phân công *", ticket), 14);
        stack(form, field("Phụ tùng *", part), 8);
        stack(form, stock, 14);

        JPanel controls = transparent(new BorderLayout(12, 0));
        controls.add(field("Số lượng yêu cầu *", quantity), BorderLayout.CENTER);
        JPanel button = transparent(new BorderLayout());
        button.add(addLine, BorderLayout.SOUTH);
        controls.add(button, BorderLayout.EAST);
        stack(form, controls, 14);

        draftTable.setRowHeight(42);
        draftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        draftTable.setFont(Theme.font(Font.PLAIN, 11));
        draftTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        draftTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        stack(form, tableScroll(draftTable, 155), 10);

        JPanel remove = transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        remove.add(removeLine);
        stack(form, remove, 14);

        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        reason.setFont(Theme.font(Font.PLAIN, 12));
        reason.setBorder(UI.pad(9, 9, 9, 9));
        JScrollPane noteScroll = new JScrollPane(reason);
        noteScroll.setPreferredSize(new Dimension(0, 80));
        stack(form, field("Lý do / ghi chú cho kho (tối đa 200 ký tự) *", noteScroll), 14);
        stack(form, send, 0);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildHistory() {
        RoundedPanel card = card("Yêu cầu đã gửi");
        JPanel content = column();
        JPanel filters = transparent(new BorderLayout(10, 0));
        search.putClientProperty("JTextField.placeholderText", "Tìm mã yêu cầu, phiếu hoặc biển số...");
        search.setPreferredSize(new Dimension(0, 38));
        search.getAccessibleContext().setAccessibleName("Tìm yêu cầu đã gửi");
        statusFilter.setPreferredSize(new Dimension(165, 38));
        filters.add(search, BorderLayout.CENTER);
        filters.add(statusFilter, BorderLayout.EAST);
        stack(content, filters, 12);

        historyTable.setRowHeight(64);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        historyTable.setFont(Theme.font(Font.PLAIN, 11));
        int[] widths = {210, 230, 150};
        for (int i = 0; i < widths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        historyTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        stack(content, tableScroll(historyTable, 275), 10);
        stack(content, count, 0);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildDetail() {
        RoundedPanel card = card("");
        JPanel header = transparent(new BorderLayout(8, 0));
        header.add(detailTitle, BorderLayout.CENTER);
        header.add(badge, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel content = column();
        stack(content, detailTicket, 8);
        stack(content, detailTime, 12);
        detailTable.setRowHeight(43);
        detailTable.setFont(Theme.font(Font.PLAIN, 11));
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(310);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        stack(content, tableScroll(detailTable, 150), 14);
        stack(content, UI.label("Ghi chú yêu cầu", Font.BOLD, 12, Theme.TEXT), 6);
        stack(content, detailReason, 14);
        stack(content, UI.label("Phản hồi từ kho / phiếu xuất", Font.BOLD, 12, Theme.TEXT), 6);
        stack(content, warehouseNote, 14);
        JPanel buttons = transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(cancel);
        stack(content, buttons, 0);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------------------------------
    // ĐỌC DỮ LIỆU CSDL
    // ---------------------------------------------------------------------

    private void reloadFromDatabase(boolean showError) {
        String maKTV = Session.maNV();
        if (maKTV == null || maKTV.isBlank()) {
            owner = null;
            tickets.clear();
            parts.clear();
            requests.clear();
            ticket.removeAllItems();
            part.removeAllItems();
            visible.clear();
            historyModel.setRowCount(0);
            clearDraft();
            showSelected();
            feedback.setForeground(Theme.DANGER);
            feedback.setText("Không xác định được tài khoản kỹ thuật viên. Vui lòng đăng nhập lại.");
            updateStats();
            updateButtons();
            return;
        }

        loading = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            owner = maKTV;
            List<YeuCauPhuTungDao.RepairTicketRow> ticketRows = queryDao.findAssignedRepairs(maKTV);
            List<PhuTung> partRows = phuTungDao.findAll();
            List<YeuCauPhuTungDao.RequestRow> requestRows = queryDao.findRequestsByTechnician(maKTV);

            tickets.clear();
            for (YeuCauPhuTungDao.RepairTicketRow row : ticketRows) {
                tickets.add(new RepairTicket(row.maPhieuSC(), row.trangThai(), row.bienSo(), row.thongTinXe()));
            }

            parts.clear();
            for (PhuTung pt : partRows) {
                parts.add(new Part(pt.getMaPhuTung(), pt.getTenPhuTung(),
                        pt.getDonViTinh(), pt.getSoLuongTon()));
            }

            requests.clear();
            for (YeuCauPhuTungDao.RequestRow row : requestRows) {
                List<Line> lines = new ArrayList<>();
                for (PhieuXuatKhoDao.ChiTietXuatKhoRow detail
                        : phieuXuatKhoDao.findDetailsByRequest(row.maPhieuXK())) {
                    int issuedQty = ISSUED.equals(row.trangThai()) ? detail.soLuongXuat() : 0;
                    lines.add(new Line(detail.maPhuTung(), detail.tenPhuTung(),
                            detail.donViTinh(), detail.soLuongXuat(), issuedQty, detail.ghiChu()));
                }
                String warehouseMessage = buildWarehouseMessage(row);
                requests.add(new Request(row.maPhieuXK(), maKTV, row.maPhieuSC(),
                        row.bienSo(), row.thongTinXe(), row.ngayYeuCau(), row.ngayXuat(),
                        row.trangThai(), row.lyDoXuat(), row.lyDoTuChoi(),
                        row.tenNVKho(), warehouseMessage, lines));
            }

            rebuildTicketOptions();
            rebuildPartOptions();
            rebuildDraft();
            refreshHistory();
            updateStock();
            feedback.setForeground(Theme.MUTED);
            feedback.setText("Đã đồng bộ " + tickets.size() + " phiếu sửa chữa, "
                    + parts.size() + " phụ tùng và " + requests.size() + " yêu cầu từ GaraTechDB.");
        } catch (Exception ex) {
            if (showError) Msg.error(this, "Không thể tải dữ liệu yêu cầu phụ tùng từ CSDL.\n" + errorMessage(ex));
            tickets.clear();
            parts.clear();
            requests.clear();
            ticket.removeAllItems();
            part.removeAllItems();
            visible.clear();
            historyModel.setRowCount(0);
            detailModel.setRowCount(0);
            feedback.setForeground(Theme.DANGER);
            feedback.setText("Không thể tải dữ liệu từ GaraTechDB: " + errorMessage(ex));
            updateStats();
            updateButtons();
        } finally {
            loading = false;
            setCursor(Cursor.getDefaultCursor());
            updateStock();
            updateButtons();
        }
    }

    private void rebuildTicketOptions() {
        RepairTicket previous = (RepairTicket) ticket.getSelectedItem();
        ticket.removeAllItems();
        for (RepairTicket item : tickets) ticket.addItem(item);
        if (previous != null) {
            for (int i = 0; i < ticket.getItemCount(); i++) {
                if (ticket.getItemAt(i).maPhieuSC.equals(previous.maPhieuSC)) {
                    ticket.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (ticket.getItemCount() > 0) ticket.setSelectedIndex(0);
    }

    private void rebuildPartOptions() {
        Part previous = (Part) part.getSelectedItem();
        part.removeAllItems();
        for (Part item : parts) part.addItem(item);
        if (previous != null) {
            for (int i = 0; i < part.getItemCount(); i++) {
                if (part.getItemAt(i).maPhuTung.equals(previous.maPhuTung)) {
                    part.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (part.getItemCount() > 0) part.setSelectedIndex(0);
    }

    private String buildWarehouseMessage(YeuCauPhuTungDao.RequestRow row) {
        if (row.lyDoTuChoi() != null && !row.lyDoTuChoi().isBlank()) {
            return "Lý do từ kho: " + row.lyDoTuChoi();
        }
        if (ISSUED.equals(row.trangThai())) {
            String by = row.tenNVKho() == null || row.tenNVKho().isBlank()
                    ? "nhân viên kho" : row.tenNVKho();
            String at = row.ngayXuat() == null ? "" : " · Thời gian xuất: " + TIME.format(row.ngayXuat());
            return "Đã xác nhận và xuất kho bởi " + by + at + ". Tồn kho đã được cập nhật.";
        }
        if (CANCELED.equals(row.trangThai())) {
            return "Yêu cầu đã được hủy. Không có phụ tùng nào được xuất từ yêu cầu này.";
        }
        return "Kho chưa xác nhận yêu cầu. Tồn kho chưa bị trừ.";
    }

    // ---------------------------------------------------------------------
    // BẢN NHÁP YÊU CẦU
    // ---------------------------------------------------------------------

    private void addDraftLine() {
        Part selectedPart = (Part) part.getSelectedItem();
        RepairTicket selectedTicket = (RepairTicket) ticket.getSelectedItem();
        if (selectedPart == null || selectedTicket == null) return;
        if (!selectedTicket.dangSuaChua()) {
            Msg.error(this, "Chỉ được yêu cầu phụ tùng cho phiếu đang ở trạng thái Đang sửa chữa.");
            return;
        }
        try {
            quantity.commitEdit();
        } catch (java.text.ParseException ex) {
            Msg.error(this, "Số lượng phải là số nguyên dương.");
            return;
        }
        int number = ((Number) quantity.getValue()).intValue();
        if (number < 1 || number > 1000000) {
            Msg.error(this, "Số lượng phải từ 1 đến 1.000.000.");
            return;
        }
        appendDraftLine(selectedPart, number);
    }

    private void appendDraftLine(Part selectedPart, int number) {
        if (number < 1 || number > 1000000) {
            throw new IllegalArgumentException("Số lượng không hợp lệ.");
        }
        Line existing = draft.stream()
                .filter(line -> line.maPhuTung.equals(selectedPart.maPhuTung))
                .findFirst().orElse(null);
        if (existing != null) {
            if (existing.soLuong > 1000000 - number) {
                feedback.setForeground(Theme.DANGER);
                feedback.setText("Tổng số lượng một phụ tùng không được vượt 1.000.000.");
                return;
            }
            existing.soLuong += number;
        } else {
            draft.add(new Line(selectedPart.maPhuTung, selectedPart.tenPhuTung,
                    selectedPart.donViTinh, number, 0, null));
        }
        rebuildDraft();
        feedback.setForeground(Theme.MUTED);
        feedback.setText("Đã thêm phụ tùng vào bản nháp. Dữ liệu chỉ được ghi khi bấm Gửi yêu cầu kho.");
    }

    private void removeDraftLine() {
        int row = draftTable.getSelectedRow();
        if (row < 0) return;
        draft.remove(draftTable.convertRowIndexToModel(row));
        rebuildDraft();
    }

    private void rebuildDraft() {
        draftModel.setRowCount(0);
        for (Line line : draft) {
            draftModel.addRow(new Object[]{
                    line.maPhuTung + " · " + line.tenPhuTung,
                    line.soLuong + " " + nullToDash(line.donViTinh)
            });
        }
        ticket.setEnabled(draft.isEmpty() && ticket.getItemCount() > 0 && !loading);
        updateButtons();
    }

    private void startNewDraft() {
        if ((!draft.isEmpty() || !reason.getText().isBlank())
                && !Msg.confirm(this, "Xóa bản nháp hiện tại để tạo yêu cầu mới?")) return;
        clearDraft();
        ticket.requestFocusInWindow();
        feedback.setForeground(Theme.MUTED);
        feedback.setText("Đang tạo yêu cầu mới. Chọn phiếu đang sửa chữa và thêm phụ tùng cần cấp.");
    }

    private void clearDraft() {
        draft.clear();
        reason.setText("");
        quantity.setValue(1);
        rebuildDraft();
    }

    private void sendDraft() {
        if (!send.isEnabled()) return;
        RepairTicket chosen = (RepairTicket) ticket.getSelectedItem();
        if (chosen == null) return;
        String note = reason.getText() == null ? "" : reason.getText().trim();
        if (note.isBlank()) {
            Msg.error(this, "Vui lòng nhập lý do yêu cầu phụ tùng.");
            return;
        }
        if (note.length() > 200) {
            Msg.error(this, "Lý do yêu cầu không được vượt quá 200 ký tự.");
            return;
        }

        StringBuilder confirmation = new StringBuilder("Gửi yêu cầu gồm ")
                .append(draft.size()).append(" loại phụ tùng cho phiếu ")
                .append(chosen.maPhieuSC).append("?\n\n");
        for (Line line : draft) {
            confirmation.append("• ").append(line.tenPhuTung)
                    .append(" — ").append(line.soLuong).append(" ")
                    .append(nullToDash(line.donViTinh)).append("\n");
        }
        confirmation.append("\nYêu cầu sẽ ở trạng thái Chờ xác nhận. Tồn kho chưa bị trừ.");
        if (!Msg.confirm(this, confirmation.toString())) return;

        try {
            // Kiểm tra lại trạng thái từ CSDL ngay trước khi gửi, tránh dùng phiếu đã đổi trạng thái.
            boolean stillWorking = queryDao.findAssignedRepairs(Session.maNV()).stream()
                    .anyMatch(row -> row.maPhieuSC().equals(chosen.maPhieuSC)
                            && "Đang sửa chữa".equals(row.trangThai()));
            if (!stillWorking) {
                throw new IllegalStateException(
                        "Phiếu sửa chữa không còn ở trạng thái Đang sửa chữa. Hãy tải lại dữ liệu.");
            }

            List<YeuCauPhuTungService.ChiTietYeuCauInput> details = new ArrayList<>();
            for (Line line : draft) {
                details.add(new YeuCauPhuTungService.ChiTietYeuCauInput(
                        line.maPhuTung, line.soLuong, note));
            }
            String maPhieuXK = service.guiYeuCauXuatPhuTung(
                    chosen.maPhieuSC, Session.maNV(), note, details);

            clearDraft();
            reloadFromDatabase(false);
            search.setText("");
            statusFilter.setSelectedItem("Tất cả trạng thái");
            Request created = requests.stream()
                    .filter(r -> r.maPhieuXK.equals(maPhieuXK))
                    .findFirst().orElse(null);
            selectRequest(created);
            feedback.setForeground(Theme.SUCCESS);
            feedback.setText("Đã lưu yêu cầu " + maPhieuXK + " vào CSDL · Chờ xác nhận.");
            Msg.info(this, "Gửi yêu cầu phụ tùng thành công.\nMã phiếu xuất kho: "
                    + maPhieuXK + "\nTrạng thái: " + PENDING
                    + "\nTồn kho chỉ được trừ khi nhân viên kho duyệt.");
        } catch (Exception ex) {
            Msg.error(this, "Không thể gửi yêu cầu phụ tùng.\n" + errorMessage(ex));
        }
    }

    // ---------------------------------------------------------------------
    // LỊCH SỬ VÀ CHI TIẾT YÊU CẦU
    // ---------------------------------------------------------------------

    private void refreshHistory() {
        if (refreshing) return;
        refreshing = true;
        try {
            Request previous = selected;
            historyModel.setRowCount(0);
            visible.clear();
            String keyword = normalize(search.getText());
            String status = (String) statusFilter.getSelectedItem();

            for (Request request : requests) {
                if (!"Tất cả trạng thái".equals(status) && !request.trangThai.equals(status)) continue;
                String haystack = normalize(request.maPhieuXK + " " + request.maPhieuSC
                        + " " + request.bienSo + " " + request.thongTinXe);
                if (!haystack.contains(keyword)) continue;
                visible.add(request);
                String time = request.ngayYeuCau == null ? "—" : TIME.format(request.ngayYeuCau);
                historyModel.addRow(new Object[]{
                        "<html><b>" + escapeHtml(request.maPhieuXK) + "</b><br><span style='color:#6B7A90'>"
                                + escapeHtml(time) + "</span></html>",
                        "<html>" + escapeHtml(request.maPhieuSC) + "<br>"
                                + escapeHtml(request.bienSo) + "</html>",
                        request.trangThai
                });
            }

            int row = previous == null ? -1 : visible.indexOf(previous);
            if (row < 0 && !visible.isEmpty()) row = 0;
            if (row >= 0) historyTable.setRowSelectionInterval(row, row);
            else historyTable.clearSelection();

            count.setText(visible.isEmpty()
                    ? "Không có yêu cầu phù hợp. Thử thay đổi từ khóa hoặc trạng thái."
                    : visible.size() + " yêu cầu · chọn một dòng để xem chi tiết.");
            updateStats();
        } finally {
            refreshing = false;
        }
        showSelected();
    }

    private void showSelected() {
        int row = historyTable.getSelectedRow();
        Request request = row < 0 ? null : visible.get(historyTable.convertRowIndexToModel(row));
        showRequest(request);
    }

    private void selectRequest(Request request) {
        if (request == null) {
            historyTable.clearSelection();
            showRequest(null);
            return;
        }
        int index = visible.indexOf(request);
        if (index >= 0) historyTable.setRowSelectionInterval(index, index);
        else showRequest(request);
    }

    private void showRequest(Request request) {
        selected = request;
        detailModel.setRowCount(0);
        if (selected == null) {
            detailTitle.setText("Chưa chọn yêu cầu");
            badge.setStatus("Chưa chọn");
            detailTicket.setText("Chọn một yêu cầu trong danh sách để xem chi tiết.");
            detailTime.setText(" ");
            detailReason.setPlainText("—");
            warehouseNote.setPlainText("—");
        } else {
            detailTitle.setText(selected.maPhieuXK);
            badge.setStatus(selected.trangThai);
            detailTicket.setText(selected.maPhieuSC + " · " + selected.bienSo + " · " + selected.thongTinXe);
            String sentAt = selected.ngayYeuCau == null ? "—" : TIME.format(selected.ngayYeuCau);
            detailTime.setText("Gửi lúc: " + sentAt + " · " + selected.lines.size() + " loại phụ tùng");
            for (Line line : selected.lines) {
                detailModel.addRow(new Object[]{
                        line.maPhuTung + " · " + line.tenPhuTung,
                        line.soLuong + " " + nullToDash(line.donViTinh),
                        line.soLuongDaXuat + " " + nullToDash(line.donViTinh)
                });
            }
            detailReason.setPlainText(selected.lyDoXuat);
            warehouseNote.setPlainText(selected.phanHoiKho);
        }
        updateButtons();
        revalidate();
        repaint();
    }

    private void cancelSelected() {
        if (selected == null || !PENDING.equals(selected.trangThai)) return;
        if (!Objects.equals(selected.owner, Session.maNV())) return;
        if (!Msg.confirm(this, "Hủy yêu cầu " + selected.maPhieuXK
                + "? Chỉ yêu cầu Chờ xác nhận mới được phép hủy.")) return;
        try {
            boolean canceled = phieuXuatKhoDao.cancelRequestByTechnician(
                    selected.maPhieuXK, Session.maNV());
            if (!canceled) {
                throw new IllegalStateException("Không thể hủy. Yêu cầu có thể đã được kho xử lý.");
            }
            String id = selected.maPhieuXK;
            reloadFromDatabase(false);
            Request latest = requests.stream().filter(r -> r.maPhieuXK.equals(id)).findFirst().orElse(null);
            selectRequest(latest);
            feedback.setForeground(Theme.SUCCESS);
            feedback.setText("Đã hủy yêu cầu " + id + " trong CSDL.");
        } catch (Exception ex) {
            Msg.error(this, "Không thể hủy yêu cầu.\n" + errorMessage(ex));
        }
    }

    private void updateButtons() {
        RepairTicket chosen = (RepairTicket) ticket.getSelectedItem();
        boolean ownSession = owner != null && Objects.equals(owner, Session.maNV());
        boolean validTicket = chosen != null && chosen.dangSuaChua();
        boolean validPart = part.getSelectedItem() != null;
        boolean validReason = reason.getText() != null && !reason.getText().isBlank()
                && reason.getText().trim().length() <= 200;

        send.setEnabled(!loading && ownSession && validTicket && !draft.isEmpty() && validReason);
        addLine.setEnabled(!loading && ownSession && validTicket && validPart);
        removeLine.setEnabled(!loading && !draft.isEmpty() && draftTable.getSelectedRow() >= 0);
        cancel.setEnabled(!loading && selected != null
                && Objects.equals(selected.owner, Session.maNV())
                && PENDING.equals(selected.trangThai));
    }

    private void updateStock() {
        Part chosen = (Part) part.getSelectedItem();
        if (chosen == null) {
            stock.setText("Không có phụ tùng trong danh mục.");
            stock.setForeground(Theme.MUTED);
            return;
        }
        stock.setText("Đơn vị: " + chosen.donViTinh + " · Tồn tham khảo: " + chosen.soLuongTon
                + " · " + (chosen.soLuongTon == 0 ? "Hết hàng" : "Còn hàng"));
        stock.setForeground(chosen.soLuongTon == 0 ? Theme.DANGER : Theme.MUTED);
        updateButtons();
    }

    private void updateStats() {
        pendingCard.setValue(String.valueOf(requests.stream().filter(r -> PENDING.equals(r.trangThai)).count()),
                "Kho đang tiếp nhận yêu cầu");
        issuedCard.setValue(String.valueOf(requests.stream().filter(r -> ISSUED.equals(r.trangThai)).count()),
                "Kho đã xác nhận và trừ tồn");
        canceledCard.setValue(String.valueOf(requests.stream().filter(r -> CANCELED.equals(r.trangThai)).count()),
                "Theo dõi yêu cầu đã hủy");
    }

    @Override
    public void onShow() {
        if (!Objects.equals(owner, Session.maNV())) {
            owner = Session.maNV();
            clearDraft();
            selected = null;
        }
        reloadFromDatabase(true);
    }

    // ---------------------------------------------------------------------
    // HÀM TIỆN ÍCH GIAO DIỆN
    // ---------------------------------------------------------------------

    private static JScrollPane tableScroll(JTable table, int height) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        scroll.setPreferredSize(new Dimension(0, height));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private static RoundedPanel card(String title) {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE);
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(UI.pad(18, 18, 18, 18));
        panel.setMinimumSize(new Dimension(0, 0));
        if (!title.isEmpty()) panel.add(UI.h2(title), BorderLayout.NORTH);
        return panel;
    }

    private static JPanel field(String title, JComponent control) {
        JPanel panel = transparent(new BorderLayout(0, 7));
        panel.add(UI.label(title, Font.BOLD, 12, Theme.TEXT), BorderLayout.NORTH);
        control.getAccessibleContext().setAccessibleName(title);
        if (!(control instanceof JScrollPane)) {
            control.setPreferredSize(new Dimension(0, 38));
            control.setFont(Theme.font(Font.PLAIN, 12));
        }
        panel.add(control, BorderLayout.CENTER);
        return panel;
    }

    private static JComponent notice(String message, boolean warning, int width) {
        RoundedPanel panel = new RoundedPanel(6, warning ? Theme.YELLOW_SOFT : Theme.INFO_BG).borderColor(null);
        panel.setLayout(new BorderLayout());
        panel.setBorder(UI.pad(12, 12, 12, 12));
        WrappedLabel label = new WrappedLabel(message, width);
        label.setForeground(warning ? Theme.WARN : Theme.INFO);
        panel.add(label);
        return panel;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JLabel small(String text) {
        return UI.label(text, Font.PLAIN, 11, Theme.MUTED);
    }

    private static void stack(JPanel parent, JComponent child, int gap) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(child, BorderLayout.CENTER);
        parent.add(wrapper);
        if (gap > 0) parent.add(Box.createVerticalStrut(gap));
    }

    private static String normalize(String input) {
        String value = input == null ? "" : input.trim();
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private static String escapeHtml(String value) {
        if (value == null) return "—";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String errorMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank()
                ? "Lỗi không xác định. Kiểm tra kết nối CSDL và nhật ký ứng dụng."
                : message;
    }

    private static DocumentListener changed(Runnable task) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { task.run(); }
            @Override public void removeUpdate(DocumentEvent e) { task.run(); }
            @Override public void changedUpdate(DocumentEvent e) { task.run(); }
        };
    }

    private static final class WrappedLabel extends JLabel {
        private final int defaultWidth;

        WrappedLabel(String text, int width) {
            defaultWidth = width;
            setFont(Theme.font(Font.PLAIN, 12));
            setForeground(Theme.MUTED);
            setPlainText(text);
        }

        void setPlainText(String text) {
            String safe = text == null ? "—" : text;
            setText("<html>" + safe.replace("&", "&amp;")
                    .replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\n", "<br>") + "</html>");
        }

        @Override
        public Dimension getPreferredSize() {
            Object html = getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
            if (html instanceof javax.swing.text.View view) {
                view.setSize(getWidth() > 0 ? getWidth() : defaultWidth, 0);
                return new Dimension(defaultWidth,
                        (int) Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS)));
            }
            return super.getPreferredSize();
        }
    }

    private record RepairTicket(String maPhieuSC, String trangThai, String bienSo, String thongTinXe) {
        boolean dangSuaChua() { return "Đang sửa chữa".equals(trangThai); }

        @Override
        public String toString() {
            return maPhieuSC + " · " + bienSo + " · " + trangThai;
        }
    }

    private record Part(String maPhuTung, String tenPhuTung, String donViTinh, int soLuongTon) {
        @Override
        public String toString() {
            return maPhuTung + " · " + tenPhuTung + " · Tồn: " + soLuongTon + " " + nullToDash(donViTinh);
        }
    }

    private static final class Line {
        final String maPhuTung;
        final String tenPhuTung;
        final String donViTinh;
        int soLuong;
        final int soLuongDaXuat;
        final String ghiChu;

        Line(String maPhuTung, String tenPhuTung, String donViTinh,
             int soLuong, int soLuongDaXuat, String ghiChu) {
            this.maPhuTung = maPhuTung;
            this.tenPhuTung = tenPhuTung;
            this.donViTinh = donViTinh;
            this.soLuong = soLuong;
            this.soLuongDaXuat = soLuongDaXuat;
            this.ghiChu = ghiChu;
        }
    }

    private static final class Request {
        final String maPhieuXK;
        final String owner;
        final String maPhieuSC;
        final String bienSo;
        final String thongTinXe;
        final LocalDateTime ngayYeuCau;
        final LocalDateTime ngayXuat;
        final String trangThai;
        final String lyDoXuat;
        final String lyDoTuChoi;
        final String tenNVKho;
        final String phanHoiKho;
        final List<Line> lines;

        Request(String maPhieuXK, String owner, String maPhieuSC, String bienSo,
                String thongTinXe, LocalDateTime ngayYeuCau, LocalDateTime ngayXuat,
                String trangThai, String lyDoXuat, String lyDoTuChoi,
                String tenNVKho, String phanHoiKho, List<Line> lines) {
            this.maPhieuXK = maPhieuXK;
            this.owner = owner;
            this.maPhieuSC = maPhieuSC;
            this.bienSo = bienSo;
            this.thongTinXe = thongTinXe;
            this.ngayYeuCau = ngayYeuCau;
            this.ngayXuat = ngayXuat;
            this.trangThai = trangThai;
            this.lyDoXuat = lyDoXuat;
            this.lyDoTuChoi = lyDoTuChoi;
            this.tenNVKho = tenNVKho;
            this.phanHoiKho = phanHoiKho;
            this.lines = List.copyOf(lines);
        }
    }
}

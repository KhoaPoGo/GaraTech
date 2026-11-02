package gui.label;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class YeuCauPhuTungLabel extends BasePanel {
	private static final String PENDING = TrangThaiXuatKho.CHO_XAC_NHAN.label();
	private static final String ISSUED = TrangThaiXuatKho.DA_XUAT_KHO.label();
	private static final String CANCELED = TrangThaiXuatKho.DA_HUY.label();
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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
			new String[] { "Tất cả trạng thái", PENDING, ISSUED, CANCELED });
	private final DefaultTableModel historyModel = UI.readOnlyModel("Yêu cầu / ngày gửi", "Phiếu / biển số",
			"Trạng thái");
	private final JTable historyTable = UI.table(historyModel);
	private final JLabel count = small(" ");
	private final JLabel detailTitle = UI.h2("Chi tiết yêu cầu");
	private final StatusBadge badge = new StatusBadge("Chưa chọn");
	private final JLabel detailTicket = small(" ");
	private final JLabel detailTime = small(" ");
	private final WrappedLabel detailReason = new WrappedLabel("", 500);
	private final WrappedLabel warehouseNote = new WrappedLabel("", 500);
	private final DefaultTableModel detailModel = UI.readOnlyModel("Phụ tùng", "Yêu cầu", "Đã xuất");
	private final JTable detailTable = UI.table(detailModel);
	private final JButton cancel = UI.secondaryButton("Hủy yêu cầu");
	private final JLabel feedback = small("Bản demo · thay đổi chỉ giữ trong phiên, chưa ghi CSDL.");
	private final StatCard pendingCard = new StatCard("Chờ xác nhận", "0", "Kho đang tiếp nhận yêu cầu");
	private final StatCard issuedCard = new StatCard("Đã xuất kho", "0", "Đã có phiếu xuất liên kết");
	private final StatCard canceledCard = new StatCard("Đã hủy", "0", "Theo dõi lịch sử yêu cầu");
	private Request selected;
	private boolean refreshing;
	private String owner;
	private int sequence = 1;

	public YeuCauPhuTungLabel() {
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
				+ "kho sẽ kiểm tra và xác nhận trước khi xuất.", false, 280), 0);
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
		stack(content, small("GaraTech · Dữ liệu minh họa · Yêu cầu của kỹ thuật viên đang đăng nhập"), 0);
		body.add(content, BorderLayout.NORTH);

		part.addActionListener(e -> updateStock());
		addLine.addActionListener(e -> addDraftLine());
		removeLine.addActionListener(e -> removeDraftLine());
		send.addActionListener(e -> sendDraft());
		cancel.addActionListener(e -> cancelSelected());
		reason.getDocument().addDocumentListener(changed(this::updateButtons));
		search.getDocument().addDocumentListener(changed(this::refreshHistory));
		statusFilter.addActionListener(e -> refreshHistory());
		historyTable.getSelectionModel().addListSelectionListener(e -> {
			if (!refreshing && !e.getValueIsAdjusting())
				showSelected();
		});
		draftTable.getSelectionModel().addListSelectionListener(e -> updateButtons());
		owner = Session.maNV();
		loadDemo();
		refreshHistory();
		updateStock();
		updateButtons();
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
		stack(form, field("Lý do / ghi chú cho kho *", noteScroll), 14);
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
		int[] widths = { 210, 230, 150 };
		for (int i = 0; i < widths.length; i++)
			historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
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

	private void addDraftLine() {
		Part selectedPart = (Part) part.getSelectedItem();
		if (selectedPart == null || ticket.getSelectedItem() == null)
			return;
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
		if (number < 1 || number > 1000000)
			throw new IllegalArgumentException("Số lượng không hợp lệ");
		Line existing = draft.stream().filter(l -> l.part.code.equals(selectedPart.code)).findFirst().orElse(null);
		if (existing != null) {
			if (existing.quantity > 1000000 - number) {
				feedback.setForeground(Theme.DANGER);
				feedback.setText("Tổng số lượng một phụ tùng không được vượt 1.000.000.");
				return;
			}
			existing.quantity += number;
		} else
			draft.add(new Line(selectedPart, number, 0));
		rebuildDraft();
		feedback.setForeground(Theme.MUTED);
		feedback.setText("Đã thêm vào bản nháp · yêu cầu chỉ được tạo khi bấm Gửi yêu cầu kho.");
	}

	private void removeDraftLine() {
		int row = draftTable.getSelectedRow();
		if (row < 0)
			return;
		draft.remove(draftTable.convertRowIndexToModel(row));
		rebuildDraft();
	}

	private void rebuildDraft() {
		draftModel.setRowCount(0);
		for (Line line : draft)
			draftModel.addRow(
					new Object[] { line.part.code + " · " + line.part.name, line.quantity + " " + line.part.unit });
		// Khi da co vat tu trong nhap, khoa phieu lien ket de tranh doi nham xe.
		ticket.setEnabled(draft.isEmpty() && ticket.getItemCount() > 0);
		updateButtons();
	}

	private void sendDraft() {
		if (!send.isEnabled())
			return;
		RepairTicket chosen = (RepairTicket) ticket.getSelectedItem();
		if (chosen == null)
			return;
		if (!Msg.confirm(this, "Tạo yêu cầu gồm " + draft.size() + " phụ tùng cho " + chosen.id
				+ "?\nBản demo: trạng thái Chờ xác nhận, chưa gửi kho hay lưu CSDL."))
			return;
		Request created = createRequestFromDraft();
		selected = created;
		search.setText("");
		statusFilter.setSelectedIndex(0);
		refreshHistory();
		feedback.setForeground(Theme.SUCCESS);
		feedback.setText("Đã tạo " + created.id + " · Chờ xác nhận (demo, chưa gửi kho / lưu CSDL).");
	}

	private Request createRequestFromDraft() {
		RepairTicket chosen = (RepairTicket) ticket.getSelectedItem();
		if (chosen == null || draft.isEmpty() || reason.getText().isBlank() || !Objects.equals(owner, Session.maNV()))
			throw new IllegalStateException("Bản nháp chưa hợp lệ");
		List<Line> snapshot = new ArrayList<>();
		for (Line line : draft)
			snapshot.add(new Line(line.part, line.quantity, 0));
		String id;
		do {
			id = "YCPT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
					+ String.format(Locale.ROOT, "%03d", sequence++);
		} while (containsId(id));
		Request created = new Request(id, owner, chosen, LocalDateTime.now(), PENDING, reason.getText().trim(),
				"Kho chưa xác nhận yêu cầu.", snapshot);
		requests.add(created);
		clearDraft();
		return created;
	}

	private boolean containsId(String id) {
		return requests.stream().anyMatch(r -> r.id.equals(id));
	}

	private void startNewDraft() {
		if ((!draft.isEmpty() || !reason.getText().isBlank())
				&& !Msg.confirm(this, "Xóa bản nháp hiện tại để tạo yêu cầu mới?"))
			return;
		clearDraft();
		ticket.requestFocusInWindow();
		ticket.scrollRectToVisible(new Rectangle(0, 0, ticket.getWidth(), ticket.getHeight()));
		feedback.setForeground(Theme.MUTED);
		feedback.setText("Đang tạo yêu cầu mới · chọn phiếu và thêm phụ tùng.");
	}

	private void clearDraft() {
		draft.clear();
		reason.setText("");
		quantity.setValue(1);
		rebuildDraft();
	}

	private void cancelSelected() {
		if (selected == null || !selected.status.equals(PENDING))
			return;
		if (!Msg.confirm(this, "Hủy " + selected.id + " trong bản demo?\nChỉ yêu cầu Chờ xác nhận được phép hủy."))
			return;
		selected.status = CANCELED;
		selected.warehouseNote = "Kỹ thuật viên đã hủy yêu cầu trước khi kho xác nhận (demo).";
		refreshHistory();
		feedback.setForeground(Theme.MUTED);
		feedback.setText("Đã hủy yêu cầu trong phiên demo · chưa ghi CSDL.");
	}

	private void refreshHistory() {
		if (refreshing)
			return;
		refreshing = true;
		try {
			Request previous = selected;
			historyModel.setRowCount(0);
			visible.clear();
			String query = normalize(search.getText());
			for (int i = requests.size() - 1; i >= 0; i--) {
				Request r = requests.get(i);
				if (!Objects.equals(r.owner, Session.maNV()))
					continue;
				if (statusFilter.getSelectedIndex() > 0 && !r.status.equals(statusFilter.getSelectedItem()))
					continue;
				if (!normalize(r.id + " " + r.ticket.id + " " + r.ticket.plate).contains(query))
					continue;
				visible.add(r);
				historyModel.addRow(new Object[] {
						"<html><b>" + r.id + "</b><br><span style='color:#6B7A90'>" + TIME.format(r.time)
								+ "</span></html>",
						"<html>" + r.ticket.id + "<br>" + r.ticket.plate + "</html>", r.status });
			}
			int row = visible.indexOf(previous);
			if (row < 0 && !visible.isEmpty())
				row = 0;
			if (row >= 0)
				historyTable.setRowSelectionInterval(row, row);
			else
				historyTable.clearSelection();
			count.setText(visible.isEmpty() ? "Không có yêu cầu phù hợp. Thử thay đổi từ khóa hoặc trạng thái."
					: visible.size() + " yêu cầu · chọn một dòng để xem chi tiết.");
			updateStats();
		} finally {
			refreshing = false;
		}
		showSelected();
	}

	private void showSelected() {
		int row = historyTable.getSelectedRow();
		selected = row < 0 ? null : visible.get(historyTable.convertRowIndexToModel(row));
		detailModel.setRowCount(0);
		if (selected == null) {
			detailTitle.setText("Chưa chọn yêu cầu");
			badge.setStatus("Chưa chọn");
			detailTicket.setText("Chọn một yêu cầu trong danh sách để xem chi tiết.");
			detailTime.setText(" ");
			detailReason.setPlainText("—");
			warehouseNote.setPlainText("—");
		} else {
			detailTitle.setText(selected.id);
			badge.setStatus(selected.status);
			detailTicket.setText(selected.ticket.id + " · " + selected.ticket.plate + " · " + selected.ticket.car);
			detailTime.setText(
					"Gửi lúc: " + TIME.format(selected.time) + " · " + selected.lines.size() + " loại phụ tùng");
			for (Line line : selected.lines)
				detailModel.addRow(new Object[] { line.part.code + " · " + line.part.name,
						line.quantity + " " + line.part.unit, line.issued + " " + line.part.unit });
			detailReason.setPlainText(selected.reason);
			warehouseNote.setPlainText(selected.warehouseNote);
		}
		updateButtons();
		revalidate();
		repaint();
	}

	private void updateButtons() {
		boolean ownSession = Objects.equals(owner, Session.maNV()) && owner != null;
		send.setEnabled(
				ownSession && ticket.getSelectedItem() != null && !draft.isEmpty() && !reason.getText().isBlank());
		addLine.setEnabled(ownSession && part.getSelectedItem() != null && ticket.getSelectedItem() != null);
		removeLine.setEnabled(!draft.isEmpty() && draftTable.getSelectedRow() >= 0);
		cancel.setEnabled(
				selected != null && Objects.equals(selected.owner, Session.maNV()) && selected.status.equals(PENDING));
	}

	private void updateStock() {
		Part chosen = (Part) part.getSelectedItem();
		if (chosen == null) {
			stock.setText("Không có phụ tùng.");
			return;
		}
		stock.setText("Đơn vị: " + chosen.unit + " · Tồn tham khảo: " + chosen.stock + " · "
				+ (chosen.stock == 0 ? "Hết hàng" : "Còn hàng"));
		stock.setForeground(chosen.stock == 0 ? Theme.DANGER : Theme.MUTED);
	}

	private void updateStats() {
		List<Request> mine = requests.stream().filter(r -> Objects.equals(r.owner, Session.maNV())).toList();
		pendingCard.setValue("" + mine.stream().filter(r -> r.status.equals(PENDING)).count(),
				"Kho đang tiếp nhận yêu cầu");
		issuedCard.setValue("" + mine.stream().filter(r -> r.status.equals(ISSUED)).count(),
				"Đã có phiếu xuất liên kết");
		canceledCard.setValue("" + mine.stream().filter(r -> r.status.equals(CANCELED)).count(),
				"Theo dõi lịch sử yêu cầu");
	}

	@Override
	public void onShow() {
		// Khi noi CSDL: nap qua SwingWorker, DAO/service loc va kiem tra quyen theo
		// Session.maNV().
		if (!Objects.equals(owner, Session.maNV())) {
			owner = Session.maNV();
			requests.clear();
			selected = null;
			clearDraft();
			ticket.removeAllItems();
			part.removeAllItems();
			loadDemo();
			updateStock();
		}
		refreshHistory();
	}

	private void loadDemo() {
		if (owner == null)
			return;
		RepairTicket a = new RepairTicket("PSC-20261006-001", "51H-12345", "Toyota Vios 2021");
		RepairTicket b = new RepairTicket("PSC-20261005-012", "51F-45678", "Ford Ranger 2020");
		ticket.addItem(a);
		ticket.addItem(b);
		Part oil = new Part("OIL-01", "Dầu 5W–30", "lít", 48);
		Part filter = new Part("OF-01", "Lọc dầu Toyota", "cái", 12);
		Part brake = new Part("BP-02", "Má phanh trước", "bộ", 3);
		Part cleaner = new Part("BC-01", "Dung dịch vệ sinh phanh", "chai", 0);
		for (Part p : new Part[] { cleaner, brake, filter, oil })
			part.addItem(p);
		requests.add(new Request("YCPT-20261006-001", owner, a, LocalDateTime.of(2026, 10, 6, 9, 15), ISSUED,
				"Bảo dưỡng định kỳ, thay dầu và lọc dầu cho xe Toyota Vios.",
				"Đã xuất đủ · Phiếu xuất: PXK-20261006-001 · Kho: Phạm Thu Dung.",
				List.of(new Line(oil, 4, 4), new Line(filter, 1, 1), new Line(brake, 1, 1))));
		requests.add(new Request("YCPT-20261006-002", owner, b, LocalDateTime.of(2026, 10, 6, 9, 40), CANCELED,
				"Bổ sung dầu động cơ.", "Kỹ thuật viên hủy: đã có đủ vật tư từ yêu cầu trước.",
				List.of(new Line(oil, 1, 0))));
		requests.add(new Request("YCPT-20261006-003", owner, a, LocalDateTime.of(2026, 10, 6, 10, 20), PENDING,
				"Cần dung dịch vệ sinh phanh sau khi thay má phanh trước. Nhờ kho kiểm tra và cấp bổ sung.",
				"Kho chưa xác nhận. Phụ tùng đang hết hàng; chờ kho kiểm tra bổ sung.",
				List.of(new Line(cleaner, 1, 0))));
	}

	private static JScrollPane tableScroll(JTable table, int height) {
		JScrollPane scroll = new JScrollPane(table);
		scroll.setColumnHeaderView(table.getTableHeader());
		scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
		scroll.setPreferredSize(new Dimension(0, height));
		scroll.getViewport().setBackground(Color.WHITE);
		return scroll;
	}

	private static RoundedPanel card(String title) {
		RoundedPanel p = new RoundedPanel(10, Color.WHITE);
		p.setLayout(new BorderLayout(0, 16));
		p.setBorder(UI.pad(18, 18, 18, 18));
		p.setMinimumSize(new Dimension(0, 0));
		if (!title.isEmpty())
			p.add(UI.h2(title), BorderLayout.NORTH);
		return p;
	}

	private static JPanel field(String title, JComponent control) {
		JPanel p = transparent(new BorderLayout(0, 7));
		p.add(UI.label(title, Font.BOLD, 12, Theme.TEXT), BorderLayout.NORTH);
		control.getAccessibleContext().setAccessibleName(title);
		if (!(control instanceof JScrollPane)) {
			control.setPreferredSize(new Dimension(0, 38));
			control.setFont(Theme.font(Font.PLAIN, 12));
		}
		p.add(control, BorderLayout.CENTER);
		return p;
	}

	private static JComponent notice(String message, boolean warning, int width) {
		RoundedPanel p = new RoundedPanel(6, warning ? Theme.YELLOW_SOFT : Theme.INFO_BG).borderColor(null);
		p.setLayout(new BorderLayout());
		p.setBorder(UI.pad(12, 12, 12, 12));
		WrappedLabel label = new WrappedLabel(message, width);
		label.setForeground(warning ? Theme.WARN : Theme.INFO);
		p.add(label);
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
		wrapper.setAlignmentX(LEFT_ALIGNMENT);
		wrapper.add(child, BorderLayout.CENTER);
		parent.add(wrapper);
		if (gap > 0)
			parent.add(Box.createVerticalStrut(gap));
	}

	private static String normalize(String input) {
		return Normalizer.normalize(input.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd')
				.replace('Đ', 'D').toLowerCase(Locale.ROOT);
	}

	private static DocumentListener changed(Runnable task) {
		return new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				task.run();
			}

			public void removeUpdate(DocumentEvent e) {
				task.run();
			}

			public void changedUpdate(DocumentEvent e) {
				task.run();
			}
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
			setText("<html>"
					+ text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
					+ "</html>");
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

	private record RepairTicket(String id, String plate, String car) {
		@Override
		public String toString() {
			return id + " · " + plate;
		}
	}

	private record Part(String code, String name, String unit, int stock) {
		@Override
		public String toString() {
			return code + " · " + name;
		}
	}

	private static final class Line {
		final Part part;
		int quantity;
		final int issued;

		Line(Part part, int quantity, int issued) {
			this.part = part;
			this.quantity = quantity;
			this.issued = issued;
		}
	}

	private static final class Request {
		final String id, owner;
		final RepairTicket ticket;
		final LocalDateTime time;
		String status, warehouseNote;
		final String reason;
		final List<Line> lines;

		Request(String id, String owner, RepairTicket ticket, LocalDateTime time, String status, String reason,
				String warehouseNote, List<Line> lines) {
			this.id = id;
			this.owner = owner;
			this.ticket = ticket;
			this.time = time;
			this.status = status;
			this.reason = reason;
			this.warehouseNote = warehouseNote;
			this.lines = List.copyOf(lines);
		}
	}
}

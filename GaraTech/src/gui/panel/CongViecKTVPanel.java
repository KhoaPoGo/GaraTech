package gui.panel;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CongViecKTVPanel extends BasePanel {
	private static final String WORKING = "Đang sửa chữa";
	private static final String WAITING = "Chờ kiểm tra";
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
	private final List<Ticket> tickets = new ArrayList<>();
	private final JPanel ticketList = column();
	private final JLabel listTitle = heading("Công việc của tôi · 0");
	private final JLabel detailTitle = heading("");
	private final JLabel customer = text("");
	private final JLabel car = text("");
	private final JLabel lastVisit = text("");
	private final JLabel state = text("");
	private final JLabel diagnosis = new WrappedLabel("", 650);
	private final JPanel checklist = column();
	private final DefaultTableModel partsModel = UI.readOnlyModel("Mã / tên phụ tùng", "SL yêu cầu", "Đã xuất",
			"Đã dùng", "Liên kết");
	private final JTable partsTable = UI.table(partsModel);
	private final JTextField linkedTicket = new JTextField();
	private final JComboBox<String> requestedPart = new JComboBox<>(new String[] { "BC-01 · Dung dịch vệ sinh phanh",
			"BP-02 · Má phanh trước", "OF-01 · Lọc dầu Toyota", "OIL-01 · Dầu 5W–30" });
	private final JTextField quantity = new JTextField("1 chai");
	private final JTextField percent = new JTextField("65%");
	private final JTextField updatedAt = new JTextField();
	private final JTextArea note = new JTextArea(3, 30);
	private final JLabel feedback = muted(" ");
	private final JButton sendRequest = UI.secondaryButton("Gửi yêu cầu kho");
	private final JButton shortage = UI.secondaryButton("Báo thiếu phụ tùng");
	private final JButton requestLink = link("+ Yêu cầu phụ tùng");
	private final JButton complete = UI.secondaryButton("Báo hoàn tất thi công");
	private final JButton saveProgress = UI.primaryButton("Cập nhật tiến độ");
	private final JPanel resultCard;
	private Ticket selected;
	private boolean loading;

	public CongViecKTVPanel() {
		super("", "", "");
		// Thay rieng header trong panel nay, khong sua BasePanel/Theme cua ca nhom.
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
		addStack(left, infoBox("Bạn có thể cập nhật kỹ thuật và yêu cầu vật tư cho phiếu được giao. "
				+ "Không có quyền xem tài chính hoặc công việc của người khác.", false), 0);

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
		footer.add(small("GaraTech · Dữ liệu minh họa · Lịch sử nghiệp vụ được lưu giữ"), BorderLayout.WEST);
		footer.add(small("Chi nhánh Trung tâm"), BorderLayout.EAST);
		center.add(footer, BorderLayout.SOUTH);
		add(center, BorderLayout.CENTER);

		saveProgress.setIcon(new SymbolIcon(false));
		saveProgress.addActionListener(e -> saveUpdate());
		complete.addActionListener(e -> submitForInspection());
		requestLink.addActionListener(e -> {
			requestedPart.requestFocusInWindow();
			requestedPart.scrollRectToVisible(requestedPart.getBounds());
			requestedPart.showPopup();
		});
		sendRequest.addActionListener(e -> sendPartsRequest());
		shortage.addActionListener(e -> reportShortage());
		note.getDocument().addDocumentListener(changed(() -> {
			if (!loading && selected != null && selected.editable()) {
				selected.note = note.getText();
				updateButtons();
			}
		}));
		percent.getDocument().addDocumentListener(changed(this::updateButtons));
		loadDemoData();
		if (!tickets.isEmpty())
			selectTicket(tickets.get(0));
		else
			showEmpty();
	}

	private JComponent buildHeader() {
		JPanel header = new JPanel(new BorderLayout(16, 0));
		header.setOpaque(false);
		JPanel labels = column();
		String userId = Session.maNV() == null ? "U003" : Session.maNV();
		labels.add(small(userId + " · CÔNG VIỆC ĐƯỢC PHÂN CÔNG"));
		labels.add(Box.createVerticalStrut(8));
		labels.add(UI.label("Công việc kỹ thuật viên", Font.BOLD, 27, Theme.TEXT));
		labels.add(Box.createVerticalStrut(8));
		String name = Session.user() == null ? "Kỹ thuật viên" : Session.user().hoTen();
		labels.add(muted(name + " · Chỉ hiển thị các phiếu được giao cho bạn."));
		header.add(labels, BorderLayout.CENTER);
		JPanel action = new JPanel(new GridBagLayout());
		action.setOpaque(false);
		action.add(saveProgress);
		header.add(action, BorderLayout.EAST);
		return header;
	}

	private JPanel buildDetailCard() {
		RoundedPanel card = card();
		JPanel top = transparent(new BorderLayout(8, 0));
		top.add(detailTitle, BorderLayout.CENTER);
		JButton history = link("Lịch sử xe ↗");
		history.addActionListener(e -> showHistory());
		top.add(history, BorderLayout.EAST);
		card.add(top, BorderLayout.NORTH);

		JPanel stack = column();
		JPanel info = transparent(new GridBagLayout());
		addInfo(info, 0, "Khách / phương tiện", customer, state);
		addInfo(info, 1, "Phiên bản / ODO", car, null);
		addInfo(info, 2, "Lịch sử gần nhất", lastVisit, null);
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
		addStack(stack, small("<html>5 hạng mục chính đã được khách xác nhận lúc 09:45. "
				+ "Vệ sinh họng ga bị từ chối, không thực hiện.</html>"), 0);
		card.add(stack, BorderLayout.CENTER);
		return card;
	}

	private JPanel buildPartsCard() {
		RoundedPanel card = card();
		JPanel head = transparent(new BorderLayout());
		head.add(heading("Phụ tùng sử dụng"), BorderLayout.WEST);
		head.add(requestLink, BorderLayout.EAST);
		card.add(head, BorderLayout.NORTH);
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
		int[] widths = { 190, 140, 145, 145, 150 };
		for (int i = 0; i < widths.length; i++)
			partsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
		addStack(content, UI.tableBlock(partsTable), 16);

		JPanel form = transparent(new GridLayout(1, 3, 14, 0));
		linkedTicket.setEditable(false);
		linkedTicket.setBackground(Color.WHITE);
		form.add(fieldBlock("Phiếu sửa chữa liên kết", linkedTicket));
		form.add(fieldBlock("Phụ tùng yêu cầu", requestedPart));
		form.add(fieldBlock("Số lượng yêu cầu > 0", quantity));
		addStack(content, form, 14);
		JPanel buttons = transparent(new FlowLayout(FlowLayout.LEFT, 12, 0));
		buttons.add(sendRequest);
		buttons.add(shortage);
		addStack(content, buttons, 0);
		card.add(content, BorderLayout.CENTER);
		return card;
	}

	private JPanel buildResultCard() {
		RoundedPanel card = card();
		card.add(heading("Cập nhật kết quả"), BorderLayout.NORTH);
		JPanel content = column();
		JPanel form = transparent(new GridLayout(1, 2, 16, 0));
		form.add(fieldBlock("Tiến độ hiện tại", percent));
		updatedAt.setEditable(false);
		updatedAt.setBackground(Color.WHITE);
		form.add(fieldBlock("Thời gian cập nhật", updatedAt));
		addStack(content, form, 16);
		note.setFont(Theme.font(Font.PLAIN, 12));
		note.setForeground(Theme.TEXT);
		note.setLineWrap(true);
		note.setWrapStyleWord(true);
		note.setBorder(UI.pad(10, 10, 10, 10));
		note.getAccessibleContext().setAccessibleName("Kết quả và ghi chú kỹ thuật");
		JScrollPane noteScroll = new JScrollPane(note);
		noteScroll.setPreferredSize(new Dimension(0, 100));
		addStack(content, fieldBlock("Kết quả / ghi chú kỹ thuật *", noteScroll), 14);
		JPanel bottom = transparent(new BorderLayout(14, 0));
		bottom.add(infoBox("Hoàn tất hạng mục còn lại để báo hoàn tất. Phiếu sẽ chuyển sang Chờ kiểm tra "
				+ "chất lượng, chưa được xuất hóa đơn.", true), BorderLayout.CENTER);
		JPanel buttonWrap = transparent(new BorderLayout());
		buttonWrap.add(complete, BorderLayout.NORTH);
		bottom.add(buttonWrap, BorderLayout.EAST);
		addStack(content, bottom, 8);
		feedback.setFont(Theme.font(Font.PLAIN, 11));
		addStack(content, feedback, 0);
		card.add(content, BorderLayout.CENTER);
		return card;
	}

	private void selectTicket(Ticket ticket) {
		if (!Objects.equals(ticket.owner, Session.maNV()))
			return;
		if (selected != null && selected.editable() && !loading) {
			Integer value = parsePercent();
			if (value == null) {
				Msg.error(this, "Tiến độ phải là số từ 0 đến 100. Hãy sửa tiến độ trước khi đổi phiếu.");
				return;
			}
			selected.percent = value;
			selected.note = note.getText();
		}
		selected = ticket;
		loading = true;
		try {
			detailTitle.setText("Chi tiết sửa chữa · " + ticket.id);
			customer.setText(ticket.customer + " · " + ticket.plate);
			car.setText(ticket.car + " · " + ticket.odo + " km");
			lastVisit.setText(ticket.history);
			state.setText(ticket.status);
			state.setOpaque(true);
			state.setFont(Theme.font(Font.PLAIN, 11));
			state.setBorder(UI.pad(5, 8, 5, 8));
			Color[] colors = Theme.statusColors(ticket.status);
			state.setBackground(colors[0]);
			state.setForeground(colors[1]);
			diagnosis.setText("<html>Chẩn đoán: " + ticket.diagnosis + "</html>");
			linkedTicket.setText(ticket.id);
			percent.setText(ticket.percent + "%");
			updatedAt.setText(ticket.updatedAt);
			note.setText(ticket.note);
			note.setCaretPosition(0);
			checklist.removeAll();
			for (WorkItem item : ticket.items) {
				JPanel row = transparent(new BorderLayout(0, 0));
				row.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), UI.pad(10, 0, 11, 0)));
				JCheckBox check = new JCheckBox(item.title, item.done);
				check.setOpaque(false);
				check.setForeground(Theme.TEXT);
				check.putClientProperty("FlatLaf.style", "icon.selectedBackground:#FFFFFF;"
						+ "icon.selectedBorderColor:#1E8E5A; icon.checkmarkColor:#1E8E5A");
				check.setFont(Theme.font(Font.BOLD, 12));
				check.setEnabled(ticket.editable());
				JPanel labels = column();
				labels.add(check);
				JLabel sub = small("    " + item.description);
				labels.add(sub);
				row.add(labels, BorderLayout.CENTER);
				check.addActionListener(e -> {
					if (!ticket.editable())
						return;
					item.done = check.isSelected();
					ticket.percent = ticket.items.stream().filter(x -> x.done).mapToInt(x -> x.weight).sum();
					percent.setText(ticket.percent + "%");
					rebuildTicketList();
					feedback.setForeground(Theme.MUTED);
					feedback.setText("Đã cập nhật checklist trong bản demo · chưa lưu CSDL.");
					updateButtons();
				});
				addStack(checklist, row, 0);
			}
			rebuildParts();
			rebuildTicketList();
			feedback.setForeground(Theme.MUTED);
			feedback.setText(ticket.editable() ? " " : "Phiếu chờ kiểm tra chất lượng · chỉ xem.");
		} finally {
			loading = false;
		}
		updateButtons();
		revalidate();
		repaint();
	}

	private void rebuildTicketList() {
		ticketList.removeAll();
		List<Ticket> mine = tickets.stream().filter(t -> Objects.equals(t.owner, Session.maNV())).toList();
		listTitle.setText("Công việc của tôi · " + mine.size());
		for (Ticket t : mine) {
			JPanel item = column();
			item.setBorder(UI.pad(0, 0, 18, 0));
			if (t == selected) {
				JLabel tag = UI.label("Đang chọn", Font.PLAIN, 11, Theme.WARN);
				tag.setOpaque(true);
				tag.setBackground(Theme.YELLOW_SOFT);
				tag.setBorder(UI.pad(4, 7, 4, 7));
				item.add(tag);
				item.add(Box.createVerticalStrut(16));
			}
			item.add(UI.label(t.plate, Font.BOLD, t == selected ? 20 : 17, Theme.TEXT));
			item.add(Box.createVerticalStrut(12));
			JLabel id = UI.label(t.id + " ↗", Font.PLAIN, 12, Theme.INFO);
			item.add(id);
			item.add(Box.createVerticalStrut(15));
			item.add(UI.label(t.summary, Font.PLAIN, 12, Theme.TEXT));
			item.add(Box.createVerticalStrut(15));
			item.add(small(t.percent + "%"));
			item.add(Box.createVerticalStrut(5));
			JProgressBar bar = new JProgressBar(0, 100);
			bar.setValue(t.percent);
			bar.setForeground(Theme.YELLOW);
			bar.setBackground(Theme.TRACK);
			bar.setBorderPainted(false);
			bar.setPreferredSize(new Dimension(0, 5));
			bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
			item.add(bar);
			item.add(Box.createVerticalStrut(15));
			item.add(small("Hạn: " + t.due));
			installClick(item, () -> selectTicket(t));
			addStack(ticketList, item, 0);
		}
		ticketList.revalidate();
		ticketList.repaint();
	}

	private void rebuildParts() {
		partsModel.setRowCount(0);
		if (selected == null)
			return;
		for (Part p : selected.parts)
			partsModel.addRow(new Object[] { p.name, p.requested, p.issued, p.used, p.reference });
	}

	private Integer parsePercent() {
		try {
			String s = percent.getText().trim();
			if (s.endsWith("%"))
				s = s.substring(0, s.length() - 1).trim();
			int value = Integer.parseInt(s);
			return value >= 0 && value <= 100 ? value : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private void updateButtons() {
		if (loading)
			return;
		boolean editable = selected != null && selected.editable();
		percent.setEditable(editable);
		note.setEditable(editable);
		requestedPart.setEnabled(editable);
		quantity.setEditable(editable);
		sendRequest.setEnabled(editable);
		shortage.setEnabled(editable);
		requestLink.setEnabled(editable);
		saveProgress.setEnabled(editable);
		Integer value = parsePercent();
		complete.setEnabled(editable && value != null && value == 100 && selected.items.stream().allMatch(i -> i.done)
				&& !note.getText().isBlank());
	}

	private void saveUpdate() {
		if (selected == null || !selected.editable())
			return;
		Integer value = parsePercent();
		if (value == null) {
			Msg.error(this, "Tiến độ phải là số từ 0 đến 100, ví dụ 65 hoặc 65%.");
			return;
		}
		if (note.getText().isBlank()) {
			Msg.error(this, "Vui lòng nhập kết quả / ghi chú kỹ thuật.");
			return;
		}
		selected.percent = value;
		selected.note = note.getText().trim();
		selected.updatedAt = TIME.format(LocalDateTime.now());
		updatedAt.setText(selected.updatedAt);
		rebuildTicketList();
		updateButtons();
		feedback.setForeground(Theme.SUCCESS);
		feedback.setText("Đã cập nhật trong phiên demo · chưa ghi vào CSDL.");
		resultCard.scrollRectToVisible(new Rectangle(0, 0, resultCard.getWidth(), resultCard.getHeight()));
	}

	private void submitForInspection() {
		if (!complete.isEnabled() || selected == null)
			return;
		if (!Msg.confirm(this, "Bản demo: báo hoàn tất " + selected.id + " và chuyển sang Chờ kiểm tra?\n"
				+ "Phiếu sẽ chỉ được xem; chưa ghi CSDL hay gửi sang màn hình quản lý."))
			return;
		selected.percent = 100;
		selected.note = note.getText().trim();
		selected.updatedAt = TIME.format(LocalDateTime.now());
		selected.status = WAITING;
		selectTicket(selected);
		feedback.setText("Đã chuyển Chờ kiểm tra trong bản demo · chưa ghi CSDL.");
	}

	private void sendPartsRequest() {
		if (selected == null || !selected.editable())
			return;
		String input = quantity.getText().trim();
		if (!input.matches("[1-9][0-9]*(\\s+[^0-9].*)?")) {
			Msg.error(this, "Số lượng phải là số nguyên dương, ví dụ 1 hoặc 1 chai.");
			return;
		}
		String[] split = input.split("\\s+", 2);
		try {
			if (Integer.parseInt(split[0]) <= 0)
				throw new NumberFormatException();
		} catch (NumberFormatException ex) {
			Msg.error(this, "Số lượng không hợp lệ hoặc quá lớn.");
			return;
		}
		selected.parts
				.add(new Part((String) requestedPart.getSelectedItem(), input, "Chờ xác nhận", "—", "Yêu cầu demo"));
		rebuildParts();
		revalidate();
		repaint();
		feedback.setForeground(Theme.INFO);
		feedback.setText("Đã thêm yêu cầu trong bản demo · chưa gửi kho hoặc ghi CSDL.");
	}

	private void reportShortage() {
		if (selected == null || !selected.editable())
			return;
		Msg.info(this, "Bản demo: báo thiếu " + requestedPart.getSelectedItem() + " cho phiếu " + selected.id
				+ ".\nChưa gửi thông báo tới kho và chưa ghi CSDL.");
	}

	private void showHistory() {
		if (selected == null)
			return;
		JTextArea history = new JTextArea("LỊCH SỬ XE " + selected.plate + " (DỮ LIỆU MINH HỌA)\n\n" + selected.history
				+ "\n\nPhiếu đang xem: " + selected.id + "\nChẩn đoán: " + selected.diagnosis);
		history.setEditable(false);
		history.setLineWrap(true);
		history.setWrapStyleWord(true);
		history.setFont(Theme.font(Font.PLAIN, 13));
		history.setBorder(UI.pad(14, 14, 14, 14));
		JScrollPane scroll = new JScrollPane(history);
		scroll.setPreferredSize(new Dimension(500, 220));
		JOptionPane.showMessageDialog(this, scroll, "Lịch sử xe", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void onShow() {
		// Noi CSDL sau: dung SwingWorker nap cac phieu theo Session.maNV().
		// Khong nap lai du lieu mau moi lan doi menu, tranh mat cac cap nhat trong
		// phien.
		if (selected != null && Objects.equals(selected.owner, Session.maNV()))
			return;
		Ticket mine = tickets.stream().filter(t -> Objects.equals(t.owner, Session.maNV())).findFirst().orElse(null);
		selected = null;
		if (mine == null)
			showEmpty();
		else
			selectTicket(mine);
	}

	private void showEmpty() {
		selected = null;
		loading = true;
		detailTitle.setText("Không có phiếu được phân công");
		customer.setText("—");
		car.setText("—");
		lastVisit.setText("—");
		state.setText("—");
		diagnosis.setText("Chưa có dữ liệu.");
		linkedTicket.setText("");
		percent.setText("0%");
		updatedAt.setText("");
		note.setText("");
		checklist.removeAll();
		loading = false;
		rebuildParts();
		rebuildTicketList();
		updateButtons();
	}

	private void loadDemoData() {
		String owner = Session.maNV();
		if (owner == null)
			return;
		Ticket first = new Ticket("PSC-20261006-001", owner, "51H-12345", "Toyota Vios 2021", "48.250",
				"Nguyễn Minh Anh", "Toyota Vios · Bảo dưỡng & phanh", "06/10 · 16:30", 65,
				"15/04/2026 · Bảo dưỡng 40.000 km",
				"má phanh trước mòn 2 mm, dầu động cơ đến kỳ thay; lọc dầu cần thay. Đĩa phanh còn trong giới hạn.",
				"Đã hoàn tất thay dầu và lọc dầu, không rò rỉ. Đang lắp má phanh. Cần kiểm tra lực siết và chạy thử trước khi báo hoàn tất.",
				new WorkItem("Bảo dưỡng định kỳ · DV0001", "Đã thay dầu và lọc dầu · 10:00", true, 65),
				new WorkItem("Thay má phanh trước · DV0002", "Đang lắp bộ má phanh; chờ chạy thử và kiểm tra lực siết",
						false, 35));
		first.parts.add(new Part("OIL-01 · Dầu 5W–30", "4 lít", "4 lít", "4 lít", "PXK-20261006-001"));
		first.parts.add(new Part("OF-01 · Lọc dầu Toyota", "1 cái", "1 cái", "1 cái", "PXK-20261006-001"));
		first.parts.add(new Part("BP-02 · Má phanh trước", "1 bộ", "1 bộ", "Đang lắp", "PXK-20261006-001"));
		tickets.add(first);
		Ticket second = new Ticket("PSC-20261005-012", owner, "51F-45678", "Ford Ranger 2020", "62.100",
				"Trần Quốc Huy", "Ford Ranger · Hệ thống treo", "06/10 · 17:00", 80,
				"05/08/2026 · Kiểm tra hệ thống treo",
				"giảm xóc trước rò dầu; kiểm tra cao su và lực siết sau khi thay.",
				"Đã thay giảm xóc trước. Chờ kiểm tra lực siết và chạy thử.",
				new WorkItem("Thay giảm xóc trước · DV0003", "Đã thay giảm xóc · 09:50", true, 80),
				new WorkItem("Kiểm tra và chạy thử · DV0004", "Kiểm tra lực siết, tiếng ồn và độ ổn định", false, 20));
		second.parts.add(new Part("GX-01 · Giảm xóc trước", "2 cái", "2 cái", "2 cái", "PXK-20261006-002"));
		tickets.add(second);
	}

	private static void addInfo(JPanel p, int row, String label, JLabel value, JLabel extra) {
		GridBagConstraints g = new GridBagConstraints();
		g.gridy = row;
		g.gridx = 0;
		g.anchor = GridBagConstraints.WEST;
		g.insets = new Insets(0, 0, 10, 14);
		p.add(muted(label), g);
		g.gridx = 1;
		g.weightx = 1;
		g.fill = GridBagConstraints.HORIZONTAL;
		value.setFont(Theme.font(Font.BOLD, 12));
		p.add(value, g);
		if (extra != null) {
			g.gridx = 2;
			g.weightx = 0;
			g.insets = new Insets(0, 0, 10, 0);
			p.add(extra, g);
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
		RoundedPanel p = new RoundedPanel(5, warning ? Theme.YELLOW_SOFT : Theme.INFO_BG).borderColor(null);
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

	private static void installClick(Component c, Runnable action) {
		c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				action.run();
			}
		});
		if (c instanceof Container container)
			for (Component child : container.getComponents())
				installClick(child, action);
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

		WrappedLabel(String text, int defaultWidth) {
			super("<html>" + text + "</html>");
			this.defaultWidth = defaultWidth;
		}

		@Override
		public Dimension getPreferredSize() {
			Object html = getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
			if (html instanceof javax.swing.text.View view) {
				int width = getWidth() > 0 ? getWidth() : defaultWidth;
				view.setSize(width, 0);
				return new Dimension(defaultWidth,
						(int) Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS)));
			}
			return super.getPreferredSize();
		}
	}

	private static final class SymbolIcon implements Icon {
		private final boolean info;

		SymbolIcon(boolean info) {
			this.info = info;
		}

		public int getIconWidth() {
			return 16;
		}

		public int getIconHeight() {
			return 16;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D p = (Graphics2D) g.create();
			p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			p.setColor(c.getForeground() == null ? Theme.INFO : c.getForeground());
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

	private static final class WorkItem {
		final String title, description;
		final int weight;
		boolean done;

		WorkItem(String title, String description, boolean done, int weight) {
			this.title = title;
			this.description = description;
			this.done = done;
			this.weight = weight;
		}
	}

	private static final class Part {
		final String name, requested, issued, used, reference;

		Part(String name, String requested, String issued, String used, String reference) {
			this.name = name;
			this.requested = requested;
			this.issued = issued;
			this.used = used;
			this.reference = reference;
		}
	}

	private static final class Ticket {
		final String id, owner, plate, car, odo, customer, summary, due, history, diagnosis;
		final List<WorkItem> items;
		final List<Part> parts = new ArrayList<>();
		int percent;
		String note;
		String status = WORKING;
		String updatedAt = "06/10/2026 · 10:20";

		Ticket(String id, String owner, String plate, String car, String odo, String customer, String summary,
				String due, int percent, String history, String diagnosis, String note, WorkItem... items) {
			this.id = id;
			this.owner = owner;
			this.plate = plate;
			this.car = car;
			this.odo = odo;
			this.customer = customer;
			this.summary = summary;
			this.due = due;
			this.percent = percent;
			this.history = history;
			this.diagnosis = diagnosis;
			this.note = note;
			this.items = List.of(items);
		}

		boolean editable() {
			return WORKING.equals(status);
		}
	}
}

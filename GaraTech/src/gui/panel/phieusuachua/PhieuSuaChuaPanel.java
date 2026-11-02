package gui.panel.phieusuachua;

import dao.PhieuSuaChuaDao.PhieuSuaChuaRow;
import dao.PhieuTiepNhanDao.PhieuTiepNhanRow;
import gui.Navigator;
import gui.component.BasePanel;
import gui.component.UI;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/**
 * Màn hình Điều phối Quản lý Phiếu Sửa Chữa (SV2 - Tiếp nhận).
 * Kế thừa BasePanel, điều phối linh hoạt giữa 3 View: Lập phiếu, Danh sách, và Chi tiết.
 * Hỗ trợ nhận Payload trực tiếp từ màn hình Tiếp nhận xe để tự động điền form.
 */
public final class PhieuSuaChuaPanel extends BasePanel {

    private static final String CARD_LAP_PHIEU = "LAP_PHIEU";
    private static final String CARD_DANH_SACH = "DANH_SACH";
    private static final String CARD_CHI_TIET = "CHI_TIET";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    private final LapPhieuSuaChuaPanel lapPhieuPanel;
    private final DanhSachPhieuSuaChuaPanel danhSachPanel;
    private final ChiTietPhieuSuaChuaPanel chiTietPanel;

    private String currentCard = CARD_DANH_SACH;

    public PhieuSuaChuaPanel() {
        super("", "Quản lý phiếu sửa chữa", "");

        lapPhieuPanel = new LapPhieuSuaChuaPanel(
                this::showDanhSachView,
                this::onTicketCreated
        );

        danhSachPanel = new DanhSachPhieuSuaChuaPanel(
                this::showLapPhieuView,
                this::showChiTietView
        );

        chiTietPanel = new ChiTietPhieuSuaChuaPanel(
                this::showDanhSachView
        );

        cardsPanel.setOpaque(false);
        cardsPanel.add(danhSachPanel, CARD_DANH_SACH);
        cardsPanel.add(lapPhieuPanel, CARD_LAP_PHIEU);
        cardsPanel.add(chiTietPanel, CARD_CHI_TIET);

        body.add(cardsPanel, BorderLayout.CENTER);

        showDanhSachView();
    }

    public void showLapPhieuView() {
        currentCard = CARD_LAP_PHIEU;
        setHeader("", "Lập phiếu sửa chữa", "");

        actions.removeAll();
        JButton btnToList = UI.secondaryButton("📋 Danh sách phiếu");
        btnToList.addActionListener(e -> showDanhSachView());
        actions.add(btnToList);
        actions.revalidate();
        actions.repaint();

        cardLayout.show(cardsPanel, CARD_LAP_PHIEU);
        revalidate();
        repaint();
    }

    public void showDanhSachView() {
        currentCard = CARD_DANH_SACH;
        setHeader("", "Quản lý phiếu sửa chữa", "");

        actions.removeAll();
        JButton btnCreate = UI.primaryButton("+ Lập phiếu mới");
        btnCreate.addActionListener(e -> {
            lapPhieuPanel.resetForm();
            showLapPhieuView();
        });
        actions.add(btnCreate);
        actions.revalidate();
        actions.repaint();

        danhSachPanel.loadData();
        cardLayout.show(cardsPanel, CARD_DANH_SACH);
        revalidate();
        repaint();
    }

    public void showChiTietView(PhieuSuaChuaRow row) {
        currentCard = CARD_CHI_TIET;
        setHeader("", "Chi tiết phiếu sửa chữa", "");

        actions.removeAll();
        JButton btnBack = UI.secondaryButton("← Quay lại danh sách");
        btnBack.addActionListener(e -> showDanhSachView());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        chiTietPanel.displayTicket(row);
        cardLayout.show(cardsPanel, CARD_CHI_TIET);
        revalidate();
        repaint();
    }

    private void onTicketCreated(String maPsc) {
        showDanhSachView();
    }

    public void lapPhieuTuTiepNhan(PhieuTiepNhanRow row) {
        showLapPhieuView();
        lapPhieuPanel.setTicket(row);
    }

    @Override
    public void onShow() {
        Object payload = Navigator.consumePayload();
        if (payload instanceof PhieuTiepNhanRow row) {
            lapPhieuTuTiepNhan(row);
        } else if (payload instanceof PhieuSuaChuaRow pscRow) {
            showChiTietView(pscRow);
        } else if (CARD_DANH_SACH.equals(currentCard)) {
            danhSachPanel.loadData();
        }
    }
}

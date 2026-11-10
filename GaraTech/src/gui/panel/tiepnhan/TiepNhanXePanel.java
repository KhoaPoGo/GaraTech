package gui.panel.tiepnhan;

import dao.PhieuTiepNhanSuaChuaDao.PhieuTiepNhanSuaChuaRow;
import entity.KhachHang;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
import gui.Navigator;
import gui.component.BasePanel;
import gui.component.UI;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/**
 * Màn hình Quản lý phiếu Tiếp nhận - Sửa chữa của vai trò Tiếp nhận (SV2).
 * Kế thừa BasePanel, điều phối linh hoạt giữa Form tạo phiếu, Danh sách phiếu và Chi tiết phiếu.
 */
public final class TiepNhanXePanel extends BasePanel {

    private static final String CARD_LAP_PHIEU = "LAP_PHIEU";
    private static final String CARD_DANH_SACH = "DANH_SACH";
    private static final String CARD_CHI_TIET = "CHI_TIET";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    private final LapPhieuTiepNhanPanel lapPhieuPanel;
    private final DanhSachTiepNhanPanel danhSachPanel;
    private final ChiTietTiepNhanPanel chiTietPanel;

    private String currentCard = CARD_LAP_PHIEU;

    public TiepNhanXePanel() {
        super("", "Tạo phiếu tiếp nhận - sửa chữa", "");

        lapPhieuPanel = new LapPhieuTiepNhanPanel(
                this::showDanhSachView,
                this::onTicketCreated
        );

        danhSachPanel = new DanhSachTiepNhanPanel(
                this::showLapPhieuView,
                this::showChiTietView
        );

        chiTietPanel = new ChiTietTiepNhanPanel(
                this::showDanhSachView
        );

        cardsPanel.setOpaque(false);
        cardsPanel.add(lapPhieuPanel, CARD_LAP_PHIEU);
        cardsPanel.add(danhSachPanel, CARD_DANH_SACH);
        cardsPanel.add(chiTietPanel, CARD_CHI_TIET);

        body.add(cardsPanel, BorderLayout.CENTER);

        showLapPhieuView();
    }

    public void showLapPhieuView() {
        currentCard = CARD_LAP_PHIEU;
        setHeader("", "Tạo phiếu tiếp nhận - sửa chữa", "");

        actions.removeAll();
        JButton btnToList = UI.secondaryButton("Danh sách phiếu tiếp nhận - sửa chữa");
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
        setHeader("", "Danh sách phiếu tiếp nhận - sửa chữa", "");

        actions.removeAll();
        // Đem nút Xem chi tiết lên cùng hàng với nút Tạo phiếu mới ở trên
        JButton btnView = UI.secondaryButton("Xem chi tiết");
        btnView.addActionListener(e -> danhSachPanel.handleViewSelectedDetail());
        actions.add(btnView);

        JButton btnCreate = UI.primaryButton("Tạo phiếu mới");
        btnCreate.addActionListener(e -> showLapPhieuView());
        actions.add(btnCreate);

        actions.revalidate();
        actions.repaint();

        danhSachPanel.loadData();
        cardLayout.show(cardsPanel, CARD_DANH_SACH);
        revalidate();
        repaint();
    }

    public void showChiTietView(PhieuTiepNhanSuaChuaRow row) {
        currentCard = CARD_CHI_TIET;
        setHeader("", "Chi tiết phiếu tiếp nhận - sửa chữa", "");

        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Quay lại danh sách");
        btnBack.addActionListener(e -> showDanhSachView());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        chiTietPanel.displayTicket(row);
        cardLayout.show(cardsPanel, CARD_CHI_TIET);
        revalidate();
        repaint();
    }

    private void onTicketCreated(String maPhieu) {
        showDanhSachView();
    }

    public void tiepNhanXe(KhachHang kh, Xe xe) {
        showLapPhieuView();
        lapPhieuPanel.setInitialCustomerAndVehicle(kh, xe);
    }

    @Override
    public void onShow() {
        lapPhieuPanel.refreshCustomerList();
        Object p = Navigator.consumePayload();
        if (p instanceof Xe x) {
            tiepNhanXe(x.getKhachHang(), x);
        } else if (CARD_DANH_SACH.equals(currentCard)) {
            danhSachPanel.loadData();
        }
    }
}

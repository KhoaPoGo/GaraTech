package gui.panel.khachhangxe;

import entity.KhachHang;
import entity.Xe;
import gui.Navigator;
import gui.Screen;
import gui.component.BasePanel;
import gui.component.UI;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Collections;
import java.util.List;

/**
 * Coordinator Panel cho Quản lý Khách hàng và Phương tiện.
 * Điều phối chuyển đổi giữa các card view:
 * 1. Danh sách khách hàng và phương tiện (Main View)
 * 2. Form thêm khách hàng
 * 3. Form thêm xe
 * 4. Form sửa xe
 * 5. Hồ sơ xe và lịch sử sửa chữa
 */
public class KhachHangXePanel extends BasePanel {

    public static final String CARD_MAIN = "MAIN";
    public static final String CARD_ADD_CUSTOMER = "ADD_CUSTOMER";
    public static final String CARD_ADD_VEHICLE = "ADD_VEHICLE";
    public static final String CARD_EDIT_VEHICLE = "EDIT_VEHICLE";
    public static final String CARD_VEHICLE_HISTORY = "VEHICLE_HISTORY";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    private final JButton btnAddCustomerHeader = UI.primaryButton("+ Thêm khách hàng");

    private final DanhSachKhachHangXePanel mainView;
    private final FormThemKhachHangPanel addCustomerView;
    private final FormThemXePanel addVehicleView;
    private final FormSuaXePanel editVehicleView;
    private final LichSuXePanel vehicleHistoryView;

    // Navigation state from other screens (e.g. TiepNhanXe)
    private boolean returnToTiepNhan = false;
    private String pendingPlateFromTiepNhan = null;

    public KhachHangXePanel() {
        super("", "Khách hàng & phương tiện", "");

        cardsPanel.setOpaque(false);

        // Sub-panels
        mainView = new DanhSachKhachHangXePanel(
                this::switchToAddCustomerView,
                this::switchToAddVehicleView,
                this::switchToEditVehicleView,
                this::switchToVehicleHistoryView
        );

        addCustomerView = new FormThemKhachHangPanel(
                this::switchToAddVehicleView,
                this::handleCancelAddCustomer
        );

        addVehicleView = new FormThemXePanel(
                this::handleSavedVehicle,
                this::handleCancelVehicleForm
        );

        editVehicleView = new FormSuaXePanel(
                this::handleUpdatedVehicle,
                this::switchToMainView,
                this::handleDeletedVehicle
        );

        vehicleHistoryView = new LichSuXePanel(
                this::handleBackFromHistory
        );

        // Add to cards
        cardsPanel.add(mainView, CARD_MAIN);
        cardsPanel.add(addCustomerView, CARD_ADD_CUSTOMER);
        cardsPanel.add(addVehicleView, CARD_ADD_VEHICLE);
        cardsPanel.add(editVehicleView, CARD_EDIT_VEHICLE);
        cardsPanel.add(vehicleHistoryView, CARD_VEHICLE_HISTORY);

        body.add(cardsPanel, BorderLayout.NORTH);

        btnAddCustomerHeader.addActionListener(e -> switchToAddCustomerView());

        switchToMainView();
    }

    public void switchToMainView() {
        setHeader("", "Khách hàng & phương tiện", "");
        actions.removeAll();
        actions.add(btnAddCustomerHeader);
        actions.revalidate();
        actions.repaint();
        cardLayout.show(cardsPanel, CARD_MAIN);
    }

    public void switchToAddCustomerView() {
        setHeader("", "Thêm khách hàng", "");
        actions.removeAll();
        JButton btnBack = UI.secondaryButton("← Quay lại");
        btnBack.addActionListener(e -> handleCancelAddCustomer());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        addCustomerView.resetForm();
        cardLayout.show(cardsPanel, CARD_ADD_CUSTOMER);
    }

    public void switchToAddVehicleView(KhachHang kh) {
        setHeader("", "Thêm xe cho khách hàng hiện có", "");
        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Mục lục →");
        btnBack.addActionListener(e -> handleCancelVehicleForm());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        addVehicleView.setCustomer(kh, pendingPlateFromTiepNhan);
        cardLayout.show(cardsPanel, CARD_ADD_VEHICLE);
    }

    public void switchToEditVehicleView(Xe xe) {
        setHeader("", "Chỉnh sửa phương tiện", "");
        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Mục lục →");
        btnBack.addActionListener(e -> handleCancelVehicleForm());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        editVehicleView.setVehicle(xe);
        cardLayout.show(cardsPanel, CARD_EDIT_VEHICLE);
    }

    public void switchToVehicleHistoryView(Xe xe) {
        setHeader("", "Hồ sơ xe và lịch sử sửa chữa", "");
        actions.removeAll();
        JButton btnBack = UI.secondaryButton("Mục lục →");
        btnBack.addActionListener(e -> handleCancelVehicleForm());
        actions.add(btnBack);
        actions.revalidate();
        actions.repaint();

        KhachHang kh = mainView.getSelectedOverview() != null
                ? mainView.getSelectedOverview().khachHang()
                : (xe != null ? xe.getKhachHang() : null);
        List<Xe> vehicles = mainView.getSelectedOverview() != null
                ? mainView.getSelectedOverview().danhSachXe()
                : (xe != null ? Collections.singletonList(xe) : Collections.emptyList());

        vehicleHistoryView.setVehicle(xe, kh, vehicles);
        cardLayout.show(cardsPanel, CARD_VEHICLE_HISTORY);
    }

    private void handleCancelAddCustomer() {
        if (returnToTiepNhan) {
            returnToTiepNhan = false;
            pendingPlateFromTiepNhan = null;
            Navigator.go(Screen.TIEP_NHAN_XE);
        } else {
            switchToMainView();
        }
    }

    private void handleCancelVehicleForm() {
        if (returnToTiepNhan) {
            returnToTiepNhan = false;
            pendingPlateFromTiepNhan = null;
            Navigator.go(Screen.TIEP_NHAN_XE);
        } else {
            switchToMainView();
        }
    }

    private void handleSavedVehicle(Xe newXe) {
        if (returnToTiepNhan) {
            returnToTiepNhan = false;
            pendingPlateFromTiepNhan = null;
            Navigator.setPayload(newXe);
            Navigator.go(Screen.TIEP_NHAN_XE);
            return;
        }
        switchToMainView();
        mainView.loadData();
        if (newXe != null && newXe.getKhachHang() != null) {
            mainView.selectCustomerById(newXe.getKhachHang().getMaKH());
        }
    }

    private void handleUpdatedVehicle(Xe updatedXe) {
        switchToMainView();
        mainView.loadData();
        if (updatedXe != null && updatedXe.getKhachHang() != null) {
            mainView.selectCustomerById(updatedXe.getKhachHang().getMaKH());
        }
    }

    private void handleDeletedVehicle(Xe deletedXe) {
        switchToMainView();
        mainView.loadData();
    }

    private void handleBackFromHistory() {
        switchToMainView();
        if (vehicleHistoryView.getCustomerInHistory() != null) {
            mainView.selectCustomerById(vehicleHistoryView.getCustomerInHistory().getMaKH());
        }
    }

    @Override
    public void onShow() {
        Object p = Navigator.consumePayload();
        if (p instanceof Navigator.TiepNhanAddCustomerRequest req) {
            this.returnToTiepNhan = true;
            this.pendingPlateFromTiepNhan = null;
            switchToAddCustomerView();
            if (req.initialQuery() != null && !req.initialQuery().isBlank()) {
                String q = req.initialQuery().trim();
                if (util.Validator.bienSo(q) || q.matches("^[0-9]{2}[A-Za-z].*")) {
                    this.pendingPlateFromTiepNhan = q.toUpperCase();
                    addCustomerView.resetForm();
                } else {
                    addCustomerView.setInitialQuery(q);
                }
            }
            return;
        }
        switchToMainView();
        mainView.loadData();
    }
}

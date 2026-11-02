package gui.panel;

import gui.Screen;
import gui.component.BasePanel;
import gui.component.Theme;
import gui.component.UI;
import util.RoundedPanel;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Trang giữ chỗ cho màn hình chưa làm: hiện người phụ trách và các yêu cầu chính lấy từ báo cáo. */
public final class PlaceholderPanel extends BasePanel {
    public PlaceholderPanel(Screen s) {
        super("Phụ trách: " + s.owner(), s.title(), "Màn hình này chưa được hiện thực.");

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel h = UI.h2("Cần làm ở màn hình này");
        JLabel p = new JLabel("<html><body style='width:640px'>" + s.hint() + "</body></html>");
        p.setFont(Theme.font(Font.PLAIN, 14));
        p.setForeground(Theme.TEXT);
        JLabel how = UI.muted("<html><body style='width:640px'>Tạo class mới kế thừa BasePanel, rồi thay <b>null</b> ở dòng "
                + s.name() + " trong Screen.java bằng NhanhCuaBan::new.</body></html>");
        h.setAlignmentX(LEFT_ALIGNMENT);
        p.setAlignmentX(LEFT_ALIGNMENT);
        how.setAlignmentX(LEFT_ALIGNMENT);
        card.add(h);
        card.add(Box.createVerticalStrut(10));
        card.add(p);
        card.add(Box.createVerticalStrut(14));
        card.add(how);
        body.add(card, BorderLayout.NORTH);
    }
}

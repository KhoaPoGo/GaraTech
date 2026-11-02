package gui.component;

import gui.Screen;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;

/**
 * Lớp cha của MỌI màn hình. Cung cấp sẵn tiêu đề trang (eyebrow + title + subtitle), vùng nút hành động
 * bên phải ({@link #actions}) và vùng nội dung ({@link #body}, layout mặc định BorderLayout).
 *
 * Cách làm một màn hình mới:
 * <pre>
 * public class KhachHangXePanel extends BasePanel {
 *     public KhachHangXePanel() {
 *         super("LỄ TÂN", "Khách hàng & xe", "Tra cứu khách hàng, phương tiện và lịch sử sửa chữa.");
 *         actions.add(UI.primaryButton("+ Thêm khách hàng"));
 *         body.add(buildContent(), BorderLayout.NORTH);
 *     }
 *     {@literal @}Override public void onShow() { reloadFromDb(); }   // gọi mỗi lần mở màn hình
 * }
 * </pre>
 * Rồi đăng ký trong {@code Screen} (chỉ thêm tham chiếu constructor).
 */
public abstract class BasePanel extends JPanel implements Scrollable {
    /** Nút ở góc phải của tiêu đề trang. */
    protected final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    /** Nội dung chính của màn hình. */
    protected final JPanel body = new JPanel(new BorderLayout());

    protected final JLabel lblEyebrow;
    protected final JLabel lblTitle;
    protected final JLabel lblSubtitle;

    protected BasePanel(String eyebrow, String title, String subtitle) {
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        lblEyebrow = UI.eyebrow(eyebrow);
        lblTitle = UI.h1(title);
        lblSubtitle = UI.muted(subtitle);
        for (JComponent c : new JComponent[]{lblEyebrow, lblTitle, lblSubtitle}) c.setAlignmentX(LEFT_ALIGNMENT);
        text.add(lblEyebrow);
        text.add(Box.createVerticalStrut(4));
        text.add(lblTitle);
        text.add(Box.createVerticalStrut(4));
        text.add(lblSubtitle);

        actions.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(text, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        body.setOpaque(false);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void setHeader(String eyebrow, String title, String subtitle) {
        if (eyebrow != null) lblEyebrow.setText(eyebrow.toUpperCase());
        if (title != null) lblTitle.setText(title);
        if (subtitle != null) lblSubtitle.setText(subtitle);
    }

    /** Được gọi mỗi lần người dùng mở màn hình này: nạp lại dữ liệu từ CSDL ở đây. */
    public void onShow() { }

    // ---- Scrollable: làm panel co giãn theo bề ngang cửa sổ, chỉ cuộn dọc ----
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}

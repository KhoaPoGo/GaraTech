package gui.component;


import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Nút menu trong sidebar: mục đang chọn tô vàng, hover tô xanh nhạt. */
final class NavButton extends JButton {
    private boolean selected;
    private boolean hover;

    NavButton(String text) {
        super(text);
        setHorizontalAlignment(SwingConstants.LEFT);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setMargin(new Insets(0, 16, 0, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(200, 42));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        setFont(Theme.font(Font.PLAIN, 13));
        setForeground(new Color(0xD4DCE8));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
    }

    void setSelectedItem(boolean s) {
        selected = s;
        setFont(Theme.font(s ? Font.BOLD : Font.PLAIN, 13));
        setForeground(s ? Theme.NAVY : new Color(0xD4DCE8));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (selected) {
            g2.setColor(Theme.YELLOW);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        } else if (hover) {
            g2.setColor(Theme.NAVY_LIGHT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

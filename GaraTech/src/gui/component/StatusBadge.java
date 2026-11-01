package gui.component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Nhãn trạng thái bo tròn, tự chọn màu theo Theme.statusColors(text). */
public class StatusBadge extends JLabel {
    private Color bg = Theme.NEUTRAL_BG;

    public StatusBadge(String text) {
        setFont(Theme.font(Font.BOLD, 11));
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setOpaque(false);
        setStatus(text);
    }

    public final void setStatus(String text) {
        Color[] c = Theme.statusColors(text);
        bg = c[0];
        setForeground(c[1]);
        setText(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}

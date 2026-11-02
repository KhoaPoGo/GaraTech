package util;

import gui.component.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Panel bo góc có viền mảnh, dùng làm "card" ở mọi màn hình. */
public class RoundedPanel extends JPanel {
    private final int arc;
    private Color borderColor = Theme.BORDER;

    public RoundedPanel() { this(16, Theme.CARD); }

    public RoundedPanel(int arc, Color background) {
        this.arc = arc;
        setOpaque(false);
        setBackground(background);
    }

    public RoundedPanel borderColor(Color c) { this.borderColor = c; repaint(); return this; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

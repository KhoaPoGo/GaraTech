package gui.component;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Biểu đồ cột vẽ tay (không cần thư viện). Cột được tô vàng là cột highlight (mặc định: cột cuối = hôm nay).
 * <pre>
 * chart.setData(new String[]{"30/09", ...}, new double[]{12.4, ...}, new String[]{"12.4tr", ...});
 * </pre>
 */
public class BarChartPanel extends JPanel {
    private String[] labels = new String[0];
    private double[] values = new double[0];
    private String[] valueLabels = new String[0];
    private int highlight = -1;

    public BarChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(520, 230));
    }

    public void setData(String[] labels, double[] values, String[] valueLabels) {
        this.labels = labels;
        this.values = values;
        this.valueLabels = valueLabels;
        repaint();
    }

    public void setHighlight(int index) { this.highlight = index; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        int n = values.length;
        if (n == 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int top = 26, bottom = 26, gap = 18;
        int barW = (w - gap * (n - 1)) / n;
        double max = 0;
        for (double v : values) max = Math.max(max, v);
        if (max <= 0) max = 1;
        int hi = highlight >= 0 ? highlight : n - 1;
        int area = h - top - bottom;

        for (int i = 0; i < n; i++) {
            int bh = (int) Math.round(values[i] / max * area);
            int x = i * (barW + gap);
            int y = h - bottom - bh;
            g2.setColor(i == hi ? Theme.YELLOW : Theme.NAVY);
            g2.fillRoundRect(x, y, barW, bh, 8, 8);
            g2.fillRect(x, y + Math.min(8, bh), barW, Math.max(0, bh - Math.min(8, bh))); // đáy vuông

            g2.setFont(Theme.font(Font.BOLD, 12));
            g2.setColor(Theme.TEXT);
            drawCentered(g2, i < valueLabels.length ? valueLabels[i] : "", x + barW / 2, y - 8);
            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.setColor(Theme.MUTED);
            drawCentered(g2, i < labels.length ? labels[i] : "", x + barW / 2, h - 8);
        }
        g2.dispose();
    }

    private static void drawCentered(Graphics2D g2, String s, int cx, int baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, cx - fm.stringWidth(s) / 2, baseline);
    }
}

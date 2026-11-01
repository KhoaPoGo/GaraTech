package gui.component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Hiển thị cột tiến độ (0–100) dạng "65%" + thanh vàng. Dùng: table.getColumnModel().getColumn(i).setCellRenderer(new ProgressCellRenderer()); */
public class ProgressCellRenderer extends JComponent implements TableCellRenderer {
    private int pct;
    private Color rowBg = Color.WHITE;

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
        pct = v instanceof Number n ? Math.max(0, Math.min(100, n.intValue())) : 0;
        rowBg = sel ? t.getSelectionBackground() : t.getBackground();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(rowBg);
        g2.fillRect(0, 0, getWidth(), getHeight());
        int pad = 12, barH = 6;
        int barW = getWidth() - pad * 2;
        int mid = getHeight() / 2;
        g2.setFont(Theme.font(Font.PLAIN, 12));
        g2.setColor(Theme.TEXT);
        g2.drawString(pct + "%", pad, mid - 3);
        g2.setColor(Theme.TRACK);
        g2.fillRoundRect(pad, mid + 3, barW, barH, barH, barH);
        g2.setColor(Theme.YELLOW);
        g2.fillRoundRect(pad, mid + 3, Math.max(barH, barW * pct / 100), barH, barH, barH);
        g2.dispose();
    }
}

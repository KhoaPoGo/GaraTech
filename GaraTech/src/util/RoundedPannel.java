package util;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * JPanel có nền một màu và các góc được bo tròn.
 */
public class RoundedPannel extends JPanel {
    private int cornerRadius;

    public RoundedPannel() {
        this(Color.WHITE, 20);
    }

    /**
     *
     * Màu sắc: @param backgroundColor
     * Độ bo góc: @param cornerRadius
     */
    public RoundedPannel(Color backgroundColor, int cornerRadius) {
        setOpaque(false);
        setBackground(backgroundColor);
        setCornerRadius(cornerRadius);
    }

    public RoundedPannel(int cornerRadius, Color backgroundColor) {
        this(backgroundColor, cornerRadius);
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        if (cornerRadius < 0) {
            throw new IllegalArgumentException("Độ bo góc không được nhỏ hơn 0.");
        }
        this.cornerRadius = cornerRadius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        graphics2D.setColor(getBackground());
        graphics2D.fillRoundRect(
                0,
                0,
                getWidth(),
                getHeight(),
                cornerRadius,
                cornerRadius
        );
        graphics2D.dispose();

        super.paintComponent(graphics);
    }

    @Override
    public boolean contains(int x, int y) {
        return new RoundRectangle2D.Float(
                0,
                0,
                getWidth(),
                getHeight(),
                cornerRadius,
                cornerRadius
        ).contains(x, y);
    }
}
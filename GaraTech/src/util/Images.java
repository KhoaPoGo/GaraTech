package util;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** Nạp ảnh trong resources/images và co theo kích thước mong muốn (giữ tỉ lệ, mượt). */
public final class Images {
    private Images() {}

    public static ImageIcon icon(String name, int maxW, int maxH) {
        try (InputStream in = Images.class.getResourceAsStream("/images/" + name)) {
            if (in == null) return null;
            BufferedImage src = ImageIO.read(in);
            double k = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
            int w = Math.max(1, (int) Math.round(src.getWidth() * k));
            int h = Math.max(1, (int) Math.round(src.getHeight() * k));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            return new ImageIcon(out);
        } catch (IOException e) {
            return null;
        }
    }
}

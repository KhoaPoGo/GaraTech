package gui.component;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

/**
 * Màu sắc, font và cấu hình FlatLaf cho toàn ứng dụng (lấy theo mockup GaraTech).
 * Chỉ SV1 sửa file này; mọi màn hình dùng hằng số ở đây thay vì tự đặt màu.
 */
public final class Theme {
    public static final Color NAVY = new Color(0x0F2342);
    public static final Color NAVY_LIGHT = new Color(0x1B3358);
    public static final Color YELLOW = new Color(0xFBC52D);
    public static final Color YELLOW_HOVER = new Color(0xF2B91A);
    public static final Color YELLOW_SOFT = new Color(0xFFF6D6);
    public static final Color BG = new Color(0xF4F5F9);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE3E7EE);
    public static final Color TEXT = new Color(0x0F2342);
    public static final Color MUTED = new Color(0x6B7A90);
    public static final Color TRACK = new Color(0xE6EAF0);

    public static final Color SUCCESS = new Color(0x1E8E5A);
    public static final Color SUCCESS_BG = new Color(0xE3F5EC);
    public static final Color DANGER = new Color(0xC8362B);
    public static final Color DANGER_BG = new Color(0xFDECEA);
    public static final Color WARN = new Color(0x9A6A00);
    public static final Color WARN_BG = new Color(0xFFF3D1);
    public static final Color INFO = new Color(0x24508F);
    public static final Color INFO_BG = new Color(0xE6EEFB);
    public static final Color NEUTRAL = new Color(0x4A5A70);
    public static final Color NEUTRAL_BG = new Color(0xECEFF4);

    private static final String FAMILY = "Inter";

    private Theme() {}

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    /** Gọi 1 lần trong App.main trước khi tạo bất kỳ cửa sổ nào. */
    public static void install() {
        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        UIManager.put("defaultFont", font(Font.PLAIN, 13));

        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("CheckBox.arc", 4);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Component.focusColor", YELLOW);
        UIManager.put("Component.borderColor", new Color(0xD5DBE5));
        UIManager.put("TextField.placeholderForeground", new Color(0x9AA6B8));

        UIManager.put("Table.cellMargins", new Insets(4, 12, 4, 12));
        UIManager.put("TableHeader.background", Color.WHITE);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.font", font(Font.BOLD, 12));
        UIManager.put("TableHeader.cellMargins", new Insets(4, 12, 4, 12));
        UIManager.put("TableHeader.separatorColor", Color.WHITE);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);

        UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 13));
        UIManager.put("OptionPane.yesButtonText", "Đồng ý");
        UIManager.put("OptionPane.noButtonText", "Không");
        UIManager.put("OptionPane.okButtonText", "Đóng");
        UIManager.put("OptionPane.cancelButtonText", "Hủy");

        FlatLightLaf.setup();
    }

    /** Màu nền + màu chữ cho nhãn trạng thái. Trả về {nền, chữ}. Thêm trạng thái mới tại đây. */
    public static Color[] statusColors(String status) {
        String s = status == null ? "" : status.trim().toLowerCase();
        return switch (s) {
            case "đang sửa chữa", "chờ duyệt", "chờ thanh toán", "chờ xử lý", "chờ xác nhận",
                 "sắp hết", "chạm ngưỡng", "đang kiểm tra" -> new Color[]{WARN_BG, WARN};
            case "chờ kiểm tra", "đã xác nhận", "chờ khách xác nhận", "đã phân công" -> new Color[]{INFO_BG, INFO};
            case "hoàn thành", "đã thanh toán", "đã xuất kho", "đủ hàng", "đã bàn giao", "rảnh", "sẵn sàng" ->
                    new Color[]{SUCCESS_BG, SUCCESS};
            case "đã hủy", "hủy", "từ chối", "hết hàng", "bị khóa" -> new Color[]{DANGER_BG, DANGER};
            default -> new Color[]{NEUTRAL_BG, NEUTRAL};
        };
    }
}

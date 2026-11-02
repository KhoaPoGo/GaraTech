package gui.component;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/** Hàm tạo nhanh các control có style thống nhất. Màn hình nào cũng nên tạo control qua đây. */
public final class UI {
    private UI() {}

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel h1(String t) { return label(t, Font.BOLD, 26, Theme.TEXT); }
    public static JLabel h2(String t) { return label(t, Font.BOLD, 16, Theme.TEXT); }
    public static JLabel muted(String t) { return label(t, Font.PLAIN, 13, Theme.MUTED); }
    public static JLabel eyebrow(String t) { return label(t.toUpperCase(), Font.PLAIN, 11, Theme.MUTED); }

    /** Nút chính (vàng). */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#FBC52D; foreground:#0F2342; hoverBackground:#F2B91A; pressedBackground:#E5AC0E;"
                        + "borderWidth:0; focusWidth:0; arc:10; margin:9,18,9,18");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Nút phụ (trắng, viền xám). */
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#FFFFFF; foreground:#0F2342; hoverBackground:#F4F5F9; pressedBackground:#E9ECF2;"
                        + "borderColor:#D5DBE5; focusWidth:0; arc:10; margin:9,18,9,18");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /** Model bảng không cho sửa trực tiếp trên ô. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Bảng đã được style giống mockup (không kẻ dọc, hàng cao 44, chọn dòng ra vàng nhạt). */
    public static JTable table(TableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(44);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setGridColor(Theme.BORDER);
        t.setSelectionBackground(Theme.YELLOW_SOFT);
        t.setSelectionForeground(Theme.TEXT);
        t.setFillsViewportHeight(true);
        t.setFocusable(false);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 38));
        return t;
    }

    /** Bọc bảng (tiêu đề + thân) vào một panel mà KHÔNG có thanh cuộn riêng, chiều cao tăng theo số dòng. */
    public static JPanel tableBlock(JTable t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(t.getTableHeader(), BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        return p;
    }
}

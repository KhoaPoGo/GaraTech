package gui.component;

import util.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Font;

/** Ô số liệu trên dashboard: nhãn nhỏ, số lớn, ghi chú. */
public class StatCard extends RoundedPanel {
    private final JLabel value;
    private final JLabel sub;

    public StatCard(String label, String valueText, String subText) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel l = UI.label(label, Font.PLAIN, 13, Theme.MUTED);
        value = UI.label(valueText, Font.BOLD, 28, Theme.TEXT);
        sub = UI.label(subText, Font.PLAIN, 12, Theme.MUTED);
        add(l);
        add(Box.createVerticalStrut(10));
        add(value);
        add(Box.createVerticalStrut(6));
        add(sub);
    }

    public void setValue(String v, String subText) { value.setText(v); sub.setText(subText); }
}

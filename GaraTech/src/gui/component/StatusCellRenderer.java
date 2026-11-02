package gui.component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/** Hiển thị cột trạng thái bằng StatusBadge màu. Cột chứa chuỗi trạng thái (vd "Đang sửa chữa"). */
public class StatusCellRenderer extends JPanel implements TableCellRenderer {
    private final StatusBadge badge = new StatusBadge("");

    public StatusCellRenderer() {
        super(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(0, 12, 0, 0);
        add(badge, gbc);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
        setBackground(sel ? t.getSelectionBackground() : t.getBackground());
        badge.setStatus(v == null ? "" : v.toString());
        return this;
    }
}

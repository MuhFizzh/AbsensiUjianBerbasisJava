package com.wms.ui.component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Tampilan (bukan editor) untuk kolom Aksi berisi tombol edit/hapus.
 * <p>
 * JTable memakai komponen renderer untuk MENGGAMBAR setiap baris, dan hanya
 * berpindah ke komponen editor saat baris tersebut benar-benar sedang diedit.
 * Sebelumnya panel-panel master data hanya memasang {@code setCellEditor(...)}
 * tanpa {@code setCellRenderer(...)}, sehingga kolom Aksi tampil kosong / hanya
 * berisi angka id mentah (nilai baris) sampai selnya diklik. Renderer ini
 * meniru persis tampilan tombol pada {@link AbstractCellEditorPanel} supaya
 * ikon edit/hapus selalu terlihat di setiap baris.
 */
public class ActionCellRenderer extends JPanel implements TableCellRenderer {

    public ActionCellRenderer() {
        super(new FlowLayout(FlowLayout.CENTER, 4, 4));
        setOpaque(true);

        // Tombol di renderer ini murni untuk digambar (JTable tidak pernah
        // mengirim event mouse ke komponen renderer), jadi tidak perlu
        // ActionListener - klik sungguhan ditangani oleh cell editor.
        RoundedButton editBtn = new RoundedButton("\u270E", RoundedButton.Style.ICON);
        editBtn.setPreferredSize(new Dimension(28, 28));

        RoundedButton delBtn = new RoundedButton("\uD83D\uDDD1", RoundedButton.Style.ICON);
        delBtn.setPreferredSize(new Dimension(28, 28));

        add(editBtn);
        add(delBtn);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
        return this;
    }
}

package com.wms.ui.component;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Mengaktifkan seleksi sel bebas (baris & kolom, bukan cuma satu baris utuh)
 * pada JTable, lalu mem-bind Ctrl+C (Cmd+C di macOS) untuk menyalin sel-sel
 * yang sedang dipilih ke clipboard sebagai teks dipisah TAB antar-kolom dan
 * baris-baru antar-baris - format yang langsung bisa ditempel ke Excel/Word/
 * Notepad, dsb.
 * <p>
 * Dipakai di semua tabel transaksi (Inbound, Outbound, Stock Opname) dan
 * request (Create Request, Approval), serta laporan aktivitas, supaya
 * datanya gampang disalin untuk keperluan laporan/lampiran.
 */
public final class TableCopyEnabler {

    private TableCopyEnabler() {
    }

    /** Panggil sekali setelah JTable dibuat, sebelum atau sesudah kolom di-setting. */
    public static void enable(JTable table) {
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);

        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke copyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask);

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(copyStroke, "wms-copy-selection");
        table.getActionMap().put("wms-copy-selection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectionToClipboard(table);
            }
        });
    }

    private static void copySelectionToClipboard(JTable table) {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                Object value = table.getValueAt(rows[r], cols[c]);
                sb.append(value == null ? "" : value.toString());
                if (c < cols.length - 1) sb.append('\t');
            }
            if (r < rows.length - 1) sb.append('\n');
        }

        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }
}

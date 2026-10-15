package com.wms.ui.panel;

/**
 * Kontrak untuk panel modul yang perlu mengambil ulang data dari database
 * setiap kali tab-nya ditampilkan (misalnya karena ada perubahan data yang
 * dilakukan dari tab lain, seperti kategori baru yang harus muncul di
 * dropdown form Barang).
 */
public interface RefreshablePanel {
    void refresh();
}

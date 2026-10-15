package com.wms;

import com.formdev.flatlaf.FlatLightLaf;
import com.wms.db.DatabaseConnection;
import com.wms.ui.MainFrame;
import com.wms.util.Session;
import com.wms.ui.dialog.LoginDialog;
import com.wms.util.AuditLogger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Gagal memuat FlatLaf, memakai Look and Feel default: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            if (!DatabaseConnection.testConnection()) {
                JOptionPane.showMessageDialog(null,
                        "Tidak dapat terhubung ke database.\n\n" +
                        "Pastikan MySQL sudah berjalan dan konfigurasi pada\n" +
                        "src/main/resources/db.properties sudah benar,\n" +
                        "lalu jalankan script database/schema.sql terlebih dahulu.",
                        "Koneksi Database Gagal",
                        JOptionPane.WARNING_MESSAGE);
            }

            showLoginThenMain();
        });
    }

    /**
     * Menampilkan LoginDialog, lalu jika berhasil login membuka MainFrame.
     * Dipanggil ulang setiap kali user Logout (lihat callback onLogout yang
     * dikirim ke MainFrame), sehingga ganti user / logout tidak perlu
     * restart aplikasi (JVM tetap sama, hanya jendela yang diganti).
     */
    private static void showLoginThenMain() {
        LoginDialog login = new LoginDialog(null);
        login.setVisible(true);

        if (!login.isAuthenticated()) {
            System.exit(0);
            return;
        }

        AuditLogger.log(
                "LOGIN",
                "LOGIN",
                null,
                "Login berhasil"
        );

        MainFrame frame = new MainFrame(Main::showLoginThenMain);
        frame.setVisible(true);
    }
}

package com.wms.ui.dialog;

import com.wms.dao.UserDAO;
import com.wms.model.User;
import com.wms.util.Session;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginDialog extends JDialog {

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    private boolean authenticated;

    public LoginDialog(Window owner) {

        super(owner, "Login WMS",
                ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(
                WindowConstants.DISPOSE_ON_CLOSE);

        setSize(380, 240);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(
                new BorderLayout(0, 14));

        root.setBorder(
                BorderFactory.createEmptyBorder(
                        22, 22, 22, 22));

        JLabel title = new JLabel(
                "Login Warehouse Management System");

        title.setFont(
                new Font("Segoe UI", Font.BOLD, 16));

        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(
                new GridLayout(2, 1, 8, 10));

        form.add(
                field("Username", usernameField));

        form.add(
                field("Password", passwordField));

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));

        JButton loginBtn = new JButton("Login");
        JButton cancelBtn = new JButton("Batal");

        loginBtn.addActionListener(e -> login());

        cancelBtn.addActionListener(
                e -> dispose());

        passwordField.addActionListener(
                e -> login());

        buttons.add(cancelBtn);
        buttons.add(loginBtn);

        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel field(
            String label,
            JComponent input) {

        JPanel p = new JPanel(
                new BorderLayout(0, 4));

        p.add(
                new JLabel(label),
                BorderLayout.NORTH);

        p.add(
                input,
                BorderLayout.CENTER);

        return p;
    }

    private void login() {

        String username =
                usernameField.getText().trim();

        String password =
                new String(
                        passwordField.getPassword());

        if (username.isBlank()
                || password.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Username dan password wajib diisi.",
                    "Validasi",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        try {

            User user =
                    new UserDAO().authenticate(
                            username,
                            password);

            if (user == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "Username/password salah " +
                        "atau user nonaktif.",
                        "Login gagal",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            Session.login(user);

            authenticated = true;

            dispose();

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Gagal login:\n"
                    + ex.getMessage(),
                    "Database",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
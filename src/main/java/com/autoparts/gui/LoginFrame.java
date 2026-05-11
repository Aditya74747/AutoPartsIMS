package com.autoparts.gui;

import com.autoparts.auth.AuthService;
import com.autoparts.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

//Login window - entry point for the app
public class LoginFrame extends JFrame {

    private final JTextField     txtUser = UIHelper.field(20);
    private final JPasswordField txtPass = UIHelper.passwordField(20);
    private final JLabel         lblMsg  = new JLabel(" ");

    public LoginFrame() {
        super("AutoParts IMS - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        drawLoginScreen();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void drawLoginScreen() {
        // Header bar
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        header.setBackground(UIHelper.HEADER_BG);
        JLabel title = new JLabel("Automotive Spare Parts IMS");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("  |  Stuttgart Assembly Division");
        sub.setFont(UIHelper.FONT_NORMAL);
        sub.setForeground(new Color(180, 210, 240));
        header.add(title);
        header.add(sub);

        // Login form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 215, 225)),
            BorderFactory.createEmptyBorder(24, 36, 16, 36)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        txtUser.setPreferredSize(new Dimension(220, 32));
        txtPass.setPreferredSize(new Dimension(220, 32));

        g.gridx = 0; g.gridy = 0; form.add(UIHelper.label("Username:"), g);
        g.gridx = 1;               form.add(txtUser, g);
        g.gridx = 0; g.gridy = 1; form.add(UIHelper.label("Password:"), g);
        g.gridx = 1;               form.add(txtPass, g);

        JButton btnLogin = UIHelper.styledBtn("Login", UIHelper.BTN_BLUE);
        btnLogin.setPreferredSize(new Dimension(160, 32));
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(14, 6, 4, 6);
        form.add(btnLogin, g);

        lblMsg.setForeground(Color.RED);
        lblMsg.setFont(UIHelper.FONT_SMALL);
        g.gridy = 3; g.insets = new Insets(0, 6, 4, 6);
        form.add(lblMsg, g);

        // Demo credentials hint just for testing and understanding
        JPanel hint = new JPanel(new GridLayout(3, 1, 0, 2));
        hint.setBackground(new Color(245, 247, 250));
        hint.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(8, 36, 10, 36)));
        hint.add(hintLabel("Demo logins:"));
        hint.add(hintLabel("Manager:  manager1 / Pass1234 "));
        hint.add(hintLabel("Manager:  manager2 / Pass5678 "));
        hint.add(hintLabel("Employee: emp1 / Emp1234 "));
        hint.add(hintLabel("Employee: emp2 / Emp5678 "));
        hint.add(hintLabel("Employee: emp3 / Emp9999 "));
        

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(form,   BorderLayout.CENTER);
        add(hint,   BorderLayout.SOUTH);

        UIHelper.attachFocusHighlight(txtUser);
        UIHelper.attachFocusHighlight(txtPass);

        ActionListener doLogin = e -> attemptLogin();
        btnLogin.addActionListener(doLogin);
        txtPass .addActionListener(doLogin);
    }

    private JLabel hintLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        l.setForeground(Color.GRAY);
        return l;
    }

    private void attemptLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            lblMsg.setText("Please enter username and password.");
            return;
        }
        User u = AuthService.login(user, pass);
        if (u == null) {
            lblMsg.setText("Incorrect username or password.");
            txtPass.setText("");
        } else {
            dispose();
            new MainFrame(u);
        }
    }
}

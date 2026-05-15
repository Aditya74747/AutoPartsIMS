package com.autoparts.gui;

import com.autoparts.db.DBConnection;
import com.autoparts.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

//Main application window, shown after login.
public class MainFrame extends JFrame {

    private final User user;

    public MainFrame(User user) {
        super("AutoParts IMS");
        this.user = user;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        setupWindow();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(MainFrame.this,
                    "Exit AutoParts IMS?", "Exit", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) { DBConnection.close(); System.exit(0); }
            }
        });
    }

    private void setupWindow() {
        // Top header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIHelper.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JLabel title = new JLabel("Automotive Spare Parts IMS - Stuttgart");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel userInfo = new JLabel("Logged in: " + user.getFullName() + "  [" + user.getRole() + "]");
        userInfo.setFont(UIHelper.FONT_NORMAL);
        userInfo.setForeground(new Color(200, 220, 245));
        JButton btnLogout = UIHelper.styledBtn("Logout", new Color(140, 50, 50));
        btnLogout.addActionListener(e -> doLogout());
        right.add(userInfo);
        right.add(btnLogout);
        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIHelper.FONT_BOLD);
        tabs.setBackground(new Color(240, 243, 248));
        tabs.setForeground(new Color(20, 20, 60));

        DashboardPanel dashPanel = new DashboardPanel(user);
        InventoryPanel invPanel  = new InventoryPanel(user);
        RequestsPanel  reqPanel  = new RequestsPanel(user);
        UsagePanel     usePanel  = new UsagePanel(user);
        OrderLogPanel  logPanel  = new OrderLogPanel(user);

        tabs.addTab("Dashboard",  dashPanel);
        tabs.addTab("Inventory",  invPanel);
        tabs.addTab("Requests",   reqPanel);
        tabs.addTab("Usage",      usePanel);
        tabs.addTab("Order Log",  logPanel);

        if (user.isManager()) {
            tabs.addTab("Parts",     new PartsPanel(user));
            tabs.addTab("Suppliers", new SuppliersPanel(user));
        }

        // Refresh whichever panel just became visible
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if      (c instanceof DashboardPanel p) p.refresh();
            else if (c instanceof InventoryPanel p) p.refresh();
            else if (c instanceof RequestsPanel  p) p.refresh();
            else if (c instanceof UsagePanel     p) p.refresh();
            else if (c instanceof OrderLogPanel  p) p.refresh();
            else if (c instanceof PartsPanel     p) p.refresh();
            else if (c instanceof SuppliersPanel p) p.refresh();
        });

        // Status bar at the bottom
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        status.setBackground(new Color(235, 240, 248));
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(180, 195, 210)));
        JLabel statusLbl = new JLabel("Connected to autoparts_ims @ localhost");
        statusLbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLbl.setForeground(new Color(60, 120, 60));
        status.add(statusLbl);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(tabs,   BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private void doLogout() {
        int r = JOptionPane.showConfirmDialog(this,
            "Log out and return to login screen?", "Logout", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) { dispose(); new LoginFrame(); }
    }
}

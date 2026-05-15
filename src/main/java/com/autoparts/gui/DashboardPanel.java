package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

//Dashboard - summary cards + three data tables
public class DashboardPanel extends JPanel {

    private final User user;

    private final JLabel valTotalParts   = cardLabel("-");
    private final JLabel valTotalUnits   = cardLabel("-");
    private final JLabel valStockValue   = cardLabel("-");
    private final JLabel valLowStock     = cardLabel("-");
    private final JLabel valOpenRequests = cardLabel("-");
    private final JLabel valUsedToday    = cardLabel("-");
    private final JLabel valSuppliers    = cardLabel("-");

    private final JTable tableLowStock  = new JTable();
    private final JTable tableTopUsed   = new JTable();
    private final JTable tableActivity  = new JTable();

    public DashboardPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);
        initLayout();
        refresh();
    }

    private void initLayout() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleArea.setOpaque(false);
        JLabel title = new JLabel("Dashboard");
        title.setFont(UIHelper.FONT_TITLE);
        JLabel sub = new JLabel("  Welcome back, " + user.getFullName());
        sub.setFont(UIHelper.FONT_NORMAL);
        sub.setForeground(Color.GRAY);
        titleArea.add(title);
        titleArea.add(sub);

        JButton btnRefresh = UIHelper.styledBtn("Refresh", UIHelper.BTN_BLUE);
        btnRefresh.addActionListener(e -> refresh());
        topBar.add(titleArea, BorderLayout.WEST);
        topBar.add(btnRefresh, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Stat cards row
        JPanel cards = new JPanel(new GridLayout(1, 7, 8, 0));
        cards.setOpaque(false);
        cards.add(statCard("Total Parts",       valTotalParts,   new Color(31, 78, 121)));
        cards.add(statCard("Total Units",        valTotalUnits,   new Color(39, 100, 59)));
        cards.add(statCard("Stock Value (EUR)",  valStockValue,   new Color(100, 60, 140)));
        cards.add(statCard("Low Stock Alerts",   valLowStock,     new Color(180, 70, 30)));
        cards.add(statCard("Open Requests",      valOpenRequests, new Color(150, 100, 20)));
        cards.add(statCard("Used Today",         valUsedToday,    new Color(40, 110, 140)));
        cards.add(statCard("Active Suppliers",   valSuppliers,    new Color(80, 80, 80)));

        // Three detail tables below the cards
        JPanel tables = new JPanel(new GridLayout(1, 3, 10, 0));
        tables.setOpaque(false);
        tables.add(miniTablePanel("Low Stock Alerts", tableLowStock, new Color(180, 70, 30)));
        tables.add(miniTablePanel("Top Used Parts",   tableTopUsed,  new Color(31, 78, 121)));
        tables.add(miniTablePanel("Recent Activity",  tableActivity, new Color(39, 100, 59)));

        JPanel centre = new JPanel(new BorderLayout(0, 10));
        centre.setOpaque(false);
        centre.add(cards,  BorderLayout.NORTH);
        centre.add(tables, BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);
    }

    public void refresh() {
        fetchKPIs();
        fetchLowStockTable();
        fetchTopPartsTable();
        fetchActivityTable();
    }

    private void fetchKPIs() {
        try {
            ResultSet rs = InventoryDAO.getDashboardStats();
            if (rs.next()) {
                valTotalParts  .setText(String.valueOf(rs.getInt("total_parts")));
                valTotalUnits  .setText(String.valueOf(rs.getInt("total_units")));
                valStockValue  .setText(String.format("%.0f", rs.getDouble("stock_value_eur")));
                valLowStock    .setText(String.valueOf(rs.getInt("low_stock_count")));
                valOpenRequests.setText(String.valueOf(rs.getInt("open_requests")));
                valUsedToday   .setText(String.valueOf(rs.getInt("used_today")));
                valSuppliers   .setText(String.valueOf(rs.getInt("active_suppliers")));
                // Turn red if any parts are below threshold
                int low = rs.getInt("low_stock_count");
                valLowStock.setForeground(low > 0 ? new Color(255, 100, 80) : Color.WHITE);
            }
            rs.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void fetchLowStockTable() {
        try { UIHelper.populateFromRS(tableLowStock, InventoryDAO.getLowStockReport()); }
        catch (SQLException e) { e.printStackTrace(); }
    }

    private void fetchTopPartsTable() {
        try { UIHelper.populateFromRS(tableTopUsed, InventoryDAO.getTopUsedParts()); }
        catch (SQLException e) {
            // No usage data yet - show empty table
            tableTopUsed.setModel(new DefaultTableModel(
                new String[]{"Part Name", "Category", "Total Used", "Current Stock"}, 0));
            UIHelper.applyTableTheme(tableTopUsed);
        }
    }

    private void fetchActivityTable() {
        try { UIHelper.populateFromRS(tableActivity, InventoryDAO.getRecentActivity()); }
        catch (SQLException e) { e.printStackTrace(); }
    }

    private JPanel statCard(String title, JLabel valueLabel, Color bg) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        titleLbl.setForeground(new Color(200, 220, 240));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLbl,   BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel miniTablePanel(String title, JTable table, Color accent) {
        UIHelper.applyTableTheme(table);
        table.setFont(UIHelper.FONT_SMALL);
        table.setRowHeight(22);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(150, 170, 190)));
        sp.getViewport().setBackground(Color.WHITE);
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(accent);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        p.add(lbl, BorderLayout.NORTH);
        p.add(sp,  BorderLayout.CENTER);
        return p;
    }

    private static JLabel cardLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 28));
        l.setForeground(Color.WHITE);
        return l;
    }
}

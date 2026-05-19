package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.*;

//Employees raise part requests; managers update statuses.
//Setting status to ARRIVED triggers trg_arrived_update_inventory automatically.
public class RequestsPanel extends JPanel {

    private final User   user;
    private final JTable tableActive = new JTable();
    private final JTable tableAll    = new JTable();
    private TableRowSorter<DefaultTableModel> sorterActive;
    private TableRowSorter<DefaultTableModel> sorterAll;

    public RequestsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);
        setupPanel();
        refresh();
    }

    private void setupPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Part Requests & Supplier Orders");
        title.setFont(UIHelper.FONT_TITLE);
        top.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton btnRefresh = UIHelper.styledBtn("Refresh",          UIHelper.BTN_BLUE);
        JButton btnNew     = UIHelper.styledBtn("New Request",      UIHelper.BTN_GREEN);
        JButton btnSummary = UIHelper.styledBtn("Pipeline Summary", UIHelper.BTN_BLUE);
        buttons.add(btnRefresh); buttons.add(btnNew); buttons.add(btnSummary);
        if (user.isManager()) {
            JButton btnStatus = UIHelper.styledBtn("Update Status", UIHelper.BTN_ORANGE);
            btnStatus.addActionListener(e -> openStatusDialog());
            buttons.add(btnStatus);
        }
        top.add(buttons, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setDividerSize(5);
        split.setTopComponent(requestTableSection("Active / Pending Orders",
            tableActive, () -> sorterActive));
        split.setBottomComponent(requestTableSection("Full Request History",
            tableAll, () -> sorterAll));
        add(split, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> refresh());
        btnNew    .addActionListener(e -> openNewRequestDialog());
        btnSummary.addActionListener(e -> openPipelineSummary());
    }

    private JPanel requestTableSection(String title, JTable t,
            java.util.function.Supplier<TableRowSorter<?>> sorterSupplier) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(UIHelper.FONT_BOLD);
        headerRow.add(lbl, BorderLayout.WEST);
        headerRow.add(InventoryPanel.filterBar(t, sorterSupplier), BorderLayout.EAST);
        p.add(headerRow,             BorderLayout.NORTH);
        p.add(UIHelper.scrollableTable(t), BorderLayout.CENTER);
        return p;
    }

    public void refresh() {
        try {
            sorterActive = InventoryPanel.tableFromRS(tableActive, InventoryDAO.getActiveRequests());
            sorterAll    = InventoryPanel.tableFromRS(tableAll,    InventoryDAO.getAllRequests());
        } catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void openPipelineSummary() {
        try {
            JTable t = new JTable();
            UIHelper.populateFromRS(t, InventoryDAO.getRequestSummary());
            JScrollPane sp = UIHelper.scrollableTable(t);
            sp.setPreferredSize(new Dimension(550, 200));
            JOptionPane.showMessageDialog(this, sp,
                "Request Pipeline Summary", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void openNewRequestDialog() {
        JComboBox<String> cbPart = new JComboBox<>();
        Map<String, Integer> partIds = new LinkedHashMap<>();
        try {
            ResultSet rs = InventoryDAO.getPartsForCombo();
            while (rs.next()) {
                String label = rs.getString("label");
                partIds.put(label, rs.getInt("part_id"));
                cbPart.addItem(label);
            }
            rs.close();
        } catch (SQLException ex) { UIHelper.error(this, ex.getMessage()); return; }

        JSpinner   qty   = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        JTextField date  = UIHelper.field(12);
        date.setText(java.time.LocalDate.now().plusDays(7).toString());
        JTextArea  notes = UIHelper.textArea(3, 24);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.anchor = GridBagConstraints.WEST;
        formRow(p, g, 0, "Part:", cbPart);
        formRow(p, g, 1, "Quantity:", qty);
        formRow(p, g, 2, "Expected arrival (YYYY-MM-DD):", date);
        formRow(p, g, 3, "Notes:", new JScrollPane(notes));

        int ok = JOptionPane.showConfirmDialog(this, p, "New Part Request",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        String sel = (String) cbPart.getSelectedItem();
        if (sel == null) return;
        java.sql.Date arrival;
        try { arrival = java.sql.Date.valueOf(date.getText().trim()); }
        catch (IllegalArgumentException ex) {
            UIHelper.error(this, "Invalid date. Use YYYY-MM-DD format."); return;
        }
        if (InventoryDAO.createRequest(partIds.get(sel), user.getUserId(),
                (int) qty.getValue(), notes.getText().trim(), arrival))
            { UIHelper.info(this, "Request submitted successfully."); refresh(); }
        else UIHelper.error(this, "Failed to submit request.");
    }

    private void openStatusDialog() {
        int row = tableActive.getSelectedRow();
        if (row < 0) { UIHelper.error(this, "Select a row from Active Orders first."); return; }
        int modelRow = tableActive.convertRowIndexToModel(row);
        int    rid     = (int)    tableActive.getModel().getValueAt(modelRow, 0);
        String part    = (String) tableActive.getModel().getValueAt(modelRow, 2);
        String current = (String) tableActive.getModel().getValueAt(modelRow, 4);

        String[] options = {"PENDING","ORDERED","SHIPPED","ARRIVED","CANCELLED"};
        JComboBox<String> cb = new JComboBox<>(options);
        cb.setSelectedItem(current);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.anchor = GridBagConstraints.WEST;
        formRow(p, g, 0, "Part:",           new JLabel(part));
        formRow(p, g, 1, "Current status:", new JLabel(current));
        formRow(p, g, 2, "New status:",     cb);

        // Note about the trigger
        JLabel note = new JLabel("Note: setting ARRIVED automatically updates stock.");
        note.setFont(UIHelper.FONT_SMALL); note.setForeground(Color.GRAY);
        GridBagConstraints gn = new GridBagConstraints();
        gn.gridx = 0; gn.gridy = 3; gn.gridwidth = 2; gn.insets = new Insets(2, 5, 2, 5);
        p.add(note, gn);

        int ok = JOptionPane.showConfirmDialog(this, p,
            "Update Status - Request #" + rid, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        String ns = (String) cb.getSelectedItem();
        if (ns.equals(current)) return;
        if (InventoryDAO.updateRequestStatus(rid, ns, user.getUserId())) {
            UIHelper.info(this, "Status changed to " + ns + "."
                + ("ARRIVED".equals(ns) ? "\nStock updated automatically." : ""));
            refresh();
        } else UIHelper.error(this, "Update failed. Check allowed transitions.");
    }

    private void formRow(JPanel p, GridBagConstraints g, int row, String lbl, Component c) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; p.add(UIHelper.label(lbl), g);
        g.gridx = 1; p.add(c, g);
    }
}

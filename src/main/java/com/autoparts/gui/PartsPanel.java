package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.*;

//Parts catalogue - managers can add, edit and delete; employees can only browse
public class PartsPanel extends JPanel {

    private final User   user;
    private final JTable table = new JTable();
    private TableRowSorter<DefaultTableModel> sorter;

    public PartsPanel(User user) {
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
        JLabel title = new JLabel("Parts Catalogue");
        title.setFont(UIHelper.FONT_TITLE);
        top.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton btnRefresh = UIHelper.styledBtn("Refresh", UIHelper.BTN_BLUE);
        buttons.add(btnRefresh);
        if (user.isManager()) {
            JButton btnAdd    = UIHelper.styledBtn("Add Part",    UIHelper.BTN_GREEN);
            JButton btnEdit   = UIHelper.styledBtn("Edit Part",   UIHelper.BTN_BLUE);
            JButton btnDelete = UIHelper.styledBtn("Delete Part", UIHelper.BTN_RED);
            buttons.add(btnAdd); buttons.add(btnEdit); buttons.add(btnDelete);
            btnAdd   .addActionListener(e -> openAddPartDialog());
            btnEdit  .addActionListener(e -> openEditPartDialog());
            btnDelete.addActionListener(e -> confirmDeletePart());
        }
        top.add(buttons, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(top,                                           BorderLayout.NORTH);
        header.add(InventoryPanel.filterBar(table, () -> sorter), BorderLayout.SOUTH);

        add(header,                    BorderLayout.NORTH);
        add(UIHelper.scrollableTable(table), BorderLayout.CENTER);
        btnRefresh.addActionListener(e -> refresh());
    }

    public void refresh() {
        try { sorter = InventoryPanel.tableFromRS(table, InventoryDAO.getAllParts()); }
        catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void openAddPartDialog() {
        JTextField fNum   = UIHelper.field(14);
        JTextField fName  = UIHelper.field(22);
        JTextField fPrice = UIHelper.field(8);
        JTextField fUnit  = UIHelper.field(6); fUnit.setText("pcs");
        JTextArea  fDesc  = UIHelper.textArea(3, 22);
        String[] cats = {"ENGINE","TRANSMISSION","BRAKES","SUSPENSION",
                         "ELECTRICAL","BODY","WHEELS","INTERIOR","OTHER"};
        JComboBox<String> cbCat  = new JComboBox<>(cats);
        JComboBox<String> cbSupp = new JComboBox<>();
        Map<String, Integer> suppIds = new LinkedHashMap<>();
        try {
            ResultSet rs = InventoryDAO.getSuppliersForCombo();
            while (rs.next()) {
                suppIds.put(rs.getString("name"), rs.getInt("supplier_id"));
                cbSupp.addItem(rs.getString("name"));
            }
            rs.close();
        } catch (SQLException ex) { ex.printStackTrace(); }

        JPanel p = formGrid(
            new String[]{"Part Number:","Name:","Category:","Unit:",
                         "Unit Price (EUR):","Supplier:","Description:"},
            new Component[]{fNum, fName, cbCat, fUnit, fPrice, cbSupp, new JScrollPane(fDesc)});

        int ok = JOptionPane.showConfirmDialog(this, p, "Add New Part",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            double price = Double.parseDouble(fPrice.getText().trim());
            int sid = suppIds.getOrDefault((String) cbSupp.getSelectedItem(), -1);
            int id  = InventoryDAO.insertPart(fNum.getText().trim(), fName.getText().trim(),
                (String) cbCat.getSelectedItem(),
                fUnit.getText().trim().isEmpty() ? "pcs" : fUnit.getText().trim(),
                price, sid, fDesc.getText().trim());
            if (id > 0) { UIHelper.info(this, "Part added (ID " + id + "). Inventory row created."); refresh(); }
            else UIHelper.error(this, "Insert failed - part number may already exist.");
        } catch (NumberFormatException ex) { UIHelper.error(this, "Invalid price value."); }
    }

    private void openEditPartDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { UIHelper.error(this, "Select a row first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        int    pid  = (int)    table.getModel().getValueAt(modelRow, 0);
        String curN = (String) table.getModel().getValueAt(modelRow, 2);
        String curC = (String) table.getModel().getValueAt(modelRow, 3);
        double curP = table.getModel().getValueAt(modelRow, 5) instanceof Number
                      ? ((Number) table.getModel().getValueAt(modelRow, 5)).doubleValue() : 0;
        JTextField fName  = UIHelper.field(22); fName.setText(curN);
        JTextField fPrice = UIHelper.field(8);  fPrice.setText(String.format("%.2f", curP));
        String[] cats = {"ENGINE","TRANSMISSION","BRAKES","SUSPENSION",
                         "ELECTRICAL","BODY","WHEELS","INTERIOR","OTHER"};
        JComboBox<String> cbCat = new JComboBox<>(cats); cbCat.setSelectedItem(curC);

        JPanel p = formGrid(new String[]{"Name:","Category:","Unit Price (EUR):"},
            new Component[]{fName, cbCat, fPrice});
        int ok = JOptionPane.showConfirmDialog(this, p, "Edit Part ID " + pid,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            double price = Double.parseDouble(fPrice.getText().trim());
            if (InventoryDAO.updatePart(pid, fName.getText().trim(),
                    (String) cbCat.getSelectedItem(), price, ""))
                { UIHelper.info(this, "Part updated."); refresh(); }
            else UIHelper.error(this, "Update failed.");
        } catch (NumberFormatException ex) { UIHelper.error(this, "Invalid price."); }
    }

    private void confirmDeletePart() {
        int row = table.getSelectedRow();
        if (row < 0) { UIHelper.error(this, "Select a row first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        int    pid  = (int)    table.getModel().getValueAt(modelRow, 0);
        String name = (String) table.getModel().getValueAt(modelRow, 2);
        int c = JOptionPane.showConfirmDialog(this,
            "Delete part \"" + name + "\"?\nThis also removes its inventory record.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (c != JOptionPane.YES_OPTION) return;
        if (InventoryDAO.deletePart(pid)) { UIHelper.info(this, "Part deleted."); refresh(); }
        else UIHelper.error(this, "Delete failed - part may be in an active request.");
    }

    private JPanel formGrid(String[] labels, Component[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.gridwidth = 1; p.add(UIHelper.label(labels[i]), g);
            g.gridx = 1; p.add(fields[i], g);
        }
        return p;
    }
}

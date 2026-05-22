package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;

//Supplier management - managers can add/edit; employees view only
public class SuppliersPanel extends JPanel {

    private final User   user;
    private final JTable table = new JTable();
    private TableRowSorter<DefaultTableModel> sorter;

    public SuppliersPanel(User user) {
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
        JLabel title = new JLabel("Suppliers");
        title.setFont(UIHelper.FONT_TITLE);
        top.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton btnRefresh = UIHelper.styledBtn("Refresh", UIHelper.BTN_BLUE);
        buttons.add(btnRefresh);
        if (user.isManager()) {
            JButton btnAdd  = UIHelper.styledBtn("Add Supplier",  UIHelper.BTN_GREEN);
            JButton btnEdit = UIHelper.styledBtn("Edit Supplier", UIHelper.BTN_BLUE);
            buttons.add(btnAdd); buttons.add(btnEdit);
            btnAdd .addActionListener(e -> openAddSupplierDialog());
            btnEdit.addActionListener(e -> openEditSupplierDialog());
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
        try { sorter = InventoryPanel.tableFromRS(table, InventoryDAO.getAllSuppliers()); }
        catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void openAddSupplierDialog() {
        JTextField[] f = blankSupplierFields();
        int ok = JOptionPane.showConfirmDialog(this, supplierFormGrid(f),
            "Add Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        if (InventoryDAO.insertSupplier(f[0].getText().trim(), f[1].getText().trim(),
                f[2].getText().trim(), f[3].getText().trim(), f[4].getText().trim()))
            { UIHelper.info(this, "Supplier added."); refresh(); }
        else UIHelper.error(this, "Insert failed.");
    }

    private void openEditSupplierDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { UIHelper.error(this, "Select a row first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        int sid = (int) table.getModel().getValueAt(modelRow, 0);
        JTextField[] f = blankSupplierFields();
        f[0].setText(cellText(modelRow, 1)); f[1].setText(cellText(modelRow, 2));
        f[2].setText(cellText(modelRow, 3)); f[3].setText(cellText(modelRow, 4));
        f[4].setText(cellText(modelRow, 5));
        int ok = JOptionPane.showConfirmDialog(this, supplierFormGrid(f),
            "Edit Supplier ID " + sid, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        if (InventoryDAO.updateSupplier(sid, f[0].getText().trim(), f[1].getText().trim(),
                f[2].getText().trim(), f[3].getText().trim(), f[4].getText().trim()))
            { UIHelper.info(this, "Supplier updated."); refresh(); }
        else UIHelper.error(this, "Update failed.");
    }

    private JTextField[] blankSupplierFields() {
        return new JTextField[]{
            UIHelper.field(24), UIHelper.field(20),
            UIHelper.field(16), UIHelper.field(24), UIHelper.field(32)};
    }

    private JPanel supplierFormGrid(JTextField[] f) {
        String[] labels = {"Company Name:","Contact Name:","Phone:","Email:","Address:"};
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; p.add(UIHelper.label(labels[i]), g);
            g.gridx = 1; p.add(f[i], g);
        }
        return p;
    }

    private String cellText(int row, int col) {
        Object v = table.getModel().getValueAt(row, col);
        return v == null ? "" : v.toString();
    }
}

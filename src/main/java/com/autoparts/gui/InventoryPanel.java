package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;

//Shows current stock levels with live search filter
public class InventoryPanel extends JPanel {

    private final User   user;
    private final JTable table = new JTable();
    private TableRowSorter<DefaultTableModel> sorter;

    public InventoryPanel(User user) {
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

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleArea.setOpaque(false);
        JLabel title = new JLabel("Current Stock Levels");
        title.setFont(UIHelper.FONT_TITLE);
        JLabel sub = new JLabel("  Real-time inventory across all parts");
        sub.setFont(UIHelper.FONT_NORMAL);
        sub.setForeground(Color.GRAY);
        titleArea.add(title);
        titleArea.add(sub);
        top.add(titleArea, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton btnRefresh  = UIHelper.styledBtn("Refresh",     UIHelper.BTN_BLUE);
        JButton btnLowStock = UIHelper.styledBtn("Low Stock",   UIHelper.BTN_ORANGE);
        JButton btnStockVal = UIHelper.styledBtn("Stock Value", UIHelper.BTN_BLUE);
        buttons.add(btnRefresh);
        buttons.add(btnLowStock);
        buttons.add(btnStockVal);
        if (user.isManager()) {
            JButton btnEdit = UIHelper.styledBtn("Edit Quantity", UIHelper.BTN_GREEN);
            btnEdit.addActionListener(e -> promptQuantityEdit());
            buttons.add(btnEdit);
        }
        top.add(buttons, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(top,                        BorderLayout.NORTH);
        header.add(filterBar(table, () -> sorter), BorderLayout.SOUTH);

        add(header,                    BorderLayout.NORTH);
        add(UIHelper.scrollableTable(table), BorderLayout.CENTER);

        btnRefresh .addActionListener(e -> refresh());
        btnLowStock.addActionListener(e -> openLowStockDialog());
        btnStockVal.addActionListener(e -> openStockValueDialog());
    }

    public void refresh() {
        try {
            sorter = tableFromRS(table, InventoryDAO.getInventorySummary());
        } catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void openLowStockDialog() {
        try {
            JTable t = new JTable();
            UIHelper.populateFromRS(t, InventoryDAO.getLowStockReport());
            JScrollPane sp = UIHelper.scrollableTable(t);
            sp.setPreferredSize(new Dimension(750, 280));
            JOptionPane.showMessageDialog(this, sp,
                "Parts At or Below Minimum Threshold", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void openStockValueDialog() {
        try {
            JTable t = new JTable();
            UIHelper.populateFromRS(t, InventoryDAO.getStockValueBySupplier());
            JScrollPane sp = UIHelper.scrollableTable(t);
            sp.setPreferredSize(new Dimension(600, 250));
            JOptionPane.showMessageDialog(this, sp,
                "Total Stock Value by Supplier", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }

    private void promptQuantityEdit() {
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        if (row < 0) { UIHelper.error(this, "Please select a row first."); return; }
        int    partId  = (int) table.getModel().getValueAt(row, 0);
        String name    = table.getModel().getValueAt(row, 2).toString();
        Object qObj    = table.getModel().getValueAt(row, 4);
        int    current = qObj instanceof Number ? ((Number) qObj).intValue() : 0;

        String input = JOptionPane.showInputDialog(this,
            "Part: " + name + "\nCurrent quantity: " + current + "\n\nEnter new quantity:",
            "Edit Quantity", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        try {
            int newQty = Integer.parseInt(input.trim());
            if (newQty < 0) throw new NumberFormatException();
            if (InventoryDAO.updateStock(partId, newQty)) {
                UIHelper.info(this, "Quantity updated to " + newQty + ".");
                refresh();
            } else UIHelper.error(this, "Update failed. Quantity cannot be negative.");
        } catch (NumberFormatException ex) {
            UIHelper.error(this, "Please enter a valid non-negative number.");
        }
    }

    // Shared search bar used by several panels
    static JPanel filterBar(JTable table, java.util.function.Supplier<TableRowSorter<?>> sorterSupplier) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JLabel lbl = new JLabel("Search: ");
        lbl.setFont(UIHelper.FONT_BOLD);
        JTextField fSearch = UIHelper.field(26);
        fSearch.setToolTipText("Type to filter rows in real time");
        JButton btnClear = UIHelper.styledBtn("Clear", new Color(120, 120, 120));

        fSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { doFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { doFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
            void doFilter() {
                TableRowSorter<?> s = sorterSupplier.get();
                if (s == null) return;
                String text = fSearch.getText().trim();
                s.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
            }
        });
        btnClear.addActionListener(e -> {
            fSearch.setText("");
            TableRowSorter<?> s = sorterSupplier.get();
            if (s != null) s.setRowFilter(null);
        });

        row.add(lbl);
        row.add(fSearch);
        row.add(Box.createHorizontalStrut(6));
        row.add(btnClear);
        return row;
    }

    // Loads a ResultSet into the table and wires up row sorting
    static TableRowSorter<DefaultTableModel> tableFromRS(JTable table, java.sql.ResultSet rs) {
        try {
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            String[] names = new String[cols];
            for (int i = 1; i <= cols; i++)
                names[i - 1] = meta.getColumnLabel(i).replace('_', ' ');
            DefaultTableModel model = new DefaultTableModel(names, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) row[i - 1] = rs.getObject(i);
                model.addRow(row);
            }
            rs.close();
            table.setModel(model);
            UIHelper.applyTableTheme(table);
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            return sorter;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

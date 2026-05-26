package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

//Record parts consumed on the factory floor.
//Trigger trg_deduct_usage_stock fires on insert and deducts from inventory automatically.
public class UsagePanel extends JPanel {

    private final User   user;
    private final JTable table = new JTable();
    private TableRowSorter<DefaultTableModel> sorter;

    private final JComboBox<String> cbPart  = new JComboBox<>();
    private final JSpinner         spinQty  = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JTextField       fReason  = UIHelper.field(28);
    private final JLabel           lblStock = new JLabel("Available stock: -");

    private final Map<String, Integer> partIds = new LinkedHashMap<>();

    public UsagePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);
        setupPanel();
        populatePartDropdown();
        refresh();
    }

    private void setupPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleArea.setBackground(Color.WHITE);
        JLabel title = new JLabel("Usage Records");
        title.setFont(UIHelper.FONT_TITLE);
        title.setForeground(new Color(20, 20, 60));
        JLabel sub = new JLabel("  Log parts consumed on the factory floor");
        sub.setFont(UIHelper.FONT_NORMAL);
        sub.setForeground(Color.GRAY);
        titleArea.add(title);
        titleArea.add(sub);

        JButton btnRefresh = UIHelper.styledBtn("Refresh", UIHelper.BTN_BLUE);
        btnRefresh.addActionListener(e -> refresh());
        top.add(titleArea,  BorderLayout.WEST);
        top.add(btnRefresh, BorderLayout.EAST);

        // Left side - the form card
        JPanel formCard = new JPanel(new BorderLayout(0, 12));
        formCard.setBackground(new Color(245, 248, 252));
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 195, 215), 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        formCard.setMinimumSize(new Dimension(400, 300));
        formCard.setPreferredSize(new Dimension(420, 400));

        JLabel formTitle = new JLabel("Record Part Usage");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        formTitle.setForeground(UIHelper.HEADER_BG);
        formTitle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(180, 195, 215)),
            BorderFactory.createEmptyBorder(0, 0, 8, 0)));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(new Color(245, 248, 252));
        GridBagConstraints g = new GridBagConstraints();
        g.anchor  = GridBagConstraints.WEST;
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 0;

        cbPart.setFont(UIHelper.FONT_NORMAL);
        cbPart.setBackground(Color.WHITE);
        cbPart.setForeground(new Color(20, 20, 20));
        cbPart.setPreferredSize(new Dimension(340, 30));
        cbPart.setMaximumSize(new Dimension(340, 30));
        cbPart.setPrototypeDisplayValue("WHL-001 - Front Wheel Rim 18in Alloy - Long Part Name");
        cbPart.addActionListener(e -> refreshStockIndicator());

        spinQty.setFont(UIHelper.FONT_NORMAL);
        spinQty.setPreferredSize(new Dimension(90, 28));
        JComponent spinEditor = spinQty.getEditor();
        if (spinEditor instanceof JSpinner.DefaultEditor) {
            JTextField spinField = ((JSpinner.DefaultEditor) spinEditor).getTextField();
            spinField.setFont(UIHelper.FONT_NORMAL);
            spinField.setForeground(new Color(20, 20, 20));
            spinField.setBackground(Color.WHITE);
            spinField.setHorizontalAlignment(JTextField.LEFT);
        }

        lblStock.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblStock.setForeground(new Color(40, 110, 40));

        // Row 0: part combo
        g.gridx = 0; g.gridy = 0; g.insets = new Insets(6, 0, 2, 10); g.weightx = 0;
        fields.add(formLabel("Part:"), g);
        g.gridx = 1; g.insets = new Insets(6, 0, 2, 0); g.weightx = 1.0;
        fields.add(cbPart, g);

        // Row 1: stock indicator
        g.gridx = 1; g.gridy = 1; g.insets = new Insets(0, 2, 8, 0);
        fields.add(lblStock, g);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(200, 210, 220));
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.insets = new Insets(0, 0, 8, 0);
        fields.add(sep, g);

        // Row 3: quantity
        g.gridwidth = 1; g.gridy = 3; g.gridx = 0; g.insets = new Insets(6, 0, 6, 10); g.weightx = 0;
        fields.add(formLabel("Quantity:"), g);
        g.gridx = 1; g.insets = new Insets(6, 0, 6, 0); g.weightx = 1.0;
        spinQty.setPreferredSize(new Dimension(100, 28));
        fields.add(spinQty, g);

        // Row 4: reason
        g.gridx = 0; g.gridy = 4; g.insets = new Insets(6, 0, 6, 10); g.weightx = 0;
        fields.add(formLabel("Reason:"), g);
        g.gridx = 1; g.insets = new Insets(6, 0, 6, 0); g.weightx = 1.0;
        fReason.setPreferredSize(new Dimension(340, 30));
        fields.add(fReason, g);

        // Row 5: submit button
        JButton btnRecord = UIHelper.styledBtn("Record Usage", UIHelper.BTN_GREEN);
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; g.insets = new Insets(12, 0, 8, 0);
        g.fill = GridBagConstraints.HORIZONTAL;
        fields.add(btnRecord, g);

        // Row 6: trigger note
        JLabel note = new JLabel(
            "<html><i>Stock is deducted automatically via<br>" +
            "trigger <b>trg_deduct_usage_stock</b>.</i></html>");
        note.setFont(UIHelper.FONT_SMALL);
        note.setForeground(new Color(100, 110, 130));
        note.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)),
            BorderFactory.createEmptyBorder(8, 2, 0, 0)));
        g.gridy = 6; g.insets = new Insets(4, 0, 0, 0);
        fields.add(note, g);

        formCard.add(formTitle, BorderLayout.NORTH);
        formCard.add(fields,    BorderLayout.CENTER);
        btnRecord.addActionListener(e -> submitUsageEntry());

        // Right side - usage history table with search
        JPanel historyPanel = new JPanel(new BorderLayout(0, 6));
        historyPanel.setBackground(Color.WHITE);

        JPanel histTop = new JPanel(new BorderLayout());
        histTop.setBackground(Color.WHITE);
        JLabel histTitle = new JLabel("Usage History");
        histTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        histTitle.setForeground(new Color(20, 20, 60));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchRow.setBackground(Color.WHITE);
        JLabel searchLbl = new JLabel("Search: ");
        searchLbl.setFont(UIHelper.FONT_BOLD);
        searchLbl.setForeground(new Color(30, 30, 30));
        JTextField fSearch = UIHelper.field(22);
        fSearch.setToolTipText("Filter by part name, reason, or user");
        fSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyHistoryFilter(fSearch.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyHistoryFilter(fSearch.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyHistoryFilter(fSearch.getText()); }
        });
        JButton btnClear = UIHelper.styledBtn("Clear", new Color(110, 110, 120));
        btnClear.addActionListener(e -> { fSearch.setText(""); applyHistoryFilter(""); });
        searchRow.add(searchLbl);
        searchRow.add(fSearch);
        searchRow.add(Box.createHorizontalStrut(6));
        searchRow.add(btnClear);

        histTop.add(histTitle, BorderLayout.WEST);
        histTop.add(searchRow, BorderLayout.EAST);
        historyPanel.add(histTop,                   BorderLayout.NORTH);
        historyPanel.add(UIHelper.scrollableTable(table),  BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formCard, historyPanel);
        split.setDividerLocation(420);
        split.setDividerSize(6);
        split.setResizeWeight(0);
        split.setBackground(Color.WHITE);

        add(top,   BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(new Color(40, 55, 80));
        return l;
    }

    private void populatePartDropdown() {
        try {
            partIds.clear();
            cbPart.removeAllItems();
            ResultSet rs = InventoryDAO.getPartsForCombo();
            while (rs.next()) {
                String lbl = rs.getString("label");
                partIds.put(lbl, rs.getInt("part_id"));
                cbPart.addItem(lbl);
            }
            rs.close();
            refreshStockIndicator();
        } catch (SQLException e) {
            UIHelper.error(this, "Failed to load parts: " + e.getMessage());
        }
    }

    // Green = ok, orange = low, red = empty
    private void refreshStockIndicator() {
        String sel = (String) cbPart.getSelectedItem();
        if (sel == null) { lblStock.setText("Available stock: -"); return; }
        int partId = partIds.getOrDefault(sel, -1);
        if (partId < 0) return;
        int stock = InventoryDAO.getStockForPart(partId);
        lblStock.setText("Available stock: " + stock + " units");
        if      (stock == 0)  lblStock.setForeground(new Color(180, 40, 40));
        else if (stock < 10)  lblStock.setForeground(new Color(190, 100, 0));
        else                  lblStock.setForeground(new Color(30, 110, 40));
    }

    public void refresh() {
        try {
            ResultSet rs = InventoryDAO.getUsageSummary();
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            String[] colNames = new String[cols];
            for (int i = 1; i <= cols; i++)
                colNames[i - 1] = meta.getColumnLabel(i).replace('_', ' ');
            DefaultTableModel model = new DefaultTableModel(colNames, 0) {
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
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
        } catch (SQLException e) {
            UIHelper.error(this, e.getMessage());
        }
        refreshStockIndicator();
    }

    private void applyHistoryFilter(String text) {
        if (sorter == null) return;
        sorter.setRowFilter(text.trim().isEmpty()
            ? null : RowFilter.regexFilter("(?i)" + text.trim()));
    }

    private void submitUsageEntry() {
        String sel = (String) cbPart.getSelectedItem();
        if (sel == null) { UIHelper.error(this, "Please select a part."); return; }
        int    partId = partIds.getOrDefault(sel, -1);
        int    qty    = (int) spinQty.getValue();
        String reason = fReason.getText().trim();
        if (reason.isEmpty()) reason = "Factory floor usage";

        int available = InventoryDAO.getStockForPart(partId);
        if (qty > available) {
            UIHelper.error(this, "Insufficient stock.\nRequested: " + qty + "   Available: " + available);
            return;
        }
        if (InventoryDAO.recordUsage(partId, user.getUserId(), qty, reason)) {
            UIHelper.info(this, "Usage recorded. Stock reduced by " + qty + " units.\n"
                + "(Trigger trg_deduct_usage_stock fired automatically)");
            spinQty.setValue(1);
            fReason.setText("");
            refresh();
        } else {
            UIHelper.error(this, "Failed to record usage. Check stock levels.");
        }
    }
}

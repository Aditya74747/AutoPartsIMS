package com.autoparts.gui;

import com.autoparts.db.InventoryDAO;
import com.autoparts.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;

//Read-only audit trail of all request status changes
//ARRIVED entries are auto-created by trigger trg_arrived_update_inventory
public class OrderLogPanel extends JPanel {

    private final JTable table = new JTable();
    private TableRowSorter<DefaultTableModel> sorter;

    public OrderLogPanel(User user) {
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
        JLabel title = new JLabel("Order Status Log");
        title.setFont(UIHelper.FONT_TITLE);
        JLabel sub = new JLabel("  Complete audit trail of all status changes");
        sub.setFont(UIHelper.FONT_NORMAL);
        sub.setForeground(Color.GRAY);
        titleArea.add(title);
        titleArea.add(sub);
        top.add(titleArea, BorderLayout.WEST);

        JButton btnRefresh = UIHelper.styledBtn("Refresh", UIHelper.BTN_BLUE);
        btnRefresh.addActionListener(e -> refresh());
        top.add(btnRefresh, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(top,                                          BorderLayout.NORTH);
        header.add(InventoryPanel.filterBar(table, () -> sorter), BorderLayout.SOUTH);

        JLabel note = new JLabel("ARRIVED entries are created automatically by trigger trg_arrived_update_inventory.");
        note.setFont(UIHelper.FONT_SMALL);
        note.setForeground(Color.GRAY);
        note.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        add(header,                    BorderLayout.NORTH);
        add(UIHelper.scrollableTable(table), BorderLayout.CENTER);
        add(note,                      BorderLayout.SOUTH);
    }

    public void refresh() {
        try { sorter = InventoryPanel.tableFromRS(table, InventoryDAO.getOrderLog()); }
        catch (SQLException e) { UIHelper.error(this, e.getMessage()); }
    }
}

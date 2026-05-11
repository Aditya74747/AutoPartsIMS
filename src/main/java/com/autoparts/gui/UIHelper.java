package com.autoparts.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

//Shared utilities - buttons, labels, table styling, dialogs
public class UIHelper {

    // Colours
    public static final Color HEADER_BG  = new Color(31, 78, 121);
    public static final Color BTN_BLUE   = new Color(52, 110, 170);
    public static final Color BTN_GREEN  = new Color(45, 130, 55);
    public static final Color BTN_RED    = new Color(175, 50, 50);
    public static final Color BTN_ORANGE = new Color(190, 100, 0);

    // Table colours
    static final Color TBL_HDR_BG   = new Color(31, 78, 121);
    static final Color TBL_HDR_FG   = Color.WHITE;
    static final Color TBL_ROW_ODD  = Color.WHITE;
    static final Color TBL_ROW_EVEN = new Color(237, 244, 251);
    static final Color TBL_SEL_BG   = new Color(173, 214, 252);
    static final Color TBL_SEL_FG   = new Color(20, 20, 60);
    static final Color TBL_FG       = new Color(30, 30, 30);
    static final Color TBL_GRID     = new Color(195, 205, 215);
    static final Color TBL_BORDER   = new Color(150, 170, 190);

    // Fonts
    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD,  16);
    public static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD   = new Font("SansSerif", Font.BOLD,  13);
    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);

    public static JButton styledBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        return btn;
    }

    public static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(FONT_NORMAL);
        f.setForeground(new Color(20, 20, 20));
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 165, 185), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return f;
    }

    public static JPasswordField passwordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(FONT_NORMAL);
        f.setForeground(new Color(20, 20, 20));
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 165, 185), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return f;
    }

    // Blue border when focused, grey when not - called on login fields
    public static void attachFocusHighlight(JComponent field) {
        Border normal  = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 165, 185), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8));
        Border focused = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 110, 170), 2),
            BorderFactory.createEmptyBorder(4, 7, 4, 7));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { field.setBorder(focused); }
            @Override public void focusLost(java.awt.event.FocusEvent e)   { field.setBorder(normal);  }
        });
    }

    public static JTextArea textArea(int rows, int cols) {
        JTextArea a = new JTextArea(rows, cols);
        a.setFont(FONT_NORMAL);
        a.setForeground(new Color(20, 20, 20));
        a.setBackground(Color.WHITE);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 165, 185), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return a;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(new Color(30, 30, 30));
        return l;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(new Color(20, 20, 60));
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return l;
    }

    // Wraps a table in a scroll pane and applies styling
    public static JScrollPane scrollableTable(JTable table) {
        applyTableTheme(table);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(TBL_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // Custom header renderer so the OS theme cannot override our colours.
    public static void applyTableTheme(JTable table) {
        table.setFont(FONT_NORMAL);
        table.setForeground(TBL_FG);
        table.setRowHeight(26);
        table.setGridColor(TBL_GRID);
        table.setSelectionBackground(TBL_SEL_BG);
        table.setSelectionForeground(TBL_SEL_FG);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = new JLabel(val == null ? "" : val.toString().toUpperCase());
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                lbl.setForeground(TBL_HDR_FG);
                lbl.setBackground(TBL_HDR_BG);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 1, TBL_HDR_BG.brighter()),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                return lbl;
            }
        });
        table.getTableHeader().setBackground(TBL_HDR_BG);
        table.getTableHeader().setForeground(TBL_HDR_FG);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(
            new Dimension(table.getTableHeader().getWidth(), 34));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? TBL_ROW_ODD : TBL_ROW_EVEN);
                    c.setForeground(TBL_FG);
                }
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                ((JLabel) c).setFont(FONT_NORMAL);
                return c;
            }
        });
    }

    // Loads a ResultSet into a table and applies styling
    public static void populateFromRS(JTable table, ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
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
            table.setModel(model);
            applyTableTheme(table);
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading data: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static JButton actionButton(String t, Color c)    { return styledBtn(t, c); }
    public static void success(Component p, String m)        { info(p, m); }
    public static void populateTable(JTable t, ResultSet rs) { populateFromRS(t, rs); }
    public static JScrollPane scrollTable(JTable t)          { return scrollableTable(t); }
}

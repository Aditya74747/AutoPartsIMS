package com.autoparts.db;
import java.sql.*;

//All database operations for the AutoParts IMS
public class InventoryDAO {

    // --- Inventory ---

    public static ResultSet getInventorySummary() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.prepareCall("{CALL sp_inventory_summary()}").executeQuery();
    }

    public static ResultSet getLowStockReport() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.prepareCall("{CALL sp_low_stock_report()}").executeQuery();
    }

    // Direct quantity update, wrapped in a transaction
    public static boolean updateStock(int partId, int qty) {
        String sql = "UPDATE inventory SET quantity = ? WHERE part_id = ?";
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, qty);
            ps.setInt(2, partId);
            ps.executeUpdate();
            ps.close();
            conn.commit();
            return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    // --- Parts ---

    public static ResultSet getAllParts() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT p.part_id, p.part_number, p.name, p.category, "
                   + "p.unit, p.unit_price, s.name AS supplier_name, p.description "
                   + "FROM parts p LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id "
                   + "ORDER BY p.category, p.name";
        return conn.createStatement().executeQuery(sql);
    }

    // Inserts part + creates matching inventory row, rolls back on failure
    public static int insertPart(String partNumber, String name, String category,
                                  String unit, double price, int suppId, String desc) {
        String sqlPart = "INSERT INTO parts (part_number, name, category, unit, unit_price, supplier_id, description) "
                       + "VALUES (?,?,?,?,?,?,?)";
        String sqlInv  = "INSERT INTO inventory (part_id, quantity, min_threshold) VALUES (?,0,10)";
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps1 = conn.prepareStatement(sqlPart, Statement.RETURN_GENERATED_KEYS);
            ps1.setString(1, partNumber); ps1.setString(2, name);  ps1.setString(3, category);
            ps1.setString(4, unit);       ps1.setDouble(5, price);
            if (suppId > 0) ps1.setInt(6, suppId); else ps1.setNull(6, Types.INTEGER);
            ps1.setString(7, desc);
            ps1.executeUpdate();
            ResultSet keys = ps1.getGeneratedKeys();
            if (!keys.next()) throw new SQLException("No generated key");
            int newId = keys.getInt(1);
            ps1.close();
            PreparedStatement ps2 = conn.prepareStatement(sqlInv);
            ps2.setInt(1, newId); ps2.executeUpdate(); ps2.close();
            conn.commit();
            return newId;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return -1;
        } finally {
            closeConn(conn);
        }
    }

    public static boolean updatePart(int partId, String name, String category,
                                      double price, String desc) {
        String sql = "UPDATE parts SET name=?, category=?, unit_price=?, description=? WHERE part_id=?";
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name); ps.setString(2, category);
            ps.setDouble(3, price); ps.setString(4, desc); ps.setInt(5, partId);
            ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    public static boolean deletePart(int partId) {
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("DELETE FROM parts WHERE part_id = ?");
            ps.setInt(1, partId); ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    // --- Suppliers ---

    public static ResultSet getAllSuppliers() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.createStatement().executeQuery("SELECT * FROM suppliers ORDER BY name");
    }

    public static boolean insertSupplier(String name, String contact, String phone,
                                          String email, String address) {
        String sql = "INSERT INTO suppliers (name, contact_name, phone, email, address) VALUES (?,?,?,?,?)";
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name); ps.setString(2, contact); ps.setString(3, phone);
            ps.setString(4, email); ps.setString(5, address);
            ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    public static boolean updateSupplier(int suppId, String name, String contact,
                                          String phone, String email, String address) {
        String sql = "UPDATE suppliers SET name=?, contact_name=?, phone=?, email=?, address=? WHERE supplier_id=?";
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name); ps.setString(2, contact); ps.setString(3, phone);
            ps.setString(4, email); ps.setString(5, address); ps.setInt(6, suppId);
            ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    // --- Requests ---

    // Active = not yet arrived or cancelled
    public static ResultSet getActiveRequests() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT r.request_id, p.part_number, p.name AS part_name, "
                   + "r.quantity, r.status, r.request_date, r.expected_arrival, "
                   + "u.full_name AS requested_by, r.notes "
                   + "FROM part_requests r "
                   + "JOIN parts p ON p.part_id = r.part_id "
                   + "JOIN users u ON u.user_id = r.requested_by "
                   + "WHERE r.status NOT IN ('ARRIVED','CANCELLED') "
                   + "ORDER BY r.request_date DESC";
        return conn.createStatement().executeQuery(sql);
    }

    public static ResultSet getAllRequests() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT r.request_id, p.part_number, p.name AS part_name, "
                   + "r.quantity, r.status, r.request_date, r.expected_arrival, "
                   + "u.full_name AS requested_by, r.notes "
                   + "FROM part_requests r "
                   + "JOIN parts p ON p.part_id = r.part_id "
                   + "JOIN users u ON u.user_id = r.requested_by "
                   + "ORDER BY r.request_date DESC";
        return conn.createStatement().executeQuery(sql);
    }

    public static ResultSet getRequestSummary() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.prepareCall("{CALL sp_request_summary_by_status()}").executeQuery();
    }

    public static boolean createRequest(int partId, int userId, int qty,
                                         String notes, java.sql.Date arrival) {
        String sql = "INSERT INTO part_requests (part_id, requested_by, quantity, notes, expected_arrival) "
                   + "VALUES (?,?,?,?,?)";
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, partId); ps.setInt(2, userId); ps.setInt(3, qty);
            ps.setString(4, notes); ps.setDate(5, arrival);
            ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    // ARRIVED status goes through a stored procedure that also updates inventory
    public static boolean updateRequestStatus(int requestId, String newStatus, int changedBy) {
        if ("ARRIVED".equals(newStatus)) {
            try (Connection conn = DBConnection.openConnection();
                 CallableStatement cs = conn.prepareCall("{CALL sp_mark_arrived(?, ?)}")) {
                cs.setInt(1, requestId); cs.setInt(2, changedBy);
                cs.execute(); return true;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }
        // For other statuses, log first then update
        logStatusChange(requestId, newStatus, changedBy);
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE part_requests SET status = ? WHERE request_id = ?");
            ps.setString(1, newStatus); ps.setInt(2, requestId);
            ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    private static void logStatusChange(int requestId, String newStatus, int changedBy) {
        try (Connection conn = DBConnection.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT status FROM part_requests WHERE request_id = ?")) {
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            String oldStatus = rs.getString("status");
            try (Connection c2 = DBConnection.openConnection();
                 PreparedStatement ps2 = c2.prepareStatement(
                     "INSERT INTO order_log (request_id, old_status, new_status, changed_by) VALUES (?,?,?,?)")) {
                ps2.setInt(1, requestId); ps2.setString(2, oldStatus);
                ps2.setString(3, newStatus); ps2.setInt(4, changedBy);
                ps2.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Order log  ---

    public static ResultSet getOrderLog() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT l.log_id, l.request_id, p.name AS part_name, "
                   + "l.old_status, l.new_status, u.full_name AS changed_by, l.changed_at, l.remarks "
                   + "FROM order_log l "
                   + "JOIN part_requests r ON r.request_id = l.request_id "
                   + "JOIN parts p ON p.part_id = r.part_id "
                   + "LEFT JOIN users u ON u.user_id = l.changed_by "
                   + "ORDER BY l.changed_at DESC";
        return conn.createStatement().executeQuery(sql);
    }

    // Stock value grouped by supplier - uses a three-table join
    public static ResultSet getStockValueBySupplier() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT s.name AS supplier_name, COUNT(p.part_id) AS num_parts, "
                   + "SUM(i.quantity) AS total_units, "
                   + "SUM(i.quantity * p.unit_price) AS total_stock_value_eur "
                   + "FROM parts p "
                   + "JOIN inventory i ON i.part_id = p.part_id "
                   + "LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id "
                   + "GROUP BY s.supplier_id, s.name "
                   + "ORDER BY total_stock_value_eur DESC";
        return conn.createStatement().executeQuery(sql);
    }

    // --- Combo helpers ---

    public static ResultSet getPartsForCombo() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.createStatement().executeQuery(
            "SELECT part_id, CONCAT(part_number,' - ',name) AS label FROM parts ORDER BY name");
    }

    public static ResultSet getSuppliersForCombo() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.createStatement().executeQuery(
            "SELECT supplier_id, name FROM suppliers WHERE active=1 ORDER BY name");
    }

    // --- Usage ---

    public static ResultSet getUsageSummary() throws SQLException {
        Connection conn = DBConnection.openConnection();
        return conn.prepareCall("{CALL sp_usage_summary()}").executeQuery();
    }

    // Checks stock before inserting; trigger handles the actual deduction
    public static boolean recordUsage(int partId, int userId, int qty, String reason) {
        Connection conn = null;
        try {
            conn = DBConnection.openConnection();
            conn.setAutoCommit(false);
            PreparedStatement chk = conn.prepareStatement(
                "SELECT quantity FROM inventory WHERE part_id = ?");
            chk.setInt(1, partId);
            ResultSet rs = chk.executeQuery();
            if (rs.next() && rs.getInt("quantity") < qty) {
                conn.rollback(); return false;
            }
            chk.close();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO usage_records (part_id, used_by, quantity, reason) VALUES (?,?,?,?)");
            ps.setInt(1, partId); ps.setInt(2, userId);
            ps.setInt(3, qty);    ps.setString(4, reason);
            ps.executeUpdate(); ps.close();
            conn.commit(); return true;
        } catch (SQLException e) {
            safeRollback(conn); e.printStackTrace(); return false;
        } finally {
            closeConn(conn);
        }
    }

    public static int getStockForPart(int partId) {
        try (Connection conn = DBConnection.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT quantity FROM inventory WHERE part_id = ?")) {
            ps.setInt(1, partId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("quantity");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // --- Dashboard ---

    public static ResultSet getDashboardStats() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql =
            "SELECT "
          + "(SELECT COUNT(*) FROM parts) AS total_parts, "
          + "(SELECT COALESCE(SUM(quantity),0) FROM inventory) AS total_units, "
          + "(SELECT COALESCE(SUM(i.quantity * p.unit_price),0) "
          + " FROM inventory i JOIN parts p ON p.part_id=i.part_id) AS stock_value_eur, "
          + "(SELECT COUNT(*) FROM inventory WHERE quantity <= min_threshold) AS low_stock_count, "
          + "(SELECT COUNT(*) FROM part_requests WHERE status IN ('PENDING','ORDERED','SHIPPED')) AS open_requests, "
          + "(SELECT COUNT(*) FROM usage_records WHERE DATE(usage_date) = CURDATE()) AS used_today, "
          + "(SELECT COUNT(*) FROM suppliers WHERE active=1) AS active_suppliers";
        return conn.createStatement().executeQuery(sql);
    }

    // Top 5 most consumed parts
    public static ResultSet getTopUsedParts() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT p.name AS part_name, p.category, "
                   + "SUM(u.quantity) AS total_used, i.quantity AS current_stock "
                   + "FROM usage_records u "
                   + "JOIN parts p ON p.part_id = u.part_id "
                   + "JOIN inventory i ON i.part_id = u.part_id "
                   + "GROUP BY p.part_id, p.name, p.category, i.quantity "
                   + "ORDER BY total_used DESC LIMIT 5";
        return conn.createStatement().executeQuery(sql);
    }

    // Last 8 status changes for the dashboard activity feed
    public static ResultSet getRecentActivity() throws SQLException {
        Connection conn = DBConnection.openConnection();
        String sql = "SELECT l.changed_at, p.name AS part_name, "
                   + "l.old_status, l.new_status, COALESCE(u.full_name,'System') AS changed_by "
                   + "FROM order_log l "
                   + "JOIN part_requests r ON r.request_id = l.request_id "
                   + "JOIN parts p ON p.part_id = r.part_id "
                   + "LEFT JOIN users u ON u.user_id = l.changed_by "
                   + "ORDER BY l.changed_at DESC LIMIT 8";
        return conn.createStatement().executeQuery(sql);
    }

    // --- Shared helpers ---

    private static void safeRollback(Connection conn) {
        if (conn != null) try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void closeConn(Connection conn) {
        if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}

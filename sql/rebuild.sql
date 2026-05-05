DROP DATABASE IF EXISTS autoparts_ims;
CREATE DATABASE autoparts_ims CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE autoparts_ims;

-- Users Table for Manager and Employee
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('MANAGER','EMPLOYEE') NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Suppliers Table
CREATE TABLE suppliers (
    supplier_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    contact_name VARCHAR(100),
    phone        VARCHAR(30),
    email        VARCHAR(100),
    address      VARCHAR(255),
    country      VARCHAR(50) DEFAULT 'Germany',
    active       BOOLEAN DEFAULT TRUE
);

-- Parts Table
CREATE TABLE parts (
    part_id     INT AUTO_INCREMENT PRIMARY KEY,
    part_number VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    category    ENUM('ENGINE','TRANSMISSION','BRAKES','SUSPENSION',
                     'ELECTRICAL','BODY','WHEELS','INTERIOR','OTHER') NOT NULL,
    unit        VARCHAR(20)    DEFAULT 'pcs',
    description TEXT,
    supplier_id INT,
    unit_price  DECIMAL(10,2),
    -- Referential integrity: part cannot reference a deleted supplier
    CONSTRAINT fk_parts_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- Inventory Table (For stock determination)
CREATE TABLE inventory (
    inventory_id  INT AUTO_INCREMENT PRIMARY KEY,
    part_id       INT NOT NULL UNIQUE,
    quantity      INT NOT NULL DEFAULT 0,
    min_threshold INT NOT NULL DEFAULT 10,
    location      VARCHAR(50),
    last_updated  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                  ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_part
        FOREIGN KEY (part_id) REFERENCES parts(part_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- part_requests Table
CREATE TABLE part_requests (
    request_id       INT AUTO_INCREMENT PRIMARY KEY,
    part_id          INT NOT NULL,
    requested_by     INT NOT NULL,
    quantity         INT NOT NULL,
    status           ENUM('PENDING','ORDERED','SHIPPED','ARRIVED','CANCELLED')
                     NOT NULL DEFAULT 'PENDING',
    request_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_arrival DATE,
    notes            TEXT,
    CONSTRAINT fk_request_part
        FOREIGN KEY (part_id)      REFERENCES parts(part_id)  ON UPDATE CASCADE,
    CONSTRAINT fk_request_user
        FOREIGN KEY (requested_by) REFERENCES users(user_id)  ON UPDATE CASCADE
);

-- order_log Table 
CREATE TABLE order_log (
    log_id     INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by INT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks    TEXT,
    -- Referential integrity: log row must point to a valid request
    CONSTRAINT fk_log_request
        FOREIGN KEY (request_id) REFERENCES part_requests(request_id) ON UPDATE CASCADE,
    CONSTRAINT fk_log_user
        FOREIGN KEY (changed_by) REFERENCES users(user_id) ON UPDATE CASCADE
);

-- Triggers
DELIMITER //

-- Trigger 1: trg_arrived_update_inventory After part_request is updated to Arrived
CREATE TRIGGER trg_arrived_update_inventory
AFTER UPDATE ON part_requests
FOR EACH ROW
BEGIN
    IF NEW.status = 'ARRIVED' AND OLD.status <> 'ARRIVED' THEN
        UPDATE inventory
        SET quantity = quantity + NEW.quantity
        WHERE part_id = NEW.part_id;

        INSERT INTO order_log(request_id, old_status, new_status, remarks)
        VALUES (NEW.request_id, OLD.status, NEW.status,
                CONCAT('Trigger: auto inventory update +', NEW.quantity, ' units'));
    END IF;
END//

-- Trigger 2: trg_log_status_change after status change on the part_request
CREATE TRIGGER trg_log_status_change
AFTER UPDATE ON part_requests
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status AND NEW.status <> 'ARRIVED' THEN
        INSERT INTO order_log(request_id, old_status, new_status, remarks)
        VALUES (NEW.request_id, OLD.status, NEW.status, 'Status updated');
    END IF;
END//

-- TRIGGER 3: trg_prevent_negative_stock fires before any update on Inventory
CREATE TRIGGER trg_prevent_negative_stock
BEFORE UPDATE ON inventory
FOR EACH ROW
BEGIN
    IF NEW.quantity < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Stock quantity cannot be negative';
    END IF;
END//

DELIMITER ;

-- Stored Procedures
DELIMITER //

-- Procedure 1: sp_inventory_summary gives the full inventory overview by joining the inventory, parts and suppliers tables.
CREATE PROCEDURE sp_inventory_summary()
BEGIN
    SELECT p.part_id,
           p.part_number,
           p.name            AS part_name,
           p.category,
           i.quantity,
           i.min_threshold,
           i.location,
           p.unit_price,
           s.name            AS supplier_name,
           i.last_updated
    FROM   inventory i
    JOIN   parts     p ON p.part_id     = i.part_id
    LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id
    ORDER  BY p.category, p.name;
END//

-- Procedure 2: sp_low_stock_report returns all parts where the stock is below the threshold
CREATE PROCEDURE sp_low_stock_report()
BEGIN
    SELECT p.part_number,
           p.name        AS part_name,
           p.category,
           i.quantity    AS current_stock,
           i.min_threshold,
           s.name        AS supplier,
           s.phone       AS supplier_phone
    FROM   inventory i
    JOIN   parts     p ON p.part_id    = i.part_id
    LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id
    WHERE  i.quantity <= i.min_threshold
    ORDER  BY i.quantity ASC;
END//

-- Procedure 3: sp_mark_arrived gives the Arrived after the current status update
CREATE PROCEDURE sp_mark_arrived(IN p_request_id INT, IN p_user_id INT)
BEGIN
    DECLARE v_status VARCHAR(20);

    SELECT status INTO v_status
    FROM part_requests
    WHERE request_id = p_request_id;

    IF v_status = 'SHIPPED' OR v_status = 'ORDERED' THEN
        UPDATE part_requests
        SET status = 'ARRIVED'
        WHERE request_id = p_request_id;

        UPDATE order_log
        SET changed_by = p_user_id
        WHERE request_id = p_request_id
        ORDER BY log_id DESC
        LIMIT 1;
    ELSE
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Request must be ORDERED or SHIPPED before marking ARRIVED';
    END IF;
END//

-- Procedure 4: sp_request_summary_by_status returns the count of requests grouped by status
CREATE PROCEDURE sp_request_summary_by_status()
BEGIN
    SELECT   r.status,
             COUNT(*)           AS total_requests,
             SUM(r.quantity)    AS total_units,
             MIN(r.request_date) AS earliest_request,
             MAX(r.request_date) AS latest_request
    FROM     part_requests r
    GROUP BY r.status
    ORDER BY FIELD(r.status,'PENDING','ORDERED','SHIPPED','ARRIVED','CANCELLED');
END//

DELIMITER ;

-- User data:
-- Passwords are in SHA-256 hashes matching Java AuthService:
--   manager1 / Pass1234    manager2 / Pass5678
--   emp1 / Emp1234         emp2 / Emp5678
--   emp3 / Emp9999

INSERT INTO users (username, password, role, full_name, email) VALUES
('manager1','d3dec3f35387156495cbc21471313f87155f878f3435b693f50077c2be479033','MANAGER', 'Klaus Weber',   'k.weber@mercedes-benz.com'),
('manager2','2fbc899086ef7067406de3304cd8637196bc948a832ab5fc1075cb54887f3191','MANAGER', 'Sabine Mueller','s.mueller@porsche.com'),
('emp1',    '8fdb6a96c8fc7b2c5556ed9b19a9fd0249825bdd134074e8d74ce45b48677c8f','EMPLOYEE','Ron Ozturk',    'r.ozturk@mercedes-benz.com'),
('emp2',    '6c8e6b4f82d92a8d523402aa00fd1cd7ba37bddf28089d7eed99f926e6552eba','EMPLOYEE','Rachel Gunther',  'r.gunther@porsche.com'),
('emp3',    '7206686340be7d37a3b6e0e304e05788efa8064fdba686947cb98defd31e87c7','EMPLOYEE','Marco Bianchi', 'm.bianchi@mercedes-benz.com');

-- Initial data for the tables:
INSERT INTO suppliers (name, contact_name, phone, email, address, country) VALUES
('Bosch GmbH',         'Hans Fischer', '+49 711 811 0','supply@bosch.de',      'Robert-Bosch-Platz 1, Gerlingen',      'Germany'),
('Continental AG',     'Maria Schmidt','+49 511 938 0','orders@continental.de','Vahrenwalder Str. 9, Hannover',        'Germany'),
('ZF Friedrichshafen', 'Peter Braun',  '+49 7541 77 0','supply@zf.com',        'Lowentalerstrasse 20, Friedrichshafen','Germany'),
('Schaeffler AG',      'Ingrid Koch',  '+49 9132 82 0','info@schaeffler.com',  'Industriestr. 1-3, Herzogenaurach',    'Germany'),
('Mahle GmbH',         'Thomas Wolf',  '+49 711 501 0','supply@mahle.com',     'Pragstrasse 26-46, Stuttgart',         'Germany');

INSERT INTO parts (part_number, name, category, unit, description, supplier_id, unit_price) VALUES
('WHL-001','Front Wheel Rim 18"',     'WHEELS',       'pcs','Alloy rim for C-Class / Cayenne',  1, 189.99),
('WHL-002','Rear Wheel Rim 18"',      'WHEELS',       'pcs','Alloy rim for C-Class / Cayenne',  1, 189.99),
('WHL-003','Run-flat Tyre 245/45R18', 'WHEELS',       'pcs','Continental sport tyre',           2, 224.50),
('BRK-001','Front Brake Disc',        'BRAKES',       'pcs','Ventilated disc 330mm dia',        1,  85.00),
('BRK-002','Rear Brake Disc',         'BRAKES',       'pcs','Solid disc 300mm dia',             1,  72.00),
('BRK-003','Brake Pad Set Front',     'BRAKES',       'set','Ceramic compound, per axle',       1,  45.00),
('ENG-001','Engine Oil Filter',       'ENGINE',       'pcs','OEM spec for M274 engine',         5,  12.50),
('ENG-002','Timing Chain Kit',        'ENGINE',       'kit','Chain, tensioner, guides',         4, 320.00),
('ENG-003','Fuel Injector',           'ENGINE',       'pcs','High-pressure direct injection',   1, 145.00),
('TRN-001','Automatic Gearbox Filter','TRANSMISSION', 'pcs','7G-Tronic filter kit with gasket', 3,  68.00),
('TRN-002','Drive Shaft Left',        'TRANSMISSION', 'pcs','CV joint assembly, complete',      3, 210.00),
('SUS-001','Front Shock Absorber',    'SUSPENSION',   'pcs','Gas-pressure monotube',            2, 135.00),
('SUS-002','Rear Coil Spring',        'SUSPENSION',   'pcs','Progressive rate spring',          4,  89.00),
('ELC-001','Alternator 180A',         'ELECTRICAL',   'pcs','Bosch reman for M274/M276',        1, 260.00),
('ELC-002','Starter Motor',           'ELECTRICAL',   'pcs','1.7 kW Bosch reman',              1, 195.00);

INSERT INTO inventory (part_id, quantity, min_threshold, location) VALUES
(1,45,10,'A1-01'),(2,38,10,'A1-02'),(3,60,15,'A1-03'),
(4,30,10,'B2-01'),(5,28,10,'B2-02'),(6,50,20,'B2-03'),
(7,120,30,'C3-01'),(8,8,10,'C3-02'),(9,15,10,'C3-03'),
(10,22,10,'D4-01'),(11,17,10,'D4-02'),(12,25,10,'E5-01'),
(13,33,10,'E5-02'),(14,9,10,'F6-01'),(15,11,10,'F6-02');

INSERT INTO part_requests (part_id, requested_by, quantity, status, expected_arrival, notes) VALUES
(8, 3,20,'SHIPPED', DATE_ADD(CURDATE(),INTERVAL  3 DAY),'Urgent - low stock'),
(14,4,10,'ORDERED', DATE_ADD(CURDATE(),INTERVAL  7 DAY),'Routine reorder'),
(15,5,15,'PENDING', DATE_ADD(CURDATE(),INTERVAL 10 DAY),'Q2 buffer stock'),
(1, 3, 5,'ARRIVED', DATE_ADD(CURDATE(),INTERVAL -2 DAY),'Delivered as expected'),
(6, 4,30,'ORDERED', DATE_ADD(CURDATE(),INTERVAL  5 DAY),NULL);

-- Confirm seed data loaded correctly
SELECT CONCAT('Users: ',     COUNT(*)) AS check_status FROM users
UNION ALL SELECT CONCAT('Suppliers: ', COUNT(*)) FROM suppliers
UNION ALL SELECT CONCAT('Parts: ',     COUNT(*)) FROM parts
UNION ALL SELECT CONCAT('Inventory: ', COUNT(*)) FROM inventory
UNION ALL SELECT CONCAT('Requests: ',  COUNT(*)) FROM part_requests;

-- usage_records Table
CREATE TABLE usage_records (
    usage_id    INT AUTO_INCREMENT PRIMARY KEY,
    part_id     INT NOT NULL,
    used_by     INT NOT NULL,              -- FK -> users.user_id
    quantity    INT NOT NULL,
    usage_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason      VARCHAR(200),
    CONSTRAINT fk_usage_part
        FOREIGN KEY (part_id) REFERENCES parts(part_id) ON UPDATE CASCADE,
    CONSTRAINT fk_usage_user
        FOREIGN KEY (used_by) REFERENCES users(user_id) ON UPDATE CASCADE
);

-- Trigger 4: trg_deduct_usage_stock fires after insert on the usage records
DELIMITER //
CREATE TRIGGER trg_deduct_usage_stock
AFTER INSERT ON usage_records
FOR EACH ROW
BEGIN
    UPDATE inventory
    SET quantity = quantity - NEW.quantity
    WHERE part_id = NEW.part_id;
END//
DELIMITER ;

-- Procedure 5: sp_usage_summary: Returns usage history joined with part name and user name.
DELIMITER //
CREATE PROCEDURE sp_usage_summary()
BEGIN
    SELECT u.usage_id,
           p.part_number,
           p.name         AS part_name,
           u.quantity,
           usr.full_name  AS used_by,
           u.usage_date,
           u.reason
    FROM   usage_records u
    JOIN   parts p   ON p.part_id   = u.part_id
    JOIN   users usr ON usr.user_id = u.used_by
    ORDER  BY u.usage_date DESC;
END//
DELIMITER ;

SELECT CONCAT('Tables including usage_records: OK') AS status;


SET FOREIGN_KEY_CHECKS = 0;

-- Clear any previous data inserted
DELETE FROM usage_records;
DELETE FROM order_log;
DELETE FROM part_requests;
DELETE FROM inventory;
DELETE FROM parts;
DELETE FROM suppliers;

-- Reset auto-increment counters
ALTER TABLE suppliers     AUTO_INCREMENT = 1;
ALTER TABLE parts         AUTO_INCREMENT = 1;
ALTER TABLE inventory     AUTO_INCREMENT = 1;
ALTER TABLE part_requests AUTO_INCREMENT = 1;
ALTER TABLE order_log     AUTO_INCREMENT = 1;
ALTER TABLE usage_records AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- Suppliers
INSERT INTO suppliers (name, contact_name, phone, email, address, country, active) VALUES
('Bosch GmbH',              'Hans Fischer',   '+49 711 811 0',     'supply@bosch.de',           'Robert-Bosch-Platz 1, 70839 Gerlingen',      'Germany',     1),
('Continental AG',           'Maria Schmidt',  '+49 511 938 0',     'orders@continental.de',     'Vahrenwalder Str. 9, 30165 Hannover',         'Germany',     1),
('ZF Friedrichshafen AG',   'Peter Braun',    '+49 7541 77 0',     'supply@zf.com',              'Lowentalerstrasse 20, 88046 Friedrichshafen', 'Germany',     1),
('Schaeffler AG',            'Ingrid Koch',    '+49 9132 82 0',     'procurement@schaeffler.com','Industriestrasse 1, 91074 Herzogenaurach',    'Germany',     1),
('Mahle GmbH',               'Thomas Wolf',    '+49 711 501 0',     'supply@mahle.com',           'Pragstrasse 26, 70376 Stuttgart',             'Germany',     1),
('Hella GmbH',               'Klaus Bauer',    '+49 2941 38 0',     'orders@hella.com',           'Rixbecker Strasse 75, 59552 Lippstadt',       'Germany',     1),
('Valeo SA',                 'Sophie Dupont',  '+33 1 40 55 20 20', 'supply@valeo.com',           '43 Rue Bayen, 75017 Paris',                   'France',      1),
('Brembo S.p.A.',            'Marco Rossi',    '+39 035 605 3111',  'orders@brembo.com',          'Via Brembo 25, 24035 Curno BG',               'Italy',       1),
('Bilstein Group',           'Stefan Mueller', '+49 2333 791 0',    'supply@bilstein.de',         'Wilhelmstrasse 239, 58093 Hagen',             'Germany',     1),
('SKF Group',                'Lars Andersson', '+46 31 337 1000',   'orders@skf.com',             'Hornsgatan 1, 415 50 Gothenburg',             'Sweden',      1);

-- Parts
INSERT INTO parts (part_number, name, category, unit, description, supplier_id, unit_price) VALUES
('ENG-001', 'Engine Oil Filter',         'ENGINE',       'pcs', 'OEM spec for M274 / M276 engine family',         5,  12.50),
('TRN-001', 'Automatic Gearbox Filter',  'TRANSMISSION', 'pcs', '7G-Tronic filter kit with gasket and seals',     3,  68.00),
('BRK-001', 'Front Brake Disc 330mm',    'BRAKES',       'pcs', 'Vented disc, C-Class W205 / Cayenne 9YA',        8,  85.00),
('SUS-001', 'Front Shock Absorber',      'SUSPENSION',   'pcs', 'Bilstein B4 gas-pressure monotube',              9, 135.00),
('ELC-001', 'Alternator 180A',           'ELECTRICAL',   'pcs', 'Bosch reman, M274/M276, 14.3V regulated',        1, 260.00),
('WHL-001', 'Front Wheel Rim 18" Alloy', 'WHEELS',       'pcs', 'Alloy rim C-Class / Cayenne, 5x112 bolt pattern',1, 189.99),
('BDY-001', 'Front Bumper Cover',        'BODY',         'pcs', 'Primed, includes sensor holes, C-Class W205',    7, 285.00),
('INT-001', 'Cabin Air Filter',          'INTERIOR',     'pcs', 'Activated carbon, W205 / 9YA, 260x205mm',        5,  18.50),
('ENG-002', 'Timing Chain Kit',          'ENGINE',       'kit', 'Complete kit: chain, tensioner, guides, sprockets',4, 320.00),
('WHL-002', 'Run-flat Tyre 245/45R18',   'WHEELS',       'pcs', 'Continental SportContact 6, reinforced sidewall', 2, 224.50);

-- Inventory
INSERT INTO inventory (part_id, quantity, min_threshold, location) VALUES
(1,  120, 30, 'C3-01'),   -- Engine Oil Filter
(2,   22, 10, 'D4-01'),   -- Gearbox Filter
(3,   30, 12, 'B2-01'),   -- Front Brake Disc
(4,   25, 10, 'E5-01'),   -- Front Shock Absorber
(5,    9, 10, 'F6-01'),   -- Alternator
(6,   40, 12, 'A1-01'),   -- Front Wheel Rim
(7,   11,  5, 'G7-01'),   -- Front Bumper Cover
(8,   70, 20, 'H8-01'),   -- Cabin Air Filter
(9,    7, 10, 'C3-02'),   -- Timing Chain Kit
(10,  55, 20, 'A1-02');   -- Run-flat Tyre

-- Part Requests
INSERT INTO part_requests (part_id, requested_by, quantity, status, request_date, expected_arrival, notes) VALUES
(9,  3, 15, 'ARRIVED',   DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 22 DAY), 'Timing chain kits - low stock reorder'),
(5,  4, 20, 'SHIPPED',   DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL  3 DAY), 'Alternator batch - stock below threshold'),
(3,  3, 25, 'ORDERED',   DATE_SUB(NOW(), INTERVAL  5 DAY), DATE_ADD(CURDATE(), INTERVAL  7 DAY), 'Brake disc reorder for C-Class service run'),
(1,  5, 80, 'PENDING',   DATE_SUB(NOW(), INTERVAL  1 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Oil filter bulk order for monthly service'),
(10, 4,  8, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL  5 DAY), 'Cancelled - sourced from Continental directly');

-- Order Log
INSERT INTO order_log (request_id, old_status, new_status, changed_by, changed_at, remarks) VALUES
(1, 'PENDING', 'ORDERED', 1, DATE_SUB(NOW(), INTERVAL 29 DAY), 'Manager approved timing chain reorder'),
(1, 'ORDERED', 'SHIPPED', 1, DATE_SUB(NOW(), INTERVAL 26 DAY), 'Schaeffler confirmed dispatch'),
(2, 'PENDING', 'ORDERED', 1, DATE_SUB(NOW(), INTERVAL  9 DAY), 'Alternator batch approved'),
(2, 'ORDERED', 'SHIPPED', 2, DATE_SUB(NOW(), INTERVAL  6 DAY), 'Bosch Stuttgart depot confirmed dispatch'),
(3, 'PENDING', 'ORDERED', 1, DATE_SUB(NOW(), INTERVAL  4 DAY), 'Brake disc order placed with Brembo'),
(5, 'PENDING', 'CANCELLED', 2, DATE_SUB(NOW(), INTERVAL 19 DAY), 'Cancelled - Continental direct supply arranged');

-- Usage Records
INSERT INTO usage_records (part_id, used_by, quantity, usage_date, reason) VALUES
(1, 3,  6, DATE_SUB(NOW(), INTERVAL 25 DAY), 'Oil filter change - 6 vehicle service run'),
(1, 4,  8, DATE_SUB(NOW(), INTERVAL 15 DAY), 'Monthly oil service batch'),
(9, 5,  2, DATE_SUB(NOW(), INTERVAL 20 DAY), 'Timing chain kit replacement M274 engine'),
(3, 3,  4, DATE_SUB(NOW(), INTERVAL 18 DAY), 'Front disc replacement C-Class W205 batch'),
(3, 4,  2, DATE_SUB(NOW(), INTERVAL  7 DAY), 'Disc replacement warranty job'),
(4, 5,  2, DATE_SUB(NOW(), INTERVAL 12 DAY), 'Shock absorber replacement C220d'),
(5, 3,  1, DATE_SUB(NOW(), INTERVAL 10 DAY), 'Alternator replacement charging fault'),
(6, 4,  2, DATE_SUB(NOW(), INTERVAL  8 DAY), 'Rim replacement kerb damage Cayenne'),
(8, 5,  4, DATE_SUB(NOW(), INTERVAL  5 DAY), 'Cabin air filter replacement service A'),
(8, 3,  6, DATE_SUB(NOW(), INTERVAL  2 DAY), 'Cabin filter batch - 6 vehicles annual service'),
(1, 4,  3, NOW(),                             'Walk-in oil service x3 vehicles today');

-- verify row count
SELECT 'Suppliers'     AS table_name, COUNT(*) AS row_count FROM suppliers
UNION ALL SELECT 'Parts',         COUNT(*) FROM parts
UNION ALL SELECT 'Inventory',     COUNT(*) FROM inventory
UNION ALL SELECT 'Part Requests', COUNT(*) FROM part_requests
UNION ALL SELECT 'Order Log',     COUNT(*) FROM order_log
UNION ALL SELECT 'Usage Records', COUNT(*) FROM usage_records;


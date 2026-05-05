USE autoparts_ims;

-- Wipe and re-insert users with hardcoded hashes
DELETE FROM users;

INSERT INTO users (username, password, role, full_name, email) VALUES
('manager1','d3dec3f35387156495cbc21471313f87155f878f3435b693f50077c2be479033','MANAGER', 'Klaus Weber',   'k.weber@mercedes-benz.com'),
('manager2','2fbc899086ef7067406de3304cd8637196bc948a832ab5fc1075cb54887f3191','MANAGER', 'Sabine Mueller','s.mueller@porsche.com'),
('emp1',    '8fdb6a96c8fc7b2c5556ed9b19a9fd0249825bdd134074e8d74ce45b48677c8f','EMPLOYEE','Ron Ozturk',    'r.ozturk@mercedes-benz.com'),
('emp2',    '6c8e6b4f82d92a8d523402aa00fd1cd7ba37bddf28089d7eed99f926e6552eba','EMPLOYEE','Rachel Gunther',  'r.gunther@porsche.com'),
('emp3',    '7206686340be7d37a3b6e0e304e05788efa8064fdba686947cb98defd31e87c7','EMPLOYEE','Marco Bianchi', 'm.bianchi@mercedes-benz.com');

SELECT username, role, full_name FROM users;

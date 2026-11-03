-- Datos iniciales que la aplicación necesita para funcionar.

-- Roles (IDs 1: ADMIN, 2: MANAGER, 3: EMPLOYEE)
INSERT INTO roles (name, description)
VALUES
('ADMIN', 'View and edit data. Can edit user''s roles'),
('MANAGER', 'View and edit data. Can''t edit user''s roles'),
('EMPLOYEE', 'View data');

-- Condiciones de pago
INSERT INTO payment_conditions (name)
VALUES
('Contado'),
('Contraentrega'),
('Crédito a 8 días'),
('Crédito a 15 días'),
('Crédito a 30 días'),
('Crédito a 45 días'),
('Crédito a 60 días'),
('Anticipo del 50% y saldo contra entrega'),
('Pago con tarjeta (crédito o débito)'),
('Transferencia bancaria previa al despacho'),
('Pago mediante consignación o giro'),
('Pago recurrente o por suscripción'),
('Otra');

IF DB_ID('SchemaFlowSource') IS NULL CREATE DATABASE SchemaFlowSource;
GO
USE SchemaFlowSource;
GO

CREATE TABLE dbo.Customer (
    customer_id INT PRIMARY KEY,
    customer_name NVARCHAR(120) NOT NULL,
    segment NVARCHAR(30) NOT NULL,
    region NVARCHAR(60) NOT NULL,
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.SalesOrder (
    order_id INT PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES dbo.Customer(customer_id),
    order_status NVARCHAR(30) NOT NULL,
    ordered_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.SalesOrderItem (
    order_item_id INT PRIMARY KEY,
    order_id INT NOT NULL REFERENCES dbo.SalesOrder(order_id),
    sku NVARCHAR(40) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0)
);

INSERT INTO dbo.Customer (customer_id, customer_name, segment, region) VALUES
(1, 'Demo Atlas Labs', 'growth', 'Tunis'),
(2, 'Demo Northstar Data', 'enterprise', 'Sfax'),
(3, 'Demo Greenline Systems', 'startup', 'Sousse');

INSERT INTO dbo.SalesOrder (order_id, customer_id, order_status, ordered_at) VALUES
(101, 1, 'completed', '2026-01-10T09:00:00'),
(102, 1, 'processing', '2026-01-12T11:30:00'),
(103, 2, 'completed', '2026-01-14T14:15:00'),
(104, 3, 'completed', '2026-01-15T08:45:00');

INSERT INTO dbo.SalesOrderItem (order_item_id, order_id, sku, quantity, unit_price) VALUES
(1001, 101, 'SKU-1001', 2, 49.90),
(1002, 101, 'SKU-1002', 1, 129.00),
(1003, 102, 'SKU-1003', 3, 19.50),
(1004, 103, 'SKU-1001', 4, 49.90),
(1005, 104, 'SKU-1004', 1, 299.00);
GO

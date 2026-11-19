-- EcoNest Home & Garden Retail Management System
-- MySQL / MariaDB schema for the OOP group project.

CREATE DATABASE IF NOT EXISTS econest_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE econest_db;

CREATE TABLE IF NOT EXISTS products (
    product_id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_code        VARCHAR(30) NOT NULL,
    product_name        VARCHAR(120) NOT NULL,
    category            ENUM('PLANT', 'PLANTER', 'GARDENING_KIT') NOT NULL,
    description         TEXT NULL,
    unit_price          DECIMAL(10,2) NOT NULL,
    eco_rating          DECIMAL(4,2) NULL COMMENT 'Rating scale is set by the project; assignment does not specify it.',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    UNIQUE KEY uq_products_product_code (product_code),
    CONSTRAINT chk_products_unit_price CHECK (unit_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clients (
    client_id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    client_type         ENUM('HOUSEHOLD', 'OFFICE') NOT NULL,
    client_name         VARCHAR(150) NOT NULL,
    contact_person      VARCHAR(120) NULL,
    email               VARCHAR(254) NOT NULL,
    phone               VARCHAR(30) NOT NULL,
    address_line        VARCHAR(200) NOT NULL,
    city                VARCHAR(100) NOT NULL,
    postal_code         VARCHAR(20) NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (client_id),
    KEY ix_clients_email (email),
    KEY ix_clients_name (client_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    supplier_name       VARCHAR(150) NOT NULL,
    contact_person      VARCHAR(120) NULL,
    email               VARCHAR(254) NULL,
    phone               VARCHAR(30) NULL,
    address_line        VARCHAR(200) NULL,
    city                VARCHAR(100) NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (supplier_id),
    KEY ix_suppliers_name (supplier_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS delivery_agents (
    agent_id                    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name                   VARCHAR(150) NOT NULL,
    phone                       VARCHAR(30) NOT NULL,
    email                       VARCHAR(254) NOT NULL,
    vehicle_registration_no     VARCHAR(30) NOT NULL,
    vehicle_type                VARCHAR(60) NOT NULL,
    vehicle_model               VARCHAR(100) NOT NULL,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (agent_id),
    UNIQUE KEY uq_delivery_agents_vehicle_registration (vehicle_registration_no),
    KEY ix_delivery_agents_name (full_name),
    KEY ix_delivery_agents_active (is_active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory (
    product_id          INT UNSIGNED NOT NULL,
    quantity_on_hand    INT UNSIGNED NOT NULL DEFAULT 0,
    reorder_level       INT UNSIGNED NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES products (product_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- Backfill inventory rows if products were added before this schema script was re-run.
INSERT INTO inventory (product_id, quantity_on_hand, reorder_level)
SELECT p.product_id, 0, 0
FROM products AS p
LEFT JOIN inventory AS i ON i.product_id = p.product_id
WHERE i.product_id IS NULL;

-- A supplier can supply many products and a product can have several suppliers.
CREATE TABLE IF NOT EXISTS supplier_products (
    supplier_id         INT UNSIGNED NOT NULL,
    product_id          INT UNSIGNED NOT NULL,
    supplier_product_code VARCHAR(60) NULL,
    last_unit_cost      DECIMAL(10,2) NULL,
    PRIMARY KEY (supplier_id, product_id),
    CONSTRAINT fk_supplier_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_supplier_products_product
        FOREIGN KEY (product_id) REFERENCES products (product_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_supplier_products_cost CHECK (last_unit_cost IS NULL OR last_unit_cost >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_ins (
    stock_in_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    supplier_id         INT UNSIGNED NOT NULL,
    supplier_reference  VARCHAR(80) NULL,
    received_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes               VARCHAR(500) NULL,
    PRIMARY KEY (stock_in_id),
    UNIQUE KEY uq_stock_ins_supplier_reference (supplier_id, supplier_reference),
    KEY ix_stock_ins_received_at (received_at),
    CONSTRAINT fk_stock_ins_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_in_items (
    stock_in_id         BIGINT UNSIGNED NOT NULL,
    product_id          INT UNSIGNED NOT NULL,
    quantity_received   INT UNSIGNED NOT NULL,
    unit_cost           DECIMAL(10,2) NULL,
    PRIMARY KEY (stock_in_id, product_id),
    CONSTRAINT fk_stock_in_items_stock_in
        FOREIGN KEY (stock_in_id) REFERENCES stock_ins (stock_in_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_stock_in_items_product
        FOREIGN KEY (product_id) REFERENCES products (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_stock_in_items_quantity CHECK (quantity_received > 0),
    CONSTRAINT chk_stock_in_items_unit_cost CHECK (unit_cost IS NULL OR unit_cost >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    order_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    client_id           INT UNSIGNED NOT NULL,
    order_date          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_status        ENUM('PENDING', 'PROCESSING', 'DISPATCHED', 'DELIVERED', 'CANCELLED')
                        NOT NULL DEFAULT 'PENDING',
    dispatched_at       DATETIME NULL,
    notes               VARCHAR(500) NULL,
    PRIMARY KEY (order_id),
    KEY ix_orders_client_date (client_id, order_date),
    KEY ix_orders_status_date (order_status, order_date),
    CONSTRAINT fk_orders_client
        FOREIGN KEY (client_id) REFERENCES clients (client_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    order_id            BIGINT UNSIGNED NOT NULL,
    product_id          INT UNSIGNED NOT NULL,
    quantity            INT UNSIGNED NOT NULL,
    unit_price          DECIMAL(10,2) NOT NULL COMMENT 'Price copied from product at order time.',
    line_total          DECIMAL(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    PRIMARY KEY (order_id, product_id),
    KEY ix_order_items_product (product_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id            BIGINT UNSIGNED NOT NULL,
    agent_id            INT UNSIGNED NULL,
    delivery_address    VARCHAR(300) NOT NULL COMMENT 'Address snapshot for this order.',
    delivery_status     ENUM('ASSIGNED', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'CANCELLED')
                        NOT NULL DEFAULT 'ASSIGNED',
    assigned_at         DATETIME NULL,
    dispatched_at       DATETIME NULL,
    delivered_at        DATETIME NULL,
    delivery_notes      VARCHAR(500) NULL,
    PRIMARY KEY (delivery_id),
    UNIQUE KEY uq_deliveries_order (order_id),
    KEY ix_deliveries_agent_status (agent_id, delivery_status),
    CONSTRAINT fk_deliveries_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_deliveries_agent
        FOREIGN KEY (agent_id) REFERENCES delivery_agents (agent_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- Optional audit log for Tasks 8 and 9 email notifications.
CREATE TABLE IF NOT EXISTS email_notifications (
    notification_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_type  ENUM('ORDER_DISPATCHED', 'DELIVERY_ASSIGNED') NOT NULL,
    order_id            BIGINT UNSIGNED NULL,
    delivery_id         BIGINT UNSIGNED NULL,
    recipient_email     VARCHAR(254) NOT NULL,
    send_status         ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at             DATETIME NULL,
    error_message       VARCHAR(500) NULL,
    PRIMARY KEY (notification_id),
    KEY ix_email_notifications_status (send_status, created_at),
    CONSTRAINT fk_email_notifications_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_email_notifications_delivery
        FOREIGN KEY (delivery_id) REFERENCES deliveries (delivery_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_email_notification_target CHECK (
        (notification_type = 'ORDER_DISPATCHED' AND order_id IS NOT NULL AND delivery_id IS NULL)
        OR
        (notification_type = 'DELIVERY_ASSIGNED' AND delivery_id IS NOT NULL AND order_id IS NULL)
    )
) ENGINE=InnoDB;

-- Create a matching inventory row automatically for each new product.
DELIMITER $$
DROP TRIGGER IF EXISTS trg_products_create_inventory$$
CREATE TRIGGER trg_products_create_inventory
AFTER INSERT ON products
FOR EACH ROW
BEGIN
    INSERT INTO inventory (product_id, quantity_on_hand, reorder_level)
    VALUES (NEW.product_id, 0, 0);
END$$

-- Stock-in line changes update the product's quantity on hand.
DROP TRIGGER IF EXISTS trg_stock_in_items_after_insert$$
CREATE TRIGGER trg_stock_in_items_after_insert
AFTER INSERT ON stock_in_items
FOR EACH ROW
BEGIN
    UPDATE inventory
    SET quantity_on_hand = quantity_on_hand + NEW.quantity_received
    WHERE product_id = NEW.product_id;
END$$

DROP TRIGGER IF EXISTS trg_stock_in_items_after_update$$
CREATE TRIGGER trg_stock_in_items_after_update
AFTER UPDATE ON stock_in_items
FOR EACH ROW
BEGIN
    IF OLD.product_id = NEW.product_id THEN
        UPDATE inventory
        SET quantity_on_hand = quantity_on_hand - OLD.quantity_received + NEW.quantity_received
        WHERE product_id = NEW.product_id;
    ELSE
        UPDATE inventory
        SET quantity_on_hand = quantity_on_hand - OLD.quantity_received
        WHERE product_id = OLD.product_id;
        UPDATE inventory
        SET quantity_on_hand = quantity_on_hand + NEW.quantity_received
        WHERE product_id = NEW.product_id;
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_stock_in_items_after_delete$$
CREATE TRIGGER trg_stock_in_items_after_delete
AFTER DELETE ON stock_in_items
FOR EACH ROW
BEGIN
    UPDATE inventory
    SET quantity_on_hand = quantity_on_hand - OLD.quantity_received
    WHERE product_id = OLD.product_id;
END$$
DELIMITER ;

-- Order total and low-stock reports.
CREATE OR REPLACE VIEW vw_order_totals AS
SELECT o.order_id,
       o.client_id,
       o.order_date,
       o.order_status,
       COALESCE(SUM(oi.line_total), 0.00) AS order_total
FROM orders AS o
LEFT JOIN order_items AS oi ON oi.order_id = o.order_id
GROUP BY o.order_id, o.client_id, o.order_date, o.order_status;

CREATE OR REPLACE VIEW vw_low_stock AS
SELECT p.product_id,
       p.product_code,
       p.product_name,
       i.quantity_on_hand,
       i.reorder_level
FROM products AS p
JOIN inventory AS i ON i.product_id = p.product_id
WHERE p.is_active = TRUE
  AND i.quantity_on_hand <= i.reorder_level;

CREATE OR REPLACE VIEW vw_monthly_sales AS
SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS sales_month,
       COUNT(DISTINCT o.order_id) AS order_count,
       SUM(oi.line_total) AS sales_total
FROM orders AS o
JOIN order_items AS oi ON oi.order_id = o.order_id
WHERE o.order_status <> 'CANCELLED'
GROUP BY DATE_FORMAT(o.order_date, '%Y-%m');

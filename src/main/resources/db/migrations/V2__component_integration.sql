-- Run once against an existing econest_db created from the original schema.
-- Do not run this migration against a fresh database created from the updated base schema.

USE econest_db;

ALTER TABLE clients ADD COLUMN client_code VARCHAR(30) NULL AFTER client_id;
UPDATE clients SET client_code = CONCAT('CL-', LPAD(client_id, 6, '0'))
WHERE client_code IS NULL OR client_code = '';
ALTER TABLE clients MODIFY client_code VARCHAR(30) NOT NULL;
ALTER TABLE clients ADD UNIQUE KEY uq_clients_client_code (client_code);

ALTER TABLE delivery_agents ADD COLUMN agent_code VARCHAR(30) NULL AFTER agent_id;
UPDATE delivery_agents SET agent_code = CONCAT('DA-', LPAD(agent_id, 6, '0'))
WHERE agent_code IS NULL OR agent_code = '';
ALTER TABLE delivery_agents MODIFY agent_code VARCHAR(30) NOT NULL;
ALTER TABLE delivery_agents ADD UNIQUE KEY uq_delivery_agents_agent_code (agent_code);

ALTER TABLE orders ADD COLUMN order_code VARCHAR(30) NULL AFTER order_id;
UPDATE orders SET order_code = CONCAT('ORD-', LPAD(order_id, 8, '0'))
WHERE order_code IS NULL OR order_code = '';
ALTER TABLE orders MODIFY order_code VARCHAR(30) NOT NULL;
ALTER TABLE orders ADD UNIQUE KEY uq_orders_order_code (order_code);

ALTER TABLE deliveries ADD COLUMN scheduled_for DATE NULL AFTER delivery_address;

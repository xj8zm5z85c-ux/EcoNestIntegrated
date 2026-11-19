-- Optional, repeat-safe demonstration data for EcoNest.
-- All records use DEMO codes/references. Re-running this script does not add
-- duplicate products, clients, agents, receipts, orders, or order lines.

USE econest_db;
START TRANSACTION;

INSERT INTO products
    (product_code, product_name, category, description, unit_price, eco_rating)
SELECT 'DEMO-PL-001', 'Monstera Deliciosa', 'PLANT',
       'Large-leaf indoor plant in a 14 cm nursery pot.', 8500.00, 4.80
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-PL-001')
UNION ALL
SELECT 'DEMO-PL-002', 'Snake Plant', 'PLANT',
       'Low-maintenance indoor plant in a 12 cm nursery pot.', 4200.00, 4.60
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-PL-002')
UNION ALL
SELECT 'DEMO-PL-003', 'Peace Lily', 'PLANT',
       'Flowering indoor plant in a 14 cm nursery pot.', 5600.00, 4.50
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-PL-003')
UNION ALL
SELECT 'DEMO-PL-004', 'Basil Starter Plant', 'PLANT',
       'Kitchen herb starter grown in a compostable pot.', 1800.00, 4.90
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-PL-004')
UNION ALL
SELECT 'DEMO-PN-001', 'Terracotta Planter - Medium', 'PLANTER',
       'Locally made unglazed terracotta planter.', 2500.00, 4.70
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-PN-001')
UNION ALL
SELECT 'DEMO-PN-002', 'Recycled Ceramic Planter', 'PLANTER',
       'Decorative planter made with recycled ceramic material.', 4900.00, 4.80
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-PN-002')
UNION ALL
SELECT 'DEMO-KT-001', 'Balcony Herb Garden Kit', 'GARDENING_KIT',
       'Starter kit with herb seeds, growing medium, and planter.', 7200.00, 4.90
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-KT-001')
UNION ALL
SELECT 'DEMO-KT-002', 'Home Compost Starter Kit', 'GARDENING_KIT',
       'Compact starter kit for home composting.', 5300.00, 4.80
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'DEMO-KT-002');

INSERT INTO clients
    (client_code, client_type, client_name, contact_person, email, phone,
     address_line, city, postal_code)
SELECT 'DEMO-CL-001', 'HOUSEHOLD', 'Perera Household', NULL,
       'perera.demo@example.com', '+94 77 123 4567',
       '18 Flower Road', 'Colombo 07', '00700'
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_code = 'DEMO-CL-001')
UNION ALL
SELECT 'DEMO-CL-002', 'OFFICE', 'Greenleaf Design Studio', 'Nimali Silva',
       'studio.demo@example.com', '+94 11 234 5678',
       '42 Park Street', 'Colombo 02', '00200'
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_code = 'DEMO-CL-002');

INSERT INTO suppliers (supplier_name, contact_person, email, phone, address_line, city)
SELECT 'Green Valley Nursery', 'Kamal Fernando', 'sales@greenvalley.example.com',
       '+94 81 222 3344', '12 Garden Lane', 'Kandy'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE supplier_name = 'Green Valley Nursery')
UNION ALL
SELECT 'ReLeaf Sustainable Supplies', 'Ayesha Peris', 'orders@releaf.example.com',
       '+94 11 555 0182', '8 Craft Road', 'Colombo'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE supplier_name = 'ReLeaf Sustainable Supplies');

INSERT INTO delivery_agents
    (agent_code, full_name, phone, email, vehicle_registration_no, vehicle_type, vehicle_model)
SELECT 'DEMO-DA-001', 'Kasun Jayawardena', '+94 77 321 6540',
       'kasun.demo@example.com', 'DEMO WP CAB-1234', 'Motorcycle', 'Honda Dio'
WHERE NOT EXISTS (SELECT 1 FROM delivery_agents WHERE agent_code = 'DEMO-DA-001')
UNION ALL
SELECT 'DEMO-DA-002', 'Ishara Peris', '+94 76 456 7890',
       'ishara.demo@example.com', 'DEMO WP CAR-5678', 'Van', 'Suzuki Every'
WHERE NOT EXISTS (SELECT 1 FROM delivery_agents WHERE agent_code = 'DEMO-DA-002');

UPDATE inventory i
JOIN products p ON p.product_id = i.product_id
SET i.reorder_level = CASE p.product_code
    WHEN 'DEMO-PL-001' THEN 4
    WHEN 'DEMO-PL-002' THEN 18
    WHEN 'DEMO-PL-003' THEN 5
    WHEN 'DEMO-PL-004' THEN 10
    WHEN 'DEMO-PN-001' THEN 5
    WHEN 'DEMO-PN-002' THEN 4
    WHEN 'DEMO-KT-001' THEN 3
    WHEN 'DEMO-KT-002' THEN 3
END
WHERE p.product_code LIKE 'DEMO-%';

INSERT INTO stock_ins (supplier_id, supplier_reference, received_at, notes)
SELECT s.supplier_id, 'DEMO-RECEIPT-001', '2026-11-01 09:30:00',
       'Demo opening receipt - plants and locally made planters.'
FROM suppliers s
WHERE s.supplier_name = 'Green Valley Nursery'
  AND NOT EXISTS (
      SELECT 1 FROM stock_ins st
      WHERE st.supplier_id = s.supplier_id AND st.supplier_reference = 'DEMO-RECEIPT-001'
  );

INSERT INTO stock_ins (supplier_id, supplier_reference, received_at, notes)
SELECT s.supplier_id, 'DEMO-RECEIPT-002', '2026-11-04 14:15:00',
       'Demo opening receipt - sustainable planters and garden kits.'
FROM suppliers s
WHERE s.supplier_name = 'ReLeaf Sustainable Supplies'
  AND NOT EXISTS (
      SELECT 1 FROM stock_ins st
      WHERE st.supplier_id = s.supplier_id AND st.supplier_reference = 'DEMO-RECEIPT-002'
  );

INSERT INTO stock_in_items (stock_in_id, product_id, quantity_received, unit_cost)
SELECT st.stock_in_id, p.product_id, demo.quantity_received, demo.unit_cost
FROM (
    SELECT 'DEMO-RECEIPT-001' AS supplier_reference, 'DEMO-PL-001' AS product_code, 12 AS quantity_received, 4600.00 AS unit_cost
    UNION ALL SELECT 'DEMO-RECEIPT-001', 'DEMO-PL-002', 20, 2100.00
    UNION ALL SELECT 'DEMO-RECEIPT-001', 'DEMO-PL-003', 16, 2800.00
    UNION ALL SELECT 'DEMO-RECEIPT-001', 'DEMO-PL-004', 35, 700.00
    UNION ALL SELECT 'DEMO-RECEIPT-001', 'DEMO-PN-001', 18, 1100.00
    UNION ALL SELECT 'DEMO-RECEIPT-002', 'DEMO-PN-002', 12, 2400.00
    UNION ALL SELECT 'DEMO-RECEIPT-002', 'DEMO-KT-001', 10, 3600.00
    UNION ALL SELECT 'DEMO-RECEIPT-002', 'DEMO-KT-002', 8, 2700.00
) AS demo
JOIN stock_ins st ON st.supplier_reference = demo.supplier_reference
JOIN suppliers s ON s.supplier_id = st.supplier_id
JOIN products p ON p.product_code = demo.product_code
WHERE s.supplier_name = CASE demo.supplier_reference
        WHEN 'DEMO-RECEIPT-001' THEN 'Green Valley Nursery'
        ELSE 'ReLeaf Sustainable Supplies'
    END
  AND NOT EXISTS (
      SELECT 1 FROM stock_in_items existing
      WHERE existing.stock_in_id = st.stock_in_id AND existing.product_id = p.product_id
  );

INSERT INTO orders (order_code, client_id, order_date, order_status, dispatched_at, notes)
SELECT 'DEMO-ORD-0001', c.client_id, '2026-10-25 10:15:00', 'DELIVERED',
       '2026-10-26 08:30:00', 'Demo completed household order.'
FROM clients c
WHERE c.client_code = 'DEMO-CL-001'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE order_code = 'DEMO-ORD-0001');

INSERT INTO orders (order_code, client_id, order_date, order_status, dispatched_at, notes)
SELECT 'DEMO-ORD-0002', c.client_id, '2026-11-18 11:00:00', 'DISPATCHED',
       '2026-11-19 08:00:00', 'Demo office order in delivery.'
FROM clients c
WHERE c.client_code = 'DEMO-CL-002'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE order_code = 'DEMO-ORD-0002');

INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT o.order_id, p.product_id, demo.quantity, p.unit_price
FROM (
    SELECT 'DEMO-ORD-0001' AS order_code, 'DEMO-PL-001' AS product_code, 2 AS quantity
    UNION ALL SELECT 'DEMO-ORD-0001', 'DEMO-PN-001', 1
    UNION ALL SELECT 'DEMO-ORD-0001', 'DEMO-KT-001', 1
    UNION ALL SELECT 'DEMO-ORD-0002', 'DEMO-PL-002', 3
    UNION ALL SELECT 'DEMO-ORD-0002', 'DEMO-PL-003', 2
    UNION ALL SELECT 'DEMO-ORD-0002', 'DEMO-PN-002', 1
) AS demo
JOIN orders o ON o.order_code = demo.order_code
JOIN products p ON p.product_code = demo.product_code
WHERE NOT EXISTS (
    SELECT 1 FROM order_items existing
    WHERE existing.order_id = o.order_id AND existing.product_id = p.product_id
);

INSERT INTO deliveries
    (order_id, agent_id, delivery_address, scheduled_for, delivery_status,
     assigned_at, dispatched_at, delivered_at, delivery_notes)
SELECT o.order_id, a.agent_id, CONCAT(c.address_line, ', ', c.city),
       '2026-10-26', 'DELIVERED', '2026-10-25 12:00:00',
       '2026-10-26 08:30:00', '2026-10-26 11:20:00',
       'Demo delivery completed.'
FROM orders o
JOIN clients c ON c.client_id = o.client_id
JOIN delivery_agents a ON a.agent_code = 'DEMO-DA-001'
WHERE o.order_code = 'DEMO-ORD-0001'
  AND NOT EXISTS (SELECT 1 FROM deliveries WHERE order_id = o.order_id);

INSERT INTO deliveries
    (order_id, agent_id, delivery_address, scheduled_for, delivery_status,
     assigned_at, dispatched_at, delivery_notes)
SELECT o.order_id, a.agent_id, CONCAT(c.address_line, ', ', c.city),
       '2026-11-19', 'DISPATCHED', '2026-11-18 12:00:00',
       '2026-11-19 08:00:00', 'Demo delivery currently with the assigned agent.'
FROM orders o
JOIN clients c ON c.client_id = o.client_id
JOIN delivery_agents a ON a.agent_code = 'DEMO-DA-002'
WHERE o.order_code = 'DEMO-ORD-0002'
  AND NOT EXISTS (SELECT 1 FROM deliveries WHERE order_id = o.order_id);

COMMIT;

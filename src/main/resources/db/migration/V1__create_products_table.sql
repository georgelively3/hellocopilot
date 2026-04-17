CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)   NOT NULL,
    description VARCHAR(500),
    price       DECIMAL(10, 2) NOT NULL,
    quantity    INT            NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP
);

INSERT INTO products (name, description, price, quantity, created_at, updated_at)
VALUES ('Laptop Pro',    'High-performance laptop',   1299.99, 50,  NOW(), NOW()),
       ('Wireless Mouse','Ergonomic wireless mouse',    29.99, 200, NOW(), NOW()),
       ('USB-C Hub',     '7-in-1 USB-C hub',            49.99, 150, NOW(), NOW());

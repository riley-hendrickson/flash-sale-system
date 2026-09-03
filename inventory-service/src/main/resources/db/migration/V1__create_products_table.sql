CREATE TABLE products(
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    price   DECIMAL(7,2) NOT NULL,
    quantity INTEGER NOT NULL
)
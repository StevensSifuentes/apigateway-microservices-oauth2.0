CREATE TABLE order_status (
	id SERIAL PRIMARY KEY,
	name VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO order_status (name) 
VALUES ('PENDING'), 
		('CONFIRMED'), 
		('CANCELLED'), 
		('DELIVERED'), 
		('FAILED');

CREATE TABLE orders (
	id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    user_name  VARCHAR(150),
    status_id  INTEGER NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT NOW(),
	total 	   NUMERIC(12,2) NOT NULL
);

CREATE TABLE order_items (
	id 			 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	order_id 	 UUID NOT NULL,
	product_id 	 UUID NOT NULL,
	product_name VARCHAR(100) NOT NULL,
	unit_price 	 NUMERIC(12,2) NOT NULL,
	quantity 	 INTEGER NOT NULL CHECK (quantity > 0),
	subtotal 	 NUMERIC(12,2) NOT NULL,
	CONSTRAINT fk_order_items_order
		FOREIGN KEY	(order_id)
		REFERENCES orders(id)
		ON DELETE CASCADE
);

CREATE TABLE order_status_history (
	id 			   	  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	order_id 	   	  UUID NOT NULL,
	previus_status_id INTEGER,
	new_status_id 	  INTEGER NOT NULL,
	changed_at 	      TIMESTAMP NOT NULL DEFAULT NOW(),
	CONSTRAINT fk_history_order
		FOREIGN KEY	(order_id) REFERENCES orders(id) ON DELETE CASCADE,
	CONSTRAINT fk_history_prev_status
		FOREIGN KEY	(previus_status_id) REFERENCES order_status(id),
	CONSTRAINT fk_history_new_status
		FOREIGN KEY	(new_status_id) REFERENCES order_status(id)
);
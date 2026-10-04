CREATE TABLE products (
	id 			UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	name 		VARCHAR(100) NOT NULL,
	description TEXT,
	price 		NUMERIC(12, 2) NOT NULL,
	stock 		INTEGER NOT NULL DEFAULT 0,
	status 		BOOLEAN NOT NULL DEFAULT TRUE,
	category 	VARCHAR(50) NOT NULL 
		CHECK (category in ('ELECTRONICO', 'HOGAR', 'ALIMENTOS', 'ROPA', 'HIGIENE')),
	version     BIGINT NOT NULL DEFAULT 0
);

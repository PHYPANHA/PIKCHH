CREATE TABLE institutes (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(40) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(30),
    website VARCHAR(2048),
    address VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_institutes_code UNIQUE (code)
);

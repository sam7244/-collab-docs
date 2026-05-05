ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN oauth2_provider VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN oauth2_provider_id VARCHAR(255) NULL;
ALTER TABLE users ADD CONSTRAINT uq_oauth2 UNIQUE (oauth2_provider, oauth2_provider_id);

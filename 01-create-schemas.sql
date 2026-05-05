-- Separate schemas per service (microservice best practice: one DB per service)
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS documents_db;

-- Grant collab user access to both
GRANT ALL PRIVILEGES ON auth_db.* TO 'collab'@'%';
GRANT ALL PRIVILEGES ON documents_db.* TO 'collab'@'%';
FLUSH PRIVILEGES;

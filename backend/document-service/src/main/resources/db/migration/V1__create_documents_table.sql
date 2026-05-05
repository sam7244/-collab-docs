CREATE TABLE IF NOT EXISTS documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    title VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    latest_snapshot LONGBLOB NULL
);

CREATE INDEX idx_documents_owner_id ON documents(owner_id);

CREATE TABLE IF NOT EXISTS audit_log (
    id BINARY(16) NOT NULL PRIMARY KEY,
    doc_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    action VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_audit_log_doc_id ON audit_log(doc_id);

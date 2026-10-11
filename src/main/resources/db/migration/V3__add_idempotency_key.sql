ALTER TABLE files ADD COLUMN idempotency_key character varying(255);
ALTER TABLE files ADD CONSTRAINT uq_files_user_idempotency_key UNIQUE (user_id, idempotency_key);

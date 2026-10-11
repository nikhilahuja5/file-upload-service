


CREATE EXTENSION IF NOT EXISTS pgcrypto;


--CREATE TYPE upload_status AS ENUM (
--    'INITIATED',
--    'UPLOADING',
--    'PROCESSING',
--    'COMPLETED',
--    'FAILED'
--);

CREATE TABLE files (
    id                      uuid                     NOT NULL DEFAULT gen_random_uuid(),
    user_id                 character varying(255)   NOT NULL,
    file_name               character varying(255)   NOT NULL,
    s3_key                  character varying(1024),
    s3_multipart_upload_id  character varying(255),
    total_size_bytes        bigint                   NOT NULL,
    mime_type               character varying(100)   NOT NULL,
    checksum                character varying(64),
    status                  character varying(20)    NOT NULL DEFAULT 'INITIATED',
    created_at              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              timestamp with time zone,
    CONSTRAINT files_pkey PRIMARY KEY (id),
    CONSTRAINT files_s3_key_key UNIQUE (s3_key),
    CONSTRAINT files_total_size_bytes_check CHECK (total_size_bytes > 0),
    CONSTRAINT files_status_check CHECK (status IN ('INITIATED','UPLOADING','PROCESSING','COMPLETED','FAILED'))
);

-- Supports "list this user's files, newest first" without a full table scan
CREATE INDEX idx_files_user_id_created_at ON files (user_id, created_at DESC);


CREATE TABLE file_chunks (
    id                   uuid                     NOT NULL DEFAULT gen_random_uuid(),
    file_id              uuid                     NOT NULL,
    chunk_number         integer                  NOT NULL,
    etag                 character varying(255),
    size_bytes           bigint                   NOT NULL,
    created_at           timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT file_chunks_pkey PRIMARY KEY (id),
    CONSTRAINT uq_file_chunk UNIQUE (file_id, chunk_number),
    CONSTRAINT file_chunks_chunk_number_check CHECK (chunk_number > 0),
    CONSTRAINT file_chunks_size_bytes_check CHECK (size_bytes > 0),
    CONSTRAINT file_chunks_file_id_fkey FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE CASCADE
);


CREATE INDEX idx_file_chunks_file_id ON file_chunks (file_id);

CREATE DATABASE IF NOT EXISTS schemaflow;

CREATE TABLE IF NOT EXISTS schemaflow.json_documents
(
    collection LowCardinality(String),
    document_id String,
    document String,
    source_row_count UInt32,
    migrated_at DateTime64(3, 'UTC'),
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
ORDER BY (collection, document_id);

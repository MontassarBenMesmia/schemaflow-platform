# Migration contract

## Source

| Table | Purpose | Key |
| --- | --- | --- |
| `Customer` | Customer identity and segmentation | `customer_id` |
| `SalesOrder` | Customer order header | `order_id` |
| `SalesOrderItem` | Order line details | `order_item_id` |

## Target

Each ClickHouse row represents one versioned analytical document:

| Column | Purpose |
| --- | --- |
| `collection` | Logical document family |
| `document_id` | Stable business identifier |
| `document` | Compact JSON projection |
| `source_row_count` | Number of relational rows represented |
| `migrated_at` | UTC migration timestamp |
| `version` | Ordering key for idempotent replacement |

## Quality gates

- Primary output identifiers and JSON bodies cannot be null.
- Source counts and output counts are recorded for reconciliation.
- Fictional demo data is deterministic, making tests and screenshots reproducible.
- ReplacingMergeTree keeps the latest version for a collection/document key.
- The notebook fails before loading when validation errors exist.

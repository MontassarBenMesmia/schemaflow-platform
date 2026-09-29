"""Load deterministic fictional documents into local ClickHouse without third-party packages."""

from __future__ import annotations

import json
import os
import time
import urllib.parse
import urllib.request
from datetime import UTC, datetime

CLICKHOUSE_URL = os.getenv("CLICKHOUSE_URL", "http://clickhouse:8123")


def documents() -> list[dict[str, object]]:
    migrated_at = datetime(2026, 1, 15, 18, tzinfo=UTC).strftime("%Y-%m-%d %H:%M:%S.000")
    rows: list[dict[str, object]] = []
    for customer_id in range(1, 7):
        order_count = 1 + customer_id % 3
        orders = [
            {
                "orderId": f"SO-{customer_id:03d}-{order_id:02d}",
                "status": "completed" if order_id % 3 else "processing",
                "amount": 55 + customer_id * 11 + order_id * 8,
                "itemCount": 1 + (customer_id + order_id) % 4,
            }
            for order_id in range(1, order_count + 1)
        ]
        source_rows = 1 + order_count + sum(int(order["itemCount"]) for order in orders)
        rows.append(
            {
                "collection": "customer_profiles",
                "document_id": f"customer-{customer_id:03d}",
                "document": json.dumps(
                    {
                        "customerId": f"C-{customer_id:03d}",
                        "name": f"Demo Customer {customer_id:02d}",
                        "segment": ("startup", "growth", "enterprise")[customer_id % 3],
                        "region": ("Tunis", "Sfax", "Sousse")[customer_id % 3],
                        "orders": orders,
                    },
                    separators=(",", ":"),
                ),
                "source_row_count": source_rows,
                "migrated_at": migrated_at,
                "version": 1,
            }
        )
    return rows


def wait_until_ready() -> None:
    for _ in range(60):
        try:
            with urllib.request.urlopen(f"{CLICKHOUSE_URL}/ping", timeout=2) as response:
                if response.status == 200:
                    return
        except OSError:
            time.sleep(2)
    raise RuntimeError("ClickHouse did not become ready")


def load() -> None:
    wait_until_ready()
    query = urllib.parse.urlencode(
        {"query": "INSERT INTO schemaflow.json_documents FORMAT JSONEachRow"}
    )
    body = "\n".join(json.dumps(item) for item in documents()).encode()
    request = urllib.request.Request(
        f"{CLICKHOUSE_URL}/?{query}", data=body, method="POST"
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        if response.status >= 300:
            raise RuntimeError(f"ClickHouse returned {response.status}")
    print(f"Loaded {len(documents())} deterministic documents")


if __name__ == "__main__":
    load()

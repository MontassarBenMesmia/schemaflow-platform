# Databricks notebook source
"""Transform SQL Server relations into nested ClickHouse documents.

Install Microsoft SQL Server and ClickHouse JDBC drivers on the cluster. Store
all connection values in a Databricks secret scope named ``schemaflow``.
"""

from pyspark.sql import functions as F

SCOPE = "schemaflow"
SOURCE_URL = dbutils.secrets.get(SCOPE, "sqlserver-jdbc-url")  # noqa: F821
SOURCE_USER = dbutils.secrets.get(SCOPE, "sqlserver-user")  # noqa: F821
SOURCE_PASSWORD = dbutils.secrets.get(SCOPE, "sqlserver-password")  # noqa: F821
TARGET_URL = dbutils.secrets.get(SCOPE, "clickhouse-jdbc-url")  # noqa: F821
TARGET_USER = dbutils.secrets.get(SCOPE, "clickhouse-user")  # noqa: F821
TARGET_PASSWORD = dbutils.secrets.get(SCOPE, "clickhouse-password")  # noqa: F821


def read_source(table: str):
    return (
        spark.read.format("jdbc")  # noqa: F821
        .option("url", SOURCE_URL)
        .option("dbtable", f"dbo.{table}")
        .option("user", SOURCE_USER)
        .option("password", SOURCE_PASSWORD)
        .option("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .load()
    )


customers = read_source("Customer").alias("customer")
orders = read_source("SalesOrder").alias("sales_order")
items = read_source("SalesOrderItem").alias("item")

order_documents = (
    orders.join(items, "order_id", "left")
    .groupBy("order_id", "customer_id", "order_status", "ordered_at")
    .agg(F.collect_list(F.struct("order_item_id", "sku", "quantity", "unit_price")).alias("items"))
)

customer_documents = (
    customers.join(order_documents, "customer_id", "left")
    .groupBy("customer_id", "customer_name", "segment", "region")
    .agg(
        F.collect_list(F.struct("order_id", "order_status", "ordered_at", "items")).alias("orders"),
        F.count("order_id").alias("joined_row_count"),
    )
    .select(
        F.lit("customer_profiles").alias("collection"),
        F.format_string("customer-%06d", F.col("customer_id")).alias("document_id"),
        F.to_json(F.struct(
            F.col("customer_id").alias("customerId"),
            F.col("customer_name").alias("name"),
            "segment", "region", "orders",
        )).alias("document"),
        (F.col("joined_row_count") + F.lit(1)).cast("int").alias("source_row_count"),
        F.current_timestamp().alias("migrated_at"),
        F.unix_millis(F.current_timestamp()).alias("version"),
    )
)

invalid = customer_documents.filter(F.col("document_id").isNull() | F.col("document").isNull()).count()
if invalid:
    raise ValueError(f"Validation failed for {invalid} documents")

(
    customer_documents.write.format("jdbc")
    .option("url", TARGET_URL)
    .option("dbtable", "schemaflow.json_documents")
    .option("user", TARGET_USER)
    .option("password", TARGET_PASSWORD)
    .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
    .mode("append")
    .save()
)

print({
    "source_customers": customers.count(),
    "documents_written": customer_documents.count(),
    "validation_errors": invalid,
})

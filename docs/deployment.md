# Deployment

## Public demo

`render.yaml` deploys one Docker image containing the Angular production bundle and Spring Boot API. It enables the `demo` profile, so the public service needs no database, Azure subscription, secret, or persistent disk.

Render supplies `PORT`; Spring Boot binds to it automatically. The health check is `/api/v1/health`.

## Local ClickHouse integration

```bash
docker compose up --build
```

The stack starts ClickHouse, creates its schema, loads deterministic documents, and starts the application in `clickhouse` mode at `http://localhost:8080`.

Stop it with:

```bash
docker compose down
```

Add `--volumes` only when you intentionally want to remove local ClickHouse data.

## Optional SQL Server source

Copy `.env.example` to `.env`, choose a strong local password, then run:

```bash
docker compose --profile source up -d sqlserver
```

After SQL Server is ready, apply `infra/sqlserver/init.sql` with SSMS or `sqlcmd`. The source service is optional because the actual transformation is designed to execute as an Azure Databricks job.

## Azure Databricks

1. Install the Microsoft SQL Server and ClickHouse JDBC drivers on the job cluster.
2. Create a secret scope named `schemaflow`.
3. Add `sqlserver-jdbc-url`, `sqlserver-user`, `sqlserver-password`, `clickhouse-jdbc-url`, `clickhouse-user`, and `clickhouse-password`.
4. Import and run `databricks/sqlserver_to_clickhouse.py`.

Never put connection strings or passwords in the notebook, cluster environment, Git history, screenshots, or job parameters.

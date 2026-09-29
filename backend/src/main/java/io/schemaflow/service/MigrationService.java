package io.schemaflow.service;

import io.schemaflow.domain.ApiModels.Architecture;
import io.schemaflow.domain.ApiModels.Dashboard;
import io.schemaflow.domain.ApiModels.DocumentView;
import io.schemaflow.domain.ApiModels.MigrationRun;
import io.schemaflow.domain.ApiModels.PipelineStage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

@Service
public class MigrationService {
    private final DocumentStore store;
    private final ArrayDeque<MigrationRun> runs = new ArrayDeque<>();

    public MigrationService(DocumentStore store) {
        this.store = store;
        Instant completed = Instant.parse("2026-01-15T18:00:04Z");
        runs.addFirst(new MigrationRun(
                "demo-baseline", "COMPLETED", completed.minusMillis(4280), completed,
                store.countSourceRows(), store.countDocuments(), 0, 4280));
    }

    public Dashboard dashboard() {
        long sourceRows = store.countSourceRows();
        long documentCount = store.countDocuments();
        double reduction = sourceRows == 0 ? 0 : (1 - (double) documentCount / sourceRows) * 100;
        return new Dashboard(
                store.mode(), documentCount, sourceRows, store.summarize().size(), 0,
                Math.round(reduction * 10.0) / 10.0, store.summarize(), List.copyOf(runs));
    }

    public List<DocumentView> documents(String collection, String query, int limit) {
        return store.find(collection, query, Math.max(1, Math.min(limit, 100)));
    }

    public synchronized MigrationRun runDemoMigration() {
        if (!"deterministic-demo".equals(store.mode())) {
            throw new IllegalStateException("Demo migrations can only run in demo mode");
        }
        Instant started = Instant.now();
        store.resetDemoData();
        Instant completed = Instant.now();
        MigrationRun run = new MigrationRun(
                UUID.randomUUID().toString(), "COMPLETED", started, completed,
                store.countSourceRows(), store.countDocuments(), 0,
                Math.max(12, completed.toEpochMilli() - started.toEpochMilli()));
        runs.addFirst(run);
        while (runs.size() > 5) {
            runs.removeLast();
        }
        return run;
    }

    public List<MigrationRun> runs() {
        return List.copyOf(runs);
    }

    public Architecture architecture() {
        return new Architecture(
                "Transform normalized SQL Server rows into analytics-ready JSON documents.",
                "Customer, order, and line-item rows become nested customer profile documents while preserving lineage counts.",
                List.of(
                        new PipelineStage(1, "Extract", "SQL Server / SSMS", "Read normalized source tables over encrypted JDBC.", "configured"),
                        new PipelineStage(2, "Transform", "Azure Databricks / PySpark", "Validate keys, join relations, and construct nested JSON documents.", "configured"),
                        new PipelineStage(3, "Load", "ClickHouse", "Write versioned JSON documents for fast analytical access.", "configured"),
                        new PipelineStage(4, "Serve", "Spring Boot", "Expose allow-listed observability, search, and migration endpoints.", "healthy"),
                        new PipelineStage(5, "Visualize", "Angular", "Present data quality, lineage, collections, and safe document previews.", "healthy")
                ),
                List.of(
                        "No credentials are committed; adapters use environment variables or Databricks secret scopes.",
                        "The API exposes no arbitrary SQL or unrestricted document mutation endpoint.",
                        "The public demo uses deterministic fictional records and stores no user data.",
                        "Search and pagination inputs are bounded server-side."
                )
        );
    }
}

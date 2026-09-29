package io.schemaflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {}

    public record DocumentView(
            String collection,
            String documentId,
            Map<String, Object> document,
            int sourceRowCount,
            Instant migratedAt) {}

    public record CollectionSummary(
            String collection,
            long documentCount,
            long sourceRowCount,
            Instant lastMigratedAt) {}

    public record Dashboard(
            String mode,
            long documentCount,
            long sourceRowCount,
            int collectionCount,
            long validationErrors,
            double rowReductionPercent,
            List<CollectionSummary> collections,
            List<MigrationRun> recentRuns) {}

    public record PipelineStage(
            int order,
            String name,
            String technology,
            String responsibility,
            String status) {}

    public record Architecture(
            String purpose,
            String dataModel,
            List<PipelineStage> stages,
            List<String> safetyControls) {}

    public record MigrationRun(
            String runId,
            String status,
            Instant startedAt,
            Instant completedAt,
            long sourceRows,
            long documentsWritten,
            long validationErrors,
            long durationMs) {}
}

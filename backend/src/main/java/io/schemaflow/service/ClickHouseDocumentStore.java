package io.schemaflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.schemaflow.domain.ApiModels.CollectionSummary;
import io.schemaflow.domain.ApiModels.DocumentView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Repository
@Profile("clickhouse")
public class ClickHouseDocumentStore implements DocumentStore {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String authorization;

    public ClickHouseDocumentStore(
            @Value("${schemaflow.clickhouse.url}") String baseUrl,
            @Value("${schemaflow.clickhouse.username:default}") String username,
            @Value("${schemaflow.clickhouse.password:}") String password) {
        this.baseUrl = baseUrl;
        this.authorization = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<DocumentView> find(String collection, String query, int limit) {
        String safeCollection = sql(collection == null ? "" : collection.trim());
        String safeQuery = sql(query == null ? "" : query.trim());
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String filters = "";
        if (!safeCollection.isBlank()) {
            filters += " AND collection = '" + safeCollection + "'";
        }
        if (!safeQuery.isBlank()) {
            filters += " AND positionCaseInsensitive(document, '" + safeQuery + "') > 0";
        }
        String statement = "SELECT collection, document_id, document, source_row_count, "
                + "formatDateTime(migrated_at, '%Y-%m-%dT%H:%i:%S.000Z') AS migrated_at "
                + "FROM schemaflow.json_documents WHERE 1 = 1" + filters
                + " ORDER BY migrated_at DESC LIMIT " + safeLimit + " FORMAT JSON";
        try {
            JsonNode data = execute(statement).path("data");
            List<DocumentView> result = new java.util.ArrayList<>();
            for (JsonNode row : data) {
                Map<String, Object> document = mapper.readValue(
                        row.path("document").asText(), new TypeReference<>() {});
                result.add(new DocumentView(
                        row.path("collection").asText(),
                        row.path("document_id").asText(),
                        document,
                        row.path("source_row_count").asInt(),
                        Instant.parse(row.path("migrated_at").asText())
                ));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("ClickHouse document query failed", exception);
        }
    }

    @Override
    public List<CollectionSummary> summarize() {
        String statement = "SELECT collection, count() AS document_count, "
                + "sum(source_row_count) AS source_row_count, "
                + "formatDateTime(max(migrated_at), '%Y-%m-%dT%H:%i:%S.000Z') AS last_migrated_at "
                + "FROM schemaflow.json_documents GROUP BY collection ORDER BY collection FORMAT JSON";
        try {
            List<CollectionSummary> result = new java.util.ArrayList<>();
            for (JsonNode row : execute(statement).path("data")) {
                result.add(new CollectionSummary(
                        row.path("collection").asText(),
                        row.path("document_count").asLong(),
                        row.path("source_row_count").asLong(),
                        Instant.parse(row.path("last_migrated_at").asText())
                ));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("ClickHouse summary query failed", exception);
        }
    }

    @Override
    public long countDocuments() {
        return summarize().stream().mapToLong(CollectionSummary::documentCount).sum();
    }

    @Override
    public long countSourceRows() {
        return summarize().stream().mapToLong(CollectionSummary::sourceRowCount).sum();
    }

    @Override
    public String mode() {
        return "clickhouse";
    }

    @Override
    public void resetDemoData() {
        throw new UnsupportedOperationException("Demo reset is unavailable in ClickHouse mode");
    }

    private JsonNode execute(String query) throws Exception {
        String uri = baseUrl + "/?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("ClickHouse returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private String sql(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }
}

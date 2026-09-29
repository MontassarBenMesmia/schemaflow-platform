package io.schemaflow.service;

import io.schemaflow.domain.ApiModels.CollectionSummary;
import io.schemaflow.domain.ApiModels.DocumentView;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@Profile("demo")
public class DemoDocumentStore implements DocumentStore {
    private final List<DocumentView> documents = new ArrayList<>();

    public DemoDocumentStore() {
        resetDemoData();
    }

    @Override
    public synchronized List<DocumentView> find(String collection, String query, int limit) {
        String normalizedCollection = collection == null ? "" : collection.trim().toLowerCase(Locale.ROOT);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return documents.stream()
                .filter(item -> normalizedCollection.isBlank()
                        || item.collection().toLowerCase(Locale.ROOT).equals(normalizedCollection))
                .filter(item -> normalizedQuery.isBlank()
                        || item.documentId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || item.document().toString().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(DocumentView::migratedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized List<CollectionSummary> summarize() {
        Map<String, List<DocumentView>> grouped = new LinkedHashMap<>();
        documents.forEach(item -> grouped.computeIfAbsent(item.collection(), ignored -> new ArrayList<>()).add(item));
        return grouped.entrySet().stream().map(entry -> new CollectionSummary(
                entry.getKey(),
                entry.getValue().size(),
                entry.getValue().stream().mapToLong(DocumentView::sourceRowCount).sum(),
                entry.getValue().stream().map(DocumentView::migratedAt).max(Instant::compareTo).orElse(null)
        )).toList();
    }

    @Override
    public synchronized long countDocuments() {
        return documents.size();
    }

    @Override
    public synchronized long countSourceRows() {
        return documents.stream().mapToLong(DocumentView::sourceRowCount).sum();
    }

    @Override
    public String mode() {
        return "deterministic-demo";
    }

    @Override
    public synchronized void resetDemoData() {
        documents.clear();
        Instant base = Instant.parse("2026-01-15T09:00:00Z");
        for (int customer = 1; customer <= 12; customer++) {
            int orders = 1 + customer % 4;
            List<Map<String, Object>> nestedOrders = new ArrayList<>();
            for (int order = 1; order <= orders; order++) {
                nestedOrders.add(Map.of(
                        "orderId", "SO-%03d-%02d".formatted(customer, order),
                        "amount", 45 + customer * 12 + order * 7,
                        "status", order % 3 == 0 ? "processing" : "completed",
                        "itemCount", 1 + (customer + order) % 5
                ));
            }
            Map<String, Object> customerDocument = new LinkedHashMap<>();
            customerDocument.put("customerId", "C-%03d".formatted(customer));
            customerDocument.put("name", "Demo Customer %02d".formatted(customer));
            customerDocument.put("segment", List.of("startup", "growth", "enterprise").get(customer % 3));
            customerDocument.put("region", List.of("Tunis", "Sfax", "Sousse", "Remote").get(customer % 4));
            customerDocument.put("orders", nestedOrders);
            documents.add(new DocumentView(
                    "customer_profiles",
                    "customer-%03d".formatted(customer),
                    customerDocument,
                    1 + orders + nestedOrders.stream().mapToInt(item -> (int) item.get("itemCount")).sum(),
                    base.plus(customer, ChronoUnit.HOURS)
            ));
        }
        for (int product = 1; product <= 8; product++) {
            documents.add(new DocumentView(
                    "product_catalog",
                    "product-%03d".formatted(product),
                    Map.of(
                            "sku", "SKU-%04d".formatted(product),
                            "name", "Analytics Product %02d".formatted(product),
                            "category", product % 2 == 0 ? "platform" : "data",
                            "unitPrice", 25 + product * 9,
                            "active", true
                    ),
                    1,
                    base.plus(24 + product, ChronoUnit.HOURS)
            ));
        }
    }
}

package io.schemaflow.service;

import io.schemaflow.domain.ApiModels.CollectionSummary;
import io.schemaflow.domain.ApiModels.DocumentView;

import java.util.List;

public interface DocumentStore {
    List<DocumentView> find(String collection, String query, int limit);
    List<CollectionSummary> summarize();
    long countDocuments();
    long countSourceRows();
    String mode();
    void resetDemoData();
}

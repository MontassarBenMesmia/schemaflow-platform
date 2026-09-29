package io.schemaflow.api;

import io.schemaflow.domain.ApiModels.Architecture;
import io.schemaflow.domain.ApiModels.Dashboard;
import io.schemaflow.domain.ApiModels.DocumentView;
import io.schemaflow.domain.ApiModels.MigrationRun;
import io.schemaflow.service.MigrationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SchemaFlowController {
    private final MigrationService migrations;

    public SchemaFlowController(MigrationService migrations) {
        this.migrations = migrations;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "service", "schemaflow-api", "version", "1.0.0");
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        return migrations.dashboard();
    }

    @GetMapping("/documents")
    public List<DocumentView> documents(
            @RequestParam(defaultValue = "") @Size(max = 80) String collection,
            @RequestParam(defaultValue = "") @Size(max = 120) String query,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int limit) {
        return migrations.documents(collection, query, limit);
    }

    @GetMapping("/migrations")
    public List<MigrationRun> runs() {
        return migrations.runs();
    }

    @PostMapping("/migrations/demo")
    public MigrationRun runDemo() {
        return migrations.runDemoMigration();
    }

    @GetMapping("/architecture")
    public Architecture architecture() {
        return migrations.architecture();
    }
}

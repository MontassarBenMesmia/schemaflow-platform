import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

interface CollectionSummary {
  collection: string;
  documentCount: number;
  sourceRowCount: number;
  lastMigratedAt: string;
}

interface MigrationRun {
  runId: string;
  status: string;
  startedAt: string;
  completedAt: string;
  sourceRows: number;
  documentsWritten: number;
  validationErrors: number;
  durationMs: number;
}

interface Dashboard {
  mode: string;
  documentCount: number;
  sourceRowCount: number;
  collectionCount: number;
  validationErrors: number;
  rowReductionPercent: number;
  collections: CollectionSummary[];
  recentRuns: MigrationRun[];
}

interface DocumentView {
  collection: string;
  documentId: string;
  document: Record<string, unknown>;
  sourceRowCount: number;
  migratedAt: string;
}

interface PipelineStage {
  order: number;
  name: string;
  technology: string;
  responsibility: string;
  status: string;
}

interface Architecture {
  purpose: string;
  dataModel: string;
  stages: PipelineStage[];
  safetyControls: string[];
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);

  readonly dashboard = signal<Dashboard | null>(null);
  readonly architecture = signal<Architecture | null>(null);
  readonly documents = signal<DocumentView[]>([]);
  readonly loading = signal(true);
  readonly running = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly selectedCollection = signal('');
  readonly query = signal('');
  readonly selectedDocument = signal<DocumentView | null>(null);

  readonly documentJson = computed(() =>
    JSON.stringify(this.selectedDocument()?.document ?? {}, null, 2),
  );

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      dashboard: this.http.get<Dashboard>('/api/v1/dashboard'),
      architecture: this.http.get<Architecture>('/api/v1/architecture'),
      documents: this.http.get<DocumentView[]>('/api/v1/documents?limit=24'),
    }).subscribe({
      next: ({ dashboard, architecture, documents }) => {
        this.dashboard.set(dashboard);
        this.architecture.set(architecture);
        this.documents.set(documents);
        this.selectedDocument.set(documents[0] ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('The API is unavailable. Please retry in a moment.');
        this.loading.set(false);
      },
    });
  }

  filterDocuments(): void {
    const params = new URLSearchParams({ limit: '24' });
    if (this.selectedCollection()) params.set('collection', this.selectedCollection());
    if (this.query().trim()) params.set('query', this.query().trim());
    this.http.get<DocumentView[]>(`/api/v1/documents?${params}`).subscribe({
      next: (documents) => {
        this.documents.set(documents);
        this.selectedDocument.set(documents[0] ?? null);
      },
      error: () => this.error.set('The document filter could not be applied.'),
    });
  }

  chooseCollection(collection: string): void {
    this.selectedCollection.set(this.selectedCollection() === collection ? '' : collection);
    this.filterDocuments();
  }

  runMigration(): void {
    if (this.running()) return;
    this.running.set(true);
    this.notice.set('');
    this.error.set('');
    this.http.post<MigrationRun>('/api/v1/migrations/demo', {}).subscribe({
      next: (run) => {
        this.notice.set(
          `Migration ${run.runId.slice(0, 8)} completed: ${run.sourceRows} rows became ${run.documentsWritten} documents with ${run.validationErrors} errors.`,
        );
        this.running.set(false);
        this.refresh();
      },
      error: () => {
        this.error.set('The demo migration could not be started.');
        this.running.set(false);
      },
    });
  }

  selectDocument(document: DocumentView): void {
    this.selectedDocument.set(document);
  }

  trackStage(_: number, stage: PipelineStage): number {
    return stage.order;
  }
}

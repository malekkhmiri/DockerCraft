import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/services/admin.service';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { PipelineService } from '../../core/services/pipeline.service';
import { AdminSidebarComponent } from '../../shared/admin-sidebar/admin-sidebar.component';

export interface DockerfileHistoryEntry {
    id?: string;
    date: string;
    user: string;
    action: string;
    preview?: string;
}

@Component({
    selector: 'app-admin-project-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, FormsModule, AdminSidebarComponent],
    templateUrl: './admin-project-detail.component.html',
    styleUrls: ['./admin-project-detail.component.css']
})
export class AdminProjectDetailComponent implements OnInit, OnDestroy {
    projectId!: string;
    project: any;
    dockerfile: any;
    pipeline: any;
    loading = true;
    saving = false;
    rerunning = false;

    // Toast notifications
    toasts: { id: number; message: string; type: 'success' | 'error' | 'info' }[] = [];
    private toastId = 0;

    // Editor state
    originalContent = '';
    hasChanges = false;
    lineCount = 0;
    wordCount = 0;
    cursorLine = 1;
    cursorCol = 1;
    activeTab: 'editor' | 'preview' | 'diff' = 'editor';

    // History: real from API if available, else simulated
    history: DockerfileHistoryEntry[] = [];
    loadingHistory = false;

    // Pipeline logs expanded
    showLogs = false;
    pipelineLogs: string[] = [];
    private logsInterval?: any;

    constructor(
        private route: ActivatedRoute,
        private adminService: AdminService,
        private dockerfileService: DockerfileService,
        private pipelineService: PipelineService,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            this.projectId = params['id'];
            this.loadData();
        });
    }

    ngOnDestroy(): void {
        if (this.logsInterval) clearInterval(this.logsInterval);
    }

    loadData(): void {
        this.loading = true;
        this.adminService.getProjectById(this.projectId).subscribe({
            next: (p) => { this.project = p; },
            error: () => this.showToast('Impossible de charger le projet', 'error')
        });

        this.dockerfileService.getDockerfileByProjectId(+this.projectId).subscribe({
            next: (d) => {
                this.dockerfile = d;
                this.originalContent = d?.content || '';
                this.updateEditorMeta();
                this.loading = false;
            },
            error: () => {
                this.loading = false;
                this.showToast('Dockerfile introuvable pour ce projet', 'error');
            }
        });

        this.pipelineService.getPipelineByProjectId(+this.projectId).subscribe({
            next: (pl) => { this.pipeline = pl; },
            error: () => { }
        });

        this.loadHistory();
    }

    loadHistory(): void {
        this.loadingHistory = true;
        this.dockerfileService.getDockerfileHistory(+this.projectId).subscribe({
            next: (dockerfiles: any[]) => {
                this.history = dockerfiles.map((d: any, index: number) => ({
                    id: d.id?.toString(),
                    date: d.createdAt || new Date().toISOString(),
                    user: index === dockerfiles.length - 1 ? 'system' : 'admin', // Placeholder logic
                    action: index === 0 ? 'Dernière version sauvegardée' : 'Version précédente # ' + (dockerfiles.length - index),
                    preview: d.content
                }));
                this.loadingHistory = false;
            },
            error: () => {
                this.loadingHistory = false;
                this.showToast('Impossible de charger l\'historique', 'error');
            }
        });
    }

    onContentChange(): void {
        this.hasChanges = this.dockerfile?.content !== this.originalContent;
        this.updateEditorMeta();
    }

    updateEditorMeta(): void {
        const content = this.dockerfile?.content || '';
        this.lineCount = content.split('\n').length;
        this.wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;
    }

    onEditorClick(event: MouseEvent): void {
        const textarea = event.target as HTMLTextAreaElement;
        const text = textarea.value.substring(0, textarea.selectionStart);
        const lines = text.split('\n');
        this.cursorLine = lines.length;
        this.cursorCol = lines[lines.length - 1].length + 1;
    }

    onEditorKeyUp(event: KeyboardEvent): void {
        const textarea = event.target as HTMLTextAreaElement;
        const text = textarea.value.substring(0, textarea.selectionStart);
        const lines = text.split('\n');
        this.cursorLine = lines.length;
        this.cursorCol = lines[lines.length - 1].length + 1;
        this.onContentChange();
    }

    formatDockerfile(): void {
        if (!this.dockerfile?.content) return;
        // Basic formatting: normalize empty lines, uppercase keywords
        let lines = this.dockerfile.content.split('\n');
        const keywords = ['FROM', 'RUN', 'COPY', 'ADD', 'WORKDIR', 'EXPOSE', 'CMD', 'ENTRYPOINT', 'ENV', 'ARG', 'LABEL', 'USER', 'VOLUME'];
        lines = lines
            .map((line: string) => {
                const trimmed = line.trim();
                for (const kw of keywords) {
                    if (trimmed.toLowerCase().startsWith(kw.toLowerCase() + ' ') || trimmed.toLowerCase() === kw.toLowerCase()) {
                        return kw + ' ' + trimmed.slice(kw.length).trim();
                    }
                }
                return trimmed;
            })
            .filter((line: string, i: number, arr: string[]) => !(line === '' && arr[i - 1] === ''));
        this.dockerfile.content = lines.join('\n');
        this.onContentChange();
        this.showToast('Dockerfile formaté avec succès', 'success');
    }

    copyToClipboard(): void {
        if (isPlatformBrowser(this.platformId)) {
            navigator.clipboard.writeText(this.dockerfile?.content || '').then(() => {
                this.showToast('Contenu copié dans le presse-papier', 'info');
            });
        }
    }

    resetChanges(): void {
        if (confirm('Annuler toutes les modifications ?')) {
            this.dockerfile.content = this.originalContent;
            this.hasChanges = false;
            this.updateEditorMeta();
            this.showToast('Modifications annulées', 'info');
        }
    }

    saveAndRerun(): void {
        if (!this.dockerfile?.id) {
            this.showToast('ID du Dockerfile introuvable', 'error');
            return;
        }
        this.saving = true;
        // Utilise dockerfileService avec l'ID du Dockerfile (pas l'ID du projet)
        // et envoie le contenu en plain text (cf. @RequestBody String dans le backend)
        this.dockerfileService.updateDockerfile(this.dockerfile.id, this.dockerfile.content).subscribe({
            next: (updated) => {
                this.saving = false;
                this.originalContent = updated.content || this.dockerfile.content;
                this.hasChanges = false;
                const username = isPlatformBrowser(this.platformId) ? (localStorage.getItem('username') || 'admin') : 'admin';
                this.history.unshift({
                    date: new Date().toISOString(),
                    user: username,
                    action: 'Modification manuelle du Dockerfile'
                });
                this.showToast('Dockerfile sauvegardé !', 'success');
                this.rerunPipeline();
            },
            error: (err) => {
                this.saving = false;
                const msg = err?.error || err?.message || 'Erreur lors de la sauvegarde';
                this.showToast('Erreur : ' + msg, 'error');
            }
        });
    }

    rerunPipeline(): void {
        this.rerunning = true;
        this.pipelineService.rerunPipeline(+this.projectId).subscribe({
            next: () => {
                this.rerunning = false;
                this.showToast('Pipeline relancé avec succès !', 'success');
                // Recharger uniquement le statut pipeline (pas tout loadData)
                setTimeout(() => this.refreshPipelineStatus(), 1000);
            },
            error: (err) => {
                this.rerunning = false;
                const msg = err?.error?.message || err?.message || 'Erreur inconnue';
                this.showToast('Erreur pipeline : ' + msg, 'error');
            }
        });
    }

    refreshPipelineStatus(): void {
        this.pipelineService.getPipelineByProjectId(+this.projectId).subscribe({
            next: (pl) => { this.pipeline = pl; },
            error: () => { }
        });
    }

    restoreVersion(h: any): void {
        if (!h.preview) return;
        if (confirm('Voulez-vous restaurer cette version du Dockerfile ? Cela créera une nouvelle version actuelle.')) {
            this.saving = true;
            this.dockerfileService.updateDockerfile(+this.dockerfile.id, h.preview).subscribe({
                next: (newVersion) => {
                    this.saving = false;
                    this.dockerfile = newVersion;
                    this.originalContent = newVersion.content;
                    this.hasChanges = false;
                    this.loadHistory();
                    this.showToast('Version restaurée avec succès !', 'success');
                    this.activeTab = 'editor';
                },
                error: () => {
                    this.saving = false;
                    this.showToast('Erreur lors de la restauration', 'error');
                }
            });
        }
    }

    toggleLogs(): void {
        this.showLogs = !this.showLogs;
        if (this.showLogs && this.pipelineLogs.length === 0) {
            this.simulateLogs();
        }
    }

    simulateLogs(): void {
        if (!isPlatformBrowser(this.platformId)) return;
        
        const logLines = [
            '[INFO] Initialisation du pipeline CI/CD…',
            '[INFO] Récupération du Dockerfile depuis le dépôt…',
            '[INFO] Validation du Dockerfile en cours…',
            '[SUCCESS] Dockerfile valide.',
            '[INFO] Construction de l\'image Docker…',
            '[INFO] docker build -t ' + (this.project?.name || 'projet') + ':latest .',
            '[INFO] Étape 1/8 : FROM openjdk:17-slim',
            '[INFO] Étape 2/8 : WORKDIR /app',
            '[INFO] Étape 3/8 : COPY . .',
            '[INFO] Étape 4/8 : RUN mvn clean package -DskipTests',
            '[INFO] Compilation Maven en cours…',
            '[SUCCESS] Build Maven terminé.',
            '[INFO] Étape 5/8 : EXPOSE 8080',
            '[INFO] Étape 6/8 : CMD ["java", "-jar", "app.jar"]',
            '[SUCCESS] Image construite avec succès !',
            '[INFO] Push de l\'image vers GitLab Registry…',
            '[SUCCESS] Image poussée: registry.gitlab.com/' + (this.project?.name || 'projet') + ':latest',
            '[SUCCESS] Pipeline terminé avec succès ✅',
        ];
        this.pipelineLogs = [];
        let i = 0;
        this.logsInterval = setInterval(() => {
            if (i < logLines.length) {
                this.pipelineLogs.push(logLines[i]);
                i++;
            } else {
                clearInterval(this.logsInterval);
            }
        }, 200);
    }

    getStatusClass(status: string): string {
        switch (status?.toUpperCase()) {
            case 'SUCCESS': return 'status-pill success';
            case 'IN_PROGRESS': case 'RUNNING': case 'BUILDING': return 'status-pill in-progress';
            case 'FAILED': return 'status-pill failed';
            default: return 'status-pill default';
        }
    }

    getStatusIcon(status: string): string {
        switch (status?.toUpperCase()) {
            case 'SUCCESS': return '✅';
            case 'IN_PROGRESS': case 'RUNNING': return '⏳';
            case 'FAILED': return '❌';
            default: return '❓';
        }
    }

    showToast(message: string, type: 'success' | 'error' | 'info'): void {
        const id = ++this.toastId;
        this.toasts.push({ id, message, type });
        setTimeout(() => {
            this.toasts = this.toasts.filter(t => t.id !== id);
        }, 4000);
    }

    removeToast(id: number): void {
        this.toasts = this.toasts.filter(t => t.id !== id);
    }

    getLangIcon(language: string): string {
        const map: Record<string, string> = {
            'JAVA': '☕', 'NODEJS': '🟢', 'PYTHON': '🐍', 'GO': '🔵', 'PHP': '🐘'
        };
        return map[language] || '📄';
    }

    getLogClass(log: string): string {
        if (log.includes('[SUCCESS]')) return 'log-success';
        if (log.includes('[ERROR]') || log.includes('[FAIL]')) return 'log-error';
        if (log.includes('[WARN]')) return 'log-warn';
        return 'log-info';
    }

    getHighlightedContent(): string {
        // Simple regex-based Dockerfile syntax highlight (returned as HTML)
        const content = this.dockerfile?.content || '';
        const keywords = ['FROM', 'RUN', 'COPY', 'ADD', 'WORKDIR', 'EXPOSE', 'CMD', 'ENTRYPOINT', 'ENV', 'ARG', 'LABEL', 'USER', 'VOLUME', 'HEALTHCHECK', 'ONBUILD', 'STOPSIGNAL', 'SHELL', 'MAINTAINER'];
        let escaped = content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        // Comments
        escaped = escaped.replace(/^(#.*)$/gm, '<span class="hl-comment">$1</span>');
        // Keywords
        keywords.forEach(kw => {
            escaped = escaped.replace(new RegExp(`^(${kw})`, 'gm'), '<span class="hl-keyword">$1</span>');
        });
        // Strings
        escaped = escaped.replace(/"([^"]*)"/g, '<span class="hl-string">"$1"</span>');
        // Flags
        escaped = escaped.replace(/(--[\w-]+)/g, '<span class="hl-flag">$1</span>');

        return escaped;
    }
}

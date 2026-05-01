import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PipelineService } from '../../core/services/pipeline.service';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { ProjectService } from '../../core/services/project.service';
import { ImageService } from '../../core/services/image.service';
import { WebSocketService } from '../../core/services/web-socket.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-project-detail',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './project-detail.component.html',
    styleUrls: ['./project-detail.component.css']
})
export class ProjectDetailComponent implements OnInit {
    projectId!: number;
    project: any;
    dockerfile: any;
    pipeline: any;
    image: any;
    loading = true;
    dockerfileHistory: any[] = [];
    showHistory = false;
    private wsSubscription!: Subscription;

    constructor(
        private route: ActivatedRoute,
        private projectService: ProjectService,
        private dockerfileService: DockerfileService,
        private pipelineService: PipelineService,
        private imageService: ImageService,
        private webSocketService: WebSocketService
    ) { }

    get logs(): string[] {
        if (this.pipeline && this.pipeline.logs) {
            return this.pipeline.logs.split('\n');
        }
        return ["En attente du démarrage du pipeline..."];
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            this.projectId = +params['id'];
            this.loadData();
            this.setupWebSocket();
        });
    }

    setupWebSocket(): void {
        if (this.wsSubscription) this.wsSubscription.unsubscribe();
        this.wsSubscription = this.webSocketService.watchProject(this.projectId).subscribe(data => {
            console.log('Received live update:', data);
            this.pipeline = data; // Mis à jour en temps réel
        });
    }

    ngOnDestroy(): void {
        if (this.wsSubscription) this.wsSubscription.unsubscribe();
    }

    loadData(): void {
        this.loading = true;
        this.projectService.getProjectById(this.projectId).subscribe(p => this.project = p);
        this.dockerfileService.getDockerfileByProjectId(this.projectId).subscribe(d => this.dockerfile = d);
        this.loadHistory();
        this.pipelineService.getPipelineByProjectId(this.projectId).subscribe(pl => this.pipeline = pl);
        this.imageService.getImagesByProjectId(this.projectId).subscribe(imgs => {
            if (imgs && imgs.length > 0) this.image = imgs[0];
            this.loading = false;
        });
    }

    loadHistory(): void {
        this.dockerfileService.getDockerfileHistory(this.projectId).subscribe((history: any[]) => {
            this.dockerfileHistory = history;
        });
    }

    toggleHistory(): void {
        this.showHistory = !this.showHistory;
        if (this.showHistory) {
            this.loadHistory();
        }
    }

    restoreVersion(version: any): void {
        if (confirm('Voulez-vous vraiment restaurer cette version du Dockerfile ?')) {
            this.dockerfileService.updateDockerfile(version.id, version.content).subscribe(newDoc => {
                this.dockerfile = newDoc;
                this.loadHistory();
                this.showHistory = false;
                // Notification succès
                console.log('Version restaurée avec succès');
            });
        }
    }

    copyDockerfile(): void {
        if (this.dockerfile) {
            navigator.clipboard.writeText(this.dockerfile.content);
            // On pourrait ajouter un toast de succès ici
        }
    }

    getStepStatus(step: string): string {
        if (!this.project) return 'pending';

        switch (step) {
            case 'upload': return 'completed';
            case 'dockerfile': return this.dockerfile ? 'completed' : 'active';
            case 'pipeline':
                if (this.pipeline?.status === 'SUCCESS') return 'completed';
                if (this.pipeline?.status === 'IN_PROGRESS') return 'active';
                if (this.pipeline?.status === 'FAILED') return 'failed';
                return 'pending';
            case 'image': return this.image ? 'completed' : 'pending';
            default: return 'pending';
        }
    }
}

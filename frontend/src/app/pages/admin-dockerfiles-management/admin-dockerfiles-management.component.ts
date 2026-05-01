import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { AdminSidebarComponent } from '../../shared/admin-sidebar/admin-sidebar.component';
import { Dockerfile } from '../../core/models/dockerfile.model';

@Component({
    selector: 'app-admin-dockerfiles-management',
    standalone: true,
    imports: [CommonModule, FormsModule, AdminSidebarComponent],
    templateUrl: './admin-dockerfiles-management.component.html',
    styleUrls: ['./admin-dockerfiles-management.component.css']
})
export class AdminDockerfilesManagementComponent implements OnInit {
    dockerfiles: Dockerfile[] = [];
    searchTerm: string = '';
    loading = false;

    constructor(
        private dockerfileService: DockerfileService,
        private router: Router,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    ngOnInit(): void {
        this.loadDockerfiles();
    }

    loadDockerfiles() {
        this.loading = true;
        this.dockerfileService.getAllDockerfiles().subscribe({
            next: (data: any[]) => {
                this.dockerfiles = data;
                this.loading = false;
            },
            error: () => (this.loading = false)
        });
    }

    get filteredDockerfiles() {
        return this.dockerfiles.filter(d => {
            const idStr = d.id?.toString() || '';
            const projIdStr = d.projectId?.toString() || '';
            const content = d.content?.toLowerCase() || '';
            const searchLower = this.searchTerm.toLowerCase();

            return idStr.includes(this.searchTerm) ||
                projIdStr.includes(this.searchTerm) ||
                content.includes(searchLower);
        });
    }

    editDockerfile(projectId: number) {
        this.router.navigate(['/admin/project', projectId]);
    }

    getUsername() {
        if (isPlatformBrowser(this.platformId)) {
            return localStorage.getItem('username') || 'Admin';
        }
        return 'Admin';
    }
}

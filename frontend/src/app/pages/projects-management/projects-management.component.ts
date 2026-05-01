import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AdminService, RecentProject } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { AdminSidebarComponent } from '../../shared/admin-sidebar/admin-sidebar.component';

@Component({
    selector: 'app-projects-management',
    standalone: true,
    imports: [CommonModule, RouterLink, FormsModule, AdminSidebarComponent],
    templateUrl: './projects-management.component.html',
    styleUrls: ['./projects-management.component.css']
})
export class ProjectsManagementComponent implements OnInit {
    projects: RecentProject[] = [];
    searchTerm: string = '';
    statusFilter: string = '';
    userFilter: string = '';
    dateFilter: string = '';
    loading = false;

    constructor(
        private adminService: AdminService,
        private authService: AuthService,
        private router: Router,
        private route: ActivatedRoute,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    ngOnInit(): void {
        this.route.queryParams.subscribe(params => {
            if (params['user']) {
                this.userFilter = params['user'];
            }
        });
        this.loadProjects();
    }

    loadProjects() {
        this.loading = true;
        this.adminService.getAllProjects().subscribe({
            next: (data) => {
                this.projects = data;
                this.loading = false;
            },
            error: () => this.loading = false
        });
    }

    deleteProject(id: string) {
        if (confirm('Voulez-vous vraiment supprimer ce projet ? Tous les Dockerfiles et pipelines associés seront supprimés.')) {
            this.adminService.deleteProject(id).subscribe({
                next: () => this.loadProjects(),
                error: () => alert('Erreur lors de la suppression')
            });
        }
    }

    get filteredProjects() {
        return this.projects.filter(p => {
            const matchesSearch = (p.name?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                (p.user?.toLowerCase().includes(this.searchTerm.toLowerCase()) || false));
            const matchesStatus = this.statusFilter ? p.status === this.statusFilter : true;
            const matchesUser = this.userFilter ? p.user?.toLowerCase().includes(this.userFilter.toLowerCase()) : true;
            const matchesDate = this.dateFilter ? (p.uploadDate?.includes(this.dateFilter) || false) : true;

            return matchesSearch && matchesStatus && matchesUser && matchesDate;
        });
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'SUCCESS': return 'badge bg-success-subtle text-success border border-success';
            case 'IN_PROGRESS': return 'badge bg-warning-subtle text-warning border border-warning';
            case 'FAILED': return 'badge bg-danger-subtle text-danger border border-danger';
            case 'UPLOADED': return 'badge bg-info-subtle text-info border border-info';
            default: return 'badge bg-secondary';
        }
    }

    getLanguageDisplay(language: string): string {
        switch (language) {
            case 'JAVA': return 'Java';
            case 'NODEJS': return 'Node.js';
            case 'PYTHON': return 'Python';
            case 'GO': return 'Go';
            case 'PHP': return 'PHP';
            case 'UNKNOWN': return 'Générique';
            default: return language || 'Générique';
        }
    }

    getUsername() {
        if (isPlatformBrowser(this.platformId)) {
            return localStorage.getItem('username') || 'Admin';
        }
        return 'Admin';
    }

    onLogout() {
        this.authService.logout();
        this.router.navigate(['/login']);
    }
}

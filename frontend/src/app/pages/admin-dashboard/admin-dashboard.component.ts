import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminStats, RecentProject, Pipeline, DockerImage, SystemActivity } from '../../core/services/admin.service';
// Chart.js imported dynamically to prevent SSR crashes
import { type Chart as ChartType } from 'chart.js';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';
import { AdminSidebarComponent } from '../../shared/admin-sidebar/admin-sidebar.component';

// Chart registration handled in initChart or protected by browser check if needed

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink, FormsModule, AdminSidebarComponent],
    templateUrl: './admin-dashboard.component.html',
    styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild('pipelineChart') pipelineChartRef!: ElementRef;
    @ViewChild('activityChart') activityChartRef!: ElementRef;

    stats: AdminStats = {
        totalUsers: 0,
        totalProjects: 0,
        generatedDockerfiles: 0,
        totalPipelines: 0,
        totalImages: 0
    };

    displayStats: AdminStats = {
        totalUsers: 0,
        totalProjects: 0,
        generatedDockerfiles: 0,
        totalPipelines: 0,
        totalImages: 0
    };

    recentProjects: RecentProject[] = [];
    pipelines: Pipeline[] = [];
    dockerImages: DockerImage[] = [];
    activities: SystemActivity[] = [];

    searchTerm: string = '';
    statusFilter: string = '';
    currentPage: number = 1;
    pageSize: number = 5;

    pipelineChart?: ChartType;
    isLoading = true;
    private animInterval?: any;

    constructor(
        private adminService: AdminService,
        private authService: AuthService,
        private router: Router,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

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

    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.isLoading = true;
        this.adminService.getStats().subscribe(s => {
            this.stats = s;
            if (isPlatformBrowser(this.platformId)) {
                this.animateCounters(s);
            } else {
                this.displayStats = { ...s };
            }
            this.isLoading = false;
        });
        this.adminService.getRecentProjects().subscribe(p => this.recentProjects = p);
        this.adminService.getRecentPipelines().subscribe(p => this.pipelines = p);
        this.adminService.getDockerImages().subscribe(i => this.dockerImages = i);
        this.adminService.getSystemActivity().subscribe(a => this.activities = a);
    }

    animateCounters(target: AdminStats): void {
        const duration = 1600;
        const fps = 60;
        const steps = duration / (1000 / fps);
        let step = 0;

        if (this.animInterval) clearInterval(this.animInterval);

        this.animInterval = setInterval(() => {
            step++;
            const progress = this.easeOutQuart(step / steps);
            this.displayStats = {
                totalUsers: Math.round(target.totalUsers * progress),
                totalProjects: Math.round(target.totalProjects * progress),
                generatedDockerfiles: Math.round(target.generatedDockerfiles * progress),
                totalPipelines: Math.round(target.totalPipelines * progress),
                totalImages: Math.round(target.totalImages * progress),
            };
            if (step >= steps) {
                clearInterval(this.animInterval);
                this.displayStats = { ...target };
            }
        }, 1000 / fps);
    }

    private easeOutQuart(x: number): number {
        return 1 - Math.pow(1 - x, 4);
    }

    async ngAfterViewInit(): Promise<void> {
        if (isPlatformBrowser(this.platformId)) {
            const { Chart, registerables } = await import('chart.js');
            Chart.register(...registerables);
            this.initChart(Chart);
        }
    }

    ngOnDestroy(): void {
        if (this.animInterval) clearInterval(this.animInterval);
        if (this.pipelineChart) this.pipelineChart.destroy();
    }

    initChart(Chart: any): void {
        this.adminService.getPipelineStats().subscribe(data => {
            const ctx = this.pipelineChartRef.nativeElement.getContext('2d');
            const gradient = ctx.createLinearGradient(0, 0, 0, 300);
            gradient.addColorStop(0, 'rgba(59, 130, 246, 0.3)');
            gradient.addColorStop(1, 'rgba(59, 130, 246, 0)');

            const gradientWarn = ctx.createLinearGradient(0, 0, 0, 300);
            gradientWarn.addColorStop(0, 'rgba(250, 204, 21, 0.3)');
            gradientWarn.addColorStop(1, 'rgba(250, 204, 21, 0)');

            const gradientErr = ctx.createLinearGradient(0, 0, 0, 300);
            gradientErr.addColorStop(0, 'rgba(239, 68, 68, 0.3)');
            gradientErr.addColorStop(1, 'rgba(239, 68, 68, 0)');

            if (this.pipelineChart) this.pipelineChart.destroy();

            this.pipelineChart = new Chart(this.pipelineChartRef.nativeElement, {
                type: 'doughnut',
                data: {
                    labels: ['✅ Succès', '⏳ En cours', '❌ Échec'],
                    datasets: [{
                        data: [data.success, data.inProgress, data.failed],
                        backgroundColor: [
                            'rgba(34, 197, 94, 0.85)',
                            'rgba(251, 191, 36, 0.85)',
                            'rgba(239, 68, 68, 0.85)'
                        ],
                        borderColor: [
                            'rgba(34, 197, 94, 0.2)',
                            'rgba(251, 191, 36, 0.2)',
                            'rgba(239, 68, 68, 0.2)'
                        ],
                        borderWidth: 3,
                        hoverOffset: 20,
                        hoverBorderWidth: 0,
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '72%',
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                color: '#94a3b8',
                                padding: 24,
                                font: { size: 13, family: 'Inter' },
                                usePointStyle: true,
                                pointStyleWidth: 10
                            }
                        },
                        tooltip: {
                            backgroundColor: 'rgba(15, 23, 42, 0.95)',
                            borderColor: 'rgba(255,255,255,0.08)',
                            borderWidth: 1,
                            titleColor: '#f1f5f9',
                            bodyColor: '#94a3b8',
                            padding: 12,
                            callbacks: {
                                label: (ctx: any) => ` ${ctx.parsed} pipelines`
                            }
                        }
                    },
                    animation: { animateScale: true, animateRotate: true, duration: 1200, easing: 'easeOutQuart' }
                }
            });
        });
    }

    get filteredProjects() {
        return this.recentProjects.filter(p => {
            const matchSearch = p.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                p.user.toLowerCase().includes(this.searchTerm.toLowerCase());
            const matchStatus = this.statusFilter ? p.status === this.statusFilter : true;
            return matchSearch && matchStatus;
        });
    }

    get paginatedProjects() {
        const start = (this.currentPage - 1) * this.pageSize;
        return this.filteredProjects.slice(start, start + this.pageSize);
    }

    get totalPages(): number {
        return Math.ceil(this.filteredProjects.length / this.pageSize);
    }

    get pageRange(): number[] {
        return Array.from({ length: this.totalPages }, (_, i) => i + 1);
    }

    goToPage(page: number): void {
        if (page >= 1 && page <= this.totalPages) this.currentPage = page;
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'SUCCESS': return 'status-pill success';
            case 'IN_PROGRESS': return 'status-pill in-progress';
            case 'FAILED': return 'status-pill failed';
            case 'UPLOADED': return 'status-pill uploaded';
            default: return 'status-pill default';
        }
    }

    getStatusLabel(status: string): string {
        switch (status) {
            case 'SUCCESS': return '✅ Succès';
            case 'IN_PROGRESS': return '⏳ En cours';
            case 'FAILED': return '❌ Échec';
            case 'UPLOADED': return '📦 Uploadé';
            default: return status;
        }
    }

    getLanguageDisplay(language: string): string {
        const map: Record<string, string> = {
            'JAVA': '☕ Java', 'NODEJS': '🟢 Node.js', 'PYTHON': '🐍 Python',
            'GO': '🔵 Go', 'PHP': '🐘 PHP', 'UNKNOWN': '📄 Générique'
        };
        return map[language] || language || 'Générique';
    }

    getActivityIcon(type: SystemActivity['type']): string {
        switch (type) {
            case 'UPLOAD': return 'bi-cloud-upload text-primary';
            case 'DOCKERFILE': return 'bi-filetype-yml text-info';
            case 'PIPELINE_START': return 'bi-play-circle-fill text-warning';
            case 'PIPELINE_END': return 'bi-check-circle-fill text-success';
            case 'IMAGE_PUSH': return 'bi-box-seam-fill text-purple';
            default: return 'bi-activity text-secondary';
        }
    }

    getSuccessRate(): number {
        const total = this.pipelines.length;
        if (!total) return 0;
        const success = this.pipelines.filter(p => p.status === 'SUCCESS').length;
        return Math.round((success / total) * 100);
    }
}

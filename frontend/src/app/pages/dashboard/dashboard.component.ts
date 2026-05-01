import { Component, OnInit } from '@angular/core';
import { CommonModule, TitleCasePipe, DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { UserSidebarComponent } from '../../shared/user-sidebar/user-sidebar.component';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface ProjectDto {
  id: number;
  name: string;
  language: string;
  status: string;
  createdAt: string;
  executionTime?: string;
  reference?: string;
  owner?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, TitleCasePipe, DatePipe, UserSidebarComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  stats = { total: 0, inProgress: 0, success: 0, failed: 0 };
  projects: ProjectDto[] = [];
  loading = true;
  searchTerm = '';
  today = new Date();

  constructor(
    private projectService: ProjectService,
    private http: HttpClient,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  get role(): string {
    return 'PRO';
  }

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    const userEmail = this.authService.getUserEmail();
    const emailFilter = this.role === 'ADMIN' ? undefined : (userEmail || undefined);

    this.projectService.getAllProjects(emailFilter).subscribe({
      next: (data: any[]) => {
        this.projects = data.map(p => ({
          id: p.id,
          name: p.name,
          language: p.language || 'Générique',
          status: p.status,
          createdAt: p.createdAt,
          executionTime: p.executionTime || '0s',
          reference: p.reference || `PROJ-${p.id}`,
          owner: p.userEmail
        }));
        this.stats.total      = this.projects.length;
        this.stats.success    = this.projects.filter(p => p.status === 'SUCCESS').length;
        this.stats.inProgress = this.projects.filter(p => p.status === 'IN_PROGRESS').length;
        this.stats.failed     = this.projects.filter(p => p.status === 'FAILED').length;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getFilteredProjects(): ProjectDto[] {
    if (!this.searchTerm) return this.projects;
    return this.projects.filter(p =>
      p.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
      p.language.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  getSuccessRate(): number {
    if (!this.stats.total) return 0;
    return Math.round((this.stats.success / this.stats.total) * 100);
  }

  getStatusPillClass(status: string): string {
    const map: Record<string, string> = {
      SUCCESS:     'pill-success',
      FAILED:      'pill-failed',
      IN_PROGRESS: 'pill-running',
      UPLOADED:    'pill-uploaded'
    };
    return map[status] || 'pill-pending';
  }

  getLangColor(lang: string): string {
    const map: Record<string, string> = {
      'Java':    'lang-java',
      'Node.js': 'lang-node',
      'Python':  'lang-python',
      'Go':      'lang-go'
    };
    return map[lang] || 'lang-default';
  }

  getLangInitial(lang: string): string {
    return lang?.charAt(0)?.toUpperCase() || '?';
  }
}

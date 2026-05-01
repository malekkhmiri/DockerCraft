import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { Dockerfile } from '../../core/models/dockerfile.model';
import { AuthService } from '../../core/services/auth.service';
import { UserSidebarComponent } from '../../shared/user-sidebar/user-sidebar.component';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../core/services/project.service';


@Component({
  selector: 'app-dockerfile',
  standalone: true,
  imports: [CommonModule, UserSidebarComponent, RouterLink, FormsModule],
  templateUrl: './dockerfile.component.html',
  styleUrl: './dockerfile.component.css'
})
export class DockerfileComponent implements OnInit {
  dockerfile?: Dockerfile;
  loading = true;
  projectId?: number;
  role: string | null = null;
  isEditing = false;
  editedContent = '';
  showToast = false;
  toastMessage = '';
  today = new Date();
  userProjects: any[] = [];
  
  regenerating = false;

  constructor(
    private dockerfileService: DockerfileService,
    private projectService: ProjectService,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.role = this.authService.getRole();
    this.route.params.subscribe(params => {
      this.projectId = params['projectId'] ? +params['projectId'] : undefined;
      this.loadUserProjects();
      if (this.projectId) {
        this.loadDockerfile();
      } else {
        this.loadLatestDockerfile();
      }
    });
  }

  loadUserProjects(): void {
    const email = this.authService.getUserEmail();
    if (!email) return;
    this.projectService.getAllProjects(email).subscribe(data => {
      this.userProjects = data;
    });
  }

  onProjectChange(event: any): void {
    const newId = +event.target.value;
    if (newId) {
      this.projectId = newId;
      this.loadDockerfile();
    }
  }

  loadLatestDockerfile(): void {
    const email = this.authService.getUserEmail();
    if (!email) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.projectService.getAllProjects(email).subscribe({
      next: (projects) => {
        if (projects && projects.length > 0) {
          const latestProjectId = projects[0].id;
          this.projectId = latestProjectId;
          this.loadDockerfile();
        } else {
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('Error loading projects:', err);
        this.loading = false;
      }
    });
  }

  isStudentFree(): boolean {
    return this.authService.isStudentFree();
  }

  isStudentPaid(): boolean {
    return this.authService.isStudentPaid();
  }

  loadDockerfile(): void {
    if (!this.projectId) return;
    this.loading = true;
    this.dockerfileService.getDockerfileByProjectId(this.projectId).subscribe({
      next: (data) => {
        this.dockerfile = data;
        this.editedContent = data ? data.content : '';
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dockerfile:', err);
        this.loading = false;
      }
    });
  }

  copyToClipboard(): void {
    const content = this.isEditing ? this.editedContent : this.dockerfile?.content;
    if (content) {
      navigator.clipboard.writeText(content);
      this.triggerToast('Dockerfile copié dans le presse-papier !');
    }
  }

  downloadDockerfile(): void {
    const content = this.isEditing ? this.editedContent : this.dockerfile?.content;
    if (content) {
      const blob = new Blob([content], { type: 'text/plain' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'Dockerfile';
      a.click();
      window.URL.revokeObjectURL(url);
    }
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.editedContent = this.dockerfile?.content || '';
    }
  }

  saveDockerfile(): void {
    if (!this.dockerfile) return;
    this.dockerfile.content = this.editedContent;
    this.isEditing = false;
    this.triggerToast('Dockerfile mis à jour avec succès !');
  }

  importLocalDockerfile(event: any): void {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.editedContent = e.target.result;
        this.triggerToast('Contenu importé avec succès !');
      };
      reader.readAsText(file);
    }
  }

  triggerToast(msg: string): void {
    this.toastMessage = msg;
    this.showToast = true;
    setTimeout(() => this.showToast = false, 3000);
  }

  onRegenerate(): void {
    const projId = this.projectId || (this.dockerfile?.projectId);
    if (!projId || this.regenerating) return;

    this.regenerating = true;
    this.dockerfileService.regenerateDockerfile(projId).subscribe({
      next: () => {
        this.triggerToast('Régénération lancée !');
        setTimeout(() => {
          this.loadDockerfile();
          this.regenerating = false;
        }, 5000);
      },
      error: (err: any) => {
        this.regenerating = false;
        this.triggerToast('Erreur lors de la régénération.');
      }
    });
  }


}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { AuthService } from '../../core/services/auth.service';
import { DockerfileDto } from '../../core/models/dockerfile.model';
import { UserSidebarComponent } from '../../shared/user-sidebar/user-sidebar.component';

@Component({
  selector: 'app-dockerfile-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UserSidebarComponent],
  templateUrl: './dockerfile-editor.component.html',
  styleUrls: ['./dockerfile-editor.component.css']
})
export class DockerfileEditorComponent implements OnInit {
  dockerfile?: DockerfileDto;
  projectId!: number;
  loading = true;
  isEditing = false;
  editedContent = '';
  saving = false;
  downloading = false;
  downloadingGitlab = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private dockerfileService: DockerfileService,
    private authService: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.projectId = +params['projectId'];
      if (this.projectId) {
        this.loadDockerfile();
      }
    });
  }

  loadDockerfile(): void {
    this.loading = true;
    this.dockerfileService.getDockerfile(this.projectId).subscribe({
      next: (data) => {
        if (data) {
          this.dockerfile = data;
          this.editedContent = data.content;
        } else {
          this.errorMessage = 'No Dockerfile found for this project yet. It might still be generating.';
        }
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Failed to load Dockerfile.';
      }
    });
  }

  toggleEdit(): void {
    if (this.dockerfile?.isReadOnly) return;
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.editedContent = this.dockerfile?.content || '';
    }
  }

  saveDockerfile(): void {
    if (!this.dockerfile || !this.isEditing || this.saving) return;

    this.saving = true;
    this.errorMessage = '';
    this.dockerfileService.updateDockerfile(this.dockerfile.id, this.editedContent).subscribe({
      next: (data) => {
        this.dockerfile = data;
        this.isEditing = false;
        this.saving = false;
        this.showToast('Dockerfile saved successfully!');
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Save failed, please try again.';
      }
    });
  }

  downloadDockerfile(): void {
    if (!this.dockerfile || this.downloading) return;

    this.downloading = true;
    this.dockerfileService.downloadDockerfile(this.dockerfile.id).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, 'Dockerfile');
        this.downloading = false;
      },
      error: () => {
        this.downloading = false;
        this.errorMessage = 'Download failed.';
      }
    });
  }

  downloadGitlabCi(): void {
    if (!this.dockerfile || !this.isPro() || this.downloadingGitlab) return;

    this.downloadingGitlab = true;
    this.dockerfileService.downloadGitlabCi(this.dockerfile.id).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, '.gitlab-ci.yml');
        this.downloadingGitlab = false;
      },
      error: () => {
        this.downloadingGitlab = false;
        this.errorMessage = 'Download failed.';
      }
    });
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  private showToast(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 3000);
  }

  get charCount(): number {
    return this.editedContent.length;
  }

  isPro(): boolean { return this.authService.isPro(); }
  isStudent(): boolean { return this.authService.isStudent(); }
}

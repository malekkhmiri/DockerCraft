import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';
import { UserSidebarComponent } from '../../shared/user-sidebar/user-sidebar.component';
import { QuotaDto } from '../../core/models/dockerfile.model';

@Component({
  selector: 'app-upload-project',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, UserSidebarComponent],
  templateUrl: './upload-project.component.html',
  styleUrls: ['./upload-project.component.css']
})
export class UploadProjectComponent implements OnInit {
  uploadForm!: FormGroup;
  selectedFile?: File;
  isDragging = false;
  isUploading = false;
  errorMessage = '';
  loadingQuota = false;

  constructor(
    private fb: FormBuilder,
    private dockerfileService: DockerfileService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.uploadForm = this.fb.group({
      file: [null, Validators.required]
    });
  }

  getQuotaColor(): string {
    return 'text-success';
  }

  onFileSelected(event: any): void {
    this.handleFile(event.target.files?.[0]);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    this.handleFile(event.dataTransfer?.files?.[0]);
  }

  private handleFile(file?: File): void {
    if (!file) return;

    if (!file.name.endsWith('.zip')) {
      this.errorMessage = 'Please select a valid .zip file.';
      return;
    }

    if (file.size > 50 * 1024 * 1024) {
      this.errorMessage = 'File too large (> 50MB).';
      return;
    }

    this.selectedFile = file;
    this.uploadForm.patchValue({ file });
    this.errorMessage = '';
  }

  removeFile(): void {
    this.selectedFile = undefined;
    this.uploadForm.patchValue({ file: null });
  }

  onSubmit(): void {
    console.log('onSubmit clicked', this.uploadForm.value, this.selectedFile);
    if (this.uploadForm.invalid || !this.selectedFile) {
      console.warn('Form invalid or no file selected', this.uploadForm.invalid, !this.selectedFile);
      return;
    }

    this.isUploading = true;
    console.log('Starting upload...');
    this.errorMessage = '';

    const projectName = this.selectedFile.name.replace('.zip', '');
    const email = this.authService.getUserEmail() || 'user@example.com';
    const username = this.authService.getUsername() || 'user';

    this.dockerfileService.uploadProject(this.selectedFile, projectName, 'Java', email, username).subscribe({
      next: (response: any) => {
        this.isUploading = false;

        // Navigate to editor with projectId (returned as 'id' by project-service)
        const projectId = response.id || response.projectId;
        if (projectId) {
          this.router.navigate(['/dockerfile-editor', projectId]);
        } else {
          this.errorMessage = 'Project created but ID not received.';
        }
      },
      error: (err) => {
        this.isUploading = false;
        console.error('Upload error:', err);
        if (err.status === 429) {
          this.errorMessage = 'Quota exceeded, please upgrade.';
        } else if (err.status === 413) {
          this.errorMessage = 'File too large.';
        } else if (err.error && typeof err.error === 'string') {
          this.errorMessage = err.error;
        } else if (err.message) {
          this.errorMessage = 'Error: ' + err.message;
        } else {
          this.errorMessage = 'Server error, please try again later.';
        }
      }
    });
  }

  isStudent(): boolean { return this.authService.isStudent(); }
  isPro(): boolean { return this.authService.isPro(); }
}

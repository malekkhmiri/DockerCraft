import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Dockerfile, DockerfileDto, QuotaDto, UploadResponse } from '../models/dockerfile.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DockerfileService {
  private apiUrl = `${environment.apiUrl}/dockerfiles`;
  private projectUrl = `${environment.apiUrl}/projects`;
  private userUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  /**
   * Upload project ZIP and get generation result.
   */
  uploadProject(file: File, name: string, language: string, email: string, username: string): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    
    const params = `?name=${name}&language=${language}&userEmail=${email}&username=${username}`;
    return this.http.post<UploadResponse>(`${this.projectUrl}/upload${params}`, formData);
  }

  /**
   * Get Dockerfile by projectId.
   */
  getDockerfile(projectId: number): Observable<DockerfileDto> {
    return this.http.get<DockerfileDto>(`${this.apiUrl}/project/${projectId}`);
  }

  /**
   * Update Dockerfile content.
   */
  updateDockerfile(id: number, content: string): Observable<DockerfileDto> {
    return this.http.put<DockerfileDto>(`${this.apiUrl}/${id}`, { content });
  }

  /**
   * Download Dockerfile as Blob.
   */
  downloadDockerfile(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, { responseType: 'blob' });
  }

  /**
   * Download .gitlab-ci.yml as Blob.
   */
  downloadGitlabCi(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download-gitlab`, { responseType: 'blob' });
  }

  /**
   * Get current user quota.
   */
  getUserQuota(): Observable<QuotaDto> {
    return this.http.get<QuotaDto>(`${this.userUrl}/me/quota`);
  }

  // Backward compatibility methods if needed (optional)
  getDockerfileByProjectId(projectId: number): Observable<Dockerfile> {
    return this.http.get<Dockerfile>(`${this.apiUrl}/project/${projectId}`);
  }

  getAllDockerfiles(): Observable<DockerfileDto[]> {
    return this.http.get<DockerfileDto[]>(this.apiUrl);
  }

  getDockerfileHistory(projectId: number): Observable<DockerfileDto[]> {
    return this.http.get<DockerfileDto[]>(`${this.apiUrl}/project/${projectId}/history`);
  }

  regenerateDockerfile(projectId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/project/${projectId}/generate`, {});
  }
}

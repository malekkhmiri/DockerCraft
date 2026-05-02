import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AdminStats {
    totalUsers: number;
    totalProjects: number;
    generatedDockerfiles: number;
    totalPipelines: number;
    totalImages: number;
}

export interface PipelineStats {
    success: number;
    inProgress: number;
    failed: number;
}

export interface RecentProject {
    id: string;
    name: string;
    user: string;
    language: 'JAVA' | 'NODEJS';
    status: 'SUCCESS' | 'IN_PROGRESS' | 'FAILED' | 'UPLOADED';
    uploadDate: string;
}

export interface Pipeline {
    id: string;
    projectName: string;
    status: 'SUCCESS' | 'IN_PROGRESS' | 'FAILED';
    duration: string;
    date: string;
}

export interface DockerImage {
    name: string;
    tag: string;
    projectName: string;
    registryUrl: string;
}

export interface SystemActivity {
    type: 'UPLOAD' | 'DOCKERFILE' | 'PIPELINE_START' | 'PIPELINE_END' | 'IMAGE_PUSH';
    message: string;
    timestamp: string;
    user: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
    private apiUrl = `${environment.apiUrl}/admin`;

    constructor(private http: HttpClient) { }

    getStats(): Observable<AdminStats> {
        return this.http.get<AdminStats>(`${this.apiUrl}/stats`).pipe(
            catchError(() => of({
                totalUsers: 0,
                totalProjects: 0,
                generatedDockerfiles: 0,
                totalPipelines: 0,
                totalImages: 0
            }))
        );
    }

    getPipelineStats(): Observable<PipelineStats> {
        return this.http.get<PipelineStats>(`${this.apiUrl}/pipelines/stats`).pipe(
            catchError(() => of({ success: 0, inProgress: 0, failed: 0 }))
        );
    }

    getRecentProjects(): Observable<RecentProject[]> {
        return this.http.get<RecentProject[]>(`${this.apiUrl}/projects/recent`).pipe(
            catchError(() => of([]))
        );
    }

    getRecentPipelines(): Observable<Pipeline[]> {
        return this.http.get<Pipeline[]>(`${this.apiUrl}/pipelines/recent`).pipe(
            catchError(() => of([]))
        );
    }

    getDockerImages(): Observable<DockerImage[]> {
        return this.http.get<DockerImage[]>(`${this.apiUrl}/images`).pipe(
            catchError(() => of([]))
        );
    }

    getSystemActivity(): Observable<SystemActivity[]> {
        return this.http.get<SystemActivity[]>(`${this.apiUrl}/activities`).pipe(
            catchError(() => of([]))
        );
    }

    // --- Gestion des Utilisateurs ---
    getUsers(): Observable<any[]> {
        return this.http.get<any[]>(`${environment.apiUrl}/users`).pipe(
            catchError(() => of([]))
        );
    }

    toggleUserStatus(id: number, active: boolean): Observable<any> {
        return this.http.put(`${environment.apiUrl}/users/${id}/status`, { active });
    }

    deleteUser(id: number): Observable<void> {
        return this.http.delete<void>(`${environment.apiUrl}/users/${id}`);
    }

    // --- Gestion des Projets ---
    getAllProjects(): Observable<any[]> {
        return this.http.get<any[]>(`${environment.apiUrl}/projects`).pipe(
            map((projects: any[]) => projects.map((p: any) => ({
                ...p,
                uploadDate: p.createdAt,
                user: p.username || 'N/A'
            }))),
            catchError(() => of([]))
        );
    }

    getProjectById(id: string): Observable<any> {
        return this.http.get<any>(`${environment.apiUrl}/projects/${id}`);
    }

    updateDockerfile(projectId: string, content: string): Observable<any> {
        const baseUrl = environment.dockerfileServiceUrl || environment.apiUrl;
        return this.http.put(`${baseUrl}/dockerfiles/project/${projectId}`, { content });
    }

    rerunPipeline(projectId: string): Observable<any> {
        return this.http.post(`${environment.apiUrl}/pipelines/project/${projectId}/rerun`, {});
    }

    deleteProject(id: string): Observable<void> {
        return this.http.delete<void>(`${environment.apiUrl}/projects/${id}`);
    }
}

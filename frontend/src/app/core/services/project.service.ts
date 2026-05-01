import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private apiUrl = `${environment.apiUrl}/projects`;

  constructor(private http: HttpClient) { }

  uploadProject(formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/upload`, formData);
  }

  getAllProjects(userEmail?: string): Observable<any[]> {
    let url = this.apiUrl;
    if (userEmail) {
      url += `?userEmail=${userEmail}`;
    }
    return this.http.get<any[]>(url);
  }

  getProjectById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getProjectCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count`);
  }
}

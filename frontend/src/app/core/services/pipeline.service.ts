import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Pipeline } from '../models/pipeline.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PipelineService {
  private apiUrl = `${environment.apiUrl}/pipelines`;

  constructor(private http: HttpClient) { }

  getAllPipelines(): Observable<Pipeline[]> {
    return this.http.get<Pipeline[]>(this.apiUrl);
  }

  getPipelineByProjectId(projectId: number): Observable<Pipeline | null> {
    return this.http.get<Pipeline>(`${this.apiUrl}/project/${projectId}`).pipe(
      catchError(() => of(null))  // 404 = pas encore de pipeline, retourne null
    );
  }

  rerunPipeline(projectId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/project/${projectId}/rerun`, {});
  }
}


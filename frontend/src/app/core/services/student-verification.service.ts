import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StudentVerificationResponse {
  id: number;
  userId: number;
  status: 'PENDING' | 'AI_PROCESSING' | 'AI_APPROVED' | 'ADMIN_REVIEW' | 'ADMIN_APPROVED' | 'REJECTED' | 'EXPIRED';
  confidenceScore: number;
  extractedName: string;
  extractedStudentId: string;
  extractedUniversity: string;
  extractedAcademicYear: string;
  rejectionReason: string;
  verifiedAt: string;
  expiresAt: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class StudentVerificationService {

  private apiUrl = `${environment.apiUrl}/users/student-verification`;

  constructor(private http: HttpClient) {}

  /**
   * Upload la carte étudiant pour analyse IA (Ollama LLaVA)
   */
  uploadStudentCard(userId: number, file: File): Observable<StudentVerificationResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', userId.toString());
    return this.http.post<StudentVerificationResponse>(`${this.apiUrl}/upload`, formData);
  }

  /**
   * Récupère le statut de vérification d'un utilisateur
   */
  getVerificationStatus(userId: number): Observable<StudentVerificationResponse> {
    return this.http.get<StudentVerificationResponse>(`${this.apiUrl}/status/${userId}`);
  }

  // ─── Admin ────────────────────────────────────────────────────────────────

  getPendingReviews(): Observable<StudentVerificationResponse[]> {
    return this.http.get<StudentVerificationResponse[]>(`${this.apiUrl}/admin/pending`);
  }

  adminApprove(verificationId: number): Observable<StudentVerificationResponse> {
    return this.http.post<StudentVerificationResponse>(`${this.apiUrl}/admin/${verificationId}/approve`, {});
  }

  adminReject(verificationId: number, reason: string): Observable<StudentVerificationResponse> {
    return this.http.post<StudentVerificationResponse>(`${this.apiUrl}/admin/${verificationId}/reject`, { reason });
  }
}

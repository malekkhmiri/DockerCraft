import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models/auth-response.model';
import { Observable } from 'rxjs';



@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  login(data: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/users/login`, data);
  }

  signup(data: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/users/register`, data);
  }

  verifyEmail(email: string, code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/users/verify-email`, { email, code });
  }

  resendOtp(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/users/resend-otp`, { email });
  }

  saveUserData(response: AuthResponse) {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('token', response.token);
      localStorage.setItem('role', response.role);
      localStorage.setItem('username', response.username);
      localStorage.setItem('userEmail', response.email);
      localStorage.setItem('userType', response.userType);
      localStorage.setItem('planType', response.planType);
      if (response.userId) {
        localStorage.setItem('userId', response.userId.toString());
      }
      if (response.isStudentVerified !== undefined) {
        localStorage.setItem('isStudentVerified', String(response.isStudentVerified));
      }
    }
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('token');
    }
    return null;
  }

  getRole(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('role');
    }
    return null;
  }

  getUsername(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('username');
    }
    return null;
  }

  getUserEmail(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('userEmail');
    }
    return null;
  }

  getUserType(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('userType');
    }
    return null;
  }

  getUserId(): number | null {
    if (isPlatformBrowser(this.platformId)) {
      const id = localStorage.getItem('userId');
      return id ? parseInt(id, 10) : null;
    }
    return null;
  }

  getPlanType(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('planType');
    }
    return null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isStudentFree(): boolean {
    return false;
  }

  isStudentPaid(): boolean {
    return false;
  }

  isStudent(): boolean {
    return false;
  }

  isPro(): boolean {
    return true; // Everyone has full access
  }

  isAdmin(): boolean {
    return this.getRole()?.toUpperCase() === 'ADMIN';
  }

  logout() {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.clear();
    }
  }

  changePassword(data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/profile/password`, data);
  }
}

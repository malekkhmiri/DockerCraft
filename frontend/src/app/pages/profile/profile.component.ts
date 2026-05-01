import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { UserSidebarComponent } from '../../shared/user-sidebar/user-sidebar.component';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, UserSidebarComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  username: string | null = '';
  email: string | null = '';
  role: string | null = '';
  currentTheme: string = 'dark';

  // Profil complet
  userProfile: any = null;

  passwordData = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  successMsg: string | null = null;
  errorMsg: string | null = null;
  showToast = false;
  toastMessage = '';

  constructor(
    private authService: AuthService,
    public themeService: ThemeService,
    private http: HttpClient
  ) { }

  ngOnInit(): void {
    this.username = this.authService.getUsername();
    this.email = this.authService.getUserEmail();
    this.role = this.authService.getRole();
    this.currentTheme = this.themeService.getCurrentTheme();
    this.loadFullProfile();
  }

  loadFullProfile(): void {
    if (!this.email) return;
    this.http.get<any>(`${environment.apiUrl}/users/profile?email=${encodeURIComponent(this.email)}`).subscribe({
      next: (profile) => {
        this.userProfile = profile;
      }
    });
  }

  getPlanLabel(): string {
    if (!this.userProfile) return 'Chargement...';
    if (this.userProfile.planType === 'PRO') return 'Professionnel (Illimité)';
    if (this.userProfile.planType === 'STUDENT_PAID') return 'Étudiant Premium';
    return 'Étudiant Gratuit';
  }

  isStudent(): boolean {
    return this.authService.isStudent();
  }

  isProfessional(): boolean {
    return this.authService.isPro();
  }

  toggleTheme() {
    this.themeService.toggleTheme();
    this.currentTheme = this.themeService.getCurrentTheme();
    this.triggerToast(`Mode ${this.currentTheme === 'light' ? 'clair' : 'sombre'} activé`);
  }

  onChangePassword() {
    this.successMsg = null;
    this.errorMsg = null;

    if (this.passwordData.newPassword !== this.passwordData.confirmPassword) {
      this.errorMsg = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.authService.changePassword(this.passwordData).subscribe({
      next: () => {
        this.successMsg = 'Mot de passe mis à jour avec succès !';
        this.passwordData = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.triggerToast('Mot de passe mis à jour !');
      },
      error: (err) => {
        this.errorMsg = err.error?.message || 'Une erreur est survenue.';
      }
    });
  }

  triggerToast(msg: string): void {
    this.toastMessage = msg;
    this.showToast = true;
    setTimeout(() => this.showToast = false, 3000);
  }
}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  constructor(
    public router: Router,
    private authService: AuthService,
    public themeService: ThemeService
  ) { }

  isLightMode(): boolean {
    return this.themeService.getCurrentTheme() === 'light';
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  get show(): boolean {
    const hideOn = ['/login', '/signup', '/', '/home'];
    return !hideOn.includes(this.router.url);
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isStudent(): boolean {
    return this.authService.isStudent();
  }

  isPro(): boolean {
    return this.authService.isPro();
  }

  getRoleLabel(): string {
    if (this.isAdmin()) return 'ADMIN';
    return this.isPro() ? 'PRO' : 'ÉTUDIANT';
  }

  getUsername(): string {
    return this.authService.getUsername() || 'Utilisateur';
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-user-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './user-sidebar.component.html',
  styleUrls: ['./user-sidebar.component.css']
})
export class UserSidebarComponent implements OnInit {
  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void { }

  get username(): string { return this.authService.getUsername() || 'Utilisateur'; }
  get initials(): string { return this.username.substring(0, 2).toUpperCase(); }

  getRoleClass(): string {
    return 'role-pro';
  }

  getPlanLabel(): string {
    return 'DOCKERCRAFT';
  }

  getPlanBadgeClass(): string {
    return 'pro';
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

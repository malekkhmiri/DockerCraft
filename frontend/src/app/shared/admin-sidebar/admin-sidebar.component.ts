import { Component, Input, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';

@Component({
    selector: 'app-admin-sidebar',
    standalone: true,
    imports: [CommonModule, RouterLink, RouterLinkActive],
    templateUrl: './admin-sidebar.component.html',
    styleUrls: ['./admin-sidebar.component.css']
})
export class AdminSidebarComponent {
    @Input() activeRoute: string = '';

    constructor(
        private authService: AuthService, 
        private router: Router,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    getUsername(): string {
        if (isPlatformBrowser(this.platformId)) {
            return localStorage.getItem('username') || 'Admin';
        }
        return 'Admin';
    }

    onLogout(): void {
        this.authService.logout();
        this.router.navigate(['/login']);
    }
}

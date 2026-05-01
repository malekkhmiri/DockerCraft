import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css']
})
export class HomeComponent {
    isLoggedIn: boolean = false;
    username: string = '';

    constructor(
        private authService: AuthService, 
        private router: Router,
        @Inject(PLATFORM_ID) private platformId: Object
    ) {
        this.isLoggedIn = this.authService.isLoggedIn();
        if (this.isLoggedIn && isPlatformBrowser(this.platformId)) {
            this.username = localStorage.getItem('username') || 'Utilisateur';
        }
    }

    onLogout() {
        this.authService.logout();
        this.isLoggedIn = false;
        this.router.navigate(['/']);
    }
}

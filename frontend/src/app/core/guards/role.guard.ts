import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const expectedRole = route.data['role'];
    const userRole = authService.getRole();

    if (authService.isLoggedIn() && userRole === expectedRole) {
        return true;
    }

    // Rediriger vers login si non connecté ou rôle incorrect
    router.navigate(['/login']);
    return false;
};

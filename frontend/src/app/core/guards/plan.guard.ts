import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard de plan — protège les routes selon le planType de l'utilisateur.
 *
 * Usage dans app.routes.ts :
 *   canActivate: [authGuard, planGuard],
 *   data: { allowedPlans: ['STUDENT_PAID', 'PRO'] }
 */
export const planGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const allowedPlans: string[] = route.data['allowedPlans'] ?? [];
  const currentPlan = authService.getPlanType();

  // Admin bypass : l'admin a toujours accès
  if (authService.isAdmin()) return true;

  if (currentPlan && allowedPlans.includes(currentPlan)) {
    return true;
  }

  // Redirige vers le dashboard avec un signal de blocage pour afficher un message
  router.navigate(['/dashboard'], { queryParams: { blocked: 'plan', requiredPlan: allowedPlans[0] } });
  return false;
};

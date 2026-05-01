import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check if a token exists
  if (authService.getToken()) {
    return true; // Allow access
  }else {
  // If no token, redirect to the login page
  router.navigate(['/login']);
  return false;
  }
};
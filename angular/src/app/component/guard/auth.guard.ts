import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

export const authGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  const router     = inject(Router);

  if (!isPlatformBrowser(platformId)) return true;

  const token = sessionStorage.getItem('jwt_token');

  if (token) return true;

  // Not logged in — redirect to login page (NOT directly to Google)
  router.navigate(['/login']);
  return false;
};
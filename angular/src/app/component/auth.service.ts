import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private platformId = inject(PLATFORM_ID);
  private router     = inject(Router);

  /**
   * Redirects the browser to Spring's OAuth2 authorization endpoint.
   * Spring handles the Google redirect from here.
   * Call ONLY from a user click event — never from ngOnInit/AfterViewInit.
   */
  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8151/oauth2/authorization/google';
  }

  /**
   * initializeGoogleAuth is kept for backward compatibility with LoginComponent.
   * It no longer auto-redirects — the (click) binding in the template handles that.
   * This method is now a no-op stub.
   */
  initializeGoogleAuth(_buttonId: string): void {
    // No-op: login is triggered by (click)="authService.loginWithGoogle()" in template.
    // This stub prevents the AfterViewInit call from throwing an error.
  }

  /**
   * Clears JWT from sessionStorage and redirects to login.
   * Called by dashboard: <button (click)="authService.logout()">Logout</button>
   */
  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('jwt_token');
    }
    this.router.navigate(['/login']);
  }

  /** Used by auth guard to check login state */
  isLoggedIn(): boolean {
    if (!isPlatformBrowser(this.platformId)) return false;
    return !!sessionStorage.getItem('jwt_token');
  }

  /** Returns the raw JWT string or null */
  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return sessionStorage.getItem('jwt_token');
  }
}
import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [],
  template: `
    <div style="display:flex;justify-content:center;align-items:center;height:100vh">
      <p>Completing login...</p>
    </div>
  `
})
export class AuthCallbackComponent implements OnInit {

  private router     = inject(Router);
  private platformId = inject(PLATFORM_ID);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // ✅ OAuth2SuccessHandler redirects to: /auth-callback#token=<JWT>
    const fragment = window.location.hash;

    if (fragment) {
      const fragParams = new URLSearchParams(fragment.substring(1));
      const token = fragParams.get('token')
                 ?? fragParams.get('accessToken')
                 ?? fragParams.get('access_token');

      if (token) {
        // ✅ MUST use exact key 'jwt_token' — interceptor reads same key
        sessionStorage.setItem('jwt_token', token);
        console.log('✅ Token from fragment saved');

        // ✅ Clear the fragment from URL (security best practice)
        window.history.replaceState(null, '', window.location.pathname);

        this.router.navigate(['/dashboard']);
        return;
      }
    }

    // Also handle ?token= query param (fallback)
    const params = new URLSearchParams(window.location.search);
    const queryToken = params.get('token') ?? params.get('jwt');

    if (queryToken) {
      sessionStorage.setItem('jwt_token', queryToken);
      console.log('✅ Token from query param saved');
      this.router.navigate(['/dashboard']);
      return;
    }

    console.error('❌ No token found in callback URL');
    this.router.navigate(['/login']);
  }
}
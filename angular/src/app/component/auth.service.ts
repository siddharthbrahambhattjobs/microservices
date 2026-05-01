import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
// Declare the global google object injected by the GSI script
declare var google: any;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private readonly GOOGLE_CLIENT_ID = '674419636360-losvfqen7mvsk4mrdauviu1vn8ku2m44.apps.googleusercontent.com';
  private jwtTokenSubject = new BehaviorSubject<string | null>(null);
  public jwtToken$ = this.jwtTokenSubject.asObservable();
  
  private router = inject(Router);
  
  // Inject the PLATFORM_ID to check where the code is running
  private platformId = inject(PLATFORM_ID); 

  constructor() { }

  private loadGoogleScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      // Ensure we only try to load the script if we are in the browser
      if (!isPlatformBrowser(this.platformId)) {
        resolve();
        return;
      }

      if (typeof google !== 'undefined') {
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject('Failed to load Google Identity script');
      
      document.head.appendChild(script);
    });
  }

  public async initializeGoogleAuth(buttonElementId: string): Promise<void> {
    // Prevent execution on the server
    if (!isPlatformBrowser(this.platformId)) return;

    try {
      await this.loadGoogleScript();

      google.accounts.id.initialize({
        client_id: this.GOOGLE_CLIENT_ID,
        callback: this.handleCredentialResponse.bind(this) 
      });

      google.accounts.id.renderButton(
        document.getElementById(buttonElementId),
        { theme: 'outline', size: 'large', width: 250 }
      );

      google.accounts.id.prompt(); 
    } catch (error) {
      console.error(error);
    }
  }

  private handleCredentialResponse(response: any): void {
    const jwt = response.credential;
    console.log('JWT Token received from Google:', jwt);
    
    this.jwtTokenSubject.next(jwt);
    
    // Safely use localStorage
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('google_jwt', jwt);
    }
    
    this.router.navigate(['/dashboard']); 
  }

  public getToken(): string | null {
    // Return memory subject first if it exists
    if (this.jwtTokenSubject.value) {
      return this.jwtTokenSubject.value;
    }
    
    // Safely check localStorage only if in the browser
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('google_jwt');
    }
    
    return null; // Return null if on the server
  }

  public logout(): void {
    this.jwtTokenSubject.next(null);
    
    // Safely use localStorage
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('google_jwt');
    }
    
    this.router.navigate(['/login']);
  }
}
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';

export const AuthInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return next(req);
  }

  const token = sessionStorage.getItem('jwt_token');

  if (token) {
    console.log('AuthInterceptor URL:', req.url);
    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    return next(authReq);
  }

  console.warn('No token in sessionStorage for URL:', req.url);
  return next(req);
};
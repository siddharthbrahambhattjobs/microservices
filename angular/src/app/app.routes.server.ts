import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // ✅ FIX 1: auth-protected pages MUST be Client-only
  //    They depend on sessionStorage which does not exist on the server.
  //    Prerender would fire HTTP calls without a token → 401.
  { path: 'dashboard',     renderMode: RenderMode.Client },
  { path: 'auth-callback', renderMode: RenderMode.Client },

  // Login page can be prerendered — no auth needed
  { path: 'login',         renderMode: RenderMode.Prerender },

  // Everything else client-side for safety
  { path: '**',            renderMode: RenderMode.Client }
];
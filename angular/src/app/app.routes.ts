import { Routes } from '@angular/router';
import { authGuard } from './component/guard/auth.guard';
import { LoginComponent } from './component/login.component/login.component';
import { CallToApiGateway } from './component/call-to-api-gateway';
import { Search } from './component/search/search';
import { AuthCallbackComponent } from './component/auth-callback.component';

export const routes: Routes = [
    {
        path: 'login',
        component: LoginComponent
    },
    {
        path: 'call-api-gateway',
        component: CallToApiGateway,
    },
    {
        path: 'search',
        component: Search,
    },
    {
        path: 'dashboard',
        loadComponent: () => import('./component/dashboard/dashboard').then(m => m.Dashboard),
        canActivate: [authGuard] // Protect the dashboard route as well
    },
    {
        path: '',
        redirectTo: '/login',
        pathMatch: 'full' // Default route redirects to login
    },
    {
        path: 'auth-callback',
        component: AuthCallbackComponent
    },
    {
        path: '**',
        redirectTo: '/login' // Wildcard route catches invalid URLs
    }
];

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  // ✅ FIX: Removed AfterViewInit + initializeGoogleAuth() call.
  //    The old code called initializeGoogleAuth() in AfterViewInit which
  //    immediately redirected to Google before the user clicked anything.
  //    Now login is purely driven by the (click) binding in the template.
  public authService = inject(AuthService);
}
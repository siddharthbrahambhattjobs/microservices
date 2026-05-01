import { Component, AfterViewInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss' ,
})
export class LoginComponent implements AfterViewInit {
  public authService = inject(AuthService);

  ngAfterViewInit(): void {
    // We call this in AfterViewInit to ensure the #google-btn div exists in the DOM
    this.authService.initializeGoogleAuth('google-btn');
  }
}
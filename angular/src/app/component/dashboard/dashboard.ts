import { Component, inject } from '@angular/core';
import { CallToApiGateway } from '../call-to-api-gateway';
import { AuthService } from '../auth.service';
import { CommonModule, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-dashboard',
  imports: [JsonPipe, CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  public authService = inject(AuthService);
  private dataService = inject(CallToApiGateway);

  public apiResponse: any = null;
  public errorMessage: string = '';

  public studentName: string = '';
  public studentCourse: string = '';

  public fetchData(): void {
  this.apiResponse = null;
  this.errorMessage = '';

  this.dataService.getSecureData().subscribe({
    next: (response) => {
      console.log('Data received successfully', response);
      this.apiResponse = response;
    },
    error: (error) => {
      console.error('API call failed', error);
      this.errorMessage = error.status === 401
        ? 'Unauthorized: Your Google token is missing, invalid, or expired.'
        : `An error occurred: ${error.message}`;
      }
    });
  }  
 
 public createStudent(): void {
    // Basic validation to ensure fields aren't empty
    if (!this.studentName.trim() || !this.studentCourse.trim()) {
      this.errorMessage = 'Please enter both a name and a course.';
      return;
    }

    this.apiResponse = null;
    this.errorMessage = '';

    // Create the dynamic payload
    const newStudent = {
      name: this.studentName,
      course: this.studentCourse
    };

    // Pass the payload to your updated service
    this.dataService.createStudent(newStudent).subscribe({
      next: (response) => {
        console.log('Student created successfully', response);
        this.apiResponse = response;
        // Clear the form after success
        this.studentName = '';
        this.studentCourse = '';
      },
      error: (error) => {
        console.error('API call failed', error);
        this.errorMessage = error.status === 401 
          ? 'Unauthorized: Your Google token is missing, invalid, or expired.' 
          : `An error occurred: ${error.message}`;
      }
    });
  }

  public getAllStudents(): void {
    // Reset state before fetching
    this.apiResponse = null;
    this.errorMessage = '';

    this.dataService.getAllStudents().subscribe({
      next: (response) => {
        console.log('Data received successfully', response);
        this.apiResponse = response;
      },
      error: (error) => {
        console.error('API call failed', error);
        // If the gateway rejects the token, you will likely get a 401 Unauthorized here
        this.errorMessage = error.status === 401 
          ? 'Unauthorized: Your Google token is missing, invalid, or expired.' 
          : `An error occurred: ${error.message}`;
      }
    });
  }
}
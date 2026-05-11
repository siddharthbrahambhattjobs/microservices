// src/app/component/dashboard/dashboard.ts
import { Component, inject, OnInit } from '@angular/core';
import { CallToApiGateway } from '../call-to-api-gateway';
import { AuthService } from '../auth.service';
import { CommonModule, JsonPipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [JsonPipe, CommonModule, FormsModule, ReactiveFormsModule], // Added ReactiveFormsModule
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
    public authService = inject(AuthService);
    private dataService = inject(CallToApiGateway);

    public apiResponse: any = null;
    public errorMessage: string = '';
    public studentName: string = '';
    public studentCourse: string = '';

    // --- Search Properties ---
    public searchControl = new FormControl('', { nonNullable: true });
    public filteredStudents: any[] = [];
    public isSearching = false;

    ngOnInit(): void {
        this.setupSearch();
    }

    private setupSearch(): void {
        this.searchControl.valueChanges.pipe(
            debounceTime(500), // Waits 500ms after the last keystroke
            distinctUntilChanged(), // Only triggers if the input actually changed
            switchMap(searchTerm => {
                this.errorMessage = '';

                // If search is cleared, reset results and skip the API call
                if (!searchTerm.trim()) {
                    this.filteredStudents = [];
                    return of([]);
                }

                this.isSearching = true;

                // Call the backend and filter the payload
                return this.dataService.getAllStudents().pipe(
                    map(students => {
                        const term = searchTerm.toLowerCase();
                        return students.filter(student =>
                            student.name?.toLowerCase().includes(term) ||
                            student.course?.toLowerCase().includes(term)
                        );
                    }),
                    catchError(error => {
                        console.error('Search API call failed', error);
                        this.errorMessage = error.status === 401
                            ? 'Unauthorized: Your Google token is missing, invalid, or expired.'
                            : `Search failed: ${error.message}`;
                        return of([]); // Return empty array to keep the observable stream alive
                    })
                );
            })
        ).subscribe(results => {
            this.filteredStudents = results;
            this.isSearching = false;
        });
    }

    // Store the key so retries use the SAME key (idempotency guarantee)
    private pendingIdempotencyKey: string | null = null;

    public createStudent(): void {
        if (!this.studentName.trim() || !this.studentCourse.trim()) {
            this.errorMessage = 'Please enter both a name and a course.';
            return;
        }

        this.apiResponse = null;
        this.errorMessage = '';

        // Reuse key if retrying same request, generate new one for a fresh submission
        if (!this.pendingIdempotencyKey) {
            this.pendingIdempotencyKey = crypto.randomUUID();
        }

        const newStudent = { name: this.studentName, course: this.studentCourse };

        this.dataService.createStudent(newStudent, this.pendingIdempotencyKey).subscribe({
            next: (response) => {
                console.log('Student created successfully:', response);
                this.apiResponse = response;
                this.studentName = '';
                this.studentCourse = '';
                this.pendingIdempotencyKey = null; // ← Clear after success
            },
            error: (error) => {
                console.error('API call failed:', error);
                // Do NOT clear pendingIdempotencyKey on error — keep it for retry
                this.errorMessage = error.status === 401
                    ? 'Unauthorized — Your Google token is missing, invalid, or expired.'
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
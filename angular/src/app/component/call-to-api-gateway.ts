import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

interface Student {
  id?: number;
  name: string;
  course: string;
  status?: string;
}

@Injectable({
  providedIn: 'root',
})
export class CallToApiGateway {

  private http = inject(HttpClient);

  private readonly GATEWAY_URL = 'http://localhost:8151/caller/';

  public getSecureData(): Observable<string> {
    return this.http.get(this.GATEWAY_URL + 'angular', { responseType: 'text' });
  }

  public getAllStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(this.GATEWAY_URL + 'getAllStudent');
  }

  public createStudent(studentData: { name: string; course: string }): Observable<string> {
    return this.http.post(
      this.GATEWAY_URL + 'create',
      studentData,
      { responseType: 'text' }
    );
  }
}
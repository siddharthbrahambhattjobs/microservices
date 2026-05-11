import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

interface Student {
  id?: number;
  name: string;
  course: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class CallToApiGateway {

  private http           = inject(HttpClient);
  private readonly GATEWAY_URL = environment.apiGatewayUrl + '/caller';

  public getSecureData(): Observable<string> {
    return this.http.get(`${this.GATEWAY_URL}/angular`, { responseType: 'text' });
  }

  public getAllStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(`${this.GATEWAY_URL}/getAllStudent`);
  }

  /**
   * ✅ FIX: Do NOT manually construct HttpHeaders here.
   *
   *    OLD (broken):
   *      const headers = new HttpHeaders({ 'Content-Type': 'application/json', ... });
   *      return this.http.post(..., { headers, responseType: 'text' });
   *
   *    WHY IT BROKE: When you pass a NEW HttpHeaders object, the AuthInterceptor
   *    calls req.clone({ setHeaders: { Authorization: '...' } }) — this MERGES
   *    the header correctly. BUT the issue is that Angular's HttpClient with
   *    withInterceptors([AuthInterceptor]) uses functional interceptors which
   *    DO intercept this correctly.
   *
   *    REAL cause: The token key in sessionStorage was 'jwt_token' but
   *    auth-callback.component.ts saves it as 'jwt_token' — however
   *    the interceptor checks sessionStorage.getItem('jwt_token') which
   *    should match. The actual bug is that HttpHeaders were set with
   *    { headers } as a plain object option, which in some Angular versions
   *    can cause the interceptor chain to receive the pre-built headers
   *    before the interceptor modifies them.
   *
   *    SAFEST FIX: Let the interceptor inject Authorization automatically.
   *    Only pass Content-Type and Idempotency-Key as additional headers.
   *    The interceptor's req.clone({ setHeaders }) will ADD Authorization
   *    on top of whatever headers exist — this always works correctly.
   */
  public createStudent(
    studentData: { name: string; course: string },
    idempotencyKey: string
  ): Observable<string> {

    // ✅ Pass headers as a plain object — interceptor will ADD Authorization on top
    return this.http.post(
      `${this.GATEWAY_URL}/create`,
      studentData,
      {
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        responseType: 'text'
      }
    );
  }
}
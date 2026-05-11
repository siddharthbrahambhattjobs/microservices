// src/app/component/idempotency.service.ts
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class IdempotencyService {

  private platformId = inject(PLATFORM_ID);

  // In-memory store: operationKey → idempotency UUID
  // e.g. 'createStudent' → 'uuid-xxxx'
  private pendingKeys = new Map<string, string>();

  /**
   * Generate a new UUID v4 idempotency key for an operation.
   * Call this ONCE before submitting a form, store the result,
   * and reuse it on retries.
   */
  generateKey(): string {
    if (isPlatformBrowser(this.platformId) && crypto?.randomUUID) {
      return crypto.randomUUID();
    }
    // Fallback for environments without crypto.randomUUID
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
  }

  /**
   * Get or create a key for the given operation name.
   * Same key is returned until you explicitly clear it.
   * This ensures retries on network failure use the same key.
   */
  getOrCreateKey(operationName: string): string {
    if (!this.pendingKeys.has(operationName)) {
      this.pendingKeys.set(operationName, this.generateKey());
    }
    return this.pendingKeys.get(operationName)!;
  }

  /**
   * Call this AFTER a successful response (2xx).
   * Clears the key so the next submission gets a fresh key.
   */
  clearKey(operationName: string): void {
    this.pendingKeys.delete(operationName);
  }

  /**
   * Call this when a user edits the form (not a retry).
   * Forces a new key for the next submission.
   */
  resetKey(operationName: string): void {
    this.clearKey(operationName);
  }
}
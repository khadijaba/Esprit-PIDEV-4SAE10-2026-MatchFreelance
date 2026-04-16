import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface ToxicityResult {
  isToxic: boolean;
  confidence: number;
  reason: string;
  detectedWords: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ToxicityApiService {
  private apiUrl = 'http://localhost:8082/api/toxicity';

  constructor(private http: HttpClient) {}

  /**
   * Check if text contains toxic content using backend API
   * @param text Text to check
   * @returns Promise<boolean> true if toxic, false if safe
   */
  async checkToxicity(text: string): Promise<boolean> {
    if (!text || text.trim().length === 0) {
      return false;
    }

    try {
      console.log('🔍 Checking toxicity via API:', text.substring(0, 50));
      
      const result = await firstValueFrom(
        this.http.post<ToxicityResult>(
          `${this.apiUrl}/check`,
          { text }
        )
      );

      console.log('✅ Toxicity check result:', result);

      if (result.isToxic) {
        console.warn('⚠️ Toxic content detected!', {
          confidence: result.confidence,
          reason: result.reason,
          words: result.detectedWords
        });
      }

      return result.isToxic;
    } catch (error) {
      console.error('❌ Toxicity check failed:', error);
      // Fallback: allow content if API fails (don't block users)
      return false;
    }
  }

  /**
   * Get detailed toxicity analysis
   * @param text Text to analyze
   * @returns Promise<ToxicityResult> Detailed analysis
   */
  async analyzeToxicity(text: string): Promise<ToxicityResult> {
    if (!text || text.trim().length === 0) {
      return {
        isToxic: false,
        confidence: 0,
        reason: 'No text provided',
        detectedWords: []
      };
    }

    try {
      const result = await firstValueFrom(
        this.http.post<ToxicityResult>(
          `${this.apiUrl}/check`,
          { text }
        )
      );

      return result;
    } catch (error) {
      console.error('❌ Toxicity analysis failed:', error);
      return {
        isToxic: false,
        confidence: 0,
        reason: 'Analysis failed',
        detectedWords: []
      };
    }
  }
}

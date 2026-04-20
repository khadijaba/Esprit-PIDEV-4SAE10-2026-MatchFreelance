import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, timeout, catchError } from 'rxjs';
import { of } from 'rxjs';
import type { PitchJob, PitchAnalysisResult } from './pitch-analyzer.service';

const OLLAMA_URL = 'http://localhost:11434/api/generate';
const MODEL = 'llama3.2:1b';
const TIMEOUT_MS = 30_000;

interface OllamaResponse {
  response: string;
  done: boolean;
}

@Injectable({ providedIn: 'root' })
export class OllamaService {
  constructor(private http: HttpClient) {}

  async analyzePitch(job: PitchJob, pitch: string): Promise<PitchAnalysisResult | null> {
    const prompt = this.buildPrompt(job, pitch);
    try {
      const res = await firstValueFrom(
        this.http
          .post<OllamaResponse>(OLLAMA_URL, {
            model: MODEL,
            prompt,
            stream: false,
            options: { temperature: 0.2, num_predict: 800 },
          })
          .pipe(
            timeout(TIMEOUT_MS),
            catchError(() => of(null))
          )
      );
      if (!res?.response) return null;
      return this.parseResponse(res.response, pitch);
    } catch {
      return null;
    }
  }

  private buildPrompt(job: PitchJob, pitch: string): string {
    return `You are a freelance hiring expert. Analyze this pitch for the job below and respond ONLY with valid JSON.

JOB:
Title: ${job.title}
Skills required: ${job.skills.join(', ')}
Description: ${job.description.slice(0, 400)}

PITCH:
${pitch.slice(0, 800)}

Respond with this exact JSON structure (no markdown, no extra text):
{
  "overallScore": <0-100>,
  "verdict": "<one sentence>",
  "hiringLikelihood": "<Very High|High|Medium|Low>",
  "toneAnalysis": "<Professional|Balanced|Underconfident|Overconfident|Casual>",
  "skillsMatched": ["<skill>"],
  "skillsMissing": ["<skill>"],
  "keyMatches": ["<strength>"],
  "keyGaps": ["<gap>"],
  "improvements": ["<actionable tip>"],
  "dimensions": [
    {"name":"Technical Relevance","score":<0-100>,"explanation":"<text>","weight":28},
    {"name":"Problem Understanding","score":<0-100>,"explanation":"<text>","weight":22},
    {"name":"Experience Signals","score":<0-100>,"explanation":"<text>","weight":20},
    {"name":"Communication Quality","score":<0-100>,"explanation":"<text>","weight":15},
    {"name":"Value Proposition","score":<0-100>,"explanation":"<text>","weight":10}
  ]
}`;
  }

  private parseResponse(raw: string, pitch: string): PitchAnalysisResult | null {
    try {
      // Extract JSON block if wrapped in markdown
      const jsonMatch = raw.match(/\{[\s\S]*\}/);
      if (!jsonMatch) return null;
      const parsed = JSON.parse(jsonMatch[0]);

      const score = Math.max(0, Math.min(100, Number(parsed.overallScore) || 0));
      const wordCount = (pitch || '').trim().split(/\s+/).filter(Boolean).length;

      return {
        overallScore: score,
        verdict: parsed.verdict || '',
        hiringLikelihood: parsed.hiringLikelihood || 'Medium',
        toneAnalysis: parsed.toneAnalysis || 'Balanced',
        skillsMatched: Array.isArray(parsed.skillsMatched) ? parsed.skillsMatched : [],
        skillsMissing: Array.isArray(parsed.skillsMissing) ? parsed.skillsMissing : [],
        keyMatches: Array.isArray(parsed.keyMatches) ? parsed.keyMatches : [],
        keyGaps: Array.isArray(parsed.keyGaps) ? parsed.keyGaps : [],
        improvements: Array.isArray(parsed.improvements) ? parsed.improvements : [],
        improvement: Array.isArray(parsed.improvements) ? parsed.improvements.join(' ') : '',
        dimensions: Array.isArray(parsed.dimensions) ? parsed.dimensions : [],
        redFlags: undefined,
        recommendedBid: undefined,
        winProbability: undefined,
        wordCount,
      };
    } catch {
      return null;
    }
  }
}

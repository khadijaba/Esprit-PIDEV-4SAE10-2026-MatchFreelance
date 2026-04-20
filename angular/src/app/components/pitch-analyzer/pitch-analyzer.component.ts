import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  PITCH_JOBS,
  analyzePitch,
  type PitchJob,
  type PitchAnalysisResult,
} from '../../services/pitch-analyzer.service';

@Component({
  selector: 'app-pitch-analyzer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pitch-analyzer.component.html',
})
export class PitchAnalyzerComponent {
  jobs = PITCH_JOBS;
  selectedJob = signal<PitchJob>(PITCH_JOBS[0]);
  pitch = signal('');
  loading = signal(false);
  result = signal<PitchAnalysisResult | null>(null);
  error = signal<string | null>(null);

  charCount = computed(() => this.pitch().length);

  selectJob(job: PitchJob) {
    this.selectedJob.set(job);
    this.result.set(null);
  }

  onPitchInput(value: string) {
    this.pitch.set(value);
    this.error.set(null);
  }

  analyze() {
    const p = this.pitch().trim();
    if (p.length < 50) {
      this.error.set('Please write at least 50 characters for a meaningful analysis.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.result.set(null);
    setTimeout(() => {
      try {
        const res = analyzePitch(this.selectedJob(), p);
        this.result.set(res);
      } catch (e) {
        this.error.set('Analysis failed. Please try again.');
      } finally {
        this.loading.set(false);
      }
    }, 600);
  }

  scoreColor(score: number): string {
    return score >= 80 ? '#6ab04c' : score >= 60 ? '#48cfad' : score >= 40 ? '#f9ca24' : '#e74c3c';
  }

  scoreLabel(score: number): string {
    return score >= 80 ? 'Excellent Match' : score >= 60 ? 'Good Match' : score >= 40 ? 'Partial Match' : 'Weak Match';
  }

  fmt(n: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'TND', maximumFractionDigits: 0 }).format(n);
  }
}

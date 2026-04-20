import { Component, signal, computed, input, output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

export interface PreviewData {
  previewId: number;
  contractId: number;
  htmlUrl: string;
  generatedAt: string;
  version: number;
  status: string;
  designStyle: string;
  featuresCount: number;
}

@Component({
  selector: 'app-preview-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './preview-modal.component.html',
  styleUrls: ['./preview-modal.component.css']
})
export class PreviewModalComponent implements OnInit {
  preview = input.required<PreviewData>();
  onClose = output<void>();

  activeTab = signal<'desktop' | 'mobile' | 'code'>('desktop');
  htmlContent = signal('');
  loading = signal(true);
  safeHtml = signal<any>(null);

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit() {
    // Load HTML content after component and inputs are initialized
    this.loadHtmlContent();
  }

  close() {
    this.onClose.emit();
  }

  async loadHtmlContent() {
    this.loading.set(true);
    try {
      const response = await fetch(`http://localhost:8081${this.preview().htmlUrl}`);
      const html = await response.text();
      this.htmlContent.set(html);
      // For desktop and mobile views, sanitize and trust the HTML
      this.safeHtml.set(this.sanitizer.bypassSecurityTrustHtml(html));
    } catch (error) {
      console.error('Failed to load HTML:', error);
    } finally {
      this.loading.set(false);
    }
  }

  setTab(tab: 'desktop' | 'mobile' | 'code') {
    this.activeTab.set(tab);
  }
}

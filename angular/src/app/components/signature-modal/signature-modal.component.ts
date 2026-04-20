import { Component, EventEmitter, Input, Output, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import SignaturePad from 'signature_pad';

@Component({
  selector: 'app-signature-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './signature-modal.component.html',
})
export class SignatureModalComponent implements AfterViewInit, OnChanges {
  @Input() show = false;
  @Input() signerName = '';
  @Output() signed = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();

  @ViewChild('signatureCanvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;
  
  private signaturePad: SignaturePad | null = null;
  hasSignature = false;

  ngAfterViewInit() {
    if (this.show && this.canvasRef) {
      setTimeout(() => this.initCanvas(), 0);
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['show'] && this.show) {
      setTimeout(() => {
        if (this.canvasRef?.nativeElement) {
          this.initCanvas();
        }
      }, 150);
    }
  }

  private initCanvas() {
    if (!this.canvasRef?.nativeElement) {
      return;
    }
    
    const canvas = this.canvasRef.nativeElement;
    
    // Set canvas size to match display size
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width;
    canvas.height = rect.height;
    
    // Initialize signature pad
    this.signaturePad = new SignaturePad(canvas, {
      backgroundColor: 'rgb(255, 255, 255)',
      penColor: 'rgb(30, 41, 59)',
      minWidth: 1,
      maxWidth: 3,
    });
    
    // Track when user draws
    this.signaturePad.addEventListener('endStroke', () => {
      this.hasSignature = !this.signaturePad!.isEmpty();
    });
    
    this.hasSignature = false;
  }

  clearCanvas() {
    if (this.signaturePad) {
      this.signaturePad.clear();
      this.hasSignature = false;
    }
  }

  onSign() {
    if (!this.hasSignature || !this.signaturePad) return;
    
    // Convert canvas to base64 PNG
    const signatureData = this.signaturePad.toDataURL('image/png');
    this.signed.emit(signatureData);
    this.clearCanvas();
  }

  onCancel() {
    this.clearCanvas();
    this.cancelled.emit();
  }
}

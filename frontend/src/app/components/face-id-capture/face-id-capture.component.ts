import {
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FaceApiLoaderService } from '../../services/face-api-loader.service';

@Component({
  selector: 'app-face-id-capture',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './face-id-capture.component.html',
})
export class FaceIdCaptureComponent implements OnDestroy {
  @ViewChild('videoEl') videoRef?: ElementRef<HTMLVideoElement>;

  @Output() descriptorReady = new EventEmitter<string>();
  @Output() error = new EventEmitter<string>();

  cameraOn = false;
  busy = false;
  status = '';

  private stream: MediaStream | null = null;

  constructor(private faceApi: FaceApiLoaderService) {}

  ngOnDestroy(): void {
    this.stopCamera();
  }

  async startCamera(): Promise<void> {
    this.status = 'Loading face models…';
    this.busy = true;
    this.error.emit('');
    try {
      await this.faceApi.ensureModelsLoaded();
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } },
        audio: false,
      });
      const v = this.videoRef?.nativeElement;
      if (!v) {
        throw new Error('Video element not ready');
      }
      v.srcObject = this.stream;
      v.muted = true;
      v.playsInline = true;
      await v.play();
      this.cameraOn = true;
      this.status = 'Position your face in the frame, then tap Capture.';
    } catch (e) {
      const msg =
        e instanceof Error ? e.message : 'Camera or models unavailable (HTTPS may be required).';
      this.status = '';
      this.error.emit(msg);
    } finally {
      this.busy = false;
    }
  }

  stopCamera(): void {
    this.stream?.getTracks().forEach((t) => t.stop());
    this.stream = null;
    const v = this.videoRef?.nativeElement;
    if (v) {
      v.srcObject = null;
    }
    this.cameraOn = false;
    this.status = '';
  }

  async capture(): Promise<void> {
    const v = this.videoRef?.nativeElement;
    if (!v || !this.cameraOn) {
      return;
    }
    this.busy = true;
    this.status = 'Analyzing face…';
    this.error.emit('');
    try {
      const csv = await this.faceApi.descriptorFromVideo(v);
      if (!csv) {
        this.error.emit('No face detected — try better lighting or move closer.');
        this.status = '';
        return;
      }
      this.descriptorReady.emit(csv);
      this.status = 'Face captured. You can stop the camera.';
    } catch (e) {
      this.error.emit(e instanceof Error ? e.message : 'Face analysis failed.');
      this.status = '';
    } finally {
      this.busy = false;
    }
  }
}

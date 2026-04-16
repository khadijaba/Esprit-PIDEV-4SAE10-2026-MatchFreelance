import { Injectable } from '@angular/core';
import * as faceapi from 'face-api.js';

@Injectable({
  providedIn: 'root'
})
export class FaceRecognitionService {
  private modelsLoaded = false;

  constructor() { }

  async loadModels(): Promise<void> {
    if (this.modelsLoaded) return;
    // Using absolute URL may help in some Angular configurations
    const modelUrl = '/assets/models';
    try {
      await Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri(modelUrl),
        faceapi.nets.faceLandmark68Net.loadFromUri(modelUrl),
        faceapi.nets.faceRecognitionNet.loadFromUri(modelUrl)
      ]);
      this.modelsLoaded = true;
      console.log('Face Recognition models loaded');
    } catch (error) {
      console.error('Failed to load Face Recognition models:', error);
      throw error;
    }
  }

  async startCamera(videoElement: HTMLVideoElement): Promise<void> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      videoElement.srcObject = stream;
    } catch (err) {
      console.error('Error accessing webcam', err);
      throw new Error('Webcam access denied or unavailable.');
    }
  }

  stopCamera(videoElement: HTMLVideoElement): void {
    const stream = videoElement.srcObject as MediaStream;
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      videoElement.srcObject = null;
    }
  }

  async detectFaceDescriptor(videoElement: HTMLVideoElement): Promise<string | null> {
    try {
      if (videoElement.videoWidth > 0) {
        videoElement.width = videoElement.videoWidth;
        videoElement.height = videoElement.videoHeight;
      }
      console.log('Running detection on resolution:', videoElement.width, 'x', videoElement.height);

      const detection = await faceapi.detectSingleFace(videoElement, new faceapi.TinyFaceDetectorOptions())
        .withFaceLandmarks()
        .withFaceDescriptor();

      if (!detection) return null;

      // Convert Float32Array to a standard comma-separated string
      const descriptorArray = Array.from(detection.descriptor);
      return descriptorArray.join(',');
    } catch (error) {
      console.error('FaceAPI Exception:', error);
      throw error;
    }
  }
}

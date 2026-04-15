import { Injectable } from '@angular/core';
import * as faceapi from 'face-api.js';

const MODEL_BASE = 'https://justadudewhohacks.github.io/face-api.js/models';

@Injectable({ providedIn: 'root' })
export class FaceApiLoaderService {
  private loaded = false;

  async ensureModelsLoaded(): Promise<void> {
    if (this.loaded) {
      return;
    }
    await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_BASE);
    await faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_BASE);
    await faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_BASE);
    this.loaded = true;
  }

  /**
   * Extrait un descripteur 128D (chaîne CSV) compatible avec le backend PIDEV.
   */
  async descriptorFromVideo(video: HTMLVideoElement): Promise<string | null> {
    await this.ensureModelsLoaded();
    const result = await faceapi
      .detectSingleFace(
        video,
        new faceapi.TinyFaceDetectorOptions({ inputSize: 416, scoreThreshold: 0.45 })
      )
      .withFaceLandmarks()
      .withFaceDescriptor();
    if (!result) {
      return null;
    }
    return Array.from(result.descriptor).join(',');
  }
}

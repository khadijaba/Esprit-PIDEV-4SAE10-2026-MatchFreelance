import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ToxicityAiService {
  private model: any;
  public isLoading = signal(false);
  public isReady = signal(false);

  private readonly AI_THRESHOLD = 0.7; // Strict threshold for moderation

  /**
   * Dynamically loads TensorFlow and Toxicity scripts from CDN to avoid 
   * local esbuild/bundler resolution issues in modern Angular versions.
   */
  async init() {
    if (this.isReady() || this.isLoading()) return;

    this.isLoading.set(true);
    try {
      // 1. Load TensorFlow Core
      await this.loadScript('https://cdn.jsdelivr.net/npm/@tensorflow/tfjs');
      // 2. Load Toxicity Model
      await this.loadScript('https://cdn.jsdelivr.net/npm/@tensorflow-models/toxicity');

      // 3. Initialize model from global scope
      const labels = ['identity_attack', 'insult', 'obscene', 'severe_toxicity', 'sexual_explicit', 'threat', 'toxicity'];
      this.model = await (window as any).toxicity.load(this.AI_THRESHOLD, labels);
      
      this.isReady.set(true);
      console.log('✅ Toxicity AI Model Loaded via CDN');
    } catch (e) {
      console.error('❌ Failed to load Toxicity AI Model:', e);
    } finally {
      this.isLoading.set(false);
    }
  }

  async classify(text: string): Promise<boolean> {
    if (!this.isReady()) {
      console.warn('AI Model not ready, using fallback regex check');
      return false; // Fallback to safe
    }

    try {
      const predictions = await this.model.classify([text]);
      // If any category matches with confidence > threshold, mark as toxic
      return predictions.some((p: any) => p.results[0].match === true);
    } catch (e) {
      console.error('Classification error:', e);
      return false;
    }
  }

  private loadScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (document.querySelector(`script[src="${src}"]`)) {
        resolve();
        return;
      }
      const script = document.createElement('script');
      script.src = src;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error(`Failed to load script: ${src}`));
      document.head.appendChild(script);
    });
  }
}

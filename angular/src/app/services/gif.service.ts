import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface GifResult {
  id: string;
  title: string;
  url: string;
  previewUrl: string;
  width: number;
  height: number;
}

@Injectable({
  providedIn: 'root'
})
export class GifService {
  // Giphy API - Get your own key from https://developers.giphy.com/
  private readonly GIPHY_API_KEY = 'YOUR_GIPHY_API_KEY_HERE'; // Replace with actual key
  private readonly GIPHY_API_URL = 'https://api.giphy.com/v1/gifs';

  constructor(private http: HttpClient) {}

  /**
   * Search GIFs
   */
  searchGifs(query: string, limit: number = 20): Observable<GifResult[]> {
    const url = `${this.GIPHY_API_URL}/search`;
    const params = {
      api_key: this.GIPHY_API_KEY,
      q: query,
      limit: limit.toString(),
      rating: 'g' // Family-friendly content
    };

    return this.http.get<any>(url, { params }).pipe(
      map(response => this.mapGiphyResponse(response))
    );
  }

  /**
   * Get trending GIFs
   */
  getTrendingGifs(limit: number = 20): Observable<GifResult[]> {
    const url = `${this.GIPHY_API_URL}/trending`;
    const params = {
      api_key: this.GIPHY_API_KEY,
      limit: limit.toString(),
      rating: 'g'
    };

    return this.http.get<any>(url, { params }).pipe(
      map(response => this.mapGiphyResponse(response))
    );
  }

  /**
   * Map Giphy API response to our GifResult interface
   */
  private mapGiphyResponse(response: any): GifResult[] {
    if (!response || !response.data) {
      return [];
    }

    return response.data.map((gif: any) => ({
      id: gif.id,
      title: gif.title,
      url: gif.images.original.url,
      previewUrl: gif.images.fixed_height_small.url,
      width: parseInt(gif.images.original.width),
      height: parseInt(gif.images.original.height)
    }));
  }

  /**
   * Get GIF by ID
   */
  getGifById(id: string): Observable<GifResult> {
    const url = `${this.GIPHY_API_URL}/${id}`;
    const params = {
      api_key: this.GIPHY_API_KEY
    };

    return this.http.get<any>(url, { params }).pipe(
      map(response => {
        const gif = response.data;
        return {
          id: gif.id,
          title: gif.title,
          url: gif.images.original.url,
          previewUrl: gif.images.fixed_height_small.url,
          width: parseInt(gif.images.original.width),
          height: parseInt(gif.images.original.height)
        };
      })
    );
  }
}

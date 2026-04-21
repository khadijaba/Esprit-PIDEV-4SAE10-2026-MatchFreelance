import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { InterviewService } from './interview.service';

describe('InterviewService', () => {
  let service: InterviewService;
  let http: {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    put: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    http = {
      get: vi.fn(() => of({})),
      post: vi.fn(() => of({})),
      put: vi.fn(() => of({})),
      delete: vi.fn(() => of({})),
    };

    service = new InterviewService(http as any);
  });

  it('searchInterviews should ignore empty params', () => {
    service.searchInterviews({ freelancerId: 7, page: 1, size: 10, status: undefined, from: '' });

    const [url, options] = http.get.mock.calls[0] as [string, { params: any }];
    expect(url).toBe('/api/interviews');
    expect(options.params.get('freelancerId')).toBe('7');
    expect(options.params.get('page')).toBe('1');
    expect(options.params.get('size')).toBe('10');
    expect(options.params.has('status')).toBe(false);
    expect(options.params.has('from')).toBe(false);
  });

  it('createReview should send reviewerId as query param', () => {
    const payload = { revieweeId: 9, score: 5, comment: 'Great interview' };
    service.createReview(21, 3, payload);

    const [url, body, options] = http.post.mock.calls[0] as [string, object, { params: any }];
    expect(url).toBe('/api/interviews/21/reviews');
    expect(body).toEqual(payload);
    expect(options.params.get('reviewerId')).toBe('3');
  });

  it('downloadNotCompletedInterviewsExcel should forward optional filters', () => {
    service.downloadNotCompletedInterviewsExcel({ ownerId: 8, freelancerId: 11 });

    const [url, options] = http.get.mock.calls[0] as [string, { params: any; responseType: string }];
    expect(url).toBe('/api/interviews/export/excel');
    expect(options.params.get('ownerId')).toBe('8');
    expect(options.params.get('freelancerId')).toBe('11');
    expect(options.responseType).toBe('blob');
  });
});


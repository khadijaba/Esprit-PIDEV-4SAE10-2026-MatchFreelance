import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductivityService } from './productivity.service';

describe('ProductivityService', () => {
  let service: ProductivityService;
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

    service = new ProductivityService(http as any);
  });

  it('builds paged task query params correctly', () => {
    service.listTasksPage(5, {
      page: 0,
      size: 8,
      q: 'api',
      status: 'TODO',
      priority: 'HIGH',
      dueFrom: '2026-04-10T00:00:00Z',
    });

    const [url, options] = http.get.mock.calls[0] as [string, { params: any }];
    expect(url).toBe('/api/productivity/owners/5/tasks/page');
    expect(options.params.get('page')).toBe('0');
    expect(options.params.get('size')).toBe('8');
    expect(options.params.get('q')).toBe('api');
    expect(options.params.get('status')).toBe('TODO');
    expect(options.params.get('priority')).toBe('HIGH');
    expect(options.params.get('dueFrom')).toBe('2026-04-10T00:00:00Z');
    expect(options.params.has('dueTo')).toBe(false);
  });

  it('posts decomposition payload with default maxSteps', () => {
    service.decomposeGoal(5, 'Launch portfolio website');

    const [url, body] = http.post.mock.calls[0] as [string, { goalText: string; maxSteps: number }];
    expect(url).toBe('/api/productivity/owners/5/ai/decompose');
    expect(body).toEqual({ goalText: 'Launch portfolio website', maxSteps: 6 });
  });

  it('posts empty body for toggle todo item endpoint', () => {
    service.toggleTodoItem(44);

    const [url, body] = http.post.mock.calls[0] as [string, object];
    expect(url).toBe('/api/productivity/todo-items/44/toggle');
    expect(body).toEqual({});
  });
});


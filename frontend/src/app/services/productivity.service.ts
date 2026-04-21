import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdaptiveRescheduleResponse,
  AiDecomposeResponse,
  ConflictResolution,
  ConflictResolutionRequest,
  ContextSuggestion,
  DecisionLog,
  DecisionLogCreateRequest,
  Dependency,
  DependencyCreateRequest,
  Goal,
  GoalCreateRequest,
  PageResponse,
  ProductivityInsights,
  ProductivityPriority,
  ProductivityTask,
  ProductivityTaskStatus,
  ProgressSummary,
  TaskCreateRequest,
  TaskOrder,
  TaskUpdateRequest,
  TodoItem,
  TodoList,
  WeeklyReview,
} from '../models/productivity.model';

@Injectable({ providedIn: 'root' })
export class ProductivityService {
  private readonly api = '/api/productivity';

  constructor(private readonly http: HttpClient) {}

  listTasks(ownerId: number): Observable<ProductivityTask[]> {
    return this.http.get<ProductivityTask[]>(`${this.api}/owners/${ownerId}/tasks`);
  }

  listTasksPage(
    ownerId: number,
    options: {
      page: number;
      size: number;
      q?: string;
      status?: ProductivityTaskStatus | '';
      priority?: ProductivityPriority | '';
      dueFrom?: string;
      dueTo?: string;
    }
  ): Observable<PageResponse<ProductivityTask>> {
    let params = new HttpParams()
      .set('page', String(options.page))
      .set('size', String(options.size));

    if (options.q) params = params.set('q', options.q);
    if (options.status) params = params.set('status', options.status);
    if (options.priority) params = params.set('priority', options.priority);
    if (options.dueFrom) params = params.set('dueFrom', options.dueFrom);
    if (options.dueTo) params = params.set('dueTo', options.dueTo);

    return this.http.get<PageResponse<ProductivityTask>>(`${this.api}/owners/${ownerId}/tasks/page`, { params });
  }

  createTask(ownerId: number, body: TaskCreateRequest): Observable<ProductivityTask> {
    return this.http.post<ProductivityTask>(`${this.api}/owners/${ownerId}/tasks`, body);
  }

  updateTask(taskId: number, body: TaskUpdateRequest): Observable<ProductivityTask> {
    return this.http.put<ProductivityTask>(`${this.api}/tasks/${taskId}`, body);
  }

  clearTaskGoal(taskId: number): Observable<ProductivityTask> {
    return this.http.post<ProductivityTask>(`${this.api}/tasks/${taskId}/clear-goal`, {});
  }

  startTask(taskId: number): Observable<ProductivityTask> {
    return this.http.post<ProductivityTask>(`${this.api}/tasks/${taskId}/start`, {});
  }

  completeTask(taskId: number): Observable<ProductivityTask> {
    return this.http.post<ProductivityTask>(`${this.api}/tasks/${taskId}/complete`, {});
  }

  deleteTask(taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/tasks/${taskId}`);
  }

  downloadOwnerIcs(ownerId: number): Observable<Blob> {
    return this.http.get(`${this.api}/owners/${ownerId}/calendar.ics`, { responseType: 'blob' });
  }

  listGoals(ownerId: number): Observable<Goal[]> {
    return this.http.get<Goal[]>(`${this.api}/owners/${ownerId}/goals`);
  }

  createGoal(ownerId: number, body: GoalCreateRequest): Observable<Goal> {
    return this.http.post<Goal>(`${this.api}/owners/${ownerId}/goals`, body);
  }

  updateGoal(goalId: number, body: GoalCreateRequest): Observable<Goal> {
    return this.http.put<Goal>(`${this.api}/goals/${goalId}`, body);
  }

  deleteGoal(goalId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/goals/${goalId}`);
  }

  listDependencies(ownerId: number): Observable<Dependency[]> {
    return this.http.get<Dependency[]>(`${this.api}/owners/${ownerId}/dependencies`);
  }

  addDependency(ownerId: number, body: DependencyCreateRequest): Observable<Dependency> {
    return this.http.post<Dependency>(`${this.api}/owners/${ownerId}/dependencies`, body);
  }

  removeDependency(dependencyId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/dependencies/${dependencyId}`);
  }

  getDependencyOrder(ownerId: number): Observable<TaskOrder> {
    return this.http.get<TaskOrder>(`${this.api}/owners/${ownerId}/dependencies/order`);
  }

  adaptiveReschedule(ownerId: number, weekStart?: string, dailyCapacityMinutes?: number): Observable<AdaptiveRescheduleResponse> {
    return this.http.post<AdaptiveRescheduleResponse>(`${this.api}/owners/${ownerId}/adaptive-reschedule`, {
      weekStart,
      dailyCapacityMinutes,
    });
  }

  resolveConflict(ownerId: number, body: ConflictResolutionRequest): Observable<ConflictResolution> {
    return this.http.post<ConflictResolution>(`${this.api}/owners/${ownerId}/conflicts/resolve`, body);
  }

  listDecisions(ownerId: number): Observable<DecisionLog[]> {
    return this.http.get<DecisionLog[]>(`${this.api}/owners/${ownerId}/decisions`);
  }

  createDecision(ownerId: number, body: DecisionLogCreateRequest): Observable<DecisionLog> {
    return this.http.post<DecisionLog>(`${this.api}/owners/${ownerId}/decisions`, body);
  }

  decomposeGoal(ownerId: number, goalText: string, maxSteps = 6): Observable<AiDecomposeResponse> {
    return this.http.post<AiDecomposeResponse>(`${this.api}/owners/${ownerId}/ai/decompose`, {
      goalText,
      maxSteps,
    });
  }

  getContextSuggestions(ownerId: number): Observable<ContextSuggestion[]> {
    return this.http.get<ContextSuggestion[]>(`${this.api}/owners/${ownerId}/ai/context-suggestions`);
  }

  getInsights(ownerId: number): Observable<ProductivityInsights> {
    return this.http.get<ProductivityInsights>(`${this.api}/owners/${ownerId}/insights`);
  }

  getWeeklyReview(ownerId: number): Observable<WeeklyReview> {
    return this.http.get<WeeklyReview>(`${this.api}/owners/${ownerId}/weekly-review`);
  }

  listTodoLists(ownerId: number): Observable<TodoList[]> {
    return this.http.get<TodoList[]>(`${this.api}/owners/${ownerId}/todo-lists`);
  }

  listTodoListsPage(ownerId: number, page: number, size: number, q?: string): Observable<PageResponse<TodoList>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (q) params = params.set('q', q);
    return this.http.get<PageResponse<TodoList>>(`${this.api}/owners/${ownerId}/todo-lists/page`, { params });
  }

  createTodoList(ownerId: number, name: string): Observable<TodoList> {
    return this.http.post<TodoList>(`${this.api}/owners/${ownerId}/todo-lists`, { name });
  }

  renameTodoList(listId: number, name: string): Observable<TodoList> {
    return this.http.put<TodoList>(`${this.api}/todo-lists/${listId}`, { name });
  }

  deleteTodoList(listId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/todo-lists/${listId}`);
  }

  listTodoItems(listId: number): Observable<TodoItem[]> {
    return this.http.get<TodoItem[]>(`${this.api}/todo-lists/${listId}/items`);
  }

  listTodoItemsPage(
    listId: number,
    options: { page: number; size: number; q?: string; done?: boolean | ''; dueFrom?: string; dueTo?: string }
  ): Observable<PageResponse<TodoItem>> {
    let params = new HttpParams().set('page', String(options.page)).set('size', String(options.size));
    if (options.q) params = params.set('q', options.q);
    if (options.done !== '' && options.done !== undefined) params = params.set('done', String(options.done));
    if (options.dueFrom) params = params.set('dueFrom', options.dueFrom);
    if (options.dueTo) params = params.set('dueTo', options.dueTo);

    return this.http.get<PageResponse<TodoItem>>(`${this.api}/todo-lists/${listId}/items/page`, { params });
  }

  createTodoItem(listId: number, title: string, dueAt?: string): Observable<TodoItem> {
    return this.http.post<TodoItem>(`${this.api}/todo-lists/${listId}/items`, { title, dueAt });
  }

  updateTodoItem(itemId: number, body: { title?: string; done?: boolean; dueAt?: string; positionIndex?: number }): Observable<TodoItem> {
    return this.http.put<TodoItem>(`${this.api}/todo-items/${itemId}`, body);
  }

  toggleTodoItem(itemId: number): Observable<TodoItem> {
    return this.http.post<TodoItem>(`${this.api}/todo-items/${itemId}/toggle`, {});
  }

  deleteTodoItem(itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/todo-items/${itemId}`);
  }

  getProgress(ownerId: number): Observable<ProgressSummary> {
    return this.http.get<ProgressSummary>(`${this.api}/owners/${ownerId}/progress`);
  }
}

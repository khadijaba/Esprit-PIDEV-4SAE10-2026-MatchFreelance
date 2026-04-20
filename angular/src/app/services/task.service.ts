import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task, TaskRequest } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly api = '/api/projects';

  constructor(private http: HttpClient) {}

  getByProjectId(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.api}/${projectId}/tasks`);
  }

  create(projectId: number, task: TaskRequest): Observable<Task> {
    return this.http.post<Task>(`${this.api}/${projectId}/tasks`, task);
  }

  update(projectId: number, taskId: number, task: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.api}/${projectId}/tasks/${taskId}`, task);
  }

  delete(projectId: number, taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${projectId}/tasks/${taskId}`);
  }
}

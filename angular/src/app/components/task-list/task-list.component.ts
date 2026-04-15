import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../services/task.service';
import { ToastService } from '../../services/toast.service';
import { Task, TaskRequest, TaskStatus } from '../../models/task.model';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-list.component.html',
})
export class TaskListComponent implements OnInit {
  @Input() projectId!: number;
  tasks: Task[] = [];
  loading = true;
  showForm = false;
  editingId: number | null = null;
  formModel: TaskRequest = { title: '', description: '', status: 'TODO' };

  constructor(
    private taskService: TaskService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadTasks();
  }

  loadTasks() {
    if (!this.projectId) return;
    this.taskService.getByProjectId(this.projectId).subscribe({
      next: (data) => {
        this.tasks = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load tasks');
      },
    });
  }

  openForm() {
    this.showForm = true;
    this.formModel = { title: '', description: '', status: 'TODO' };
  }

  closeForm() {
    this.showForm = false;
    this.editingId = null;
  }

  editTask(task: Task) {
    this.editingId = task.id;
    this.formModel = {
      title: task.title,
      description: task.description ?? '',
      status: task.status,
      dueDate: task.dueDate,
      assigneeId: task.assigneeId,
    };
  }

  saveTask() {
    if (!this.formModel.title?.trim()) {
      this.toast.error('Title is required');
      return;
    }
    if (this.editingId) {
      this.taskService.update(this.projectId, this.editingId, this.formModel).subscribe({
        next: () => {
          this.toast.success('Task updated');
          this.loadTasks();
          this.closeForm();
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to update task'),
      });
    } else {
      this.taskService.create(this.projectId, this.formModel).subscribe({
        next: () => {
          this.toast.success('Task added');
          this.loadTasks();
          this.closeForm();
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to add task'),
      });
    }
  }

  deleteTask(task: Task) {
    if (!confirm('Delete this task?')) return;
    this.taskService.delete(this.projectId, task.id).subscribe({
      next: () => {
        this.toast.success('Task deleted');
        this.loadTasks();
        this.closeForm();
      },
      error: () => this.toast.error('Failed to delete task'),
    });
  }

  statusClass(status: TaskStatus): string {
    const map: Record<TaskStatus, string> = {
      TODO: 'bg-slate-100 text-slate-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      DONE: 'bg-emerald-100 text-emerald-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }
}

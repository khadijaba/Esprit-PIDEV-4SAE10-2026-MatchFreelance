import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { ProductivityService } from '../../services/productivity.service';
import { forkJoin } from 'rxjs';
import {
  AiSource,
  AdaptiveRescheduleResponse,
  AiDecomposeResponse,
  ConflictResolution,
  ContextSuggestion,
  DecisionLog,
  Dependency,
  Goal,
  ProductivityInsights,
  ProductivityPriority,
  ProductivityTask,
  ProductivityTaskStatus,
  ProgressSummary,
  WeeklyReview,
} from '../../models/productivity.model';

@Component({
  selector: 'app-productivity-board',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './productivity-board.component.html',
})
export class ProductivityBoardComponent implements OnInit {
  private readonly workspaceViewStorageKey = 'productivity.workspaceView';

  loading = true;

  tasks: ProductivityTask[] = [];
  allTasks: ProductivityTask[] = [];
  summary?: ProgressSummary;

  goals: Goal[] = [];
  dependencies: Dependency[] = [];
  decisions: DecisionLog[] = [];
  insights?: ProductivityInsights;
  contextSuggestions: ContextSuggestion[] = [];
  decomposition?: AiDecomposeResponse;
  decompositionDraft: { text: string; selected: boolean }[] = [];
  decompositionBusy = false;
  adaptivePlan?: AdaptiveRescheduleResponse;
  weeklyReview?: WeeklyReview;
  conflictResult?: ConflictResolution;
  goalTaskSelection: Record<number, '' | number> = {};
  readonly dependencyUnnestDropListId = 'dependency-unnest-zone';

  private autoConflictPairKey = '';
  private autoAdaptiveKey = '';

  workspaceView: 'board' | 'list' | 'hierarchy' | 'automation' = 'board';

  // Task filters + pagination
  taskQuery = '';
  taskStatusFilter: ProductivityTaskStatus | '' = '';
  taskPriorityFilter: ProductivityPriority | '' = '';
  taskDueFrom = '';
  taskDueTo = '';
  taskPage = 0;
  taskSize = 8;
  taskTotalPages = 0;

  // New forms
  newGoalTitle = '';
  newGoalDescription = '';
  newGoalTargetDate = '';
  newTaskGoalId: '' | number = '';

  conflictFirstTaskId?: number;
  conflictSecondTaskId?: number;

  aiGoalText = '';
  aiMaxSteps = 6;

  adaptiveWeekStart = '';
  adaptiveDailyCapacityMinutes = 240;

  newTaskTitle = '';
  newTaskDescription = '';
  newTaskPriority: ProductivityPriority = 'MEDIUM';
  newTaskPlannedMinutes = 30;
  newTaskActualMinutes?: number;
  newTaskDueAt = '';
  newTaskAfterTaskId: '' | number = '';


  readonly priorities: ProductivityPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
  readonly statuses: ProductivityTaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

  readonly boardStatuses: Array<'TODO' | 'IN_PROGRESS' | 'DONE'> = ['TODO', 'IN_PROGRESS', 'DONE'];

  boardDraftTitle: Record<'TODO' | 'IN_PROGRESS' | 'DONE', string> = {
    TODO: '',
    IN_PROGRESS: '',
    DONE: '',
  };

  boardDraftParentId: Record<'TODO' | 'IN_PROGRESS' | 'DONE', '' | number> = {
    TODO: '',
    IN_PROGRESS: '',
    DONE: '',
  };

  boardComposerOpen: Record<'TODO' | 'IN_PROGRESS' | 'DONE', boolean> = {
    TODO: false,
    IN_PROGRESS: false,
    DONE: false,
  };

  boardComposerShowParent: Record<'TODO' | 'IN_PROGRESS' | 'DONE', boolean> = {
    TODO: false,
    IN_PROGRESS: false,
    DONE: false,
  };

  boardDraftPriority: Record<'TODO' | 'IN_PROGRESS' | 'DONE', ProductivityPriority> = {
    TODO: 'MEDIUM',
    IN_PROGRESS: 'MEDIUM',
    DONE: 'MEDIUM',
  };

  boardDraftDueAt: Record<'TODO' | 'IN_PROGRESS' | 'DONE', string> = {
    TODO: '',
    IN_PROGRESS: '',
    DONE: '',
  };

  constructor(
    private readonly auth: AuthService,
    private readonly productivityService: ProductivityService,
    private readonly toast: ToastService
  ) {}

  setWorkspaceView(view: 'board' | 'list' | 'hierarchy' | 'automation'): void {
    this.workspaceView = view;
    localStorage.setItem(this.workspaceViewStorageKey, view);
  }

  ngOnInit(): void {
    const persistedView = localStorage.getItem(this.workspaceViewStorageKey);
    if (persistedView === 'board' || persistedView === 'list' || persistedView === 'hierarchy' || persistedView === 'automation') {
      this.workspaceView = persistedView;
    }
    this.reloadAll();
  }

  get ownerId(): number {
    return this.auth.currentUser()?.id ?? 0;
  }

  reloadAll(): void {
    if (!this.ownerId) {
      this.toast.error('Please login first');
      return;
    }

    this.loading = true;
    this.loadTasks();
    this.loadAllTasks();
    this.loadSummary();
    this.loadGoals();
    this.loadDependencies();
    this.loadDecisionLogs();
    this.loadInsights();
    this.loadContextSuggestions();
    this.loadWeeklyReview();
  }

  private toIso(input: string): string | undefined {
    return input ? new Date(input).toISOString() : undefined;
  }

  private hasActiveTaskFilters(): boolean {
    return !!(
      this.taskQuery.trim() ||
      this.taskStatusFilter ||
      this.taskPriorityFilter ||
      this.taskDueFrom ||
      this.taskDueTo ||
      this.taskPage > 0
    );
  }

  private loadTasks(options?: { allowAutomation?: boolean }): void {
    const allowAutomation = options?.allowAutomation ?? true;
    this.productivityService
      .listTasksPage(this.ownerId, {
        page: this.taskPage,
        size: this.taskSize,
        q: this.taskQuery.trim() || undefined,
        status: this.taskStatusFilter || undefined,
        priority: this.taskPriorityFilter || undefined,
        dueFrom: this.toIso(this.taskDueFrom),
        dueTo: this.toIso(this.taskDueTo),
      })
      .subscribe({
        next: (resp) => {
          this.tasks = resp.content;
          this.taskTotalPages = resp.totalPages;
          if (allowAutomation && !this.hasActiveTaskFilters()) {
            this.triggerAutomaticAiSuggestions();
          }
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.toast.error(err?.error?.message || 'Failed to load tasks');
        },
      });
  }

  private loadAllTasks(): void {
    this.productivityService.listTasks(this.ownerId).subscribe({
      next: (tasks) => {
        this.allTasks = tasks;
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load board tasks'),
    });
  }

  private triggerAutomaticAiSuggestions(): void {
    this.autoSuggestAdaptivePlan();
  }

  private taskUrgencyScore(task: ProductivityTask): number {
    const priorityScore: Record<ProductivityPriority, number> = {
      LOW: 1,
      MEDIUM: 2,
      HIGH: 3,
      URGENT: 4,
    };
    let score = priorityScore[task.priority] * 100;
    if (task.dueAt) {
      const due = new Date(task.dueAt).getTime();
      const now = Date.now();
      const hours = (due - now) / (1000 * 60 * 60);
      if (hours <= 0) {
        score += 500;
      } else {
        score += Math.max(0, 240 - Math.min(240, hours));
      }
    }
    if (task.status === 'IN_PROGRESS') {
      score += 40;
    }
    return score;
  }

  private autoSuggestConflictResolution(force = false): void {
    const active = this.tasks.filter((t) => t.status !== 'DONE');
    const pool = active.length >= 2 ? active : this.tasks;
    if (pool.length < 2) return;

    const ranked = [...pool].sort((a, b) => this.taskUrgencyScore(b) - this.taskUrgencyScore(a));
    const first = ranked[0];
    const second = ranked[1];
    const pairKey = `${first.id}-${second.id}-${this.tasks.length}`;
    if (!force && pairKey === this.autoConflictPairKey) {
      return;
    }
    this.autoConflictPairKey = pairKey;

    this.conflictFirstTaskId = first.id;
    this.conflictSecondTaskId = second.id;

    this.productivityService
      .resolveConflict(this.ownerId, {
        firstTaskId: first.id,
        secondTaskId: second.id,
      })
      .subscribe({
        next: (result) => {
          this.conflictResult = result;
          this.applyConflictResolution(result);
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to auto-generate conflict suggestion'),
      });
  }

  private autoSuggestAdaptivePlan(force = false): void {
    const source = this.allTasks.length ? this.allTasks : this.tasks;
    const openCount = source.filter((t) => t.status !== 'DONE').length;
    if (!force && openCount === 0) return;

    if (!this.adaptiveWeekStart) {
      const now = new Date();
      const day = now.getDay();
      const diffToMonday = day === 0 ? -6 : 1 - day;
      const monday = new Date(now);
      monday.setDate(now.getDate() + diffToMonday);
      this.adaptiveWeekStart = monday.toISOString().slice(0, 10);
    }

    const key = `${this.adaptiveWeekStart}-${this.adaptiveDailyCapacityMinutes}-${openCount}`;
    if (!force && key === this.autoAdaptiveKey) {
      return;
    }
    this.autoAdaptiveKey = key;

    this.productivityService
      .adaptiveReschedule(this.ownerId, this.adaptiveWeekStart, this.adaptiveDailyCapacityMinutes)
      .subscribe({
        next: (plan) => (this.adaptivePlan = plan),
        error: (err) => this.toast.error(err?.error?.message || 'Failed to auto-generate adaptive plan'),
      });
  }

  refreshAutoConflictSuggestion(): void {
    this.autoSuggestConflictResolution(true);
  }

  refreshAutoAdaptiveSuggestion(): void {
    this.autoSuggestAdaptivePlan(true);
  }

  private applyConflictResolution(result: ConflictResolution): void {
    const recommended = this.tasks.find((t) => t.id === result.recommendedTaskId);
    const deferred = this.tasks.find((t) => t.id === result.deferredTaskId);
    const calls: Array<ReturnType<ProductivityService['updateTask']> | ReturnType<ProductivityService['startTask']> | ReturnType<ProductivityService['createDecision']>> = [];

    if (recommended && recommended.status === 'TODO') {
      calls.push(this.productivityService.startTask(recommended.id));
    } else if (recommended && recommended.status === 'BLOCKED') {
      calls.push(this.productivityService.updateTask(recommended.id, { status: 'IN_PROGRESS' }));
    }

    if (deferred && deferred.status === 'IN_PROGRESS') {
      calls.push(this.productivityService.updateTask(deferred.id, { status: 'TODO' }));
    }

    calls.push(
      this.productivityService.createDecision(this.ownerId, {
        taskId: result.recommendedTaskId,
        decisionType: 'AUTO_CONFLICT_RESOLUTION',
        reason: `Auto-applied: focus on #${result.recommendedTaskId}, defer #${result.deferredTaskId}. ${result.rationale}`,
      })
    );

    forkJoin(calls).subscribe({
      next: () => {
        this.loadDecisionLogs();
        this.toast.info('Conflict resolution was auto-applied.');
        this.loadTasks({ allowAutomation: false });
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to auto-apply conflict resolution'),
    });
  }

  aiSourceLabel(source?: AiSource): string {
    if (!source || source === 'HEURISTIC') return 'Heuristic';
    if (source === 'HEURISTIC_FALLBACK') return 'Fallback';
    return 'Ollama';
  }

  isOllamaSource(source?: AiSource): boolean {
    return source === 'OLLAMA';
  }

  private loadGoals(): void {
    this.productivityService.listGoals(this.ownerId).subscribe({
      next: (goals) => (this.goals = goals),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load goals'),
    });
  }

  private loadDependencies(): void {
    this.productivityService.listDependencies(this.ownerId).subscribe({
      next: (deps) => (this.dependencies = deps),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load dependencies'),
    });
  }

  private loadDecisionLogs(): void {
    this.productivityService.listDecisions(this.ownerId).subscribe({
      next: (logs) => (this.decisions = logs),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load decision log'),
    });
  }

  private loadInsights(): void {
    this.productivityService.getInsights(this.ownerId).subscribe({
      next: (insights) => (this.insights = insights),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load insights'),
    });
  }

  private loadContextSuggestions(): void {
    this.productivityService.getContextSuggestions(this.ownerId).subscribe({
      next: (suggestions) => (this.contextSuggestions = suggestions),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load context suggestions'),
    });
  }

  private loadWeeklyReview(): void {
    this.productivityService.getWeeklyReview(this.ownerId).subscribe({
      next: (review) => (this.weeklyReview = review),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load weekly review'),
    });
  }

  runAdaptiveReschedule(): void {
    const weekStart = this.adaptiveWeekStart ? this.adaptiveWeekStart : undefined;
    this.productivityService
      .adaptiveReschedule(this.ownerId, weekStart, this.adaptiveDailyCapacityMinutes)
      .subscribe({
        next: (plan) => {
          this.adaptivePlan = plan;
          this.toast.success('Adaptive weekly plan generated');
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to generate adaptive plan'),
      });
  }

  decomposeGoal(): void {
    const goalText = this.aiGoalText.trim();
    if (!goalText) {
      this.toast.error('Enter a goal to decompose');
      return;
    }

    this.decompositionBusy = true;
    this.productivityService.decomposeGoal(this.ownerId, goalText, this.aiMaxSteps).subscribe({
      next: (response) => {
        this.decomposition = response;
        this.decompositionDraft = response.suggestedSteps.map((s) => ({ text: s, selected: true }));
        this.decompositionBusy = false;
      },
      error: (err) => {
        this.decompositionBusy = false;
        this.toast.error(err?.error?.message || 'Failed to decompose goal');
      },
    });
  }

  redoDecomposition(): void {
    this.decomposeGoal();
  }

  scratchDecomposition(): void {
    this.decomposition = undefined;
    this.decompositionDraft = [];
  }

  applyDecompositionAsTasks(): void {
    const selected = this.decompositionDraft
      .filter((x) => x.selected)
      .map((x) => x.text.trim())
      .filter(Boolean);

    if (!selected.length) {
      this.toast.error('Select at least one decomposed task');
      return;
    }

    const goalTitle = (this.decomposition?.inputGoal ?? this.aiGoalText).trim();
    if (!goalTitle) {
      this.toast.error('Goal title is missing');
      return;
    }

    this.decompositionBusy = true;
    const existing = this.goals.find((g) => g.title.toLowerCase() === goalTitle.toLowerCase());

    const createTasks = (goalId?: number) => {
      const created: ProductivityTask[] = [];

      const finalizeSuccess = (message: string) => {
        this.toast.success(message);
        this.decompositionBusy = false;
        this.scratchDecomposition();
        this.reloadAll();
      };

      const linkSequential = () => {
        if (created.length < 2) {
          finalizeSuccess('AI tasks added');
          return;
        }

        const links = created.slice(1).map((task, index) =>
          this.productivityService.addDependency(this.ownerId, {
            predecessorTaskId: created[index].id,
            successorTaskId: task.id,
          })
        );

        forkJoin(links).subscribe({
          next: () => finalizeSuccess('AI tasks added as a sequential chain'),
          error: (err) => {
            this.decompositionBusy = false;
            this.toast.error(err?.error?.message || 'Tasks created but failed to link sequence');
            this.reloadAll();
          },
        });
      };

      const createNext = (index: number) => {
        if (index >= selected.length) {
          linkSequential();
          return;
        }

        this.productivityService
          .createTask(this.ownerId, {
            title: selected[index],
            goalId,
            priority: 'MEDIUM',
            plannedMinutes: 30,
          })
          .subscribe({
            next: (task) => {
              created.push(task);
              createNext(index + 1);
            },
            error: (err) => {
              this.decompositionBusy = false;
              this.toast.error(err?.error?.message || 'Failed to add AI tasks');
            },
          });
      };

      createNext(0);
    };

    if (existing) {
      createTasks(existing.id);
      return;
    }

    this.productivityService.createGoal(this.ownerId, { title: goalTitle }).subscribe({
      next: (goal) => createTasks(goal.id),
      error: (err) => {
        this.decompositionBusy = false;
        this.toast.error(err?.error?.message || 'Failed to create goal');
      },
    });
  }

  createGoal(): void {
    const title = this.newGoalTitle.trim();
    if (!title) {
      this.toast.error('Goal title is required');
      return;
    }

    this.productivityService
      .createGoal(this.ownerId, {
        title,
        description: this.newGoalDescription.trim() || undefined,
        targetDate: this.newGoalTargetDate ? new Date(this.newGoalTargetDate).toISOString() : undefined,
      })
      .subscribe({
        next: () => {
          this.newGoalTitle = '';
          this.newGoalDescription = '';
          this.newGoalTargetDate = '';
          this.toast.success('Goal created');
          this.loadGoals();
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to create goal'),
      });
  }

  deleteGoal(goalId: number): void {
    if (!confirm('Delete this goal?')) return;
    this.productivityService.deleteGoal(goalId).subscribe({
      next: () => {
        this.toast.success('Goal deleted');
        this.loadGoals();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to delete goal'),
    });
  }

  removeDependency(dependencyId: number): void {
    this.productivityService.removeDependency(dependencyId).subscribe({
      next: () => {
        this.toast.success('Dependency removed');
        this.loadDependencies();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to remove dependency'),
    });
  }

  private loadSummary(): void {
    this.productivityService.getProgress(this.ownerId).subscribe({
      next: (summary) => (this.summary = summary),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to load progress summary'),
    });
  }

  onTaskFiltersChanged(): void {
    this.taskPage = 0;
    this.loadTasks({ allowAutomation: false });
  }

  previousTaskPage(): void {
    if (this.taskPage <= 0) return;
    this.taskPage--;
    this.loadTasks({ allowAutomation: false });
  }

  nextTaskPage(): void {
    if (this.taskPage + 1 >= this.taskTotalPages) return;
    this.taskPage++;
    this.loadTasks({ allowAutomation: false });
  }


  createTask(): void {
    const title = this.newTaskTitle.trim();
    if (!title) {
      this.toast.error('Task title is required');
      return;
    }

    const payload = {
      title,
      description: this.newTaskDescription.trim() || undefined,
      priority: this.newTaskPriority,
      plannedMinutes: this.newTaskPlannedMinutes,
      actualMinutes: this.newTaskActualMinutes,
      goalId: this.newTaskGoalId === '' ? undefined : this.newTaskGoalId,
      dueAt: this.newTaskDueAt ? new Date(this.newTaskDueAt).toISOString() : undefined,
    };
    const afterTaskId = this.newTaskAfterTaskId === '' ? undefined : this.newTaskAfterTaskId;

    this.productivityService.createTask(this.ownerId, payload).subscribe({
      next: (created) => {
        const finalize = () => {
          this.newTaskTitle = '';
          this.newTaskDescription = '';
          this.newTaskPriority = 'MEDIUM';
          this.newTaskPlannedMinutes = 30;
          this.newTaskActualMinutes = undefined;
          this.newTaskGoalId = '';
          this.newTaskDueAt = '';
          this.newTaskAfterTaskId = '';
          this.taskPage = 0;
          this.reloadAll();
        };

        if (!afterTaskId) {
          this.toast.success('Task created');
          finalize();
          return;
        }

        this.productivityService
          .addDependency(this.ownerId, {
            predecessorTaskId: afterTaskId,
            successorTaskId: created.id,
          })
          .subscribe({
            next: () => {
              this.toast.success('Task created and linked after predecessor');
              finalize();
            },
            error: (err) => {
              this.toast.error(err?.error?.message || 'Task created but sequence link failed');
              finalize();
            },
          });
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to create task'),
    });
  }

  private openPredecessors(taskId: number): ProductivityTask[] {
    const predecessorIds = this.dependencies.filter((d) => d.successorTaskId === taskId).map((d) => d.predecessorTaskId);
    if (!predecessorIds.length) return [];
    return this.allTasks.filter((t) => predecessorIds.includes(t.id) && t.status !== 'DONE');
  }

  private canCompleteTask(taskId: number): boolean {
    const blockers = this.openPredecessors(taskId);
    if (!blockers.length) return true;
    const labels = blockers.slice(0, 3).map((t) => `#${t.id}`).join(', ');
    this.toast.error(`Complete predecessors first: ${labels}${blockers.length > 3 ? '...' : ''}`);
    return false;
  }

  setTaskStatus(task: ProductivityTask, status: ProductivityTaskStatus): void {
    if (status === 'IN_PROGRESS') {
      this.productivityService.startTask(task.id).subscribe({
        next: () => this.reloadAll(),
        error: (err) => this.toast.error(err?.error?.message || 'Failed to start task'),
      });
      return;
    }

    if (status === 'DONE') {
      if (!this.canCompleteTask(task.id)) {
        return;
      }
      this.productivityService.completeTask(task.id).subscribe({
        next: () => this.reloadAll(),
        error: (err) => this.toast.error(err?.error?.message || 'Failed to complete task'),
      });
      return;
    }

    this.productivityService.updateTask(task.id, { status }).subscribe({
      next: () => this.reloadAll(),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to update task status'),
    });
  }

  assignTaskGoal(task: ProductivityTask, value: '' | number): void {
    if (value === '') {
      this.productivityService.clearTaskGoal(task.id).subscribe({
        next: () => {
          this.toast.success('Task unassigned from goal');
          this.reloadAll();
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to update task goal'),
      });
      return;
    }

    this.productivityService.updateTask(task.id, { goalId: value }).subscribe({
      next: () => {
        this.toast.success('Task linked to goal');
        this.reloadAll();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to update task goal'),
    });
  }

  assignSelectedTaskToGoal(goalId: number): void {
    const selectedTaskId = this.goalTaskSelection[goalId];
    if (!selectedTaskId) {
      this.toast.error('Select a task to assign');
      return;
    }

    const task = this.tasks.find((t) => t.id === selectedTaskId);
    if (!task) {
      this.toast.error('Task not found');
      return;
    }

    this.assignTaskGoal(task, goalId);
    this.goalTaskSelection[goalId] = '';
  }

  deleteTask(taskId: number): void {
    if (!confirm('Delete this task?')) return;
    this.productivityService.deleteTask(taskId).subscribe({
      next: () => {
        this.toast.success('Task deleted');
        this.reloadAll();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to delete task'),
    });
  }

  downloadCalendar(): void {
    this.productivityService.downloadOwnerIcs(this.ownerId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `productivity-${this.ownerId}.ics`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to download calendar'),
    });
  }

  percent(value: number): string {
    return `${Math.round(value * 100)}%`;
  }

  goalAlignedTasksCount(): number {
    return this.tasks.filter((t) => !!t.goalId).length;
  }

  goalAlignmentRate(): number {
    if (!this.tasks.length) return 0;
    return this.goalAlignedTasksCount() / this.tasks.length;
  }

  unassignedTaskCount(): number {
    return this.tasks.filter((t) => !t.goalId).length;
  }

  automatedDecisionCount(days = 7): number {
    const from = Date.now() - days * 24 * 60 * 60 * 1000;
    return this.decisions.filter((d) => d.decisionType.startsWith('AUTO_') && !!d.createdAt)
      .filter((d) => new Date(d.createdAt!).getTime() >= from).length;
  }

  tasksForGoal(goalId: number): ProductivityTask[] {
    return this.tasks.filter((t) => t.goalId === goalId);
  }

  unassignedTasks(): ProductivityTask[] {
    return this.tasks.filter((t) => !t.goalId);
  }

  findGoalTitle(goalId?: number | null): string {
    if (!goalId) return 'Unassigned';
    return this.goals.find((g) => g.id === goalId)?.title ?? 'Unknown';
  }

  private boardOrderedTasks(status: ProductivityTaskStatus): ProductivityTask[] {
    const lane = this.laneStatus(status);
    const laneTasks = this.allTasks
      .filter((t) => this.laneStatus(t.status) === lane)
      .sort((a, b) => (a.id ?? 0) - (b.id ?? 0));

    const parentMap = this.parentByChildMap();
    const childrenMap = new Map<number, ProductivityTask[]>();
    const roots: ProductivityTask[] = [];

    for (const task of laneTasks) {
      const parentId = parentMap.get(task.id);
      if (!parentId || !laneTasks.some((t) => t.id === parentId)) {
        roots.push(task);
        continue;
      }
      const existing = childrenMap.get(parentId) ?? [];
      existing.push(task);
      childrenMap.set(parentId, existing);
    }

    const flatten: ProductivityTask[] = [];
    const append = (task: ProductivityTask, level: number): void => {
      flatten.push(task);
      if (level >= 2) return;
      const children = (childrenMap.get(task.id) ?? []).sort((a, b) => a.id - b.id);
      for (const child of children) append(child, level + 1);
    };

    for (const root of roots) append(root, 0);
    return flatten;
  }

  boardTasks(status: ProductivityTaskStatus): ProductivityTask[] {
    return this.boardOrderedTasks(status);
  }

  toggleBoardComposer(status: 'TODO' | 'IN_PROGRESS' | 'DONE'): void {
    this.boardComposerOpen[status] = !this.boardComposerOpen[status];
    if (!this.boardComposerOpen[status]) {
      this.boardComposerShowParent[status] = false;
    }
  }

  toggleBoardParent(status: 'TODO' | 'IN_PROGRESS' | 'DONE'): void {
    this.boardComposerShowParent[status] = !this.boardComposerShowParent[status];
    if (!this.boardComposerShowParent[status]) {
      this.boardDraftParentId[status] = '';
    }
  }

  boardStatusLabel(status: ProductivityTaskStatus): string {
    if (status === 'IN_PROGRESS') return 'In Progress';
    if (status === 'DONE') return 'Done';
    return 'To Do';
  }

  private laneStatus(status: ProductivityTaskStatus): 'TODO' | 'IN_PROGRESS' | 'DONE' {
    return status === 'BLOCKED' ? 'TODO' : (status as 'TODO' | 'IN_PROGRESS' | 'DONE');
  }

  private parentByChildMap(): Map<number, number> {
    const map = new Map<number, number>();
    for (const dep of this.dependencies) {
      if (!map.has(dep.successorTaskId)) {
        map.set(dep.successorTaskId, dep.predecessorTaskId);
      }
    }
    return map;
  }

  taskDepth(taskId: number): number {
    const parentMap = this.parentByChildMap();
    let depth = 0;
    let current = taskId;
    const seen = new Set<number>();
    while (parentMap.has(current) && depth < 3) {
      if (seen.has(current)) break;
      seen.add(current);
      current = parentMap.get(current)!;
      depth++;
    }
    return Math.min(depth, 2);
  }

  nestedPrefix(taskId: number): string {
    const depth = this.taskDepth(taskId);
    if (depth <= 0) return '';
    if (depth === 1) return '↳ ';
    return '↳↳ ';
  }

  boardParentCandidates(status: 'TODO' | 'IN_PROGRESS' | 'DONE'): ProductivityTask[] {
    return this.allTasks
      .filter((t) => this.laneStatus(t.status) === status)
      .filter((t) => this.taskDepth(t.id) < 2)
      .sort((a, b) => a.id - b.id);
  }

  dependencyRootTasks(): ProductivityTask[] {
    const parented = new Set(this.dependencies.map((d) => d.successorTaskId));
    return this.allTasks.filter((t) => !parented.has(t.id)).sort((a, b) => a.id - b.id);
  }

  dependencyChildren(taskId: number): ProductivityTask[] {
    const childIds = this.dependencies.filter((d) => d.predecessorTaskId === taskId).map((d) => d.successorTaskId);
    return this.allTasks.filter((t) => childIds.includes(t.id)).sort((a, b) => a.id - b.id);
  }

  boardDropListId(status: ProductivityTaskStatus): string {
    return `task-board-${status}`;
  }

  boardDropListIds(): string[] {
    return this.boardStatuses.map((s) => this.boardDropListId(s));
  }

  dependencyDropListId(taskId: number): string {
    return `dependency-task-${taskId}`;
  }

  dependencyDropListIds(): string[] {
    return [this.dependencyUnnestDropListId, ...this.allTasks.map((t) => this.dependencyDropListId(t.id))];
  }

  private isDescendant(taskId: number, potentialAncestorId: number): boolean {
    const parentMap = this.parentByChildMap();
    let cursor = taskId;
    const seen = new Set<number>();
    while (parentMap.has(cursor)) {
      if (seen.has(cursor)) break;
      seen.add(cursor);
      const parent = parentMap.get(cursor)!;
      if (parent === potentialAncestorId) return true;
      cursor = parent;
    }
    return false;
  }

  nestTaskUnder(taskId: number, parentTaskId: number): void {
    if (taskId === parentTaskId) {
      this.toast.error('A task cannot be its own parent');
      return;
    }
    if (this.isDescendant(parentTaskId, taskId)) {
      this.toast.error('This nesting would create a cycle');
      return;
    }
    if (this.taskDepth(parentTaskId) >= 2) {
      this.toast.error('Maximum nesting depth is 2');
      return;
    }

    const current = this.dependencies.find((d) => d.successorTaskId === taskId);
    const addLink = () => {
      this.productivityService
        .addDependency(this.ownerId, {
          predecessorTaskId: parentTaskId,
          successorTaskId: taskId,
        })
        .subscribe({
          next: () => this.loadDependencies(),
          error: (err) => this.toast.error(err?.error?.message || 'Failed to nest task'),
        });
    };

    if (current) {
      if (current.predecessorTaskId === parentTaskId) return;
      this.productivityService.removeDependency(current.id).subscribe({
        next: () => addLink(),
        error: (err) => this.toast.error(err?.error?.message || 'Failed to move nested task'),
      });
      return;
    }

    addLink();
  }

  onDependencyTaskDrop(parentTaskId: number, event: CdkDragDrop<ProductivityTask[]>): void {
    const movedTask = event.item.data as ProductivityTask | undefined;
    if (!movedTask) return;
    this.nestTaskUnder(movedTask.id, parentTaskId);
  }

  removeParentDependency(taskId: number): void {
    const dep = this.dependencies.find((d) => d.successorTaskId === taskId);
    if (!dep) return;
    this.removeDependency(dep.id);
  }

  onDependencyUnnestDrop(event: CdkDragDrop<ProductivityTask[]>): void {
    const movedTask = event.item.data as ProductivityTask | undefined;
    if (!movedTask) return;
    this.removeParentDependency(movedTask.id);
  }

  private setTaskStatusFromBoard(taskId: number, status: ProductivityTaskStatus): void {
    if (status === 'IN_PROGRESS') {
      this.productivityService.startTask(taskId).subscribe({
        next: () => this.reloadAll(),
        error: (err) => this.toast.error(err?.error?.message || 'Failed to start task from board'),
      });
      return;
    }

    if (status === 'DONE') {
      if (!this.canCompleteTask(taskId)) {
        this.reloadAll();
        return;
      }
      this.productivityService.completeTask(taskId).subscribe({
        next: () => this.reloadAll(),
        error: (err) => this.toast.error(err?.error?.message || 'Failed to complete task from board'),
      });
      return;
    }

    this.productivityService.updateTask(taskId, { status }).subscribe({
      next: () => this.reloadAll(),
      error: (err) => this.toast.error(err?.error?.message || 'Failed to update task status from board'),
    });
  }

  onTaskBoardDrop(targetStatus: ProductivityTaskStatus, event: CdkDragDrop<ProductivityTask[]>): void {
    const movedTask = event.item.data as ProductivityTask | undefined;
    if (!movedTask) return;

    if (event.previousContainer === event.container) {
      // Keep DnD fluid in the UI; task ordering persistence can be added in a next iteration.
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
    this.setTaskStatusFromBoard(movedTask.id, this.laneStatus(targetStatus));
  }

  addBoardTask(status: 'TODO' | 'IN_PROGRESS' | 'DONE'): void {
    const title = this.boardDraftTitle[status].trim();
    if (!title) {
      this.toast.error('Enter a task title');
      return;
    }

    const parentId = this.boardDraftParentId[status] === '' ? undefined : this.boardDraftParentId[status];
    if (parentId && this.taskDepth(parentId) >= 2) {
      this.toast.error('Maximum nesting depth is 2');
      return;
    }

    this.productivityService
      .createTask(this.ownerId, {
        title,
        plannedMinutes: 30,
        priority: this.boardDraftPriority[status],
        dueAt: this.boardDraftDueAt[status] ? new Date(this.boardDraftDueAt[status]).toISOString() : undefined,
      })
      .subscribe({
        next: (created) => {
          const afterParent = () => {
            if (status === 'TODO') {
              this.toast.success('Task added to board');
              this.reloadAll();
              return;
            }
            this.productivityService.updateTask(created.id, { status }).subscribe({
              next: () => {
                this.toast.success('Task added to board');
                this.reloadAll();
              },
              error: (err) => this.toast.error(err?.error?.message || 'Failed to set task lane'),
            });
          };

          if (!parentId) {
            afterParent();
            return;
          }

          this.productivityService
            .addDependency(this.ownerId, {
              predecessorTaskId: parentId,
              successorTaskId: created.id,
            })
            .subscribe({
              next: () => afterParent(),
              error: (err) => this.toast.error(err?.error?.message || 'Failed to create nested task link'),
            });
        },
        error: (err) => this.toast.error(err?.error?.message || 'Failed to add board task'),
      });

    this.boardDraftTitle[status] = '';
    this.boardDraftParentId[status] = '';
    this.boardDraftPriority[status] = 'MEDIUM';
    this.boardDraftDueAt[status] = '';
    this.boardComposerOpen[status] = false;
    this.boardComposerShowParent[status] = false;
  }

  hasDependency(taskId: number): boolean {
    return this.dependencies.some((d) => d.predecessorTaskId === taskId || d.successorTaskId === taskId);
  }

  taskDecisionCount(taskId: number): number {
    return this.decisions.filter((d) => d.taskId === taskId).length;
  }
}


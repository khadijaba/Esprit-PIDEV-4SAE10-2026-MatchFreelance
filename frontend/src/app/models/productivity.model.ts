export type ProductivityTaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED';
export type ProductivityPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type AiSource = 'OLLAMA' | 'HEURISTIC' | 'HEURISTIC_FALLBACK';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ProductivityTask {
  id: number;
  ownerId: number;
  goalId?: number | null;
  title: string;
  description?: string | null;
  status: ProductivityTaskStatus;
  priority: ProductivityPriority;
  plannedMinutes: number;
  actualMinutes?: number | null;
  dueAt?: string | null;
  completedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaskCreateRequest {
  title: string;
  description?: string;
  priority?: ProductivityPriority;
  plannedMinutes?: number;
  actualMinutes?: number;
  goalId?: number | null;
  dueAt?: string;
}

export interface TaskUpdateRequest {
  title?: string;
  description?: string;
  priority?: ProductivityPriority;
  plannedMinutes?: number;
  actualMinutes?: number;
  goalId?: number | null;
  dueAt?: string;
  status?: ProductivityTaskStatus;
}

export interface Goal {
  id: number;
  ownerId: number;
  title: string;
  description?: string;
  targetDate?: string | null;
  totalTasks: number;
  doneTasks: number;
  completionRate: number;
  weeklyVelocity: number;
}

export interface GoalCreateRequest {
  title: string;
  description?: string;
  targetDate?: string;
}

export interface Dependency {
  id: number;
  ownerId: number;
  predecessorTaskId: number;
  successorTaskId: number;
  createdAt?: string;
}

export interface DependencyCreateRequest {
  predecessorTaskId: number;
  successorTaskId: number;
}

export interface TaskOrder {
  ownerId: number;
  orderedTaskIds: number[];
}

export interface DailyTaskAllocation {
  taskId: number;
  title: string;
  scheduledDate: string;
  allocatedMinutes: number;
}

export interface AdaptiveRescheduleResponse {
  ownerId: number;
  weekStart: string;
  dailyCapacityMinutes: number;
  allocations: DailyTaskAllocation[];
  aiSource?: AiSource;
}

export interface ConflictResolutionRequest {
  firstTaskId: number;
  secondTaskId: number;
}

export interface ConflictResolution {
  recommendedTaskId: number;
  deferredTaskId: number;
  recommendedScore: number;
  deferredScore: number;
  rationale: string;
  aiSource?: AiSource;
}

export interface DecisionLog {
  id: number;
  ownerId: number;
  taskId?: number | null;
  decisionType: string;
  reason: string;
  createdAt?: string;
}

export interface DecisionLogCreateRequest {
  taskId?: number;
  decisionType: string;
  reason: string;
}

export interface AiDecomposeResponse {
  inputGoal: string;
  suggestedSteps: string[];
  rationale: string;
  aiSource?: AiSource;
}

export interface ContextSuggestion {
  category: string;
  message: string;
  confidence: number;
}

export interface ProductivityInsights {
  ownerId: number;
  estimationAccuracyScore: number;
  completionRate: number;
  currentCompletionStreakDays: number;
  bestPerformanceHour: number;
  worstPerformanceHour: number;
}

export interface WeeklyReview {
  ownerId: number;
  prompts: string[];
}

export interface TodoList {
  id: number;
  ownerId: number;
  name: string;
  totalItems: number;
  completedItems: number;
  createdAt?: string;
}

export interface TodoItem {
  id: number;
  ownerId: number;
  listId: number;
  title: string;
  done: boolean;
  positionIndex: number;
  dueAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProgressSummary {
  ownerId: number;
  totalTasks: number;
  doneTasks: number;
  inProgressTasks: number;
  blockedTasks: number;
  overdueTasks: number;
  completionRate: number;
  totalPlannedMinutesOpen: number;
}

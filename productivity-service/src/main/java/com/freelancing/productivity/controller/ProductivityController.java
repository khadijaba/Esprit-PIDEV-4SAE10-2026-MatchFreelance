package com.freelancing.productivity.controller;

import com.freelancing.productivity.dto.*;
import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import com.freelancing.productivity.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/productivity")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class ProductivityController {

    private final TaskService taskService;
    private final TodoService todoService;
    private final ProgressService progressService;
    private final GoalService goalService;
    private final DependencyService dependencyService;
    private final AdaptivePlanningService adaptivePlanningService;
    private final DecisionIntelligenceService decisionIntelligenceService;
    private final DecisionLogService decisionLogService;
    private final CognitiveAssistService cognitiveAssistService;
    private final InsightsService insightsService;

    @GetMapping("/owners/{ownerId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getTasks(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(taskService.getTasksByOwner(ownerId));
    }

    @GetMapping("/owners/{ownerId}/tasks/page")
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> getTasksPaged(
            @PathVariable @Min(1) Long ownerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductivityTaskStatus status,
            @RequestParam(required = false) ProductivityPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taskService.getTasksByOwnerPaged(ownerId, q, status, priority, dueFrom, dueTo, page, size));
    }

    @PostMapping("/owners/{ownerId}/tasks")
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody TaskCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(ownerId, request));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable @Min(1) Long taskId,
            @Valid @RequestBody TaskUpdateRequestDTO request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @PostMapping("/tasks/{taskId}/start")
    public ResponseEntity<TaskResponseDTO> startTask(@PathVariable @Min(1) Long taskId) {
        return ResponseEntity.ok(taskService.startTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/clear-goal")
    public ResponseEntity<TaskResponseDTO> clearTaskGoal(@PathVariable @Min(1) Long taskId) {
        return ResponseEntity.ok(taskService.clearTaskGoal(taskId));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<TaskResponseDTO> completeTask(@PathVariable @Min(1) Long taskId) {
        return ResponseEntity.ok(taskService.completeTask(taskId));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable @Min(1) Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/owners/{ownerId}/calendar.ics", produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> exportOwnerCalendar(@PathVariable @Min(1) Long ownerId) {
        String ics = taskService.exportOwnerCalendar(ownerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productivity-" + ownerId + ".ics")
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(ics);
    }

    @GetMapping(value = "/tasks/{taskId}/calendar.ics", produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> exportTaskCalendar(@PathVariable @Min(1) Long taskId) {
        String ics = taskService.exportSingleTaskCalendar(taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=task-" + taskId + ".ics")
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(ics);
    }

    @GetMapping("/owners/{ownerId}/todo-lists")
    public ResponseEntity<List<TodoListResponseDTO>> getLists(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(todoService.getListsByOwner(ownerId));
    }

    @GetMapping("/owners/{ownerId}/todo-lists/page")
    public ResponseEntity<PageResponseDTO<TodoListResponseDTO>> getListsPaged(
            @PathVariable @Min(1) Long ownerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(todoService.getListsByOwnerPaged(ownerId, q, page, size));
    }

    @PostMapping("/owners/{ownerId}/todo-lists")
    public ResponseEntity<TodoListResponseDTO> createList(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody TodoListCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createList(ownerId, request));
    }

    @PutMapping("/todo-lists/{listId}")
    public ResponseEntity<TodoListResponseDTO> renameList(
            @PathVariable @Min(1) Long listId,
            @Valid @RequestBody TodoListCreateRequestDTO request) {
        return ResponseEntity.ok(todoService.renameList(listId, request));
    }

    @DeleteMapping("/todo-lists/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable @Min(1) Long listId) {
        todoService.deleteList(listId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/todo-lists/{listId}/items")
    public ResponseEntity<List<TodoItemResponseDTO>> getItems(@PathVariable @Min(1) Long listId) {
        return ResponseEntity.ok(todoService.getItems(listId));
    }

    @GetMapping("/todo-lists/{listId}/items/page")
    public ResponseEntity<PageResponseDTO<TodoItemResponseDTO>> getItemsPaged(
            @PathVariable @Min(1) Long listId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean done,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(todoService.getItemsPaged(listId, q, done, dueFrom, dueTo, page, size));
    }

    @PostMapping("/todo-lists/{listId}/items")
    public ResponseEntity<TodoItemResponseDTO> createItem(
            @PathVariable @Min(1) Long listId,
            @Valid @RequestBody TodoItemCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createItem(listId, request));
    }

    @PutMapping("/todo-items/{itemId}")
    public ResponseEntity<TodoItemResponseDTO> updateItem(
            @PathVariable @Min(1) Long itemId,
            @Valid @RequestBody TodoItemUpdateRequestDTO request) {
        return ResponseEntity.ok(todoService.updateItem(itemId, request));
    }

    @PostMapping("/todo-items/{itemId}/toggle")
    public ResponseEntity<TodoItemResponseDTO> toggleItem(@PathVariable @Min(1) Long itemId) {
        return ResponseEntity.ok(todoService.toggleItem(itemId));
    }

    @DeleteMapping("/todo-items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable @Min(1) Long itemId) {
        todoService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owners/{ownerId}/progress")
    public ResponseEntity<ProgressSummaryDTO> progress(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(progressService.summarize(ownerId));
    }

    @GetMapping("/owners/{ownerId}/goals")
    public ResponseEntity<List<GoalResponseDTO>> listGoals(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(goalService.listByOwner(ownerId));
    }

    @PostMapping("/owners/{ownerId}/goals")
    public ResponseEntity<GoalResponseDTO> createGoal(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody GoalCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(ownerId, request));
    }

    @PutMapping("/goals/{goalId}")
    public ResponseEntity<GoalResponseDTO> updateGoal(
            @PathVariable @Min(1) Long goalId,
            @Valid @RequestBody GoalCreateRequestDTO request) {
        return ResponseEntity.ok(goalService.update(goalId, request));
    }

    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable @Min(1) Long goalId) {
        goalService.delete(goalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owners/{ownerId}/dependencies")
    public ResponseEntity<List<DependencyResponseDTO>> listDependencies(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(dependencyService.listByOwner(ownerId));
    }

    @PostMapping("/owners/{ownerId}/dependencies")
    public ResponseEntity<DependencyResponseDTO> addDependency(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody DependencyCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dependencyService.addDependency(ownerId, request));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    public ResponseEntity<Void> removeDependency(@PathVariable @Min(1) Long dependencyId) {
        dependencyService.removeDependency(dependencyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owners/{ownerId}/dependencies/order")
    public ResponseEntity<TaskOrderResponseDTO> dependencyOrder(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(dependencyService.topologicalOrder(ownerId));
    }

    @PostMapping("/owners/{ownerId}/adaptive-reschedule")
    public ResponseEntity<AdaptiveRescheduleResponseDTO> adaptiveReschedule(
            @PathVariable @Min(1) Long ownerId,
            @RequestBody(required = false) AdaptiveRescheduleRequestDTO request) {
        AdaptiveRescheduleRequestDTO safeRequest = request == null ? new AdaptiveRescheduleRequestDTO() : request;
        return ResponseEntity.ok(adaptivePlanningService.buildAdaptiveWeekPlan(ownerId, safeRequest));
    }

    @PostMapping("/owners/{ownerId}/conflicts/resolve")
    public ResponseEntity<ConflictResolutionResponseDTO> resolveConflict(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody ConflictResolutionRequestDTO request) {
        return ResponseEntity.ok(decisionIntelligenceService.resolve(request));
    }

    @GetMapping("/owners/{ownerId}/decisions")
    public ResponseEntity<List<DecisionLogResponseDTO>> listDecisions(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(decisionLogService.listByOwner(ownerId));
    }

    @PostMapping("/owners/{ownerId}/decisions")
    public ResponseEntity<DecisionLogResponseDTO> createDecision(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody DecisionLogCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionLogService.create(ownerId, request));
    }

    @PostMapping("/owners/{ownerId}/ai/decompose")
    public ResponseEntity<AiDecomposeResponseDTO> decomposeGoal(
            @PathVariable @Min(1) Long ownerId,
            @Valid @RequestBody AiDecomposeRequestDTO request) {
        return ResponseEntity.ok(cognitiveAssistService.decomposeGoal(request));
    }

    @GetMapping("/owners/{ownerId}/ai/context-suggestions")
    public ResponseEntity<List<ContextSuggestionDTO>> contextSuggestions(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(cognitiveAssistService.contextSuggestions(ownerId));
    }

    @GetMapping("/owners/{ownerId}/insights")
    public ResponseEntity<ProductivityInsightsDTO> insights(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(insightsService.insights(ownerId));
    }

    @GetMapping("/owners/{ownerId}/weekly-review")
    public ResponseEntity<WeeklyReviewDTO> weeklyReview(@PathVariable @Min(1) Long ownerId) {
        return ResponseEntity.ok(insightsService.weeklyReview(ownerId));
    }
}

# Productivity Database - Data for User Amira (ID: 5)

## ✅ All Data Successfully Created for User 5

### Data Summary

| Table Name | Record Count | Owner ID |
|------------|--------------|----------|
| **productivity_goals** | 30 | 5 (Amira) |
| **productivity_tasks** | 40 | 5 (Amira) |
| **todo_lists** | 30 | 5 (Amira) |
| **todo_items** | 60 | 5 (Amira) |
| **decision_log_entries** | 30 | 5 (Amira) |
| **task_dependencies** | 30 | 5 (Amira) |

### Total Records: **220** (all for Amira)

## Data Details

### Productivity Goals (30 items)
- Professional development goals
- Technical skills (Spring Boot, React, Kubernetes, Docker, etc.)
- Platform development milestones
- Certifications and learning objectives
- Target dates ranging from 30 to 180 days in the future

### Productivity Tasks (40 items)
- **Statuses Distribution**:
  - TODO: 10 tasks
  - IN_PROGRESS: 10 tasks
  - DONE: 10 tasks
  - BLOCKED: 10 tasks
- **Priorities**: HIGH, MEDIUM, LOW (distributed evenly)
- **Time Tracking**: 
  - Planned minutes: 60-645 minutes
  - Actual minutes tracked for IN_PROGRESS and DONE tasks
  - Completion timestamps for DONE tasks
- **Goal Association**: All tasks linked to productivity goals
- **Due Dates**: Spread across next 40 days

### Todo Lists (30 items)
Variety of list categories:
- Work-related (Daily Tasks, Project Backlog, Bug Fixes, Code Reviews)
- Personal (Shopping, Fitness Goals, Travel Plans, Meal Planning)
- Learning (Research Topics, Books to Read, Skills to Learn)
- Professional (Career Development, Networking, Client Follow-ups)
- Creative (Blog Post Ideas, Creative Projects)
- Health & Wellness

### Todo Items (60 items)
- **Completion Status**: ~33% marked as done (20 items)
- **Due Dates**: 15 items have due dates
- **Position Tracking**: Items ordered within lists (0-9)
- **Variety**: Development tasks, reviews, documentation, fixes, updates

### Decision Log Entries (30 items)
- **Decision Types**: 
  - ARCHITECTURE
  - TECHNOLOGY
  - DESIGN
  - PROCESS
  - PRIORITY
  - SCOPE
  - RESOURCE
- **Task Association**: Each linked to a productivity task
- **Detailed Reasoning**: Comprehensive rationale for each decision

### Task Dependencies (30 items)
- Sequential dependencies between tasks
- Represents workflow and task ordering
- Useful for testing dependency management and task hierarchy

## How to Access

1. **Login as Amira** (User ID: 5)
2. **Navigate to Productivity Page**: http://localhost:4200/productivity
3. **All data will now be visible** for your user

## API Endpoints (via Gateway)

All accessible at: **http://localhost:8081/api/productivity**

- `GET /owners/5/tasks` - List all tasks
- `GET /owners/5/tasks/page` - Paginated tasks with filters
- `GET /owners/5/goals` - List all goals
- `GET /owners/5/todo-lists` - List all todo lists
- `GET /owners/5/decisions` - List decision logs
- `GET /owners/5/dependencies` - List task dependencies

## Testing Scenarios

With this data, you can test:
- ✅ Task management with all statuses
- ✅ Priority-based filtering and sorting
- ✅ Time tracking and estimation
- ✅ Goal-task relationships
- ✅ Todo list organization
- ✅ Task dependencies and workflows
- ✅ Decision logging and audit trails
- ✅ Date-based queries and filtering
- ✅ Completion tracking and analytics
- ✅ Task hierarchy and nested tasks

## Refresh the Page

After the service restart, **refresh your browser** at http://localhost:4200/productivity to see all your data!

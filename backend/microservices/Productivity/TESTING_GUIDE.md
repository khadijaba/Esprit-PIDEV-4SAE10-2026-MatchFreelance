# Productivity Service - Website Testing Guide (No PowerShell)

This guide is **UI-first**: you test everything from the site at `http://localhost:4200`, not from terminal commands.

## 1) Before You Start

- Backend services are running (especially `productivity-service`, API gateway, and auth/user service).
- Angular frontend is running.
- You can log in as a freelancer account (example: user id `5`).
- Open the productivity page from the app menu or route: `.../productivity`.

---

## 2) Understand the 3 Core Areas (What to Verify)

## Task Planning (left section)

Purpose:
- Manage high-level work units (create, status changes, due dates, priorities).

What makes it important:
- It drives progress metrics.
- It feeds Smart Plan scoring and daily optimization.

You should verify:
- CRUD behavior (create, status update, delete).
- Search/filter behavior.
- Pagination behavior.

## To-do Lists (bottom section)

Purpose:
- Break execution into small actionable checklist items.

What makes it important:
- Tracks completion ratio per list.
- Supports list/item level organization.

You should verify:
- List CRUD.
- Item CRUD + toggle done/undone.
- Search/filter + pagination.

## Smart Plan (right section)

Purpose:
- Prioritize tasks and build a daily allocation based on score and available focus time.

What makes it advanced:
- Suggestions are scored (priority/urgency/state/quick-win).
- Daily plan uses a **0/1 knapsack optimization** (`KNAPSACK_01`) to maximize total value under focus-minute constraints.

You should verify:
- Suggestions look rational.
- Daily plan changes when focus minutes change.
- `Allocated / Focus`, utilization %, and optimization score are coherent.

---

## 3) Task Planning - Step-by-Step UI Test

1. In **Task planning**, create 4 tasks with mixed priorities and due dates.
2. Set one task to `IN_PROGRESS`, one to `BLOCKED`, one to `DONE`.
3. Use search box to find one task by keyword in title/description.
4. Apply **Status filter** (`DONE`) and verify only done tasks appear.
5. Apply **Priority filter** (`HIGH`/`URGENT`) and verify results.
6. Apply due date range (from/to) and verify date filtering.
7. Test pagination:
   - Reduce visible set with `size` default behavior from backend.
   - Click `Next` / `Previous` and verify page indicator updates.
8. Delete one task and confirm it disappears and summary updates.

Expected result:
- Every filter narrows list correctly.
- Pagination is stable and not duplicating/missing records.
- Progress cards react to status changes.

---

## 4) To-do Lists - Step-by-Step UI Test

1. Create at least 2 lists (example: `Freelance Sprint`, `Client Follow-up`).
2. Search list by name in the list search input.
3. Switch between list pages with `Prev/Next` when enough lists exist.
4. Select a list and add 5-10 items.
5. Toggle several items to done.
6. Use item search to find one item by title.
7. Apply item done filter:
   - `Open only`
   - `Done only`
8. Verify item pagination with `Prev/Next`.
9. Delete one item and verify counters update in selected list.
10. Delete one list and verify all its items are removed from UI.

Expected result:
- List/item counters stay accurate.
- Search/filter/pagination remain consistent after create/delete/toggle actions.

---

## 5) Smart Plan - Step-by-Step UI Test

1. Ensure you have tasks with varied:
   - Priority (`LOW`..`URGENT`)
   - Status (`TODO`, `IN_PROGRESS`, `BLOCKED`)
   - Due dates (overdue, near, far)
   - Planned minutes (short and long)
2. Open **Smart plan**.
3. Validate suggestion cards:
   - Higher urgency and higher priority should usually score higher.
   - `IN_PROGRESS` tasks should get a boost.
4. Set focus minutes to `90`, click `Refresh`:
   - Check `Allocated` is near focus budget.
   - Check plan tasks fit the budget logic.
5. Set focus minutes to `300`, click `Refresh`:
   - More tasks should be selected.
   - Utilization and optimization score should change.
6. Confirm `Algorithm: KNAPSACK_01` appears.

Expected result:
- Plan quality improves with larger focus budget.
- Selection is not just top-first greedy; it balances score and duration.

---

## 6) Cross-Feature Validation

1. Mark a task `DONE` in Task Planning.
2. Check:
   - Progress completion rate increases.
   - That task no longer dominates Smart suggestions.
3. Toggle several todo items done.
4. Check list completion counts update immediately.
5. Export `.ics` and verify file downloads.

Expected result:
- Data is consistent across all widgets after each mutation.

---

## 7) Quick Regression Checklist

- Task create/status/delete works.
- Task search/filter/pagination works.
- List create/delete works.
- Item create/toggle/delete works.
- Item search/filter/pagination works.
- Smart suggestions load.
- Daily plan optimization loads with algorithm and metrics.
- Progress cards update correctly.
- Calendar export works.

If all are green, your productivity module is functionally healthy from an end-user perspective.

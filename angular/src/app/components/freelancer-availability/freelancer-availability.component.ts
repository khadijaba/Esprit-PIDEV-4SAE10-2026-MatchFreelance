import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import { AvailabilitySlot, PageResponse } from '../../models/interview.model';

const DAY_NAMES = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

/** One row in the weekly schedule: day name + up to 2 time ranges (from–to). */
interface DaySchedule {
  name: string;
  from1: string;
  to1: string;
  from2: string;
  to2: string;
}

@Component({
  selector: 'app-freelancer-availability',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './freelancer-availability.component.html',
})
export class FreelancerAvailabilityComponent implements OnInit {
  page?: PageResponse<AvailabilitySlot>;
  slots: AvailabilitySlot[] = [];
  loading = true;
  pageIndex = 0;
  pageSize = 10;

  /** Weekly schedule (store-hours style): Mon–Sun, each with 2 optional ranges. */
  weekSchedule: DaySchedule[] = DAY_NAMES.map((name) => ({
    name,
    from1: '',
    to1: '',
    from2: '',
    to2: '',
  }));

  /** How many weeks ahead to generate slots from the weekly template. */
  weeksAhead = 4;
  generating = false;

  /** Live preview: number of slots that would be generated (read-only). */
  get previewSlotCount(): number {
    return this.buildSlotsFromWeeklySchedule().length;
  }

  /** Whether this day row has at least one range set. */
  dayHasRange(day: DaySchedule): boolean {
    return !!(day.from1 && day.to1) || !!(day.from2 && day.to2);
  }

  showSingleSlotForm = false;
  formStart = '';
  formEnd = '';
  submitting = false;

  constructor(
    private auth: AuthService,
    private interviewService: InterviewService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadSlots();
  }

  get freelancerId(): number {
    return this.auth.currentUser()?.id ?? 0;
  }

  loadSlots() {
    this.loading = true;
    this.interviewService
      .listAvailabilitySlots({
        freelancerId: this.freelancerId,
        onlyFree: false,
        page: this.pageIndex,
        size: this.pageSize,
        sort: 'startAt,asc',
      })
      .subscribe({
        next: (p) => {
          this.page = p;
          this.slots = p.content;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.toast.error('Failed to load availability');
        },
      });
  }

  /** Build list of { startAt, endAt } from weekSchedule for the next weeksAhead weeks. */
  private buildSlotsFromWeeklySchedule(): { startAt: string; endAt: string }[] {
    const result: { startAt: string; endAt: string }[] = [];
    const now = new Date();

    // Next Monday (or today if Monday)
    const nextMonday = new Date(now);
    const dayOfWeek = now.getDay();
    const daysUntilMonday = dayOfWeek === 0 ? 1 : dayOfWeek === 1 ? 0 : 8 - dayOfWeek;
    nextMonday.setDate(now.getDate() + daysUntilMonday);
    nextMonday.setHours(0, 0, 0, 0);

    for (let week = 0; week < this.weeksAhead; week++) {
      for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
        const dayDate = new Date(nextMonday);
        dayDate.setDate(nextMonday.getDate() + week * 7 + dayIndex);
        const day = this.weekSchedule[dayIndex];

        const addRange = (from: string, to: string) => {
          if (!from || !to) return;
          const [fh, fm] = from.split(':').map(Number);
            const [th, tm] = to.split(':').map(Number);
          const startAt = new Date(dayDate);
          startAt.setHours(fh, fm ?? 0, 0, 0);
          const endAt = new Date(dayDate);
          endAt.setHours(th, tm ?? 0, 0, 0);
          if (endAt <= startAt) return;
          result.push({
            startAt: startAt.toISOString(),
            endAt: endAt.toISOString(),
          });
        };

        addRange(day.from1, day.to1);
        addRange(day.from2, day.to2);
      }
    }
    return result;
  }

  generateFromWeeklySchedule() {
    const batch = this.buildSlotsFromWeeklySchedule();
    if (batch.length === 0) {
      this.toast.error('Set at least one time range (From – To) in the weekly schedule');
      return;
    }
    if (batch.length > 200) {
      this.toast.error('Too many slots. Reduce weeks or number of ranges.');
      return;
    }
    this.generating = true;
    this.interviewService.createAvailabilitySlotsBatch(this.freelancerId, batch).subscribe({
      next: (created) => {
        this.toast.success(`${created.length} slot(s) added`);
        this.loadSlots();
        this.generating = false;
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Failed to generate slots');
        this.generating = false;
      },
    });
  }

  openSingleSlotForm() {
    this.showSingleSlotForm = true;
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    const next = new Date(now.getTime() + 60 * 60 * 1000);
    this.formStart = now.toISOString().slice(0, 16);
    this.formEnd = next.toISOString().slice(0, 16);
  }

  closeSingleSlotForm() {
    this.showSingleSlotForm = false;
  }

  addSingleSlot() {
    if (!this.formStart || !this.formEnd) {
      this.toast.error('Start and end time are required');
      return;
    }
    const start = new Date(this.formStart);
    const end = new Date(this.formEnd);
    if (end <= start) {
      this.toast.error('End time must be after start time');
      return;
    }
    this.submitting = true;
    this.interviewService
      .createAvailabilitySlot(this.freelancerId, {
        startAt: start.toISOString(),
        endAt: end.toISOString(),
      })
      .subscribe({
        next: () => {
          this.toast.success('Slot added');
          this.closeSingleSlotForm();
          this.loadSlots();
          this.submitting = false;
        },
        error: (err) => {
          this.toast.error(err?.error?.message || 'Failed to add slot');
          this.submitting = false;
        },
      });
  }

  /** Copy Monday's time ranges to every other day (quick setup). Keeps each day's name. */
  copyMondayToAll() {
    const monday = this.weekSchedule[0];
    for (let i = 1; i < this.weekSchedule.length; i++) {
      this.weekSchedule[i] = {
        name: this.weekSchedule[i].name,
        from1: monday.from1,
        to1: monday.to1,
        from2: monday.from2,
        to2: monday.to2,
      };
    }
    this.toast.success('Monday copied to all days');
  }

  /** Clear all time ranges in the weekly schedule. */
  clearWeeklySchedule() {
    this.weekSchedule = DAY_NAMES.map((name) => ({
      name,
      from1: '',
      to1: '',
      from2: '',
      to2: '',
    }));
    this.toast.success('Weekly schedule cleared');
  }

  deleteSlot(slot: AvailabilitySlot) {
    if (slot.booked) {
      this.toast.error('Cannot delete a booked slot');
      return;
    }
    if (!confirm('Remove this slot?')) return;
    this.interviewService.deleteAvailabilitySlot(slot.id).subscribe({
      next: () => {
        this.toast.success('Slot removed');
        this.loadSlots();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to remove slot'),
    });
  }

  prevPage() {
    if (!this.page || this.pageIndex <= 0) return;
    this.pageIndex -= 1;
    this.loadSlots();
  }

  nextPage() {
    if (!this.page) return;
    if (this.pageIndex + 1 >= this.page.totalPages) return;
    this.pageIndex += 1;
    this.loadSlots();
  }
}

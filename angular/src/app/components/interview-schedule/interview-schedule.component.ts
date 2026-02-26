import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import {
  AvailabilitySlot,
  Interview,
  InterviewCreateRequest,
  InterviewStatus,
  MeetingMode,
  PageResponse,
} from '../../models/interview.model';

@Component({
  selector: 'app-interview-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './interview-schedule.component.html',
})
export class InterviewScheduleComponent implements OnInit {
  @Input() candidatureId!: number;
  @Input() projectId!: number;
  @Input() freelancerId!: number;
  @Input() ownerId!: number;

  interviewsPage?: PageResponse<Interview>;
  interviews: Interview[] = [];
  loading = true;
  showForm = false;
  slotsLoading = false;
  slotsPage?: PageResponse<AvailabilitySlot>;
  slots: AvailabilitySlot[] = [];

  interviewPage = 0;
  interviewSize = 5;

  slotPage = 0;
  slotSize = 5;

  /** Calendar-style inputs */
  selectedDate = '';
  startTime = '';
  durationMinutes = 30;
  readonly durationOptions: number[] = [15, 30, 45, 60, 90];

  daySlots: AvailabilitySlot[] = [];
  allSlots: AvailabilitySlot[] = [];
  showAllAvailability = false;

  /** Tunis address model: state (gouvernorat) + delegation + free street. */
  readonly tunisStates: { name: string; delegations: string[] }[] = [
    {
      name: 'Tunis',
      delegations: [
        'Bab Bhar',
        'Bab Souika',
        'Carthage',
        'Cité El Khadra',
        'El Kabaria',
        'El Krasa',
        'El Menzah',
        'El Omrane',
        'El Omrane Supérieur',
        'Ettahrir',
        'Ezzouhour',
        'La Goulette',
        'La Marsa',
        'Sidi El Béchir',
        'Séjoumi'
      ],
    },
    {
      name: 'Ariana',
      delegations: [
        'Ariana Ville',
        'La Soukra',
        'Raoued',
        'Kalâat el-Andalous',
        'Sidi Thabet',
      ],
    },
    {
      name: 'Ben Arous',
      delegations: [
        'Ben Arous',
        'Ezzahra',
        'Hammam Chatt',
        'Hammam Lif',
        'Megrine',
        'Mornag',
        'Rades',
      ],
    },
    {
      name: 'Manouba',
      delegations: [
        'Manouba',
        'Douar Hicher',
        'Oued Ellil',
        'Den Den',
        'Mornaguia',
      ],
    },
  ];

  selectedState = '';
  selectedDelegation = '';
  streetLine = '';

  formModel: InterviewCreateRequest = {
    candidatureId: 0,
    projectId: 0,
    freelancerId: 0,
    ownerId: 0,
    slotId: 0,
    mode: 'ONLINE',
    meetingUrl: '',
    addressLine: '',
    city: '',
    notes: '',
  };

  /** Whether the owner wants to use an external meeting URL instead of pure in-app visio. */
  useExternalMeeting = false;

  constructor(
    private interviewService: InterviewService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadInterviews();
  }

  loadInterviews() {
    if (!this.candidatureId) return;
    this.loading = true;
    this.interviewService
      .searchInterviews({
        candidatureId: this.candidatureId,
        page: this.interviewPage,
        size: this.interviewSize,
        sort: 'startAt,desc',
      })
      .subscribe({
      next: (page) => {
        this.interviewsPage = page;
        this.interviews = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load interviews');
      },
    });
  }

  openForm() {
    this.showForm = true;
    this.formModel = {
      candidatureId: this.candidatureId,
      projectId: this.projectId,
      freelancerId: this.freelancerId,
      ownerId: this.ownerId,
      slotId: undefined,
      mode: 'ONLINE',
      meetingUrl: '',
      addressLine: '',
      city: '',
      notes: '',
    };
    this.slotPage = 0;
    // default date: today
    const today = new Date();
    this.selectedDate = today.toISOString().slice(0, 10);
    this.startTime = '';
    this.durationMinutes = 30;
    this.useExternalMeeting = false;
    this.formModel.meetingUrl = '';
    this.selectedState = '';
    this.selectedDelegation = '';
    this.streetLine = '';
    this.loadSlots();
  }

  closeForm() {
    this.showForm = false;
  }

  loadSlots() {
    if (!this.freelancerId) return;
    this.slotsLoading = true;
    // If a date is selected, restrict to that day
    let from: string | undefined;
    let to: string | undefined;
    if (this.selectedDate) {
      const dayStart = new Date(this.selectedDate);
      dayStart.setHours(0, 0, 0, 0);
      const dayEnd = new Date(dayStart);
      dayEnd.setHours(23, 59, 59, 999);
      from = dayStart.toISOString();
      to = dayEnd.toISOString();
    }
    this.interviewService
      .listAvailabilitySlots({
        freelancerId: this.freelancerId,
        onlyFree: true,
        page: this.slotPage,
        size: this.slotSize,
        sort: 'startAt,asc',
        from,
        to,
      })
      .subscribe({
        next: (page) => {
          this.slotsPage = page;
          this.slots = page.content;
          this.daySlots = this.slots;
          this.slotsLoading = false;
        },
        error: () => {
          this.slotsLoading = false;
          this.toast.error('Failed to load freelancer availability');
        },
      });
  }

  toggleAllAvailability() {
    this.showAllAvailability = !this.showAllAvailability;
    if (this.showAllAvailability && this.allSlots.length === 0) {
      this.loadAllSlots();
    }
  }

  private loadAllSlots() {
    if (!this.freelancerId) return;
    this.interviewService
      .listAvailabilitySlots({
        freelancerId: this.freelancerId,
        onlyFree: true,
        page: 0,
        size: 200,
        sort: 'startAt,asc',
      })
      .subscribe({
        next: (page) => {
          this.allSlots = page.content;
        },
        error: () => {
          this.toast.error('Failed to load full availability');
        },
      });
  }

  scheduleInterview() {
    if (!this.selectedDate) {
      this.toast.error('Pick a date');
      return;
    }
    if (!this.startTime) {
      this.toast.error('Pick a start time');
      return;
    }
    if (!this.durationMinutes || this.durationMinutes <= 0) {
      this.toast.error('Pick a valid duration');
      return;
    }

    if (this.formModel.mode === 'ONLINE') {
      if (this.useExternalMeeting && !this.formModel.meetingUrl?.trim()) {
        this.toast.error('Fill the external meeting URL or uncheck external meeting.');
        return;
      }
      if (!this.useExternalMeeting) {
        // Pure in-app visio: ignore any accidentally filled URL.
        this.formModel.meetingUrl = '';
      }
    }

    if (this.formModel.mode === 'FACE_TO_FACE') {
      if (!this.selectedState || !this.selectedDelegation) {
        this.toast.error('Pick a state and a delegation for the address');
        return;
      }
      // Street is optional; state + delegation go into city for better maps.
      this.formModel.addressLine = this.streetLine?.trim() || '';
      this.formModel.city = `${this.selectedDelegation}, ${this.selectedState}`;
    }

    // Build startAt/endAt from date + time in local timezone, send as ISO; backend will:
    // 1) check against freelancer availability slots,
    // 2) enforce no overlaps.
    const [hh, mm] = this.startTime.split(':').map(Number);
    const startLocal = new Date(this.selectedDate);
    startLocal.setHours(hh ?? 0, mm ?? 0, 0, 0);
    const endLocal = new Date(startLocal.getTime() + this.durationMinutes * 60000);

    this.formModel.startAt = startLocal.toISOString();
    this.formModel.endAt = endLocal.toISOString();
    this.formModel.slotId = undefined;
    this.formModel.durationMinutes = this.durationMinutes;

    this.interviewService.createInterview(this.formModel).subscribe({
      next: () => {
        this.toast.success('Interview scheduled');
        this.loadInterviews();
        this.closeForm();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to schedule interview'),
    });
  }

  confirm(interview: Interview) {
    this.interviewService.confirmInterview(interview.id).subscribe({
      next: () => {
        this.toast.success('Interview confirmed');
        this.loadInterviews();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to confirm interview'),
    });
  }

  cancel(interview: Interview) {
    this.interviewService.cancelInterview(interview.id).subscribe({
      next: () => {
        this.toast.success('Interview cancelled');
        this.loadInterviews();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to cancel interview'),
    });
  }

  complete(interview: Interview) {
    this.interviewService.completeInterview(interview.id).subscribe({
      next: () => {
        this.toast.success('Interview completed');
        this.loadInterviews();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to complete interview'),
    });
  }

  deleteInterview(interview: Interview) {
    if (!confirm('Cancel/delete this interview?')) return;
    this.interviewService.deleteInterview(interview.id).subscribe({
      next: () => {
        this.toast.success('Interview removed');
        this.loadInterviews();
      },
      error: () => this.toast.error('Failed to remove interview'),
    });
  }

  statusClass(status: InterviewStatus): string {
    const map: Record<InterviewStatus, string> = {
      PROPOSED: 'bg-amber-100 text-amber-700',
      CONFIRMED: 'bg-sky-100 text-sky-700',
      COMPLETED: 'bg-emerald-100 text-emerald-700',
      CANCELLED: 'bg-red-100 text-red-700',
      NO_SHOW: 'bg-slate-100 text-slate-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  modeLabel(mode: MeetingMode): string {
    return mode === 'FACE_TO_FACE' ? 'Face to face' : 'Online';
  }

  mapUrl(i: Interview): string | null {
    if (i.mode !== 'FACE_TO_FACE') return null;
    if (i.lat != null && i.lng != null) {
      return `https://www.google.com/maps?q=${encodeURIComponent(`${i.lat},${i.lng}`)}`;
    }
    const q = [i.addressLine, i.city].filter(Boolean).join(', ');
    if (!q) return null;
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
  }
}

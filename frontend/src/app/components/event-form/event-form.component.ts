import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { ToastService } from '../../services/toast.service';
import { RewardType, RewardRequest } from '../../models/reward.model';

/** End date must be after start date. */
function endDateAfterStartDate(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    if (!start || !end) return null;
    if (new Date(end) <= new Date(start)) {
        return { endDateAfterStart: true };
    }
    return null;
}

/** Title must not be only whitespace. */
function titleNotOnlyWhitespace(control: AbstractControl): ValidationErrors | null {
    const v = control.value;
    if (v == null) return null;
    return (typeof v === 'string' && v.trim().length === 0) ? { required: true } : null;
}

/** maxParticipants: if set, must be between 1 and 999999. */
function maxParticipantsRange(control: AbstractControl): ValidationErrors | null {
    const v = control.value;
    if (v == null || v === '') return null;
    const n = Number(v);
    if (Number.isNaN(n) || n < 1 || n > 999999) {
        return { maxParticipantsRange: true };
    }
    return null;
}

@Component({
    selector: 'app-event-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './event-form.component.html',
})
export class EventFormComponent implements OnInit {
    form!: FormGroup;
    isEdit = false;
    eventId: number | null = null;
    loading = false;
    submitting = false;

    eventTypes = [
        'HACKATHON', 'CHALLENGE', 'WEBINAR', 'MILESTONE',
        'COMMUNITY', 'SKILL_RACE', 'FORMATION_WEBINAR', 'OTHER'
    ];

    eventStatuses = ['UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED', 'CLOSED'];

    rewardTypes = [
        'BADGE', 'POINTS', 'CERTIFICATE', 'PRIORITY_BOOST', 'DISCOUNT', 'FEATURED_PROFILE', 'TOKEN', 'OTHER'
    ];

    private destroyRef = inject(DestroyRef);

    constructor(
        private fb: FormBuilder,
        private eventService: EventService,
        private toast: ToastService,
        private route: ActivatedRoute,
        private router: Router
    ) { }

    readonly TITLE_MIN = 1;
    readonly TITLE_MAX = 200;
    readonly DESC_MAX = 5000;
    readonly ELIGIBILITY_MAX = 2000;
    readonly REWARD_VALUE_MAX = 500;
    readonly REWARD_DESC_MAX = 2000;

    ngOnInit() {
        this.form = this.fb.group({
            title: ['', [
                Validators.required,
                Validators.minLength(this.TITLE_MIN),
                Validators.maxLength(this.TITLE_MAX),
                titleNotOnlyWhitespace,
            ]],
            description: ['', [Validators.maxLength(this.DESC_MAX)]],
            type: ['HACKATHON', Validators.required],
            startDate: ['', Validators.required],
            endDate: ['', Validators.required],
            eligibilityCriteria: ['', [Validators.maxLength(this.ELIGIBILITY_MAX)]],
            maxParticipants: [null, [maxParticipantsRange]],
            teamEvent: [false],
            status: ['UPCOMING', Validators.required],
            createdById: [null],
            imageUrl: [''],
            requiredSkills: [''],
            plannedRewards: this.fb.array([]),
        }, { validators: endDateAfterStartDate });

        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.isEdit = true;
            this.eventId = Number(id);
            this.loading = true;
            this.eventService.getById(this.eventId)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                    next: (event) => {
                        this.form.patchValue({
                            title: event.title,
                            description: event.description ?? '',
                            type: event.type,
                            startDate: event.startDate ? event.startDate.substring(0, 16) : '',
                            endDate: event.endDate ? event.endDate.substring(0, 16) : '',
                            eligibilityCriteria: event.eligibilityCriteria ?? '',
                            maxParticipants: event.maxParticipants,
                            teamEvent: event.teamEvent ?? false,
                            status: event.status,
                            createdById: event.createdById ?? null,
                            imageUrl: event.imageUrl ?? '',
                            requiredSkills: event.requiredSkills ?? '',
                        });
                        this.plannedRewards.clear();
                        (event.plannedRewards || []).forEach((r: { type?: string; value?: string; description?: string; visibleOnProfile?: boolean }) => {
                            this.plannedRewards.push(this.fb.group({
                                type: [r.type ?? 'BADGE', Validators.required],
                                value: [r.value ?? '', [Validators.required, Validators.maxLength(this.REWARD_VALUE_MAX)]],
                                description: [r.description ?? '', [Validators.maxLength(this.REWARD_DESC_MAX)]],
                                visibleOnProfile: [r.visibleOnProfile ?? true],
                            }));
                        });
                        this.loading = false;
                    },
                    error: () => {
                        this.toast.error('Failed to load event');
                        this.loading = false;
                    },
                });
        }
    }

    get plannedRewards() {
        return this.form.get('plannedRewards') as FormArray;
    }

    addReward() {
        this.plannedRewards.push(this.fb.group({
            type: ['BADGE', Validators.required],
            value: ['', [Validators.required, Validators.maxLength(this.REWARD_VALUE_MAX)]],
            description: ['', [Validators.maxLength(this.REWARD_DESC_MAX)]],
            visibleOnProfile: [true],
        }));
    }

    removeReward(idx: number) {
        this.plannedRewards.removeAt(idx);
    }

    onSubmit() {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }
        this.submitting = true;

        const data = this.form.value;
        // Remove empty rewards
        data.plannedRewards = data.plannedRewards.filter((r: RewardRequest) => r.type && r.value);

        const obs = this.isEdit && this.eventId
            ? this.eventService.update(this.eventId, data)
            : this.eventService.create(data);

        obs.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.toast.success(this.isEdit ? 'Event updated' : 'Event created');
                    this.router.navigate(['/admin/events']);
                },
                error: () => {
                    this.toast.error('Failed to save event');
                    this.submitting = false;
                },
            });
    }

    typeLabel(type: string): string {
        return type.replace(/_/g, ' ');
    }

    getRewardTypeBadgeClass(type: string | null): string {
        const colors: Record<string, string> = {
            BADGE: 'inline-flex items-center gap-1 rounded-full bg-purple-100 px-2.5 py-1 text-xs font-semibold text-purple-700',
            POINTS: 'inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-700',
            CERTIFICATE: 'inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-1 text-xs font-semibold text-green-700',
            PRIORITY_BOOST: 'inline-flex items-center gap-1 rounded-full bg-orange-100 px-2.5 py-1 text-xs font-semibold text-orange-700',
            DISCOUNT: 'inline-flex items-center gap-1 rounded-full bg-pink-100 px-2.5 py-1 text-xs font-semibold text-pink-700',
            FEATURED_PROFILE: 'inline-flex items-center gap-1 rounded-full bg-indigo-100 px-2.5 py-1 text-xs font-semibold text-indigo-700',
            TOKEN: 'inline-flex items-center gap-1 rounded-full bg-yellow-100 px-2.5 py-1 text-xs font-semibold text-yellow-700',
            OTHER: 'inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-1 text-xs font-semibold text-gray-700',
        };
        return colors[type || 'OTHER'] || colors['OTHER'];
    }

    getPlaceholderForType(type: string | null): string {
        const placeholders: Record<string, string> = {
            BADGE: 'e.g. "Spring Boot Expert"',
            POINTS: 'e.g. "500 points"',
            CERTIFICATE: 'e.g. "Spring Boot Professional"',
            PRIORITY_BOOST: 'e.g. "1 month boost"',
            DISCOUNT: 'e.g. "20% off premium"',
            FEATURED_PROFILE: 'e.g. "1 month featured"',
            TOKEN: 'e.g. "1000 tokens"',
            OTHER: 'e.g. "Custom reward"',
        };
        return placeholders[type || 'OTHER'] || placeholders['OTHER'];
    }

    getError(controlName: string): string | null {
        const c = this.form.get(controlName);
        if (!c || !c.errors || !c.touched) return null;
        const e = c.errors;
        if (e['required']) return 'This field is required.';
        if (e['minlength']) return `Min length is ${e['minlength'].requiredLength}.`;
        if (e['maxlength']) return `Max length is ${e['maxlength'].requiredLength}.`;
        if (e['maxParticipantsRange']) return 'Must be between 1 and 999,999.';
        return null;
    }

    getRewardError(group: AbstractControl, field: string): string | null {
        const c = group.get(field);
        if (!c || !c.errors || !c.touched) return null;
        const e = c.errors;
        if (e['required']) return 'Required.';
        if (e['maxlength']) return `Max ${e['maxlength'].requiredLength} characters.`;
        return null;
    }
}
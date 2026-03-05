import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  AfterViewInit,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';

declare global {
  interface Window {
    JitsiMeetExternalAPI: new (domain: string, options: JitsiMeetOptions) => JitsiMeetExternalAPIInstance;
  }
}

interface JitsiMeetOptions {
  roomName: string;
  width: string | number;
  height: string | number;
  parentNode: HTMLElement;
  configOverwrite?: Record<string, unknown>;
  userInfo?: { displayName?: string; email?: string };
}

interface JitsiMeetExternalAPIInstance {
  dispose: () => void;
}

interface VisioRoom {
  roomId: string;
  joinUrl: string;
}

@Component({
  selector: 'app-visio-call',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './visio-call.component.html',
})
export class VisioCallComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('jitsiContainer', { static: false }) jitsiContainer!: ElementRef<HTMLElement>;

  interviewId!: number;
  visio?: VisioRoom;
  loading = true;
  accessMessage = '';

  private jitsiApi: JitsiMeetExternalAPIInstance | null = null;
  private pendingRoom: VisioRoom | null = null;

  constructor(
    private route: ActivatedRoute,
    private interviewService: InterviewService,
    private toast: ToastService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.interviewId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.interviewId) {
      this.toast.error('Invalid interview id');
      this.loading = false;
      return;
    }
    this.interviewService.getById(this.interviewId).subscribe({
      next: (i) => {
        const now = Date.now();
        const start = new Date(i.startAt).getTime();
        const end = new Date(i.endAt).getTime();

        if (now < start) {
          this.accessMessage = 'You cannot join before the scheduled start time.';
          this.loading = false;
          this.toast.error(this.accessMessage);
          return;
        }
        if (now > end) {
          this.accessMessage = 'You cannot join after the scheduled end time.';
          this.loading = false;
          this.toast.error(this.accessMessage);
          return;
        }

        this.interviewService.getVisioRoom(this.interviewId).subscribe({
          next: (room) => {
            this.visio = room;
            this.pendingRoom = room;
            this.loading = false;
            this.cdr.detectChanges();
            setTimeout(() => this.initJitsiIfReady(), 0);
          },
          error: (err) => {
            this.loading = false;
            this.toast.error(err?.error?.message || 'Failed to open online meeting');
          },
        });
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.message || 'Failed to load interview');
      },
    });
  }

  ngAfterViewInit(): void {
    this.initJitsiIfReady();
  }

  ngOnDestroy(): void {
    if (this.jitsiApi) {
      this.jitsiApi.dispose();
      this.jitsiApi = null;
    }
  }

  private initJitsiIfReady(): void {
    if (!this.pendingRoom || !this.jitsiContainer?.nativeElement) return;
    const parent = this.jitsiContainer.nativeElement;
    if (!parent || this.jitsiApi) return;

    const parsed = this.parseJoinUrl(this.pendingRoom.joinUrl);
    if (!parsed.domain || !parsed.roomName) {
      this.toast.error('Invalid meeting URL');
      return;
    }

    const user = this.auth.currentUser();
    const displayName = user?.fullName?.trim() || user?.email || 'Participant';

    const scriptUrl = this.getJitsiScriptUrl(this.pendingRoom.joinUrl);

    const loadScript = (): Promise<void> => {
      if (typeof window.JitsiMeetExternalAPI !== 'undefined') return Promise.resolve();
      return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = scriptUrl;
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Failed to load Jitsi script'));
        document.head.appendChild(script);
      });
    };

    loadScript()
      .then(() => {
        this.jitsiApi = new window.JitsiMeetExternalAPI(parsed.domain, {
          roomName: parsed.roomName,
          width: '100%',
          height: '100%',
          parentNode: parent,
          configOverwrite: {
            everyoneIsModerator: true,
          },
          userInfo: {
            displayName,
          },
        });
        this.pendingRoom = null;
      })
      .catch(() => this.toast.error('Could not load video meeting. Please refresh.'));
  }

  private getJitsiScriptUrl(joinUrl: string): string {
    try {
      const u = new URL(joinUrl);
      return u.origin + '/external_api.js';
    } catch {
      return 'https://meet.jit.si/external_api.js';
    }
  }

  private parseJoinUrl(joinUrl: string): { domain: string; roomName: string } {
    try {
      const u = new URL(joinUrl);
      const path = u.pathname.replace(/^\/+/, '');
      return { domain: u.host, roomName: path || 'meet' };
    } catch {
      return { domain: '', roomName: '' };
    }
  }
}

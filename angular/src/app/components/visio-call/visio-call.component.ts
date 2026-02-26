import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';

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
export class VisioCallComponent implements OnInit {
  interviewId!: number;
  visio?: VisioRoom;
  safeUrl?: SafeResourceUrl;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private interviewService: InterviewService,
    private toast: ToastService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.interviewId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.interviewId) {
      this.toast.error('Invalid interview id');
      this.loading = false;
      return;
    }
    this.interviewService.getVisioRoom(this.interviewId).subscribe({
      next: (room) => {
        this.visio = room;
        this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(room.joinUrl);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.message || 'Failed to open online meeting');
      },
    });
  }
}


import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { phaseMeetingJitsiUrl, phaseMeetingRoomName } from '../../utils/phase-meeting-room.util';

@Component({
  selector: 'app-phase-meeting-room',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './phase-meeting-room.component.html',
})
export class PhaseMeetingRoomComponent implements OnInit {
  projectId = 0;
  phaseId = 0;
  meetingId = 0;
  roomName = '';
  iframeSrc: SafeResourceUrl | null = null;
  /** Même URL que l’iframe, pour ouvrir dans un onglet si le navigateur bloque micro/caméra dans l’iframe. */
  directJitsiUrl = '';
  backLink: (string | number)[] = ['/'];
  invalidParams = false;
  displayName = '';

  constructor(
    private route: ActivatedRoute,
    private sanitizer: DomSanitizer,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const pid =
      this.route.snapshot.paramMap.get('projectId') ?? this.route.snapshot.paramMap.get('id');
    this.projectId = Number(pid);
    this.phaseId = Number(this.route.snapshot.paramMap.get('phaseId'));
    this.meetingId = Number(this.route.snapshot.paramMap.get('meetingId'));

    if (
      !Number.isFinite(this.projectId) ||
      this.projectId <= 0 ||
      !Number.isFinite(this.phaseId) ||
      this.phaseId <= 0 ||
      !Number.isFinite(this.meetingId) ||
      this.meetingId <= 0
    ) {
      this.invalidParams = true;
      return;
    }

    this.roomName = phaseMeetingRoomName(this.projectId, this.phaseId, this.meetingId);
    const u = this.auth.getCurrentUser();
    this.displayName =
      String(u?.fullName ?? '')
        .trim() ||
      [u?.firstName, u?.lastName].filter(Boolean).join(' ').trim() ||
      String(u?.username ?? '').trim() ||
      String(u?.email ?? '').trim() ||
      '';

    const url = phaseMeetingJitsiUrl(this.projectId, this.phaseId, this.meetingId);
    this.directJitsiUrl = url;
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(url);

    const path = typeof window !== 'undefined' ? window.location.pathname : '';
    if (path.includes('/freelancer/')) {
      this.backLink = ['/freelancer', 'supervision'];
    } else {
      this.backLink = ['/project-owner', 'projects', this.projectId, 'supervision'];
    }
  }
}

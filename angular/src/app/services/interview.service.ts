import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AvailabilitySlot,
  AvailabilitySlotCreateRequest,
  Interview,
  InterviewCreateRequest,
  InterviewStatus,
  InterviewUpdateRequest,
  MeetingMode,
  PageResponse,
} from '../models/interview.model';

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly interviewsApi = '/api/interviews';
  private readonly availabilityApi = '/api/availability';

  constructor(private http: HttpClient) {}

  searchInterviews(params: {
    freelancerId?: number;
    ownerId?: number;
    projectId?: number;
    candidatureId?: number;
    status?: InterviewStatus;
    mode?: MeetingMode;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PageResponse<Interview>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') return;
      httpParams = httpParams.set(k, String(v));
    });
    return this.http.get<PageResponse<Interview>>(this.interviewsApi, { params: httpParams });
  }

  createInterview(req: InterviewCreateRequest): Observable<Interview> {
    return this.http.post<Interview>(this.interviewsApi, req);
  }

  getVisioRoom(interviewId: number): Observable<{ roomId: string; joinUrl: string }> {
    return this.http.get<{ roomId: string; joinUrl: string }>(`${this.interviewsApi}/${interviewId}/visio-room`);
  }

  updateInterview(interviewId: number, req: InterviewUpdateRequest): Observable<Interview> {
    return this.http.put<Interview>(`${this.interviewsApi}/${interviewId}`, req);
  }

  confirmInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/confirm`, {});
  }

  cancelInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/cancel`, {});
  }

  completeInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/complete`, {});
  }

  deleteInterview(interviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.interviewsApi}/${interviewId}`);
  }

  createAvailabilitySlot(freelancerId: number, req: AvailabilitySlotCreateRequest): Observable<AvailabilitySlot> {
    return this.http.post<AvailabilitySlot>(`${this.availabilityApi}/freelancers/${freelancerId}/slots`, req);
  }

  createAvailabilitySlotsBatch(
    freelancerId: number,
    slots: AvailabilitySlotCreateRequest[]
  ): Observable<AvailabilitySlot[]> {
    return this.http.post<AvailabilitySlot[]>(
      `${this.availabilityApi}/freelancers/${freelancerId}/slots/batch`,
      { slots }
    );
  }

  listAvailabilitySlots(params: {
    freelancerId: number;
    onlyFree?: boolean;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PageResponse<AvailabilitySlot>> {
    const { freelancerId, ...rest } = params;
    let httpParams = new HttpParams();
    Object.entries(rest).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') return;
      httpParams = httpParams.set(k, String(v));
    });
    return this.http.get<PageResponse<AvailabilitySlot>>(`${this.availabilityApi}/freelancers/${freelancerId}/slots`, {
      params: httpParams,
    });
  }

  deleteAvailabilitySlot(slotId: number): Observable<void> {
    return this.http.delete<void>(`${this.availabilityApi}/slots/${slotId}`);
  }
}

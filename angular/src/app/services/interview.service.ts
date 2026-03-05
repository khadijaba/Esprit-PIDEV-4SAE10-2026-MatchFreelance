import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AvailabilitySlot,
  AvailabilitySlotCreateRequest,
  Interview,
  InterviewAlternativeSuggestion,
  InterviewCreateRequest,
  InterviewStatus,
  InterviewUpdateRequest,
  MeetingMode,
  PageResponse,
  ReliabilitySummary,
  ReviewAggregate,
  ReviewCreateRequest,
  ReviewResponse,
  TopFreelancerInInterviews,
  WorkloadSummary,
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

  suggestAlternatives(req: InterviewCreateRequest): Observable<InterviewAlternativeSuggestion[]> {
    return this.http.post<InterviewAlternativeSuggestion[]>(`${this.interviewsApi}/suggestions`, req);
  }

  getById(interviewId: number): Observable<Interview> {
    return this.http.get<Interview>(`${this.interviewsApi}/${interviewId}`);
  }

  downloadIcs(interviewId: number): Observable<Blob> {
    return this.http.get(`${this.interviewsApi}/${interviewId}/ics`, {
      responseType: 'blob',
    });
  }

  /** Download all not-completed (PROPOSED + CONFIRMED) interviews as Excel. */
  downloadNotCompletedInterviewsExcel(params?: { ownerId?: number; freelancerId?: number }): Observable<Blob> {
    let httpParams = new HttpParams();
    if (params?.ownerId != null) httpParams = httpParams.set('ownerId', String(params.ownerId));
    if (params?.freelancerId != null) httpParams = httpParams.set('freelancerId', String(params.freelancerId));
    return this.http.get(`${this.interviewsApi}/export/excel`, {
      params: httpParams,
      responseType: 'blob',
    });
  }

  getReviewsForInterview(
    interviewId: number,
    page = 0,
    size = 10
  ): Observable<PageResponse<ReviewResponse>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<PageResponse<ReviewResponse>>(
      `${this.interviewsApi}/${interviewId}/reviews`,
      { params }
    );
  }

  createReview(
    interviewId: number,
    reviewerId: number,
    body: ReviewCreateRequest
  ): Observable<ReviewResponse> {
    const params = new HttpParams().set('reviewerId', String(reviewerId));
    return this.http.post<ReviewResponse>(
      `${this.interviewsApi}/${interviewId}/reviews`,
      body,
      { params }
    );
  }

  getReviewsForReviewee(
    revieweeId: number,
    page = 0,
    size = 10
  ): Observable<PageResponse<ReviewResponse>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<PageResponse<ReviewResponse>>(
      `/api/reviews/reviewee/${revieweeId}`,
      { params }
    );
  }

  getReviewAggregate(revieweeId: number): Observable<ReviewAggregate> {
    return this.http.get<ReviewAggregate>(
      `/api/reviews/reviewee/${revieweeId}/aggregate`
    );
  }

  getVisioRoom(interviewId: number): Observable<{ roomId: string; joinUrl: string }> {
    return this.http.get<{ roomId: string; joinUrl: string }>(`${this.interviewsApi}/${interviewId}/visio-room`);
  }

  getReliabilityForFreelancer(freelancerId: number): Observable<ReliabilitySummary> {
    return this.http.get<ReliabilitySummary>(`${this.interviewsApi}/reliability`, {
      params: new HttpParams().set('freelancerId', String(freelancerId)),
    });
  }

  getReliabilityForOwner(ownerId: number): Observable<ReliabilitySummary> {
    return this.http.get<ReliabilitySummary>(`${this.interviewsApi}/reliability`, {
      params: new HttpParams().set('ownerId', String(ownerId)),
    });
  }

  getWorkloadForFreelancer(freelancerId: number): Observable<WorkloadSummary> {
    return this.http.get<WorkloadSummary>(`${this.interviewsApi}/workload`, {
      params: new HttpParams().set('freelancerId', String(freelancerId)),
    });
  }

  getTopFreelancers(params: {
    ownerId?: number;
    limit?: number;
    minReviews?: number;
  }): Observable<TopFreelancerInInterviews[]> {
    let httpParams = new HttpParams();
    if (params.ownerId != null) httpParams = httpParams.set('ownerId', String(params.ownerId));
    if (params.limit != null) httpParams = httpParams.set('limit', String(params.limit));
    if (params.minReviews != null) httpParams = httpParams.set('minReviews', String(params.minReviews));
    return this.http.get<TopFreelancerInInterviews[]>(`${this.interviewsApi}/top-freelancers`, {
      params: httpParams,
    });
  }

  updateInterview(interviewId: number, req: InterviewUpdateRequest): Observable<Interview> {
    return this.http.put<Interview>(`${this.interviewsApi}/${interviewId}`, req);
  }

  confirmInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/confirm`, {});
  }

  rejectInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/reject`, {});
  }

  cancelInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/cancel`, {});
  }

  completeInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/complete`, {});
  }

  noShowInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.interviewsApi}/${interviewId}/no-show`, {});
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

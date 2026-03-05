export type InterviewStatus = 'PROPOSED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export type MeetingMode = 'ONLINE' | 'FACE_TO_FACE';

export interface Interview {
  id: number;
  candidatureId?: number | null;
  projectId?: number | null;
  freelancerId: number;
  ownerId: number;
  slotId?: number | null;
  startAt: string;
  endAt: string;
  mode: MeetingMode;
  meetingUrl?: string | null;
  addressLine?: string | null;
  city?: string | null;
  lat?: number | null;
  lng?: number | null;
  status: InterviewStatus;
  notes?: string | null;
  createdAt?: string;
}

export interface InterviewCreateRequest {
  candidatureId?: number;
  projectId?: number;
  freelancerId: number;
  ownerId: number;
  slotId?: number;
  startAt?: string;
  endAt?: string;
  durationMinutes?: number;
  mode: MeetingMode;
  meetingUrl?: string;
  addressLine?: string;
  city?: string;
  lat?: number;
  lng?: number;
  notes?: string;
}

export interface InterviewUpdateRequest {
  mode?: MeetingMode;
  meetingUrl?: string | null;
  addressLine?: string | null;
  city?: string | null;
  lat?: number | null;
  lng?: number | null;
  status?: InterviewStatus;
  notes?: string | null;
}

export interface AvailabilitySlot {
  id: number;
  freelancerId: number;
  startAt: string;
  endAt: string;
  booked: boolean;
  bookedInterviewId?: number | null;
}

export interface AvailabilitySlotCreateRequest {
  startAt: string;
  endAt: string;
}

export interface InterviewAlternativeSuggestion {
  startAt: string;
  endAt: string;
  slotId: number | null;
  score: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ReliabilitySummary {
  userId: number;
  role: 'FREELANCER' | 'OWNER';
  score: number;
  completedCount: number;
  noShowCount: number;
  cancelledCount: number;
  from: string;
  to: string;
}

export interface WorkloadSummary {
  freelancerId: number;
  from: string;
  to: string;
  totalMinutes7: number;
  totalMinutes1: number;
  interviewsNext24h: number;
  interviewsNext3d: number;
  interviewsNext7d: number;
  maxDailyMinutes: number;
  level: 'LIGHT' | 'NORMAL' | 'BUSY' | 'OVERLOADED';
}

export interface TopFreelancerInInterviews {
  freelancerId: number;
  combinedScore: number;
  reliabilityScore: number;
  averageReviewScore: number | null;
  reviewCount: number;
  completedCount: number;
  noShowCount: number;
  cancelledCount: number;
}

export interface ReviewCreateRequest {
  revieweeId: number;
  score: number;
  comment?: string;
}

export interface ReviewResponse {
  id: number;
  interviewId: number;
  reviewerId: number;
  revieweeId: number;
  score: number;
  comment: string | null;
  createdAt: string;
}

export interface ReviewAggregate {
  revieweeId: number;
  averageScore: number | null;
  reviewCount: number;
}

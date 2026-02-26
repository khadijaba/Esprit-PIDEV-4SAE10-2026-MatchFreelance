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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

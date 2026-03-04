export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Interview {
  id: number;
  candidatureId: number;
  scheduledAt: string;
  status: InterviewStatus;
  notes?: string;
}

export interface InterviewRequest {
  scheduledAt: string;
  status?: InterviewStatus;
  notes?: string;
}

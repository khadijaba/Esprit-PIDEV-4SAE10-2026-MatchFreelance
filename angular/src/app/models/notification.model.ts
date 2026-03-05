import { PageResponse } from './interview.model';

export type NotificationType =
  | 'INTERVIEW_PROPOSED'
  | 'INTERVIEW_CONFIRMED'
  | 'INTERVIEW_CANCELLED'
  | 'INTERVIEW_COMPLETED'
  | 'INTERVIEW_NO_SHOW'
  | 'REMINDER_24H'
  | 'REMINDER_1H';

export interface Notification {
  id: number;
  userId: number;
  interviewId: number;
  type: NotificationType;
  message: string;
  readAt?: string | null;
  createdAt: string;
}

export type NotificationPage = PageResponse<Notification>;


export type FeedbackCategory = 'CONTENT' | 'ORGANIZATION' | 'SPEAKER' | 'VENUE' | 'OVERALL';
export type FeedbackStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'FLAGGED';

export interface Feedback {
  id: number;
  eventId: number;
  eventTitle: string;
  rating: number;
  comment: string;
  authorName: string;
  authorEmail: string;
  category: FeedbackCategory;
  status: FeedbackStatus;
  anonymous: boolean;
  createdAt: string;
}

export interface CreateFeedbackRequest {
  eventId: number;
  rating: number;
  comment: string;
  authorName: string;
  authorEmail: string;
  category: FeedbackCategory;
  anonymous: boolean;
}

export interface FeedbackStats {
  eventId: number;
  eventTitle: string;
  averageRating: number;
  totalFeedbacks: number;
  ratingDistribution: { [key: number]: number };
}

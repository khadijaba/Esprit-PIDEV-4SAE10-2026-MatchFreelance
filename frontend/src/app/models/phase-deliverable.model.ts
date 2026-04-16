export interface PhaseDeliverable {
  id: number;
  title: string;
  description: string | null;
  type: string;
  reviewStatus: string;
  reviewComment: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
}

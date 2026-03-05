/**
 * Notification envoyée au freelancer (sélection équipe projet).
 */
export interface Notification {
  id: string;
  freelancerId: number;
  projectId: number;
  projectTitle: string;
  subject: string;
  message: string;
  read: boolean;
  createdAt: string; // ISO
  email?: string;
}

import { Reward, RewardRequest } from './reward.model';

export type EventType = 'HACKATHON' | 'CHALLENGE' | 'WEBINAR' | 'MILESTONE' | 'COMMUNITY' | 'SKILL_RACE' | 'FORMATION_WEBINAR' | 'OTHER';
export type EventStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELLED' | 'CLOSED';
export type ParticipationStatus = 'REGISTERED' | 'ACTIVE' | 'COMPLETED' | 'DISQUALIFIED';

export interface Event {
  id: number;
  title: string;
  description: string;
  type: EventType;
  startDate: string;
  endDate: string;
  eligibilityCriteria: string;
  maxParticipants: number | null;
  teamEvent: boolean;
  status: EventStatus;
  createdById: number | null;
  createdAt: string;
  updatedAt: string;
  plannedRewards?: Reward[];
}

export interface EventRequest {
  title: string;
  description: string;
  type: EventType;
  startDate: string;
  endDate: string;
  eligibilityCriteria?: string;
  maxParticipants?: number | null;
  teamEvent?: boolean;
  status?: EventStatus;
  createdById?: number | null;
  plannedRewards?: RewardRequest[];
}

export interface Participation {
  eventId: number;
  userId: number;
  status: ParticipationStatus;
  score: number | null;
  teamId: number | null;
  joinedAt: string;
}

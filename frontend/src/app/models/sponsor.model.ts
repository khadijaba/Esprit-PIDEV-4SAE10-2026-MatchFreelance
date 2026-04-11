export type SponsorType = 'COMPANY' | 'INDIVIDUAL' | 'ORGANIZATION' | 'STARTUP';
export type SponsorshipStatus = 'PENDING' | 'CONFIRMED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type ContributionType = 'MONETARY' | 'REWARDS' | 'SERVICES' | 'EQUIPMENT' | 'VENUE';

export interface Sponsor {
  id: number;
  name: string;
  description: string;
  website: string | null;
  email: string | null;
  phone: string | null;
  type: SponsorType;
  totalBudget: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  sponsorships: Sponsorship[];
  totalSponsorships: number;
  totalSpent: number;
}

export interface CreateSponsorRequest {
  name: string;
  description?: string;
  website?: string;
  email?: string;
  phone?: string;
  type?: SponsorType;
  totalBudget?: number;
}

export interface Sponsorship {
  id: number;
  sponsorId: number;
  sponsorName: string;
  eventId: number;
  eventTitle: string;
  rewardId: number | null;
  contributionType: ContributionType;
  amount: number;
  description: string;
  status: SponsorshipStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSponsorshipRequest {
  sponsorId: number;
  eventId: number;
  rewardId?: number;
  contributionType?: ContributionType;
  amount?: number;
  description?: string;
  status?: SponsorshipStatus;
}

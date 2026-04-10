export type RewardType = 'BADGE' | 'POINTS' | 'CERTIFICATE' | 'PRIORITY_BOOST' | 'DISCOUNT' | 'FEATURED_PROFILE' | 'TOKEN' | 'OTHER';
export type RewardStatus = 'PENDING' | 'DELIVERED' | 'EXPIRED' | 'REVOKED';

export interface Reward {
    id: number;
    recipientId?: number | null;
    eventId: number | null;
    type: RewardType;
    value: string;
    description: string;
    dateAwarded?: string;
    status?: RewardStatus;
    visibleOnProfile: boolean;
    createdAt?: string;
    updatedAt?: string;
}

export interface RewardRequest {
    recipientId?: number | null;
    eventId?: number | null;
    type: RewardType;
    value: string;
    description?: string;
    visibleOnProfile?: boolean;
}

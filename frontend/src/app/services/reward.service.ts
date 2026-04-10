import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Reward, RewardRequest } from '../models/reward.model';

@Injectable({ providedIn: 'root' })
export class RewardService {
    private readonly api = '/api/events';

    constructor(private http: HttpClient) { }

    awardReward(eventId: number, reward: RewardRequest): Observable<Reward> {
        return this.http.post<Reward>(`${this.api}/${eventId}/award`, reward);
    }

    getEventRewards(eventId: number): Observable<Reward[]> {
        return this.http.get<Reward[]>(`${this.api}/${eventId}/rewards`);
    }

    getUserRewards(userId: number): Observable<Reward[]> {
        return this.http.get<Reward[]>(`${this.api}/users/${userId}/rewards`);
    }

    revokeReward(rewardId: number): Observable<Reward> {
        return this.http.put<Reward>(`${this.api}/rewards/${rewardId}/revoke`, {});
    }
}

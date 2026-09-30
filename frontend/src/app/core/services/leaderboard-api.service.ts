import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import { LeaderboardEntry } from '../models/leaderboard.model';

@Injectable({ providedIn: 'root' })
export class LeaderboardApiService {
    private readonly http = inject(HttpClient);
    private stompClient: Client | null = null;
    private readonly leaderboardUpdates = new Subject<LeaderboardEntry>();

    getLeaderboard(circuitId: number, season?: number): Observable<LeaderboardEntry[]> {
        const url = season
            ? `/api/leaderboard?circuitId=${circuitId}&season=${season}`
            : `/api/leaderboard?circuitId=${circuitId}`;
        return this.http.get<LeaderboardEntry[]>(url);
    }

    liveUpdates(circuitId: number): Observable<LeaderboardEntry> {
        this.stompClient?.deactivate();
        this.connect(circuitId);
        return this.leaderboardUpdates.asObservable();
    }

    private connect(circuitId: number): void {
        this.stompClient = new Client({
            brokerURL: 'ws://localhost:8090/ws',
            reconnectDelay: 5000
        });

        this.stompClient.onConnect = () => {
            this.stompClient!.subscribe(`/topic/leaderboard/${circuitId}`, (message) => {
                const entry: LeaderboardEntry = JSON.parse(message.body);
                this.leaderboardUpdates.next(entry);
            });
        };

        this.stompClient.activate();
    }
}
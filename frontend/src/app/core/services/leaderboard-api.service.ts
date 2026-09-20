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

    getLeaderboard(): Observable<LeaderboardEntry[]> {
        return this.http.get<LeaderboardEntry[]>('/api/leaderboard');
    }

    liveUpdates(): Observable<LeaderboardEntry> {
        if (!this.stompClient) {
            this.connect();
        }
        return this.leaderboardUpdates.asObservable();
    }

    private connect(): void {
        this.stompClient = new Client({
            brokerURL: 'ws://localhost:8090/ws',
            reconnectDelay: 5000
        });

        this.stompClient.onConnect = () => {
            this.stompClient!.subscribe('/topic/leaderboard', (message) => {
                const entry: LeaderboardEntry = JSON.parse(message.body);
                this.leaderboardUpdates.next(entry);
            });
        };

        this.stompClient.activate();
    }
}
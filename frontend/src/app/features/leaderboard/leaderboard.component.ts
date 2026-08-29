import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LeaderboardApiService } from '../../core/services/leaderboard-api.service';
import { LeaderboardEntry } from '../../core/models/leaderboard.model';

@Component({
    selector: 'app-leaderboard',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './leaderboard.component.html',
    styleUrl: './leaderboard.component.scss'
})
export class LeaderboardComponent {
    private readonly api = inject(LeaderboardApiService);

    readonly entries = signal<LeaderboardEntry[]>([]);
    readonly isLoading = signal(true);

    constructor() {
        this.api.getLeaderboard().subscribe(list => {
            this.entries.set(list);
            this.isLoading.set(false);
        });
    }
}
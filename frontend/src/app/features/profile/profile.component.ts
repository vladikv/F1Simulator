import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { ProfileApiService } from '../../core/services/profile-api.service';
import { ProfileSimulation } from '../../core/models/profile.model';

@Component({
    selector: 'app-profile',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './profile.component.html',
    styleUrl: './profile.component.scss'
})
export class ProfileComponent {
    readonly auth = inject(AuthService);
    private readonly profileApi = inject(ProfileApiService);

    readonly history = signal<ProfileSimulation[]>([]);
    readonly isLoading = signal(true);

    readonly scoredCount = computed(() =>
        this.history().filter(s => s.deltaVsActualSeconds !== null).length
    );

    readonly averageAbsDelta = computed(() => {
        const scored = this.history().filter(s => s.deltaVsActualSeconds !== null);
        if (scored.length === 0) return null;

        const sum = scored.reduce((acc, s) => acc + Math.abs(s.deltaVsActualSeconds!), 0);
        return sum / scored.length;
    });

    constructor() {
        this.profileApi.getHistory().subscribe(history => {
            this.history.set(history);
            this.isLoading.set(false);
        });
    }

    deltaClass(sim: ProfileSimulation): 'scored' | 'pending' {
        return sim.deltaVsActualSeconds !== null ? 'scored' : 'pending';
    }

    deltaLabel(sim: ProfileSimulation): string {
        if (sim.deltaVsActualSeconds === null) return 'Race not finished yet';

        const sign = sim.deltaVsActualSeconds > 0 ? '+' : '-';
        const totalMs = Math.round(Math.abs(sim.deltaVsActualSeconds) * 1000);
        const minutes = Math.floor(totalMs / 60000);
        const seconds = Math.floor((totalMs % 60000) / 1000);
        const millis = totalMs % 1000;

        return `${sign}${minutes}:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}`;
    }
}
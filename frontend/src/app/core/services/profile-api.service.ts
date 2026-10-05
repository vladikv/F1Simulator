import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileSimulation } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
    private readonly http = inject(HttpClient);

    getHistory(): Observable<ProfileSimulation[]> {
        return this.http.get<ProfileSimulation[]>('/api/profile/history');
    }
}
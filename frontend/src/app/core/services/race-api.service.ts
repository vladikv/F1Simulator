import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RaceSummary } from '../models/race.model';

@Injectable({ providedIn: 'root' })
export class RaceApiService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = '/api/races';

    getRaces(): Observable<RaceSummary[]> {
        return this.http.get<RaceSummary[]>(this.baseUrl);
    }

    getRace(id: number): Observable<RaceSummary> {
        return this.http.get<RaceSummary>(`${this.baseUrl}/${id}`);
    }
}
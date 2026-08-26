import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {RaceDriver, RaceSummary} from '../models/race.model';

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

    getDrivers(raceId: number): Observable<RaceDriver[]> {
        return this.http.get<RaceDriver[]>(`${this.baseUrl}/${raceId}/drivers`);
    }
}
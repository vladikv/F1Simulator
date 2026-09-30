import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {RaceDriver, RaceSummary, WeatherWindow} from '../models/race.model';

@Injectable({ providedIn: 'root' })
export class RaceApiService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = '/api/races';

    getRaces(season?: number): Observable<RaceSummary[]> {
        const url = season ? `${this.baseUrl}?season=${season}` : this.baseUrl;
        return this.http.get<RaceSummary[]>(url);
    }

    getSeasons(): Observable<number[]> {
        return this.http.get<number[]>(`${this.baseUrl}/seasons`);
    }

    getRace(id: number): Observable<RaceSummary> {
        return this.http.get<RaceSummary>(`${this.baseUrl}/${id}`);
    }

    getDrivers(raceId: number): Observable<RaceDriver[]> {
        return this.http.get<RaceDriver[]>(`${this.baseUrl}/${raceId}/drivers`);
    }

    getWeatherWindows(raceId: number): Observable<WeatherWindow[]> {
        return this.http.get<WeatherWindow[]>(`${this.baseUrl}/${raceId}/weather`);
    }
}
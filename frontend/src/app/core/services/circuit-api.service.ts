import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CircuitSummary } from '../models/race.model';

@Injectable({ providedIn: 'root' })
export class CircuitApiService {
    private readonly http = inject(HttpClient);

    getCircuits(): Observable<CircuitSummary[]> {
        return this.http.get<CircuitSummary[]>('/api/circuits');
    }
}
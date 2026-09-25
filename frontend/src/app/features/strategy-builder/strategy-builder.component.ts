import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { StrategyApiService } from '../../core/services/strategy-api.service';
import { RaceApiService } from '../../core/services/race-api.service';
import {RaceSummary, WeatherWindow} from '../../core/models/race.model';
import { Stint, StrategySimulationResponse, TyreCompound } from '../../core/models/strategy.model';
import { StrategyRingComponent } from '../../shared/components/strategy-ring/strategy-ring.component';
import { CircuitTrackComponent } from '../../shared/components/circuit-track/circuit-track.component';
import { CIRCUIT_TRACKS } from '../../core/data/circuit-tracks.data';
import { CIRCUIT_TO_OPENF1_NAME } from '../../core/data/circuit-race-map';

const COMPOUND_COLOR_VAR: Record<TyreCompound, string> = {
  SOFT: 'var(--tyre-soft)',
  MEDIUM: 'var(--tyre-medium)',
  HARD: 'var(--tyre-hard)',
  INTERMEDIATE: 'var(--tyre-intermediate)',
  WET: 'var(--tyre-wet)'
};

@Component({
  selector: 'app-strategy-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, StrategyRingComponent, CircuitTrackComponent, RouterLink],
  templateUrl: './strategy-builder.component.html',
  styleUrl: './strategy-builder.component.scss'
})
export class StrategyBuilderComponent {
  // Bound automatically from the URL by withComponentInputBinding().
  raceId = input.required<number>();
  driverId = input.required<number>();

  private readonly api = inject(StrategyApiService);
  private readonly raceApi = inject(RaceApiService);

  readonly race = signal<RaceSummary | null>(null);
  readonly totalLaps = computed(() => this.race()?.totalLaps ?? 0);

  readonly weatherWindows = signal<WeatherWindow[]>([]);

  // Reverse lookup: race.circuitName (OpenF1) -> local track id -> full track data.
  // Same matching key as race-list's matchedRace(), just inverted.
  readonly matchedTrack = computed(() => {
    const circuitName = this.race()?.circuitName;
    if (!circuitName) return null;

    const trackId = Object.entries(CIRCUIT_TO_OPENF1_NAME)
        .find(([, name]) => name === circuitName)?.[0];

    return CIRCUIT_TRACKS.find(t => t.id === trackId) ?? null;
  });

  constructor() {
    // effect() re-runs whenever raceId() changes — covers navigating
    // directly between two /strategy/:raceId/:driverId URLs without a
    // full page reload, not just the first load.
    effect(() => {
      const id = this.raceId();
      this.raceApi.getRace(id).subscribe(race => this.race.set(race));
      this.raceApi.getWeatherWindows(id).subscribe(windows => this.weatherWindows.set(windows));
    });
  }

  readonly stints = signal<Stint[]>([
    { compound: 'MEDIUM', startLap: 1, endLap: 20 }
  ]);

  readonly compounds: TyreCompound[] = ['SOFT', 'MEDIUM', 'HARD', 'INTERMEDIATE', 'WET'];

  readonly result = signal<StrategySimulationResponse | null>(null);
  readonly isSimulating = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly lapsCovered = computed(() => {
    const list = this.stints();
    return list.length === 0 ? 0 : list[list.length - 1].endLap;
  });

  readonly isStrategyValid = computed(() => this.lapsCovered() === this.totalLaps());

  readonly coveragePercent = computed(() => {
    const total = this.totalLaps();
    return total > 0 ? Math.min(100, (this.lapsCovered() / total) * 100) : 0;
  });

  addStint(): void {
    const current = this.stints();
    const lastEndLap = current.length > 0 ? current[current.length - 1].endLap : 0;

    this.stints.update(list => [
      ...list,
      { compound: 'HARD', startLap: lastEndLap + 1, endLap: this.totalLaps() }
    ]);
  }

  removeStint(index: number): void {
    this.stints.update(list => list.filter((_, i) => i !== index));
  }

  updateStint(index: number, patch: Partial<Stint>): void {
    this.stints.update(list => {
      const updated = list.map((stint, i) => (i === index ? { ...stint, ...patch } : stint));

      // If endLap changed, push the next stint's startLap forward to stay
      // contiguous — but only if the next stint hasn't been touched
      // manually to start elsewhere (i.e. it was already startLap = old endLap + 1).
      if (patch.endLap !== undefined && index + 1 < updated.length) {
        const oldEndLap = list[index].endLap;
        const nextStint = updated[index + 1];
        if (nextStint.startLap === oldEndLap + 1) {
          updated[index + 1] = { ...nextStint, startLap: patch.endLap + 1 };
        }
      }

      return updated;
    });
  }

  runSimulation(): void {
    if (!this.isStrategyValid()) {
      this.errorMessage.set(`Stints must cover exactly ${this.totalLaps()} laps (currently ${this.lapsCovered()}).`);
      return;
    }

    this.isSimulating.set(true);
    this.errorMessage.set(null);

    this.api.simulate({
      raceId: this.raceId(),
      driverId: this.driverId(),
      stints: this.stints()
    }).subscribe({
      next: (response) => {
        this.result.set(response);
        this.isSimulating.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Simulation failed: ' + (err.error?.message ?? 'unknown error'));
        this.isSimulating.set(false);
      }
    });
  }

  colorFor(compound: TyreCompound): string {
    return COMPOUND_COLOR_VAR[compound];
  }

  stintLabel(index: number): string {
    return String(index + 1).padStart(2, '0');
  }

  deltaClass(res: StrategySimulationResponse): 'faster' | 'slower' | null {
    if (res.deltaVsActualSeconds === null) return null;
    return res.deltaVsActualSeconds < 0 ? 'faster' : 'slower';
  }

  deltaLabel(res: StrategySimulationResponse): string {
    if (res.deltaVsActualSeconds === null) return '—';

    const sign = res.deltaVsActualSeconds > 0 ? '+' : '-';
    const totalMs = Math.round(Math.abs(res.deltaVsActualSeconds) * 1000);

    const minutes = Math.floor(totalMs / 60000);
    const seconds = Math.floor((totalMs % 60000) / 1000);
    const millis = totalMs % 1000;

    return `${sign}${minutes}:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}`;
  }
}
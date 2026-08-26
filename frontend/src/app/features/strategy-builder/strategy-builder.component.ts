import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StrategyApiService } from '../../core/services/strategy-api.service';
import { RaceApiService } from '../../core/services/race-api.service';
import { RaceSummary } from '../../core/models/race.model';
import { Stint, StrategySimulationResponse, TyreCompound } from '../../core/models/strategy.model';
import { StrategyRingComponent } from '../../shared/components/strategy-ring/strategy-ring.component';

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
  imports: [CommonModule, FormsModule, StrategyRingComponent],
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

  constructor() {
    // effect() re-runs whenever raceId() changes — covers navigating
    // directly between two /strategy/:raceId/:driverId URLs without a
    // full page reload, not just the first load.
    effect(() => {
      const id = this.raceId();
      this.raceApi.getRace(id).subscribe(race => this.race.set(race));
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
      { compound: 'HARD', startLap: lastEndLap + 1, endLap: Math.min(lastEndLap + 15, this.totalLaps()) }
    ]);
  }

  removeStint(index: number): void {
    this.stints.update(list => list.filter((_, i) => i !== index));
  }

  updateStint(index: number, patch: Partial<Stint>): void {
    this.stints.update(list =>
        list.map((stint, i) => (i === index ? { ...stint, ...patch } : stint))
    );
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
}
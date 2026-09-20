import {Component, computed, effect, inject, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CircuitTrackComponent } from '../../shared/components/circuit-track/circuit-track.component';
import { CIRCUIT_TRACKS } from '../../core/data/circuit-tracks.data';
import { RaceApiService } from '../../core/services/race-api.service';
import {RaceDriver, RaceSummary} from '../../core/models/race.model';
import {CIRCUIT_TO_OPENF1_NAME} from "../../core/data/circuit-race-map";

const STANDARD_RACE_DISTANCE_M = 305_000;
const MONACO_RACE_DISTANCE_M = 260_000;
const PACE_SECONDS_PER_KM = 19;
const PIT_STOP_ALLOWANCE_SECONDS = 2 * 23;

@Component({
  selector: 'app-race-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, CircuitTrackComponent],
  templateUrl: './race-list.component.html',
  styleUrl: './race-list.component.scss'
})
export class RaceListComponent {
  private readonly raceApi = inject(RaceApiService);

  readonly availableTracks = CIRCUIT_TRACKS.filter(
      t => t.id in CIRCUIT_TO_OPENF1_NAME
  );
  readonly selectedTrackId = signal(
      CIRCUIT_TRACKS.find(t => t.id === 'mc-1929')?.id ?? CIRCUIT_TRACKS[0].id
  );

  readonly drivers = signal<RaceDriver[]>([]);
  readonly selectedDriverId = signal<number | null>(null);

  constructor() {
    this.raceApi.getRaces().subscribe(list => this.races.set(list));

    effect(() => {
      const race = this.matchedRace();
      this.drivers.set([]);
      this.selectedDriverId.set(null);
      if (!race) return;

      this.raceApi.getDrivers(race.id).subscribe(list => {
        this.drivers.set(list);
        this.selectedDriverId.set(list[0]?.id ?? null);
      });
    }, { allowSignalWrites: true });
  }

  // Real races synced from OpenF1 via RaceSyncService — separate from
  // the local GeoJSON track outlines, which exist purely for the visual.
  readonly races = signal<RaceSummary[]>([]);

  readonly selectedTrack = computed(() =>
      this.availableTracks.find(t => t.id === this.selectedTrackId())!
  );

  // Matched by country, not circuit name — OpenF1's circuit_short_name
  // ("Monte Carlo") and the local GeoJSON dataset's displayName ("Circuit
  // de Monaco") diverge in wording across most of the 40 tracks. Country
  // is a single clean field on both sides and almost always unambiguous
  // (one Grand Prix per country per season, with the exception of a
  // handful of double-header seasons like Emilia Romagna/Italy).
  readonly matchedRace = computed<RaceSummary | null>(() => {
    const openF1Name = CIRCUIT_TO_OPENF1_NAME[this.selectedTrackId()];
    if (!openF1Name) return null;
    return this.races().find(race => race.circuitName === openF1Name) ?? null;
  });

  readonly estimatedLaps = computed(() => {
    const track = this.selectedTrack();
    if (!track.lengthM) return null;
    const raceDistance = track.id === 'mc-1929' ? MONACO_RACE_DISTANCE_M : STANDARD_RACE_DISTANCE_M;
    return Math.round(raceDistance / track.lengthM);
  });

  readonly estimatedTotalTime = computed(() => {
    const track = this.selectedTrack();
    const laps = this.estimatedLaps();
    if (!laps || !track.lengthM) return null;

    const lapTimeSeconds = (track.lengthM / 1000) * PACE_SECONDS_PER_KM;
    const totalSeconds = lapTimeSeconds * laps + PIT_STOP_ALLOWANCE_SECONDS;
    return this.formatRaceTime(totalSeconds);
  });

  private formatRaceTime(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = Math.floor(totalSeconds % 60);
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  // // Strips punctuation/common words so "Circuit de Monaco" and "Monaco"
  // // can match each other. Deliberately loose — a heuristic, not a join key.
  // private normalize(name: string): string {
  //   return name.toLowerCase().trim();
  // }
}
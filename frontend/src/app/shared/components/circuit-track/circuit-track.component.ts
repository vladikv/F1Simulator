import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CircuitTrackData } from '../../../core/models/circuit-track.model';

@Component({
    selector: 'app-circuit-track',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './circuit-track.component.html',
    styleUrl: './circuit-track.component.scss'
})
export class CircuitTrackComponent {
    track = input.required<CircuitTrackData>();

    readonly pathId = `lap-path-${Math.random().toString(36).slice(2, 9)}`;
}
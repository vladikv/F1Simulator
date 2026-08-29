import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Stint, TyreCompound } from '../../../core/models/strategy.model';

/**
 * The signature visual element of the app: instead of a generic
 * horizontal Gantt bar, pit-stop strategy is rendered as a radial
 * ring — because laps around a circuit are, literally, a circle.
 * Each stint becomes a colored arc; pit stops become the gaps
 * between them.
 */

interface StintArc {
    compound: TyreCompound;
    startAngle: number; // degrees, 0 = top (12 o'clock)
    endAngle: number;
    pathD: string;
    midAngle: number;
}

const COMPOUND_COLOR_VAR: Record<TyreCompound, string> = {
    SOFT: 'var(--tyre-soft)',
    MEDIUM: 'var(--tyre-medium)',
    HARD: 'var(--tyre-hard)',
    INTERMEDIATE: 'var(--tyre-intermediate)',
    WET: 'var(--tyre-wet)'
};

@Component({
    selector: 'app-strategy-ring',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './strategy-ring.component.html',
    styleUrl: './strategy-ring.component.scss'
})
export class StrategyRingComponent {
    stints = input.required<Stint[]>();
    totalLaps = input.required<number>();
    formattedTotalTime = input<string | null>(null);

    private readonly radius = 130;
    private readonly center = 150;
    private readonly gapDegrees = 3; // visual gap at each pit stop

    // computed() re-derives the arc geometry only when stints/totalLaps
    // actually change — the trigonometry doesn't re-run on unrelated
    // change detection cycles.
    readonly arcs = computed<StintArc[]>(() => {
        const stints = this.stints();
        const total = this.totalLaps();
        if (!stints.length || !total) return [];

        return stints.map(stint => {
            const startAngle = (stint.startLap - 1) / total * 360 + this.gapDegrees / 2;
            const endAngle = stint.endLap / total * 360 - this.gapDegrees / 2;
            return {
                compound: stint.compound,
                startAngle,
                endAngle,
                midAngle: (startAngle + endAngle) / 2,
                pathD: this.describeArc(startAngle, endAngle)
            };
        });
    });

    colorFor(compound: TyreCompound): string {
        return COMPOUND_COLOR_VAR[compound];
    }

    /** Converts a start/end angle pair into an SVG arc path string. */
    private describeArc(startDeg: number, endDeg: number): string {
        const start = this.polarToCartesian(endDeg);
        const end = this.polarToCartesian(startDeg);
        const largeArcFlag = endDeg - startDeg <= 180 ? '0' : '1';

        return [
            'M', start.x, start.y,
            'A', this.radius, this.radius, 0, largeArcFlag, 0, end.x, end.y
        ].join(' ');
    }

    private polarToCartesian(angleDeg: number) {
        // -90 offset so 0 degrees points to 12 o'clock instead of 3 o'clock
        const angleRad = ((angleDeg - 90) * Math.PI) / 180;
        return {
            x: this.center + this.radius * Math.cos(angleRad),
            y: this.center + this.radius * Math.sin(angleRad)
        };
    }
}
export interface TrackCorner {
    number: number;
    x: number;
    y: number;
    labelOffset?: { dx: number; dy: number }; // manual override for tightly-clustered corners
}

export interface TrackSector {
    name: 'S1' | 'S2' | 'S3';
    pathD: string;
    colorVar: string;
}

export interface CircuitTrackData {
    id: string;
    displayName: string;
    location: string;
    lengthM: number | null;
    viewBox: string;
    sectors: TrackSector[];
    fullPathD: string;
    corners: TrackCorner[];
}
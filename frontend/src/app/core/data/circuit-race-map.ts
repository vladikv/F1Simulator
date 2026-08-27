// Manual bridge between the local CIRCUIT_TRACKS ids (GeoJSON dataset,
// bacinger/f1-circuits) and OpenF1's circuitName field. The two
// datasets don't share a key, and OpenF1 is inconsistent about whether
// circuitName is the track name ("Monza", "Silverstone") or the host
// city ("Monte Carlo", "Imola", "Spielberg") — no normalization rule
// covers both cases reliably, hence an explicit table instead of fuzzy
// string matching.
//
// Only covers tracks currently on the F1 calendar (paired against a
// real OpenF1 circuitName). Historic/future-only tracks in
// CIRCUIT_TRACKS (e.g. old Hockenheim, Estoril, Kyalami) are
// deliberately left unmapped — matchedRace() correctly returns null
// for them until/unless OpenF1 ever has data for that era.
export const CIRCUIT_TO_OPENF1_NAME: Record<string, string> = {
    'ae-2009': 'Yas Marina Circuit',
    'au-1953': 'Melbourne',
    'at-1969': 'Spielberg',
    'az-2016': 'Baku',
    'be-1925': 'Spa-Francorchamps',
    'bh-2002': 'Sakhir',
    'br-1940': 'Interlagos',
    'ca-1978': 'Montreal',
    'cn-2004': 'Shanghai',
    'es-1991': 'Catalunya',
    'gb-1948': 'Silverstone',
    'hu-1986': 'Hungaroring',
    'it-1922': 'Monza',
    'it-1953': 'Imola',
    'jp-1962': 'Suzuka',
    'mc-1929': 'Monte Carlo',
    'mx-1962': 'Mexico City',
    'nl-1948': 'Zandvoort',
    'qa-2004': 'Lusail',
    'sa-2021': 'Jeddah',
    'sg-2008': 'Singapore',
    'us-2012': 'Austin',
    'us-2022': 'Miami',
    'us-2023': 'Las Vegas'
};
-- V2__openf1_sync_fields.sql
-- Adds the external key OpenF1 uses to identify a circuit, so re-running
-- a sync updates existing rows instead of creating duplicates. Also
-- relaxes total_laps to be nullable: OpenF1 doesn't expose a race's lap
-- count before it happens, so it starts null and gets filled in once
-- known (or curated manually) rather than blocking the sync entirely.

ALTER TABLE circuits ADD COLUMN external_circuit_key INTEGER UNIQUE;
ALTER TABLE circuits ALTER COLUMN total_laps DROP NOT NULL;
ALTER TABLE teams ADD CONSTRAINT uq_teams_name UNIQUE (name);
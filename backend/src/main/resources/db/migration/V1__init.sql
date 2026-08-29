-- V1__init.sql
-- Initial schema for F1 Strategy Simulator, matching the JPA entities
-- in com.f1sim.entity. Column names/types mirror the @Column
-- annotations exactly, since ddl-auto is set to `validate`.

CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       avg_pit_stop_seconds DOUBLE PRECISION
);

CREATE TABLE drivers (
                         id BIGSERIAL PRIMARY KEY,
                         full_name VARCHAR(255) NOT NULL,
                         driver_code VARCHAR(10) NOT NULL UNIQUE,
                         permanent_number INTEGER,
                         team_id BIGINT REFERENCES teams(id)
);

CREATE TABLE circuits (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          country VARCHAR(255) NOT NULL,
                          total_laps INTEGER NOT NULL,
                          lap_length_km DOUBLE PRECISION NOT NULL,
                          pit_lane_time_loss_seconds DOUBLE PRECISION NOT NULL,
                          overtaking_difficulty INTEGER
);

CREATE TABLE races (
                       id BIGSERIAL PRIMARY KEY,
                       grand_prix_name VARCHAR(255) NOT NULL,
                       season INTEGER NOT NULL,
                       circuit_id BIGINT NOT NULL REFERENCES circuits(id),
                       race_datetime TIMESTAMP NOT NULL,
                       external_session_key VARCHAR(255),
                       status VARCHAR(20) NOT NULL
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(30) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       rating_score DOUBLE PRECISION NOT NULL DEFAULT 1000.0
);

CREATE TABLE strategy_simulations (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_id BIGINT NOT NULL REFERENCES users(id),
                                      race_id BIGINT NOT NULL REFERENCES races(id),
                                      driver_id BIGINT NOT NULL REFERENCES drivers(id),
                                      predicted_total_time_seconds DOUBLE PRECISION,
                                      delta_vs_actual_seconds DOUBLE PRECISION,
                                      created_at TIMESTAMP NOT NULL
);

CREATE TABLE tyre_stints (
                             id BIGSERIAL PRIMARY KEY,
                             simulation_id BIGINT NOT NULL REFERENCES strategy_simulations(id) ON DELETE CASCADE,
                             compound VARCHAR(20) NOT NULL,
                             stint_order INTEGER NOT NULL,
                             start_lap INTEGER NOT NULL,
                             end_lap INTEGER NOT NULL
);

-- Indexes for the lookups the app actually performs
CREATE INDEX idx_drivers_team_id ON drivers(team_id);
CREATE INDEX idx_races_circuit_id ON races(circuit_id);
CREATE INDEX idx_simulations_user_id ON strategy_simulations(user_id);
CREATE INDEX idx_stints_simulation_id ON tyre_stints(simulation_id);
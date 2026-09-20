CREATE TABLE race_incidents (
                                id BIGSERIAL PRIMARY KEY,
                                race_id BIGINT NOT NULL REFERENCES races(id),
                                type VARCHAR(30) NOT NULL,
                                start_lap INTEGER NOT NULL,
                                end_lap INTEGER,
                                time_loss_seconds DOUBLE PRECISION
);

CREATE TABLE race_weather_windows (
                                      id BIGSERIAL PRIMARY KEY,
                                      race_id BIGINT NOT NULL REFERENCES races(id),
                                      start_lap INTEGER NOT NULL,
                                      end_lap INTEGER NOT NULL
);

CREATE INDEX idx_race_incidents_race_id ON race_incidents(race_id);
CREATE INDEX idx_race_weather_windows_race_id ON race_weather_windows(race_id);
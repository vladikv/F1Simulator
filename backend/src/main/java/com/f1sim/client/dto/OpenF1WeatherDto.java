package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Maps GET /v1/weather — one weather sample, roughly every minute during a session. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1WeatherDto(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("date") String date,
        @JsonProperty("rainfall") Integer rainfall, // 0 = dry, 1 = rain
        @JsonProperty("track_temperature") Double trackTemperature,
        @JsonProperty("air_temperature") Double airTemperature
) {}
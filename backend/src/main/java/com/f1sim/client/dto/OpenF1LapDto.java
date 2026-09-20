package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Maps GET /v1/laps — used to translate timestamps to lap numbers and estimate clean race pace. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1LapDto(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("lap_number") Integer lapNumber,
        @JsonProperty("date_start") String dateStart,
        @JsonProperty("lap_duration") Double lapDurationSeconds
) {}
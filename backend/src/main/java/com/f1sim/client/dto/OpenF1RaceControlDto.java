package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Maps GET /v1/race_control — flags, safety cars, and other race control messages. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1RaceControlDto(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("date") String date,
        @JsonProperty("lap_number") Integer lapNumber,
        @JsonProperty("category") String category,
        @JsonProperty("flag") String flag,
        @JsonProperty("scope") String scope,
        @JsonProperty("message") String message
) {}
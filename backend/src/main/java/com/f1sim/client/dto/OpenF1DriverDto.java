package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Maps GET /v1/drivers — a driver's entry in a specific session. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1DriverDto(
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("name_acronym") String nameAcronym,
        @JsonProperty("team_name") String teamName,
        @JsonProperty("session_key") Integer sessionKey
) {}
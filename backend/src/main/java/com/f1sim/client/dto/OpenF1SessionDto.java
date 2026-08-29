package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Maps GET /v1/sessions — one session within a meeting (Practice, Quali, Race...). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1SessionDto(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("meeting_key") Integer meetingKey,
        @JsonProperty("session_name") String sessionName,
        @JsonProperty("session_type") String sessionType,
        @JsonProperty("date_start") String dateStart,
        @JsonProperty("circuit_key") Integer circuitKey,
        @JsonProperty("circuit_short_name") String circuitShortName,
        @JsonProperty("location") String location,
        @JsonProperty("country_name") String countryName,
        @JsonProperty("year") Integer year
) {}
package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Maps GET /v1/meetings — one race weekend (a Grand Prix). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1MeetingDto(
        @JsonProperty("meeting_key") Integer meetingKey,
        @JsonProperty("meeting_name") String meetingName,
        @JsonProperty("meeting_official_name") String meetingOfficialName,
        @JsonProperty("location") String location,
        @JsonProperty("country_name") String countryName,
        @JsonProperty("circuit_key") Integer circuitKey,
        @JsonProperty("circuit_short_name") String circuitShortName,
        @JsonProperty("date_start") String dateStart,
        @JsonProperty("year") Integer year
) {}
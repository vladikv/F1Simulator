package com.f1sim.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maps GET /v1/session_result — final classification for a session.
 * Only the race winner (position 1) gets a populated `duration`; every
 * other driver's actual race time has to be derived as
 * winnerDuration + gap_to_leader. gap_to_leader is JSON-heterogeneous —
 * a number of seconds for drivers on the lead lap, but a string like
 * "+1 LAP" for lapped drivers — hence JsonNode instead of Double, so
 * deserialization doesn't blow up on the string case.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1SessionResultDto(
        @JsonProperty("position") Integer position,
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("points") Double points,
        @JsonProperty("dnf") Boolean dnf,
        @JsonProperty("dns") Boolean dns,
        @JsonProperty("dsq") Boolean dsq,
        @JsonProperty("duration") Double durationSeconds,
        @JsonProperty("gap_to_leader") JsonNode gapToLeader
) {
    /** True only when gap_to_leader is a plain numeric seconds value (lead-lap finish). */
    public boolean hasNumericGap() {
        return gapToLeader != null && gapToLeader.isNumber();
    }

    public double gapToLeaderSeconds() {
        return gapToLeader.asDouble();
    }
}
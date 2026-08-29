package com.f1sim.client;

import com.f1sim.client.dto.OpenF1DriverDto;
import com.f1sim.client.dto.OpenF1MeetingDto;
import com.f1sim.client.dto.OpenF1SessionDto;
import com.f1sim.client.dto.OpenF1SessionResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenF1Client {

    private final RestClient openF1RestClient;

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1MeetingDto> getMeetings(int year) {
        log.info("Fetching OpenF1 meetings for {}", year);
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/meetings")
                        .queryParam("year", year)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1MeetingDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1SessionDto> getRaceSessions(int meetingKey) {
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/sessions")
                        .queryParam("meeting_key", meetingKey)
                        .queryParam("session_type", "Race")
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1SessionDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1SessionResultDto> getSessionResult(int sessionKey) {
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/session_result")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1SessionResultDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1DriverDto> getDrivers(int sessionKey) {
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/drivers")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1DriverDto>>() {});
    }
}
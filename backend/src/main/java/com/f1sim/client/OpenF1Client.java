package com.f1sim.client;

import com.f1sim.client.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenF1Client {

    private final RestClient openF1RestClient;
    private static final int MAX_REQUESTS_PER_WINDOW = 28;
    private static final long WINDOW_MILLIS = 60_000;

    // Timestamps of recent requests, oldest first — a sliding window rate limiter.
    // Shared across every method on this client, including retries, so the
    // 30 req/min OpenF1 limit can never be exceeded no matter which method fires.
    private final Deque<Instant> requestTimestamps = new ArrayDeque<>();

    private synchronized void awaitRateLimitSlot() {
        Instant now = Instant.now();

        while (!requestTimestamps.isEmpty()
                && now.toEpochMilli() - requestTimestamps.peekFirst().toEpochMilli() > WINDOW_MILLIS) {
            requestTimestamps.pollFirst();
        }

        if (requestTimestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
            long waitMillis = WINDOW_MILLIS - (now.toEpochMilli() - requestTimestamps.peekFirst().toEpochMilli()) + 100;
            log.info("OpenF1 rate limit guard: waiting {}ms before next request", waitMillis);
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        requestTimestamps.addLast(Instant.now());
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1MeetingDto> getMeetings(int year) {
        awaitRateLimitSlot();
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
        awaitRateLimitSlot();
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
        awaitRateLimitSlot();
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/session_result")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1SessionResultDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1DriverDto> getDrivers(int sessionKey) {
        awaitRateLimitSlot();
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/drivers")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1DriverDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1RaceControlDto> getRaceControl(int sessionKey) {
        awaitRateLimitSlot();
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/race_control")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1RaceControlDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1WeatherDto> getWeather(int sessionKey) {
        awaitRateLimitSlot();
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/weather")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1WeatherDto>>() {});
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<OpenF1LapDto> getLaps(int sessionKey, int driverNumber) {
        awaitRateLimitSlot();
        return openF1RestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/laps")
                        .queryParam("session_key", sessionKey)
                        .queryParam("driver_number", driverNumber)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OpenF1LapDto>>() {});
    }
}
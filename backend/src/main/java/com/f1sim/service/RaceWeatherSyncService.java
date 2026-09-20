package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1LapDto;
import com.f1sim.client.dto.OpenF1WeatherDto;
import com.f1sim.entity.Race;
import com.f1sim.entity.RaceWeatherWindow;
import com.f1sim.repository.RaceWeatherWindowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns OpenF1's per-minute weather samples into contiguous lap-number
 * windows of rain, so the strategy engine can penalize dry tyres in
 * the wet and vice versa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceWeatherSyncService {

    private final OpenF1Client openF1Client;
    private final RaceWeatherWindowRepository weatherWindowRepository;
    private final SessionLapTimelineService timeline;

    public void sync(Race race, List<OpenF1WeatherDto> samples, List<OpenF1LapDto> referenceLaps) {
        List<RaceWeatherWindow> windows = new ArrayList<>();
        Integer wetStartLap = null;
        Integer lastLapSeen = null;

        for (OpenF1WeatherDto sample : samples) {
            Integer lap = timeline.lapNumberAt(referenceLaps, Instant.parse(sample.date()));
            if (lap == null) continue;

            boolean isRaining = sample.rainfall() == 1;

            if (isRaining && wetStartLap == null) {
                wetStartLap = lap;
            } else if (!isRaining && wetStartLap != null) {
                windows.add(closeWindow(race, wetStartLap, lastLapSeen != null ? lastLapSeen : lap));
                wetStartLap = null;
            }
            lastLapSeen = lap;
        }

        if (wetStartLap != null && lastLapSeen != null) {
            windows.add(closeWindow(race, wetStartLap, lastLapSeen));
        }

        weatherWindowRepository.saveAll(windows);
        log.info("Race {}: synced {} rain window(s)", race.getId(), windows.size());
    }

    private RaceWeatherWindow closeWindow(Race race, int startLap, int endLap) {
        return RaceWeatherWindow.builder()
                .race(race)
                .startLap(startLap)
                .endLap(Math.max(startLap, endLap))
                .build();
    }
}
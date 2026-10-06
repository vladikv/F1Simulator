package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1LapDto;
import com.f1sim.client.dto.OpenF1WeatherDto;
import com.f1sim.entity.Race;
import com.f1sim.entity.RaceWeatherWindow;
import com.f1sim.repository.RaceWeatherWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceWeatherSyncServiceTest {

    @Mock
    private OpenF1Client openF1Client;

    @Mock
    private RaceWeatherWindowRepository weatherWindowRepository;

    private SessionLapTimelineService timeline;
    private RaceWeatherSyncService service;
    private Race race;

    @BeforeEach
    void setUp() {
        // Using the REAL SessionLapTimelineService here, not a mock — it's
        // pure logic (no network/DB), so it's simpler to just run it for real
        // and feed it realistic lap data, rather than mocking its output.
        timeline = new SessionLapTimelineService(openF1Client);
        service = new RaceWeatherSyncService(openF1Client, weatherWindowRepository, timeline);
        race = Race.builder().id(25L).build();
    }

    private OpenF1LapDto lap(int number, String dateStart) {
        return new OpenF1LapDto(9523, 1, number, dateStart, 90.0);
    }

    @Test
    @DisplayName("Detects a single contiguous rain window spanning several laps")
    void detectsSingleRainWindow() {
        List<OpenF1LapDto> laps = List.of(
                lap(1, "2024-05-26T13:00:00Z"),
                lap(2, "2024-05-26T13:01:30Z"),
                lap(3, "2024-05-26T13:03:00Z"),
                lap(4, "2024-05-26T13:04:30Z")
        );

        List<OpenF1WeatherDto> samples = List.of(
                new OpenF1WeatherDto(9523, "2024-05-26T13:00:00Z", 0, 35.0, 22.0),
                new OpenF1WeatherDto(9523, "2024-05-26T13:01:30Z", 1, 33.0, 20.0),
                new OpenF1WeatherDto(9523, "2024-05-26T13:03:00Z", 1, 32.0, 19.0),
                new OpenF1WeatherDto(9523, "2024-05-26T13:04:30Z", 0, 34.0, 21.0)
        );

        service.sync(race, samples, laps);

        ArgumentCaptor<List<RaceWeatherWindow>> captor = ArgumentCaptor.forClass(List.class);
        verify(weatherWindowRepository).saveAll(captor.capture());

        List<RaceWeatherWindow> windows = captor.getValue();
        assertEquals(1, windows.size());
        assertEquals(2, windows.get(0).getStartLap());
        assertEquals(3, windows.get(0).getEndLap());
    }

    @Test
    @DisplayName("Rain that never stops is closed at the last known lap")
    void rainContinuingToEndOfSamplesClosesAtLastLap() {
        List<OpenF1LapDto> laps = List.of(
                lap(1, "2024-05-26T13:00:00Z"),
                lap(2, "2024-05-26T13:01:30Z")
        );

        List<OpenF1WeatherDto> samples = List.of(
                new OpenF1WeatherDto(9523, "2024-05-26T13:00:00Z", 0, 35.0, 22.0),
                new OpenF1WeatherDto(9523, "2024-05-26T13:01:30Z", 1, 33.0, 20.0)
        );

        service.sync(race, samples, laps);

        ArgumentCaptor<List<RaceWeatherWindow>> captor = ArgumentCaptor.forClass(List.class);
        verify(weatherWindowRepository).saveAll(captor.capture());

        List<RaceWeatherWindow> windows = captor.getValue();
        assertEquals(1, windows.size());
        assertEquals(2, windows.get(0).getStartLap());
        assertEquals(2, windows.get(0).getEndLap());
    }

    @Test
    @DisplayName("No rain in any sample means no windows are saved")
    void noRainMeansNoWindows() {
        List<OpenF1LapDto> laps = List.of(lap(1, "2024-05-26T13:00:00Z"));
        List<OpenF1WeatherDto> samples = List.of(
                new OpenF1WeatherDto(9523, "2024-05-26T13:00:00Z", 0, 35.0, 22.0)
        );

        service.sync(race, samples, laps);

        ArgumentCaptor<List<RaceWeatherWindow>> captor = ArgumentCaptor.forClass(List.class);
        verify(weatherWindowRepository).saveAll(captor.capture());

        assertTrue(captor.getValue().isEmpty());
    }
}